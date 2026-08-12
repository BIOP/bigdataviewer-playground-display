package sc.fiji.bdvpg.bindings;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.bdv.supplier.biop.BiopBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.biop.BiopSerializableBdvOptions;
import sc.fiji.bdvpg.dataset.importer.XMLToDatasetImporter;
import sc.fiji.bdvpg.service.SourceServices;
import sc.fiji.bdvpg.source.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.viewer.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.viewer.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.viewer.bdv.overlay.AxesOverlay;

import java.util.List;

/**
 * Opens a BIOP window with the axes gizmo, to try the navigation of the BIOP
 * keymap by hand.
 * <p>
 * What to check, in the window that opens:
 * <ul>
 * <li>anywhere in the image: left drag and right drag pan, middle drag and
 * shift + left drag rotate, scroll zooms, shift + scroll moves through Z,</li>
 * <li>over the gizmo, the circle which lights up in the top right corner: left
 * drag rotates instead of panning, and a left click on one of the axis circles
 * still animates the view to that plane,</li>
 * <li>over the gizmo, scrolling still zooms and shift + scrolling still moves
 * through Z: the gizmo only takes over the plain left button,</li>
 * <li>in the preferences dialog, File &gt; Preferences or {@code ctrl COMMA},
 * the keymap page offers "BIOP" besides "Default", "BIOP" is the selected one,
 * and the table lists the bindings above. Pressing OK and reopening the dialog
 * has to leave "BIOP" selected: it used to fall back to "Default" and save
 * that.</li>
 * </ul>
 */
public class AxesOverlayRotationDemo {

	public static void main(String[] args) {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		SourceServices.getBdvDisplayService().setDefaultBdvSupplier(
			new BiopBdvSupplier(BiopSerializableBdvOptions.options()));

		final BdvHandle bdv = SourceServices.getBdvDisplayService().getNewBdv();

		new XMLToDatasetImporter("src/test/resources/mri-stack.xml").run();

		final List<SourceAndConverter<?>> sources = SourceServices
			.getSourceService().getSources();
		sources.forEach(source -> {
			SourceServices.getBdvDisplayService().show(bdv, source);
			new ViewerTransformAdjuster(bdv, source).run();
			new BrightnessAutoAdjuster<>(source, 0).run();
		});

		BdvHandleHelper.addOverlay(bdv, new AxesOverlay(bdv), "axes_overlay");
	}

}