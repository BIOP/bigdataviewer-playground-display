
package sc.fiji.bdvpg.bdv.supplier.biop;

import bdv.BigDataViewer;
import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapManager;
import bdv.util.BdvOptions;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The {@link KeymapManager} of the BIOP viewer, which ships a "BIOP" keymap
 * next to the "Default" one of BigDataViewer.
 * <p>
 * The BIOP keymap makes navigation behave the way it does in ImageJ, QuPath,
 * napari or any map viewer: dragging pans, scrolling zooms, and rotating is a
 * deliberate gesture rather than the one you get by accident. In both the 3D
 * and the 2D navigation modes:
 * <ul>
 * <li>left drag and right drag pan,</li>
 * <li>middle drag and {@code shift} + left drag rotate,</li>
 * <li>scroll zooms,</li>
 * <li>{@code shift} + scroll moves through Z, in 3D mode only.</li>
 * </ul>
 * {@code shift} is the single modifier used, because it is the only one which
 * behaves identically on Windows, macOS and Linux: AWT turns {@code alt} +
 * click into a middle click and {@code meta} + click into a right click on
 * macOS, and {@code ctrl} + click is the system secondary click there.
 * {@code org.scijava.ui.behaviour.MouseAndKeyHandler} additionally resolves
 * {@code shift} against the physical key state, so {@code shift} + scroll is
 * not confused with the horizontal scrolling AWT encodes the same way.
 * <p>
 * Both keymaps are <em>builtin</em> styles, which has two consequences worth
 * knowing: they are never written to disk and can never be overwritten by a
 * user, and conversely they cannot be edited in place — the keymap page of the
 * preferences dialog ({@code ctrl COMMA}) requires duplicating one first. Only
 * the selection, and any keymap the user derived, are persisted. "BIOP" comes
 * first so that it is the one selected on an installation which has no keymap
 * configuration yet, while an existing selection is restored by name and wins.
 * <p>
 * This manager is deliberately <em>not</em> the one of
 * {@code sc.fiji.bdvpg.viewer.bdv.config.BdvKeymapHelper}: it belongs to the
 * BIOP window style only, and windows created by the other suppliers keep the
 * bindings of BigDataViewer. It therefore keeps its keymaps in a directory of
 * its own, so that saving one selection never clobbers the other.
 * <p>
 * Note that the per window helpers of {@code BdvKeymapHelper} — {@code
 * getConfig(BdvHandle)}, {@code onKeymapChanged} and {@code getTriggerLabel} —
 * resolve the manager from the window itself, so they keep working unchanged
 * on windows built by this one.
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class BiopKeymapManager extends KeymapManager {

	/** Name of the shipped keymap, as shown in the preferences dialog. */
	public static final String BIOP_KEYMAP_NAME = "BIOP";

	/** Keymap file, next to this class. */
	private static final String KEYMAP_RESOURCE = "biop.yaml";

	/**
	 * Subdirectory of {@link BigDataViewer#configDir} holding the keymaps of the
	 * BIOP style, kept apart from the ones of the other window styles.
	 */
	private static final String CONFIG_SUBDIR = "biop";

	private static List<Keymap> builtinKeymaps;

	private static BiopKeymapManager instance;

	public BiopKeymapManager() {
		super(BigDataViewer.configDir + "/" + CONFIG_SUBDIR);
	}

	/**
	 * @return the manager shared by all BIOP windows, created on first access, so
	 *         that editing the keymap in one window applies to every other one
	 *         and the selection is saved once.
	 */
	public static synchronized BiopKeymapManager getInstance() {
		if (instance == null) {
			instance = new BiopKeymapManager();
		}
		return instance;
	}

	/**
	 * Declares the BIOP keymap on {@code options}, so that the resulting window
	 * uses it and shares it with the other BIOP windows.
	 * <p>
	 * Both the manager and the config have to be set:
	 * {@code BdvHandleFrame#createViewer} passes an explicit
	 * {@link InputTriggerConfig} to {@link BigDataViewer}, which then ignores the
	 * config of the keymap. Setting the config here to the keymap's own means the
	 * window starts with exactly the bindings the preferences dialog displays,
	 * instead of falling back to the triggers hardcoded in the source.
	 *
	 * @param options the options of the window about to be created
	 * @return {@code options}, with the keymap declared
	 */
	public static BdvOptions applyTo(BdvOptions options) {
		final BiopKeymapManager manager = getInstance();
		return options.keymapManager(manager).inputTriggerConfig(manager
			.getForwardSelectedKeymap().getConfig());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Called from the constructor of {@code AbstractStyleManager}, before this
	 * class is initialized, hence the static state.
	 */
	@Override
	protected List<Keymap> loadBuiltinStyles() {
		synchronized (BiopKeymapManager.class) {
			if (builtinKeymaps == null) {
				final List<Keymap> keymaps = new ArrayList<>();
				final Keymap biop = loadBiopKeymap();
				// First, so that it is the one selected when nothing was saved yet
				if (biop != null) {
					keymaps.add(biop);
				}
				// Taken from a plain manager rather than read again from
				// bdv/ui/keymap/default.yaml, so that "Default" stays exactly the
				// keymap bigdataviewer-core ships, wherever it keeps it.
				keymaps.addAll(new KeymapManager().getBuiltinStyles());
				builtinKeymaps = Collections.unmodifiableList(keymaps);
			}
		}
		return builtinKeymaps;
	}

	private static Keymap loadBiopKeymap() {
		try (InputStream in = BiopKeymapManager.class.getResourceAsStream(
			KEYMAP_RESOURCE))
		{
			if (in == null) {
				System.err.println("Keymap resource " + KEYMAP_RESOURCE +
					" not found, falling back to the BigDataViewer bindings.");
				return null;
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				return new Keymap(BIOP_KEYMAP_NAME, new InputTriggerConfig(YamlConfigIO
					.read(reader)));
			}
		}
		catch (final IOException e) {
			System.err.println("Could not read the keymap resource " +
				KEYMAP_RESOURCE + ", falling back to the BigDataViewer bindings.");
			e.printStackTrace();
			return null;
		}
	}

}