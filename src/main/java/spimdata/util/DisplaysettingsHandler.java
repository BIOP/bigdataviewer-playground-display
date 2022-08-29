
package spimdata.util;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.spimdata.EntityHandler;

@Plugin(type = EntityHandler.class)
public class DisplaysettingsHandler implements EntityHandler {

	@Override
	public Class<? extends Entity> getEntityType() {
		return Displaysettings.class;
	}

	@Override
	public boolean writeEntity(BasicViewSetup viewSetup,
		SourceAndConverter<?> sac)
	{
		Displaysettings displaysettings = new Displaysettings(viewSetup.getId());
		Displaysettings.PullDisplaySettings(sac, displaysettings);
		viewSetup.setAttribute(displaysettings);
		return false;
	}

	@Override
	public boolean loadEntity(AbstractSpimData<?> spimData,
		BasicViewSetup viewSetup)
	{
		return false;
	}

	@Override
	public boolean loadEntity(AbstractSpimData<?> spimData,
		BasicViewSetup viewSetup, SourceAndConverter<?> sac)
	{
		Displaysettings displaysettings = viewSetup.getAttribute(
			Displaysettings.class);
		Displaysettings.applyDisplaysettings(sac, displaysettings);
		return true;
	}

}
