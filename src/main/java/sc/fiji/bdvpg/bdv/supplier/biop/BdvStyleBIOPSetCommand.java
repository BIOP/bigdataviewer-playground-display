
package sc.fiji.bdvpg.bdv.supplier.biop;

import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.BdvPgMenus;
import sc.fiji.bdvpg.viewer.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.service.SourceBdvDisplayService;

import java.util.Arrays;

@SuppressWarnings({ "CanBeFinal", "Unused" })
@Plugin(type = BdvPlaygroundActionCommand.class,
	//menuPath = ScijavaBdvDefaults.RootMenu + "View>BDV>Settings>BDV - Set Style (BIOP)",
		menu = {
				@Menu(label = BdvPgMenus.L1),
				@Menu(label = BdvPgMenus.L2),
				@Menu(label = BdvPgMenus.DisplayMenu, weight = BdvPgMenus.DisplayW),
				@Menu(label = BdvPgMenus.BDVMenu, weight = BdvPgMenus.BDVW),
				@Menu(label = "Settings", weight = -2),
				@Menu(label = "BDV - Set Style (BIOP)", weight = -2)
		},
	description = "Set preferences of Bdv Window (Biop)")
public class BdvStyleBIOPSetCommand implements
	BdvPlaygroundActionCommand
{

	@Parameter(
		label = "Click this checkbox to ignore all parameters and reset the default biop viewer",
		persist = false)
	boolean resetToDefault = false;

	@Parameter
	int width = 640;

	@Parameter
	int height = 480;

	@Parameter
	String screenscales = "1, 0.5, 0.25, 0.125";

	@Parameter
	int numrenderingthreads = 3;

	@Parameter
	int numsourcegroups = 10;

	@Parameter
	String frametitle = "BigDataViewer";

	@Parameter
	boolean is2d = false;

	@Parameter
	boolean interpolate = false;

	@Parameter
	int numtimepoints = 1;

	@Parameter
	int fontSize = 18;

	@Parameter(choices = {"Courier", "TimesRoman"})
	String font;

	@Parameter
	SourceBdvDisplayService source_display_service;

	@Override
	public void run() {
		if (resetToDefault) {
			IBdvSupplier bdvSupplier = new BiopBdvSupplier(
				new BiopSerializableBdvOptions());
			source_display_service.setDefaultBdvSupplier(bdvSupplier);
		}
		else {
			BiopSerializableBdvOptions options = new BiopSerializableBdvOptions();
			options.frameTitle = frametitle;
			options.is2D = is2d;
			options.numRenderingThreads = numrenderingthreads;
			options.screenScales = Arrays.stream(screenscales.split(",")).mapToDouble(
				Double::parseDouble).toArray();
			options.height = height;
			options.width = width;
			options.numSourceGroups = numsourcegroups;
			options.numTimePoints = numtimepoints;
			options.interpolate = interpolate;
			options.font = font;
			options.fontSize = fontSize;
			IBdvSupplier bdvSupplier = new BiopBdvSupplier(options);
			source_display_service.setDefaultBdvSupplier(bdvSupplier);
		}

	}
}
