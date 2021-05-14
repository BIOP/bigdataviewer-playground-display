package sc.fiji.bdvpg.projectors.test;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.AccumulateProjectorARGB;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class DefaultProjectorFactory implements AccumulateProjectorFactory<ARGBType> {

    public DefaultProjectorFactory() {
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
        //System.out.println(sourceProjectors.size()+"\t"+sources.size()+"\t"+sourceScreenImages.size());
        try
        {

          // I couldn't find any perf improvement for this one when the two sources are displayed
          return new AccumulateProjectorARGB( sourceProjectors, sourceScreenImages, targetScreenImage, numThreads, executorService );
        }
        catch ( IllegalArgumentException ignored )
        {}
        return new AccumulateProjectorARGB.AccumulateProjectorARGBGeneric( sourceProjectors, sourceScreenImages, targetScreenImage, numThreads, executorService );

    }
}

/*package bdv.viewer.render;

public class AccumulateProjectorARGB implements VolatileProjector*/