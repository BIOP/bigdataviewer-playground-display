
package sc.fiji.bdvpg.bdv.supplier.biop;

import bdv.BigDataViewer;
import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapManager;
import bdv.util.BdvOptions;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

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
 * <h2>Why the keymap is seeded rather than builtin</h2> A keymap shipped as a
 * <em>builtin</em> style would be the natural choice — builtins are never
 * written to disk, so they can neither overwrite nor be overwritten by what the
 * user edits. It cannot be used here, because the keymap page of the
 * preferences dialog ({@code ctrl COMMA}) does not survive it:
 * {@code BigDataViewer} builds that page with the two argument
 * {@code KeymapSettingsPage} constructor, which creates a plain
 * {@code new KeymapManager()} to hold the edits which have not been applied
 * yet. That editing manager only knows the builtins of BigDataViewer, so a
 * builtin of ours is missing from it, and
 * {@code AbstractStyleManager#setSnapshot}, which resolves the selection by
 * name, falls back to the first builtin it does know. The dialog would
 * therefore show "Default" and its bindings, and applying it would push that
 * selection back into this manager and save it — quietly turning the BIOP
 * navigation off for good.
 * <p>
 * User styles, on the other hand, are part of the snapshot the editing manager
 * is filled from, so a seeded keymap shows up correctly, stays selected, and
 * can be edited in place instead of having to be duplicated first. The price is
 * that it is written to {@code <configDir>/biop/keymaps/}: a later version of
 * this module cannot update a keymap a user already has, and a user who breaks
 * theirs restores it by deleting it and restarting. The keymap is seeded once,
 * when no keymap of that name exists, and is only made the selected one when
 * there is no user keymap at all, so that it never takes over from a keymap the
 * user built for themselves.
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

	private static BiopKeymapManager instance;

	public BiopKeymapManager() {
		super(BigDataViewer.configDir + "/" + CONFIG_SUBDIR);
		seedBiopKeymap();
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
	 * Adds the BIOP keymap to the user keymaps, unless one of that name is
	 * already there, in which case whatever the user made of it is kept.
	 * <p>
	 * Called at the end of the constructor, hence after the superclass has read
	 * {@code <configDir>/biop/keymaps/} and discovered the command descriptions.
	 */
	private void seedBiopKeymap() {
		if (styleForName(BIOP_KEYMAP_NAME).isPresent()) return;

		final Keymap biop = loadBiopKeymap();
		if (biop == null) return;

		// Only on an installation which has no keymap of its own, so that a user
		// who deleted this one in favour of theirs does not get it back on top
		final boolean noUserKeymapYet = getUserStyles().isEmpty();

		userStyles.add(biop);

		// The descriptions were applied by the superclass, before this keymap
		// existed. Setting them again fills in the default trigger of every
		// command the resource does not mention, which is what the keymap editor
		// lists and what the window falls back to.
		final CommandDescriptions descriptions = getCommandDescriptions();
		if (descriptions != null) {
			setCommandDescriptions(descriptions);
		}

		if (noUserKeymapYet) {
			setSelectedStyle(biop);
		}
		saveStyles();
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
