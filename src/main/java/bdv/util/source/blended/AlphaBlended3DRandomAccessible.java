package bdv.util.source.blended;

import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.function.Supplier;

import static bdv.util.source.blended.AlphaBlendedResampledSource.AVERAGE;
import static bdv.util.source.blended.AlphaBlendedResampledSource.SUM;

public class AlphaBlended3DRandomAccessible<T extends NumericType<T>> implements RandomAccessible<T> {

    final RandomAccessible<T>[] origins;
    final RandomAccessible<FloatType>[] origins_alpha;
    final SubSetBlendedRandomAccess<T> ra;
    final Supplier<T> pixelSupplier;
    final String blendingMode;

    public AlphaBlended3DRandomAccessible(String blendingMode, RandomAccessible<T>[] origins, RandomAccessible<FloatType>[] origins_alpha, Supplier<T> pixelSupplier) {
        this.blendingMode = blendingMode;
        this.origins = origins;
        this.origins_alpha = origins_alpha;
        this.pixelSupplier = pixelSupplier;

        int l = origins.length;
        assert origins.length==origins_alpha.length;

        RandomAccess<T>[] origins_ra = new RandomAccess[origins.length];
        RandomAccess<FloatType>[] origins_alpha_ra = new RandomAccess[origins.length];

        for (int i=0;i<l;i++) {
            origins_ra[i] = origins[i].randomAccess();
            origins_alpha_ra[i] = origins_alpha[i].randomAccess();
        }

        switch (blendingMode) {
            case AVERAGE:
                this.ra = new AverageAlphaBlended3DRandomAccess<T>(origins_ra, origins_alpha_ra, pixelSupplier);
                break;
            case SUM:
            default:
                this.ra = new SumAlphaBlended3DRandomAccess<T>(origins_ra, origins_alpha_ra, pixelSupplier);
        }
    }

    @Override
    public RandomAccess<T> randomAccess() {
        return (RandomAccess<T>) (ra.copy());
    }

    @Override
    public RandomAccess<T> randomAccess(Interval interval) {
        // Could be optimized to remove out of bounds sources - but this is not called
        //System.out.println("COULD BE OPTIMIZED!!");
        return randomAccess();
    }

    public RandomAccess<T> randomAccess(boolean[] sourcesPresentInCell) {
        // Could be optimized to remove out of bounds sources - but this is not called
        //System.out.println("COULD BE OPTIMIZED!!");

        return ra.copy(sourcesPresentInCell);
    }

    @Override
    public int numDimensions() {
        return 3;
    }
}
