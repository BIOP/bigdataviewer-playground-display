package sc.fiji.bdvpg.projectors.test;

import bdv.util.*;
import bdv.viewer.render.AccumulateProjectorARGB;
import bdv.viewer.render.AccumulateProjectorFactory;
import net.imglib2.type.numeric.ARGBType;

import java.util.function.Function;

/**
 * Performance tests for various bigdataviewer sc.fiji.bdvpg.projectors and datasets
 *
 * Tests :
 * Images:
 * - 1 image
 * - 2 images
 * - 25 images
 *
 * Projector:
 * - default sum projector
 * - a slow one (made slow on purpose)
 * - one which ignores all sources except one
 * - one which ignores all sources and display black
 *
 * Weirdly, the last two sc.fiji.bdvpg.projectors which are ignoring sources are not much faster than those who display everything
 *
 *
 * @author Nicolas Chiaruttini, EPFL, 2021
 */
public class PerfTests {

    /**
     * Do you want to measure the perf ? Meaning rendering a few images offscreen and measuring the time it takes
     */
    final public static boolean testPerf = true;

    /**
     * Close the bdv frame after testing ?
     */
    final public static boolean closeAfterTest = true;

    public static void main(final String... args) {

        test(AccumulateProjectorARGB.factory, BdvSampleDatasets::oneImage);
        test(new SlowProjectorFactory(), BdvSampleDatasets::oneImage);
        test(new TakeFirstProjectorFactory(), BdvSampleDatasets::oneImage);
        test(new BlackProjectorFactory(), BdvSampleDatasets::oneImage);

        test(AccumulateProjectorARGB.factory, BdvSampleDatasets::twoImages);
        test(new SlowProjectorFactory(), BdvSampleDatasets::twoImages);
        test(new TakeFirstProjectorFactory(), BdvSampleDatasets::twoImages);
        test(new BlackProjectorFactory(), BdvSampleDatasets::twoImages);

        test(AccumulateProjectorARGB.factory, BdvSampleDatasets::twentyFiveImages);
        test(new SlowProjectorFactory(), BdvSampleDatasets::twentyFiveImages);
        test(new TakeFirstProjectorFactory(), BdvSampleDatasets::twentyFiveImages); // Surprisingly slow : the projector does not care so much about how many sources it projects
        test(new BlackProjectorFactory(), BdvSampleDatasets::twentyFiveImages); // Surprisingly slow : the projector does not care so much about how many sources it projects

    }

    static public void test(AccumulateProjectorFactory<ARGBType> accumulator, Function<BdvOptions, BdvHandle> dataprovider) {
        BdvOptions options = BdvOptions.options().accumulateProjectorFactory(accumulator);
        BdvHandle bdvh = dataprovider.apply(options);
        if (testPerf) System.out.print(BdvProbeFPS.getStdMsPerFrame(bdvh)+" ms per frame");
        if (closeAfterTest) bdvh.close();
    }


}
