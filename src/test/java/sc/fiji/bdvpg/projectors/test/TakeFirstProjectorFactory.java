package sc.fiji.bdvpg.projectors.test;

import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class TakeFirstProjectorFactory implements AccumulateProjectorFactory<ARGBType> {

    public TakeFirstProjectorFactory() {
        System.out.print(this.getClass()+"\t");
    }

    public VolatileProjector createProjector(
            final List< VolatileProjector > sourceProjectors,
            final List<SourceAndConverter< ? >> sources,
            final List<? extends RandomAccessible<? extends ARGBType>> sourceScreenImages,
            final RandomAccessibleInterval<ARGBType> targetScreenImage,
            final int numThreads,
            final ExecutorService executorService )
    {
        return new AccumulateProjectorARGBGeneric( sourceProjectors, sourceScreenImages, targetScreenImage, numThreads, executorService );
    }

    public static class AccumulateProjectorARGBGeneric extends AccumulateProjector< ARGBType, ARGBType >
    {
        public AccumulateProjectorARGBGeneric(
                final List< VolatileProjector > sourceProjectors,
                final List< ? extends RandomAccessible< ? extends ARGBType > > sources,
                final RandomAccessibleInterval< ARGBType > target,
                final int numThreads,
                final ExecutorService executorService )
        {
            super( sourceProjectors, sources, target, numThreads, executorService );
        }

        @Override
        protected void accumulate(final Cursor< ? extends ARGBType >[] accesses, final ARGBType target )
        {

            final int value = accesses[0].get().get();
            final int a = ARGBType.alpha( value );
            final int r = ARGBType.red( value );
            final int g = ARGBType.green( value );
            final int b = ARGBType.blue( value );

            target.set( ARGBType.rgba( r, g, b, a ) );
        }
    }
}

