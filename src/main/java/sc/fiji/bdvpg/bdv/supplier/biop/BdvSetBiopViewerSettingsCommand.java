package sc.fiji.bdvpg.bdv.supplier.biop;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;

import java.util.Arrays;

@SuppressWarnings({"CanBeFinal","Unused"})
@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Set BDV window (biop)",
        description = "Set preferences of Bdv Window (Biop)")
public class BdvSetBiopViewerSettingsCommand implements BdvPlaygroundActionCommand{

    @Parameter(label = "Click this checkbox to ignore all parameters and reset the default biop viewer", persist = false)
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
    SourceAndConverterBdvDisplayService sacDisplayService;

    @Override
    public void run() {
        if (resetToDefault) {
            IBdvSupplier bdvSupplier = new BiopBdvSupplier(new BiopSerializableBdvOptions());
            sacDisplayService.setDefaultBdvSupplier(bdvSupplier);
        } else {
            BiopSerializableBdvOptions options = new BiopSerializableBdvOptions();
            options.frameTitle = frametitle;
            options.is2D = is2d;
            options.numRenderingThreads = numrenderingthreads;
            options.screenScales = Arrays.stream(screenscales.split(",")).mapToDouble(Double::parseDouble).toArray();
            options.height = height;
            options.width = width;
            options.numSourceGroups = numsourcegroups;
            options.numTimePoints = numtimepoints;
            options.interpolate = interpolate;
            IBdvSupplier bdvSupplier = new BiopBdvSupplier(options);
            sacDisplayService.setDefaultBdvSupplier(bdvSupplier);
        }

    }
}
