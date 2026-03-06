
package sc.fiji.bdvpg.projectors.bdvsupplierdemo;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import org.junit.After;
import org.junit.Test;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.viewer.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.viewer.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.biop.BiopBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.biop.BiopSerializableBdvOptions;
import sc.fiji.bdvpg.service.SourceServices;
import sc.fiji.bdvpg.source.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.source.display.ColorChanger;
import sc.fiji.bdvpg.dataset.importer.XMLToDatasetImporter;

import java.util.List;

public class BdvSupplierExampleDemo {

	static ImageJ ij;

	public static void main(String[] args) {
		ij = new ImageJ();
		ij.ui().showUI();

		IBdvSupplier bdvSupplier = new BiopBdvSupplier(BiopSerializableBdvOptions.options());//BdvSupplierExample();

		SourceServices.getBdvDisplayService().setDefaultBdvSupplier(
			bdvSupplier);

		BdvHandle bdv = SourceServices.getBdvDisplayService()
			.getNewBdv();

		// Import SpimData
		new XMLToDatasetImporter("src/test/resources/mri-stack.xml").run();
		new XMLToDatasetImporter("src/test/resources/mri-stack-shiftedX.xml")
			.run();
		new XMLToDatasetImporter("src/test/resources/mri-stack-shiftedY.xml")
			.run();

		// Get a handle on the sources
		final List<SourceAndConverter<?>> sources = SourceServices
			.getSourceService().getSources();

		// Show all three sources
		sources.forEach(source -> {
			SourceServices.getBdvDisplayService().show(bdv, source);
			new ViewerTransformAdjuster(bdv, source).run();
			new BrightnessAutoAdjuster<>(source, 0).run();
		});

		// Change color of third one
		new ColorChanger(sources.get(2), new ARGBType(ARGBType.rgba(0, 255, 0, 255)))
			.run();

	}

	@Test
	public void demoRunOk() {
		main(new String[] { "" });
	}

	@After
	public void closeFiji() {
		TestHelper.closeFijiAndBdvs(ij);
	}
}
