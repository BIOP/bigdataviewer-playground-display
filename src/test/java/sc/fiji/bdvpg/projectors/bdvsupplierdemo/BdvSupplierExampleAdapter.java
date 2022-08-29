
package sc.fiji.bdvpg.projectors.bdvsupplierdemo;

import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.persist.IClassRuntimeAdapter;

@Plugin(type = IClassRuntimeAdapter.class)
public class BdvSupplierExampleAdapter implements
	IClassRuntimeAdapter<IBdvSupplier, BdvSupplierExample>
{

	@Override
	public Class<? extends IBdvSupplier> getBaseClass() {
		return IBdvSupplier.class;
	}

	@Override
	public Class<? extends BdvSupplierExample> getRunTimeClass() {
		return BdvSupplierExample.class;
	}

	@Override
	final public boolean useCustomAdapter() {
		return false; // Change to true if you want to use the custom serializer /
									// deserializer below
	}

	/*@Override
	public BdvSupplierExample deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
	
	    ... so custom de serialization here
	
	    return new BdvSupplierExample();
	}
	
	@Override
	public JsonElement serialize(BdvSupplierExample src, Type typeOfSrc, JsonSerializationContext context) {
	    JsonObject obj = new JsonObject();
	    obj.addProperty("type", BdvSupplierExample.class.getSimpleName());
	
	    ... do custom serialization here
	
	    return obj;
	}*/
}
