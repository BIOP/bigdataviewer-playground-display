package bdv.util.source.blended;

import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static bdv.util.source.blended.AlphaBlendedResampledSource.AVERAGE;
import static bdv.util.source.blended.AlphaBlendedResampledSource.SUM;

public class AlphaBlended3DRandomAccessible<T extends NumericType<T>> implements RandomAccessible<T> {

    final List<RandomAccessible<T>> origins;
    final List<RandomAccessible<FloatType>> origins_alpha;
    final RandomAccess<T> ra;
    final Supplier<T> pixelSupplier;

    public AlphaBlended3DRandomAccessible(String blendingMode, List<RandomAccessible<T>> origins, List<RandomAccessible<FloatType>> origins_alpha, Supplier<T> pixelSupplier) {
        this.origins = new ArrayList<>(origins);
        this.origins_alpha = new ArrayList<>(origins_alpha);
        this.pixelSupplier = pixelSupplier;

        assert origins.size()==origins_alpha.size();

        List<RandomAccess<T>> origins_ra = new ArrayList<>();
        List<RandomAccess<FloatType>> origins_alpha_ra = new ArrayList<>();

        for (RandomAccessible<T> ra: origins) {
            origins_ra.add(ra.randomAccess().copyRandomAccess());
        }
        for (RandomAccessible<FloatType> ra: origins_alpha) {
            origins_alpha_ra.add(ra.randomAccess().copyRandomAccess());
        }

        switch (blendingMode) {
            case AVERAGE:
                this.ra = new AverageAlphaBlended3DRandomAccess<T>(origins_ra.toArray(new RandomAccess[0]), origins_alpha_ra.toArray(new RandomAccess[0]), pixelSupplier);
                break;
            case SUM:
            default:
                this.ra = new SumAlphaBlended3DRandomAccess<T>(origins_ra.toArray(new RandomAccess[0]), origins_alpha_ra.toArray(new RandomAccess[0]), pixelSupplier);
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

    @Override
    public int numDimensions() {
        return 3;
    }
}
