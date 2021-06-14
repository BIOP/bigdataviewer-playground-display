package sc.fiji.bdvpg.bdv.supplier.biop;

import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.persist.IClassRuntimeAdapter;

/**
 * For serialization of {@link DefaultBdvSupplier} objects
 */

@Plugin(type = IClassRuntimeAdapter.class)
public class BiopBdvSupplierAdapter implements IClassRuntimeAdapter<IBdvSupplier, BiopBdvSupplier> {

    @Override
    public Class<? extends IBdvSupplier> getBaseClass() {
        return IBdvSupplier.class;
    }

    @Override
    public Class<? extends BiopBdvSupplier> getRunTimeClass() {
        return BiopBdvSupplier.class;
    }

    public boolean useCustomAdapter() {return false;}
}
