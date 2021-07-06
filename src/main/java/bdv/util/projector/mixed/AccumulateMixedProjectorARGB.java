/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.util.projector.mixed;

import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * BDV Projector which allows some flexibility in the way sources are combined
 * when displayed in a BDV window.
 *
 * {@link SourceAndConverter} have associated Metadata which specifies how they
 * should be displayed.
 *
 * Their metadata are accessed through {@link sc.fiji.bdvpg.scijava.services.SourceAndConverterService#getMetadata(SourceAndConverter, String)}
 * where the key is {@link BlendingMode#BLENDING_MODE}
 *
 * The final pixel value is the result of the sum and/or average of sources based
 * on their metadata + an occluding layer can be used to cover completely some sources
 *
 * By looking if the alpha channel is 0 or not, sources are ignored or not in the pixel computation
 * with the source present at every pixel. To rephrase : if the alpha channel of a source
 * at a specific position is 0, it is ignored in the computation of this pixel.
 *
 * @author Christian Tischer, EMBL
 */

public class AccumulateMixedProjectorARGB extends AccumulateProjector< ARGBType, ARGBType >
{
	private final BlendingMode[] blendingModes;
	private static boolean containsOccludingSources = false;
	private static ArrayList< Integer > occluding;

	public AccumulateMixedProjectorARGB(
			final List< VolatileProjector > sourceProjectors,
			final List< SourceAndConverter< ? > > sources,
			final List< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
			final RandomAccessibleInterval< ARGBType > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		super( sourceProjectors, sourceScreenImages, target, numThreads, executorService );
		blendingModes = getBlendingModes( sources );
		initOccludingBlendingModes( blendingModes );
	}

	public static BlendingMode[] getBlendingModes( List< SourceAndConverter<?> > sources )
	{
		final ISourceAndConverterService sacService = SourceAndConverterServices.getSourceAndConverterService();

		return sources.stream()
				.map(sac -> sacService.getMetadata( sac, BlendingMode.BLENDING_MODE ) )
				.map(it -> it==null ? BlendingMode.Sum:it)
				.toArray( BlendingMode[]::new );
	}

	private static void initOccludingBlendingModes( BlendingMode[] blendingModes )
	{
		occluding = new ArrayList();

		for ( int i = 0; i < blendingModes.length; i++ )
		{
			if ( BlendingMode.isOccluding( blendingModes[ i ] ) )
			{
				occluding.add( i );
				containsOccludingSources = true;
			}
		}
	}

	public BlendingMode[] getBlendingModes()
	{
		return blendingModes;
	}

	public static boolean isContainsOccludingSources()
	{
		return containsOccludingSources;
	}

	@Override
	protected void accumulate(
			final Cursor< ? extends ARGBType >[] accesses,
			final ARGBType target )
	{
		final int argbIndex = getArgbIndex( accesses, blendingModes );
		target.set( argbIndex );
	}

	public static int getArgbIndex( Cursor< ? extends ARGBType >[] accesses, BlendingMode[] blendingModes )
	{
		int aAvg = 0, rAvg = 0, gAvg = 0, bAvg = 0, numAvg = 0;
		int aAccu = 0, rAccu = 0, gAccu = 0, bAccu = 0;
		int occludingAlpha = 0;

		int[] argbs = new int[ accesses.length ];

		for ( int sourceIndex = 0; sourceIndex < accesses.length; sourceIndex++ )
		{
			argbs[ sourceIndex ] = accesses[ sourceIndex ].get().get();
		}

		for ( Integer occludingSourceIndex : occluding )
		{
			occludingAlpha += ARGBType.alpha( argbs[ occludingSourceIndex ] );
		}

		for ( int sourceIndex = 0; sourceIndex < accesses.length; sourceIndex++ )
		{
			final BlendingMode blendingMode = blendingModes[ sourceIndex ];

			if ( ! BlendingMode.isOccluding( blendingMode ) && occludingAlpha > 0 )
			{
				// The source itself is not occluding
				// and there are occluding sources with an alpha > 0
				// Thus, do not paint this source
				continue;
			}

			final int argb = argbs[ sourceIndex ];

			final int a = ARGBType.alpha( argb );

			if ( a == 0 ) continue;

			final int r = ARGBType.red( argb );
			final int g = ARGBType.green( argb );
			final int b = ARGBType.blue( argb );

			if ( blendingMode.equals( BlendingMode.Sum ) || blendingMode.equals( BlendingMode.SumOccluding ) )
			{
				aAccu += a; // does this make sense??
				rAccu += r;
				gAccu += g;
				bAccu += b;
			}
			else if ( blendingMode.equals( BlendingMode.Average ) || blendingMode.equals( BlendingMode.AverageOccluding ) )
			{
				aAvg += a; // does this make sense??
				rAvg += r;
				gAvg += g;
				bAvg += b;
				numAvg++;
			}
		}

		if ( numAvg > 1 )
		{
			aAvg /= numAvg;
			rAvg /= numAvg;
			gAvg /= numAvg;
			bAvg /= numAvg;
		}

		aAccu += aAvg;
		rAccu += rAvg;
		gAccu += gAvg;
		bAccu += bAvg;

		if ( aAccu > 255 )
			aAccu = 255;
		if ( rAccu > 255 )
			rAccu = 255;
		if ( gAccu > 255 )
			gAccu = 255;
		if ( bAccu > 255 )
			bAccu = 255;

		return ARGBType.rgba( rAccu, gAccu, bAccu, aAccu );
	}
}
