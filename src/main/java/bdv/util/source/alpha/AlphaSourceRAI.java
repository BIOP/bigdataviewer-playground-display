package bdv.util.source.alpha;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

/**
 * Alpha Source for a source which is backed by a RAI and an AffineTransform
 *
 * Typically, any source which is not Warped
 *
 */

public class AlphaSourceRAI extends AlphaSource {

    public AlphaSourceRAI(Source<?> origin) {
        super(origin);
    }

    public AlphaSourceRAI(Source<?> origin, float alpha) {
        super(origin, alpha);
    }

    @Override
    public RandomAccessibleInterval<FloatType> getSource(int t, int level) {
        final float finalAlpha = alpha;

        final RandomAccessible< FloatType > randomAccessible =
                new FunctionRandomAccessible<>( 3, () -> (loc, out) -> out.setReal( finalAlpha ), FloatType::new );

        return Views.interval(randomAccessible, origin.getSource(t, level));
    }

    @Override
    public RealRandomAccessible<FloatType> getInterpolatedSource(int t, int level, Interpolation method) {
        ExtendedRandomAccessibleInterval<FloatType, RandomAccessibleInterval< FloatType >>
                eView = Views.extendZero(getSource( t, level ));
        RealRandomAccessible< FloatType > realRandomAccessible = Views.interpolate( eView, interpolators.get(Interpolation.NEARESTNEIGHBOR) );
        return realRandomAccessible;
    }
}
