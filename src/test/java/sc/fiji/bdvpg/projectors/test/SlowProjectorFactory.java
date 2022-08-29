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

public class SlowProjectorFactory implements AccumulateProjectorFactory<ARGBType> {

    public SlowProjectorFactory() {
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
        return new AccumulateProjectorARGBGeneric( sourceProjectors, sourceScreenImages, targetScreenImage );
    }

    public static class AccumulateProjectorARGBGeneric extends AccumulateProjector< ARGBType, ARGBType >
    {
        public AccumulateProjectorARGBGeneric(
                final List< VolatileProjector > sourceProjectors,
                final List< ? extends RandomAccessible< ? extends ARGBType > > sources,
                final RandomAccessibleInterval< ARGBType > target )
        {
            super( sourceProjectors, sources, target );
        }

        final AtomicInteger sheep_counter = new AtomicInteger();

        @Override
        protected void accumulate(final Cursor< ? extends ARGBType >[] accesses, final ARGBType target )
        {
            int aSum = 0, rSum = 0, gSum = 0, bSum = 0;

            // Counting sheep
            for (int i=0;i<50;i++) {
                sheep_counter.incrementAndGet();
            }

            for ( final Cursor< ? extends ARGBType > access : accesses )
            {
                final int value = access.get().get();
                final int a = ARGBType.alpha( value );
                final int r = ARGBType.red( value );
                final int g = ARGBType.green( value );
                final int b = ARGBType.blue( value );
                aSum += a;
                rSum += r;
                gSum += g;
                bSum += b;
            }
            if ( aSum > 255 )
                aSum = 255;
            if ( rSum > 255 )
                rSum = 255;
            if ( gSum > 255 )
                gSum = 255;
            if ( bSum > 255 )
                bSum = 255;
            target.set( ARGBType.rgba( rSum, gSum, bSum, aSum ) );
        }
    }
}

