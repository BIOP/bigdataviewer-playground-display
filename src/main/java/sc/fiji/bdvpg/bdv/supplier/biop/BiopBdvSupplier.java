
package sc.fiji.bdvpg.bdv.supplier.biop;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlaySource;
import bdv.util.BdvStackSource;
import bdv.viewer.Interpolation;
import bdv.viewer.ViewerStateChange;
import bdv.viewer.ViewerStateChangeListener;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.ByteType;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.RayCastPositionerSliderAdder;
import sc.fiji.bdvpg.bdv.navigate.SourceNavigatorSliderAdder;
import sc.fiji.bdvpg.bdv.navigate.TimepointAdapterAdder;
import sc.fiji.bdvpg.bdv.overlay.SourceNameOverlay;
import sc.fiji.bdvpg.bdv.overlay.SourceNameOverlayAdder;
import sc.fiji.bdvpg.bdv.supplier.BdvSupplierHelper;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.scijava.command.bdv.MultiBdvZSliderAdderCommand;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.*;
import java.awt.Font;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BiopBdvSupplier implements IBdvSupplier {

	public final BiopSerializableBdvOptions sOptions;

	public BiopBdvSupplier(BiopSerializableBdvOptions sOptions) {
		this.sOptions = sOptions;
	}

	public BiopBdvSupplier() {
		this.sOptions = BiopSerializableBdvOptions.options();
	}

	@Override
	public BdvHandle get() {
		BdvOptions options = sOptions.getBdvOptions();

		// create dummy image to instantiate the BDV
		ArrayImg<ByteType, ByteArray> dummyImg = ArrayImgs.bytes(2, 2, 2);
		options = options.sourceTransform(new AffineTransform3D());
		BdvStackSource<ByteType> bss = BdvFunctions.show(dummyImg, "dummy",
			options);
		BdvHandle bdvh = bss.getBdvHandle();

		if (sOptions.interpolate) bdvh.getViewerPanel().setInterpolation(
			Interpolation.NLINEAR);

		// remove dummy image
		bdvh.getViewerPanel().state().removeSource(bdvh.getViewerPanel().state()
			.getCurrentSource());
		bdvh.getViewerPanel().setNumTimepoints(sOptions.numTimePoints);

		BdvSupplierHelper.addSourcesDragAndDrop(bdvh);

		SourceSelectorBehaviour ssb = BdvSupplierHelper.addEditorMode(bdvh, "");
		bdvh.getSplitPanel().setCollapsed(false);

		JPanel editorModeToggle = new JPanel();
		JButton editorToggle = new JButton("Editor Mode");
		editorToggle.addActionListener((e) -> {
			if (ssb.isEnabled()) {
				ssb.disable();
				editorToggle.setText("Editor Mode 'E'");
			}
			else {
				ssb.enable();
				editorToggle.setText("Navigation Mode 'E'");
			}
		});

		editorModeToggle.add(editorToggle);

		JButton nameToggle = new JButton("Display sources name");
		AtomicBoolean nameOverlayEnabled = new AtomicBoolean();
		nameOverlayEnabled.set(true);

		SourceNameOverlayAdder nameOverlayAdder = new SourceNameOverlayAdder(bdvh, new Font(sOptions.font, Font.PLAIN, sOptions.fontSize));

		nameToggle.addActionListener((e) -> {
			if (nameOverlayEnabled.get()) {
				nameOverlayEnabled.set(false);
				nameToggle.setText("Display sources names");
				nameOverlayAdder.removeFromBdv();

			}
			else {
				nameOverlayEnabled.set(true);
				nameToggle.setText("Hide sources name");
				nameOverlayAdder.addToBdv();
			}
		});

		editorModeToggle.add(nameToggle);

		SwingUtilities.invokeLater(() -> {
			nameOverlayAdder.run();
			BdvHandleHelper.addCenterCross(bdvh);
			new RayCastPositionerSliderAdder(bdvh).run();
			new SourceNavigatorSliderAdder(bdvh).run();
			new TimepointAdapterAdder(bdvh).run();
		});
		//SwingUtilities.invokeLater(() -> BdvHandleHelper.addCenterCross(bdvh));
		//SwingUtilities.invokeLater(() -> new RayCastPositionerSliderAdder(bdvh).run());
		BdvHandleHelper.addCard(bdvh, "Mode", editorModeToggle, true);

		return bdvh;
	}

}
