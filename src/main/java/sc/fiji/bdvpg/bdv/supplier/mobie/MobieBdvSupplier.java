package sc.fiji.bdvpg.bdv.supplier.mobie;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Interpolation;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.ByteType;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;

public class MobieBdvSupplier implements IBdvSupplier {

    public final MobieSerializableBdvOptions sOptions;

    public MobieBdvSupplier(MobieSerializableBdvOptions sOptions) {
        this.sOptions = sOptions;
    }

    @Override
    public BdvHandle get() {
        BdvOptions options = sOptions.getBdvOptions();

        // create dummy image to instantiate the BDV
        ArrayImg<ByteType, ByteArray> dummyImg = ArrayImgs.bytes(2, 2, 2);
        options = options.sourceTransform( new AffineTransform3D() );
        BdvStackSource<ByteType> bss = BdvFunctions.show( dummyImg, "dummy", options );
        BdvHandle bdv = bss.getBdvHandle();

        if ( sOptions.interpolate ) bdv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

        // remove dummy image
        bdv.getViewerPanel().state().removeSource(bdv.getViewerPanel().state().getCurrentSource());
        bdv.getViewerPanel().setNumTimepoints( sOptions.numTimePoints );

        return bdv;
    }

}