package sc.fiji.bdvpg.projectors.test;

import bdv.util.*;
import sc.fiji.bdvpg.projectors.test.BlackProjectorFactory;
import bdv.viewer.render.AccumulateProjectorFactory;
import net.imglib2.type.numeric.ARGBType;

import java.util.function.Function;

public class WeirdIssueAccumulatorIgnored {

    public static void main(final String... args) {
        test(new BlackProjectorFactory(), BdvSampleDatasets::oneImage); // We got an image!!!
        // This is because no projector is used when a single image is displayed...
        test(new BlackProjectorFactory(), BdvSampleDatasets::twoImages); // No image, as expected
    }

    static public void test(AccumulateProjectorFactory<ARGBType> accumulator, Function<BdvOptions, BdvHandle> dataprovider) {
        BdvOptions options = BdvOptions.options().accumulateProjectorFactory(accumulator);
        BdvHandle bdvh = dataprovider.apply(options);
    }
}
