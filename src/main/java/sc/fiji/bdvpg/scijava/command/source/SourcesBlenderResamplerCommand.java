package sc.fiji.bdvpg.scijava.command.source;

import bdv.util.source.blended.AlphaBlendedResampledSource;
import bdv.viewer.SourceAndConverter;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceBlenderResampler;
import java.util.Arrays;
import java.util.List;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Blend and Resample Sources Based on Model Source")
public class SourcesBlenderResamplerCommand implements BdvPlaygroundActionCommand {

    @Parameter(label = "Select Source(s)")
    SourceAndConverter[] sacs;

    @Parameter
    SourceAndConverter model;

    @Parameter(label="Re-use MipMaps")
    boolean reusemipmaps;

    @Parameter(label="MipMap level if not re-used (0 = max resolution)")
    int defaultmipmaplevel;

    @Parameter
    boolean interpolate;

    @Parameter
    boolean cache;

    @Parameter
    int cacheX = 64, cacheY=64, cacheZ=64;

    @Parameter(label="Name of the blended resampled source")
    String name; // CSV separate for multiple sources

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter sac_out;

    @Parameter(choices = {AlphaBlendedResampledSource.SUM, AlphaBlendedResampledSource.AVERAGE})
    String blendingMode;

    @Override
    public void run() {
        // Should not be parallel
        System.out.println("Starting");
        List<SourceAndConverter> sacs_list = Arrays.asList(sacs);
        System.out.println("Middle");
        sac_out = new SourceBlenderResampler(sacs_list, blendingMode,  model, name, reusemipmaps, cache, interpolate, defaultmipmaplevel, cacheX, cacheY, cacheZ).get();
        System.out.println("Done.");
    }

}