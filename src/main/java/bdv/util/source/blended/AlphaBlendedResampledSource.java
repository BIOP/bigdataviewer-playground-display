package bdv.util.source.blended;

import bdv.img.WarpedSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.DefaultInterpolators;
import bdv.util.RAIHelper;
import bdv.util.source.alpha.AlphaSource;
import bdv.util.source.alpha.AlphaSourceHelper;
import bdv.util.source.alpha.IAlphaSource;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * A {@link AlphaBlendedResampledSource} is a {@link Source} which is computed on the fly
 * by resampling a collection of {@link Source} source at space and time coordinates
 * defined by a model {@link AlphaBlendedResampledSource#resamplingModel} source
 *
 * The returned resampled source is a source which is:
 * - the sampling of the fused origin source fields
 * at the points which are defined by the model source {@link AlphaBlendedResampledSource#resamplingModel} source
 *
 * Note:
 * - To be present at a certain timepoint, both the origin and the model source need to exist
 * - There is no duplication of data, unless {@link AlphaBlendedResampledSource#cache} is true
 * - proper reuse of mipmaps for model and origin work in certain conditions: see constructor documentation
 *
 * How multiple sources are blended ? They are blended by making a weighted average of their
 * {@link bdv.util.source.alpha.AlphaSource}, which means that each origin source needs to have
 * a linked AlphaSource.
 *
 * TODO : handle alpha source of a ResampledFusedSource
 *
 * @param <T> Type of the output source, identical to the origin sources
 *
 * @author Nicolas Chiaruttini, BIOP EPFL, 2022
 */

public class AlphaBlendedResampledSource< T extends NumericType<T> & NativeType<T>> implements Source<T> {

    final public static String SUM = "SUM";
    final public static String AVERAGE = "AVERAGE";

    protected static Logger logger = LoggerFactory.getLogger(AlphaBlendedResampledSource.class);

    final Collection<Source<T>> origins;

    /**
     * Hashmap to cache RAIs (mipmaps and timepoints), used only if {@link AlphaBlendedResampledSource#cache} is true
     */
    transient ConcurrentHashMap<Integer, ConcurrentHashMap<Integer,RandomAccessibleInterval<T>>> cachedRAIs
            = new ConcurrentHashMap<>();

    /**
     * Model source, no need to be of type {@link T}
     */
    Source<?> resamplingModel;

    protected final DefaultInterpolators< T > interpolators = new DefaultInterpolators<>();

    protected Map<Source<T>,Interpolation> originsInterpolation;

    protected Map<Source<T>, IAlphaSource> originsAlpha = new HashMap<>();

    final boolean reuseMipMaps;

    final int defaultMipMapLevel;

    boolean cache;

    private String name;

    final int cacheX, cacheY, cacheZ;

    final String blendingMode;

    /**
     * The origin sources are accessed through their RealRandomAccessible representation :
     * - It can be accessed at any 3d point in space, with real valued coordinates : it's a field of {@link T} objects
     *
     * The model source defines a portion of space and how it is sampled :
     *  - through its RandomAccessibleInterval bounds
     *  - and the Source affine transform
     *  - and mipmaps, if reuseMipMaps is true
     *  @param origins origin sources
     *
     * @param resamplingModel model source used for resampling the origin source
     *@param name
     * @param reuseMipMaps allows to reuse mipmaps of both the origin and the model source in the resampling
     *  mipmap reuse tries to be clever by matching the voxel size between the model source and the origin source
     *  so for instance the model source mipmap level 0 will resample the origin mipmap level 2, if the voxel size
     *  of the origin is much smaller then the model (and provided that the origin is also a multiresolution source)
     *  the way the matching is performed is specified in {@link SourceAndConverterHelper#bestLevel(Source, int, double)}.
     *  For more details and limitation, please read the documentation in the linked method above
     *@param cache specifies whether the result of the resampling should be cached.
     *  This allows for a fast access of resampled source after the first computation - but the synchronization with
     *  the origin and model source is lost.
     *  TODO : check how the cache can be accessed / reset
     * @param originsInterpolation specifies whether the origin source should be interpolated of not in the resampling process
     *
     */
    public AlphaBlendedResampledSource(Collection<Source<T>> origins,
                                       String blendingMode,
                                       Source< T > resamplingModel,
                                       String name,
                                       boolean reuseMipMaps,
                                       boolean cache,
                                       Map<Source<T>, Interpolation> originsInterpolation,
                                       int defaultMipMapLevel,
                                       int cacheX,
                                       int cacheY,
                                       int cacheZ) {
        this.blendingMode = blendingMode;
        this.origins=origins;
        this.resamplingModel=resamplingModel;
        this.name = name;
        this.reuseMipMaps=reuseMipMaps;
        this.cache = cache;
        this.originsInterpolation = originsInterpolation;
        this.defaultMipMapLevel = defaultMipMapLevel;
        for (Source<T> origin:origins) {
            computeMipMapsCorrespondance(origin);
            getAlpha(origin);
        }
        this.cacheX = cacheX;
        this.cacheY = cacheY;
        this.cacheZ = cacheZ;
    }

    Map<Source<T>,Map<Integer, Integer>> mipmapModelToOrigin = new HashMap<>();

    Map<Source<T>,List<Double>> originVoxSize = new HashMap<>();

    private void getAlpha(Source<T> origin) {
        originsAlpha.put(origin, (IAlphaSource) AlphaSourceHelper.getOrBuildAlphaSource(origin).getSpimSource());
    }

    private int bestMatch(Source<T> origin, double voxSize) {
        if (originVoxSize.get(origin)==null) {
            computeOriginSize(origin);
        }
        int level = 0;
        while((originVoxSize.get(origin).get(level)<voxSize)&&(level<originVoxSize.get(origin).size()-1)) {
            level=level+1;
        }
        return level;
    }

    private void computeOriginSize(Source<T> origin) {
        originVoxSize.put(origin,new ArrayList<>());
        Source<?> rootOrigin = origin;

        while ((rootOrigin instanceof WarpedSource)||(rootOrigin instanceof TransformedSource)) {
            if (rootOrigin instanceof WarpedSource) {
                rootOrigin = ((WarpedSource<?>) rootOrigin).getWrappedSource();
            } else { // rootOrigin instanceof TransformedSource
                rootOrigin = ((TransformedSource<?>) rootOrigin).getWrappedSource();
            }
        }

        for (int l=0;l<rootOrigin.getNumMipmapLevels();l++) {
            AffineTransform3D at3d = new AffineTransform3D();
            rootOrigin.getSourceTransform(0,l,at3d);
            double mid = SourceAndConverterHelper.getCharacteristicVoxelSize(at3d);
            originVoxSize.get(origin).add(mid);
        }

    }

    private void computeMipMapsCorrespondance(Source<T> origin) {
        AffineTransform3D at3D = new AffineTransform3D();
        mipmapModelToOrigin.put(origin, new HashMap<>());
        for (int l=0;l<resamplingModel.getNumMipmapLevels();l++) {
            if (reuseMipMaps) {
                resamplingModel.getSourceTransform(0,l, at3D);
                double middleDim = SourceAndConverterHelper.getCharacteristicVoxelSize(at3D);
                int match = bestMatch(origin,middleDim);
                mipmapModelToOrigin.get(origin).put(l, match);
            } else {
                mipmapModelToOrigin.get(origin).put(l, defaultMipMapLevel); // Always taking the highest resolution
            }

            // For debugging resampling issues
            logger.debug("Model mipmap level "+l+" correspond to origin mipmap level "+mipmapModelToOrigin.get(l));
            logger.debug("Model mipmap level "+l+" has a characteristic voxel size of "+
                    SourceAndConverterHelper.getCharacteristicVoxelSize(resamplingModel,0,l));
            logger.debug("Origin level "+mipmapModelToOrigin.get(l)+" has a characteristic voxel size of "+
                    SourceAndConverterHelper.getCharacteristicVoxelSize(origin,0,mipmapModelToOrigin.get(origin).get(l)));

        }
    }

    @Override
    public boolean isPresent(int t) {
        if (!resamplingModel.isPresent(t)) return false;

        // If any of the origin source is present, then the resampled fused source is present
        for (Source source: origins) {
            if (source.isPresent(t)) {
                return true;
            }
        }

        return false;
    }

    public Collection<Source<T>> getOriginalSources() {
        return origins;
    }

    public Source<?> getModelResamplerSource() {
        return resamplingModel;
    }

    public boolean areMipmapsReused() {
        return reuseMipMaps;
    }

    public boolean isCached() {
        return cache;
    }

    public Map<Source<T>,Interpolation> originsInterpolation() {
        return originsInterpolation;
    }

    @Override
    public RandomAccessibleInterval<T> getSource(int t, int level) {
        if (cache) {
            if (!cachedRAIs.containsKey(t)) {
                cachedRAIs.put(t, new ConcurrentHashMap<>());
            }

            if (!cachedRAIs.get(t).containsKey(level)) {
                if (cache) {
                    RandomAccessibleInterval<T> nonCached = buildSource(t, level);

                    int[] blockSize = {cacheX, cacheY, cacheZ};

                    if (nonCached.dimension(0) < cacheX) blockSize[0] = (int) nonCached.dimension(0);
                    if (nonCached.dimension(1) < cacheY) blockSize[1] = (int) nonCached.dimension(1);
                    if (nonCached.dimension(2) < cacheZ) blockSize[2] = (int) nonCached.dimension(2);

                    cachedRAIs.get(t).put(level, RAIHelper.wrapAsVolatileCachedCellImg(nonCached, blockSize));
                } else {
                    cachedRAIs.get(t).put(level,buildSource(t, level));
                }
            }
            return cachedRAIs.get(t).get(level);
        } else {
            return buildSource(t,level);
        }

    }

    public RandomAccessibleInterval<T> buildSource(int t, int level) {
        // Get current model source transformation
        AffineTransform3D at_ori = new AffineTransform3D();
        resamplingModel.getSourceTransform(t,level,at_ori);
        at_ori = at_ori.inverse();

        //int mipmap = getModelToOriginMipMapLevel(level);

        // Get bounds of model source RAI
        // TODO check if -1 is necessary
        long sx = resamplingModel.getSource(t,level).dimension(0)-1;
        long sy = resamplingModel.getSource(t,level).dimension(1)-1;
        long sz = resamplingModel.getSource(t,level).dimension(2)-1;

        List<RandomAccessible<T>> presentSources = new ArrayList<>();
        List<RandomAccessible<FloatType>> presentSourcesAlpha = new ArrayList<>();

        for (Source<T> origin: origins) {
            if (origin.isPresent(t)) {
                // Get field of origin source
                final RealRandomAccessible<T> ipimg =
                        origin.getInterpolatedSource(t, mipmapModelToOrigin.get(origin).get(level), originsInterpolation.get(origin));

                // Get field of origin source
                final RealRandomAccessible<FloatType> ipimg_alpha =
                        originsAlpha.get(origin).getInterpolatedSource(t, mipmapModelToOrigin.get(origin).get(level), Interpolation.NEARESTNEIGHBOR);

                // Gets randomAccessible... ( with appropriate transform )

                AffineTransform3D at = new AffineTransform3D();
                at.set(at_ori);
                AffineTransform3D atOrigin = new AffineTransform3D();
                origin.getSourceTransform(t, mipmapModelToOrigin.get(origin).get(level), atOrigin);
                at.concatenate(atOrigin);

                RandomAccessible<T> ra = RealViews.affine(ipimg, at); // Gets the view
                RandomAccessible<FloatType> ra_alpha = RealViews.affine(ipimg_alpha, at); // Gets the view

                presentSources.add(ra);
                presentSourcesAlpha.add(ra_alpha);
            }
        }

        AlphaBlended3DRandomAccessible<T> ra = new AlphaBlended3DRandomAccessible<>(blendingMode, presentSources, presentSourcesAlpha, this::getType);

        // ... interval
        RandomAccessibleInterval<T> view =
                Views.interval(ra, new long[]{0, 0, 0}, new long[]{sx, sy, sz}); //Sets the interval

        return view;
    }


    @Override
    public RealRandomAccessible<T> getInterpolatedSource(int t, int level, Interpolation method) {
        final T zero = getType();
        zero.setZero();
        ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval< T >>
                eView = Views.extendZero(getSource( t, level ));
        RealRandomAccessible< T > realRandomAccessible = Views.interpolate( eView, interpolators.get(method) );
        return realRandomAccessible;
    }

    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform) {
        resamplingModel.getSourceTransform(t,level,transform);
    }

    @Override
    public T getType() {
        return origins.stream().findAny().get().getType().createVariable();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return resamplingModel.getVoxelDimensions();
    }

    @Override
    public int getNumMipmapLevels() {
        return resamplingModel.getNumMipmapLevels();
    }

    public int getDefaultMipMapLevel() {
        return defaultMipMapLevel;
    }
}
