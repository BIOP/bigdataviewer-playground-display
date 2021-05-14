package bdv.util.projector.mixed;

import bdv.viewer.render.AccumulateProjectorFactory;
import org.scijava.plugin.Plugin;
import sc.fiji.persist.IClassRuntimeAdapter;

/**
 * For serialization of {@link AccumulateMixedProjectorARGBFactory} objects
 *
 * Used in {@link sc.fiji.bdvpg.bdv.supplier.mobie.MobieSerializableBdvOptions}
 *
 */

@Plugin(type = IClassRuntimeAdapter.class)
public class AccumulatorMixedProjectorARGBFactoryAdapter implements IClassRuntimeAdapter<AccumulateProjectorFactory, AccumulateMixedProjectorARGBFactory> {
    @Override
    public Class<? extends AccumulateProjectorFactory> getBaseClass() {
        return AccumulateProjectorFactory.class;
    }

    @Override
    public Class<? extends AccumulateMixedProjectorARGBFactory> getRunTimeClass() {
        return AccumulateMixedProjectorARGBFactory.class;
    }

    public boolean useCustomAdapter() {return false;}

}
