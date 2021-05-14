package bdv.util.source.alpha;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import net.imglib2.type.numeric.real.FloatType;

/**
 * Alpha Source for a Transformed Source
 *
 * This class is made in order to identify the fact that it is an AlphaSource by
 * implementing the {@link IAlphaSource} interface.
 *
 * Otherwise, bdv would try to make an alpha source out of an alpha source out of
 * an alpha source etc. (stack overflow)
 *
 */

public class AlphaSourceTransformed extends TransformedSource<FloatType> implements IAlphaSource {

    public AlphaSourceTransformed(Source<FloatType> source, TransformedSource<?> shareTransform) {
        super(source, shareTransform);
    }
}
