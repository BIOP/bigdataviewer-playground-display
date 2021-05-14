package sc.fiji.bdvpg.projectors.bdvsupplierdemo;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import org.junit.After;
import org.junit.Test;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;

public class BdvSupplierExampleDemo {
    static ImageJ ij;

    public static void main( String[] args )
    {
        ij = new ImageJ();
        ij.ui().showUI();


        IBdvSupplier bdvSupplier = new BdvSupplierExample();

        SourceAndConverterServices.getBdvDisplayService().setDefaultBdvSupplier(bdvSupplier);

        BdvHandle bdv = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        // Import SpimData
        new SpimDataFromXmlImporter( "src/test/resources/mri-stack.xml" ).run();
        new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml").run();
        new SpimDataFromXmlImporter( "src/test/resources/mri-stack-shiftedY.xml" ).run();

        // Get a handle on the sacs
        final List<SourceAndConverter> sacs = SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters();

        // Show all three sacs
        sacs.forEach( sac -> {
            SourceAndConverterServices.getBdvDisplayService().show(bdv, sac);
            new ViewerTransformAdjuster(bdv, sac).run();
            new BrightnessAutoAdjuster(sac, 0).run();
        });

        // Change color of third one
        new ColorChanger( sacs.get( 2 ), new ARGBType( ARGBType.rgba( 0, 255, 0, 255 ) ) ).run();

    }

    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }

    @After
    public void closeFiji() {
        TestHelper.closeFijiAndBdvs(ij);
    }
}
