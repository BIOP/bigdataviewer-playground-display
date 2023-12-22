
package sc.fiji.bdvpg.projectors.bdvsupplierdemo;

import bdv.ui.BdvDefaultCards;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Interpolation;
import bdv.viewer.ViewerStateChange;
import bdv.viewer.ViewerStateChangeListener;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.type.numeric.integer.ByteType;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;

import javax.swing.*;

public class BdvSupplierExample implements IBdvSupplier {

	public BdvSupplierExample() {
		IJ.log("The Bdv Supplier is created");
		// you could instantiate transient parameter here
	}

	// all parameters here will be serialized

	int numberOfTimepoints = 1;

	boolean interpolate = false;

	// transient parameters won't be serialized

	transient ImagePlus welcomeImage;

	@Override
	public BdvHandle get() {

		BdvOptions options = new BdvOptions();

		// --------------- Creates empty bdv
		// create dummy image to instantiate the BDV
		ArrayImg<ByteType, ByteArray> dummyImg = ArrayImgs.bytes(2, 2, 2);
		BdvStackSource<ByteType> bss = BdvFunctions.show(dummyImg, "dummy",
			options);
		BdvHandle bdv = bss.getBdvHandle();

		// remove dummy image
		bdv.getViewerPanel().state().removeSource(bdv.getViewerPanel().state()
			.getCurrentSource());

		// ------------ Makes it linear if specified, change timepoints
		if (interpolate) bdv.getViewerPanel().setInterpolation(
			Interpolation.NLINEAR);
		bdv.getViewerPanel().setNumTimepoints(numberOfTimepoints); // One time point

		// ------------ Register all listeners that you want
		bdv.getViewerPanel().state().changeListeners().add(new ExampleListener(
			bdv));

		// ------------ Add any behaviour (editor, etc...)

		// ------------ Add any card Panel / prepare your window
		bdv.getSplitPanel().setCollapsed(false); // show the split panel by default

		// Append a new Card

		JLabel label = new JLabel("Hello!");
		BdvHandleHelper.addCard(bdv, "My Card", label, true);

		// Collapse group card -> todo in swingutilities
		// bdv.getCardPanel().setCardExpanded(
		//	BdvDefaultCards.DEFAULT_SOURCEGROUPS_CARD, false);

		IJ.log("You have created a new bdv window from " + this);

		return bdv;
	}

	public String toString() {
		return this.getClass().getSimpleName() + "| tp = " + numberOfTimepoints +
			" interpolate = " + interpolate;
	}

	public static class ExampleListener implements ViewerStateChangeListener {

		final BdvHandle bdvh;

		public ExampleListener(BdvHandle bdvh) {
			this.bdvh = bdvh;
		}

		@Override
		public void viewerStateChanged(ViewerStateChange change) {
			IJ.log(BdvHandleHelper.getWindowTitle(bdvh) + " state has changed : " +
				change);
			if (change.equals(ViewerStateChange.NUM_SOURCES_CHANGED)) {
				int nSources = bdvh.getViewerPanel().state().getSources().size();
				if (nSources == 0) IJ.log("There's no source in this bdv window");
				if (nSources == 1) IJ.log("There's a single source in this bdv window");
				if (nSources > 1) IJ.log("There are now " + bdvh.getViewerPanel()
					.state().getSources().size() + " sources in this window");
			}
		}
	}

}
