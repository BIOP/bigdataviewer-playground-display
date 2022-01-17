package bdv.util.source.blended;

import net.imglib2.RandomAccess;

public interface SubSetBlendedRandomAccess<T> extends RandomAccess<T> {

    RandomAccess<T> copy(boolean[] subset);

}
