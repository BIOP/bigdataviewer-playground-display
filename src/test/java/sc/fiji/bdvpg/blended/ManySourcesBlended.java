package sc.fiji.bdvpg.blended;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SourceGroup;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.After;
import org.junit.Test;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.ArrayList;

public class ManySourcesBlended {

    static ImageJ ij;

    public static void main(String... args) {
        // Initializes static SourceService and Display Service

        ij = new ImageJ();
        ij.ui().showUI();

        demo(20);

        AffineTransform3D location = new AffineTransform3D();
        location.translate(0,0,120);

        SourceAndConverter model = new EmptySourceAndConverterCreator("Model", location, 5000,5000,1).get();

        SourceAndConverterServices.getSourceAndConverterService().register(model);

    }

    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }

    @After
    public void closeFiji() {
        TestHelper.closeFijiAndBdvs(ij);
    }

    public static void demo(int numberOfSourcesInOneAxis) {

        // Creates a BdvHandle
        BdvHandle bdvHandle = SourceAndConverterServices
                .getBdvDisplayService().getActiveBdv();

        final String filePath = "src/test/resources/mri-stack.xml";
        // Import SpimData
        SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter(filePath);
        //importer.run();

        final AbstractSpimData spimData = importer.get();

        SourceAndConverter sac = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(spimData)
                .get(0);

        new ViewerTransformAdjuster(bdvHandle, sac).run();
        new BrightnessAutoAdjuster(sac, 0).run();

        ArrayList<SourceAndConverter<?>> sacs = new ArrayList<>();
        for (int x = 0; x < numberOfSourcesInOneAxis;x++) {
            for (int y = 0; y < numberOfSourcesInOneAxis; y++) {

                if (Math.random()>0.0) {
                    AffineTransform3D at3d = new AffineTransform3D();

                    at3d.rotate(2, Math.random());
                    at3d.scale(0.5 + Math.random() / 4, 0.5 + Math.random() / 4, 1);
                    at3d.translate(200 * x, 200 * y, 0);

                    SourceAffineTransformer sat = new SourceAffineTransformer(sac, at3d);
                    sat.run();

                    SourceAndConverter transformedSac = sat.getSourceOut();

                    sacs.add(transformedSac);
                }
            }
        }

        SourceAndConverterServices
                .getBdvDisplayService()
                .show(bdvHandle, sacs.toArray(new SourceAndConverter[0]));

        SourceGroup sg = bdvHandle.getViewerPanel().state().getGroups().get(1);

        bdvHandle.getViewerPanel().state().addSourcesToGroup(sacs, sg);
    }
}
