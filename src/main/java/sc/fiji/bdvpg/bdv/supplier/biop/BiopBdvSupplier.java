package sc.fiji.bdvpg.bdv.supplier.biop;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Interpolation;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.ByteType;
import sc.fiji.bdvpg.bdv.supplier.BdvSupplierHelper;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;

import javax.swing.*;

public class BiopBdvSupplier implements IBdvSupplier {

    public final BiopSerializableBdvOptions sOptions;

    public BiopBdvSupplier(BiopSerializableBdvOptions sOptions) {
        this.sOptions = sOptions;
    }

    @Override
    public BdvHandle get() {
        BdvOptions options = sOptions.getBdvOptions();

        // create dummy image to instantiate the BDV
        ArrayImg<ByteType, ByteArray> dummyImg = ArrayImgs.bytes(2, 2, 2);
        options = options.sourceTransform( new AffineTransform3D() );
        BdvStackSource<ByteType> bss = BdvFunctions.show( dummyImg, "dummy", options );
        BdvHandle bdvh = bss.getBdvHandle();

        if ( sOptions.interpolate ) bdvh.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

        // remove dummy image
        bdvh.getViewerPanel().state().removeSource(bdvh.getViewerPanel().state().getCurrentSource());
        bdvh.getViewerPanel().setNumTimepoints( sOptions.numTimePoints );

        BdvSupplierHelper.addSourcesDragAndDrop(bdvh);

        SourceSelectorBehaviour ssb = BdvSupplierHelper.addEditorMode(bdvh, "");
        bdvh.getSplitPanel().setCollapsed(false);

        JPanel editorModeToggle = new JPanel();
        JButton editorToggle = new JButton("Editor Mode");
        editorToggle.addActionListener((e) -> {
            if (ssb.isEnabled()) {
                ssb.disable();
                editorToggle.setText("Editor Mode 'E'");
            } else {
                ssb.enable();
                editorToggle.setText("Navigation Mode 'E'");
            }
        });

        editorModeToggle.add(editorToggle);

        bdvh.getCardPanel().addCard("Mode", editorModeToggle, true);

        return bdvh;
    }

}