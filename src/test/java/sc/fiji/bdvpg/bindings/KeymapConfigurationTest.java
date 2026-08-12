package sc.fiji.bdvpg.bindings;

import bdv.KeyConfigContexts;
import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapManager;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import net.imagej.ImageJ;
import org.junit.After;
import org.junit.Test;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.gui.Command;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.bdv.supplier.BdvSupplierHelper;
import sc.fiji.bdvpg.bdv.supplier.biop.BiopBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.biop.BiopKeymapManager;
import sc.fiji.bdvpg.bdv.supplier.biop.BiopSerializableBdvOptions;
import sc.fiji.bdvpg.service.SourceServices;
import sc.fiji.bdvpg.viewer.bdv.config.BdvKeymapHelper;
import sc.fiji.bdvpg.viewer.behaviour.EditorBehaviourInstaller;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Checks that the key and mouse bindings of the BIOP viewer are configurable,
 * i.e. that the commands of the playground and of the selector reach the keymap
 * editor, and that a trigger set in the keymap actually wins over the trigger
 * hardcoded in the source.
 */
public class KeymapConfigurationTest {

	static ImageJ ij;

	/**
	 * Every command a user should be able to rebind has to be declared by a
	 * CommandDescriptionProvider, otherwise it is simply absent from the keymap
	 * page of the preferences dialog.
	 */
	@Test
	public void testCommandsAreDiscoverable() {
		final KeymapManager manager = new KeymapManager();
		manager.discoverCommandDescriptions();
		final CommandDescriptions descriptions = manager.getCommandDescriptions();
		assertNotNull("no command descriptions were discovered", descriptions);

		final Map<Command, String> commands = descriptions
			.createCommandDescriptionsMap();

		// BDV's own commands, which already worked
		assertTrue("BDV commands are missing", containsCommand(commands,
			"drag rotate"));

		// the selector, from bigdataviewer-selector
		assertTrue(containsCommand(commands,
			SourceSelectorBehaviour.SOURCES_SELECTOR_TOGGLE_MAP));
		assertTrue(containsCommand(commands,
			SourceSelectorBehaviour.SELECT_SET_SOURCES));
		assertTrue(containsCommand(commands,
			SourceSelectorBehaviour.SELECT_ALL_VISIBLE_SOURCES));

		// the editor mode, from bigdataviewer-playground
		assertTrue(containsCommand(commands,
			EditorBehaviourInstaller.REMOVE_SOURCES_FROM_BDV));
		assertTrue(containsCommand(commands,
			EditorBehaviourInstaller.SOURCES_CONTEXT_MENU));

		// the drag and drop of sources, from this module
		assertTrue(containsCommand(commands,
			BdvSupplierHelper.DRAG_SELECTED_SOURCES));
	}

	/**
	 * BdvHandleFrame passes an explicit InputTriggerConfig to BigDataViewer,
	 * which then ignores the keymap. The supplier has to declare both, otherwise
	 * the window starts with bindings that differ from the ones the preferences
	 * dialog shows.
	 */
	@Test
	public void testOptionsDeclareBothKeymapAndConfig() {
		final BdvOptions options = BiopSerializableBdvOptions.options()
			.getBdvOptions();

		final KeymapManager manager = options.values.getKeymapManager();
		assertNotNull("the supplier did not declare a KeymapManager", manager);

		final InputTriggerConfig config = options.values.getInputTriggerConfig();
		assertNotNull("the supplier did not declare an InputTriggerConfig", config);

		assertSame(
			"the declared config is not the one of the keymap, so editing the keymap would not match what the window uses",
			manager.getForwardSelectedKeymap().getConfig(), config);

		assertSame("the supplier did not share the BIOP keymap manager",
			BiopKeymapManager.getInstance(), manager);

		// The BIOP bindings belong to the BIOP window style only, the other
		// suppliers keep the ones of BigDataViewer
		assertNotSame(
			"the BIOP style must not take over the keymap of the other window styles",
			BdvKeymapHelper.getKeymapManager(), manager);
	}

	/**
	 * The keymap page of the preferences dialog does not work on the manager of
	 * the window, but on a plain {@code new KeymapManager()} which
	 * {@code KeymapSettingsPage} creates to hold the edits which have not been
	 * applied yet, and which it fills with
	 * {@code AbstractStyleManager#set(other)}. Only the user keymaps and the
	 * <em>name</em> of the selection make it through that copy, so a keymap
	 * shipped as a builtin of ours would be missing from the dialog, and the
	 * selection would silently fall back to the first builtin the plain manager
	 * knows, "Default". Applying that would then save it, turning the BIOP
	 * navigation off for good.
	 */
	@Test
	public void testBiopKeymapReachesThePreferencesDialog() {
		final BiopKeymapManager manager = BiopKeymapManager.getInstance();

		assertTrue("the BIOP keymap is not a user keymap, so the preferences" +
			" dialog cannot see it", manager.getUserStyles().stream().anyMatch(
				k -> BiopKeymapManager.BIOP_KEYMAP_NAME.equals(k.getName())));

		// Exactly what KeymapSettingsPage does with the manager of the window
		final KeymapManager editing = new KeymapManager();
		editing.set(manager);

		assertTrue("the BIOP keymap is absent from the preferences dialog", editing
			.getUserStyles().stream().anyMatch(
				k -> BiopKeymapManager.BIOP_KEYMAP_NAME.equals(k.getName())));

		assertEquals(
			"the preferences dialog changed the selected keymap just by opening",
			manager.getSelectedStyle().getName(), editing.getSelectedStyle()
				.getName());
	}

	/**
	 * The navigation bindings the BIOP keymap is for. Both the 3D commands and
	 * their '2d ' counterparts are checked: BigDataViewer keeps two parallel
	 * families and only one is active in a given window, so a 2D window would
	 * otherwise keep the old feel.
	 * <p>
	 * The 'not mapped' assertions are the point of the test as much as the others
	 * are: CommandDescriptions#augmentInputTriggerConfig puts back the default
	 * trigger of every command a keymap does not mention, and the defaults it
	 * would put back here are exactly the triggers used for panning and zooming.
	 */
	@Test
	public void testBiopKeymapRebindsBothNavigationFamilies() {
		final Keymap biop = BiopKeymapManager.getInstance().getUserStyles().stream()
			.filter(k -> BiopKeymapManager.BIOP_KEYMAP_NAME.equals(k.getName()))
			.findFirst().orElse(null);
		assertNotNull("the BIOP keymap was not loaded", biop);

		// 3D mode
		assertTriggers(biop, "drag translate", "button1", "button3");
		assertTriggers(biop, "drag rotate", "button2", "shift button1");
		assertTriggers(biop, "drag rotate fast", "not mapped");
		assertTriggers(biop, "drag rotate slow", "not mapped");
		assertTriggers(biop, "scroll zoom", "scroll");
		assertTriggers(biop, "scroll browse z", "shift scroll");
		assertTriggers(biop, "scroll browse z fast", "not mapped");
		assertTriggers(biop, "scroll browse z slow", "not mapped");

		// 2D mode, same gestures, minus the Z which does not exist there
		assertTriggers(biop, "2d drag translate", "button1", "button3");
		assertTriggers(biop, "2d drag rotate", "button2", "shift button1");
		assertTriggers(biop, "2d scroll zoom", "scroll");
		assertTriggers(biop, "2d scroll zoom fast", "not mapped");
		assertTriggers(biop, "2d scroll zoom slow", "not mapped");
	}

	private static void assertTriggers(Keymap keymap, String action,
		String... expected)
	{
		final Set<String> actual = keymap.getConfig().getInputs(action,
			KeyConfigContexts.BIGDATAVIEWER).stream().map(InputTrigger::toString)
			.collect(Collectors.toSet());
		assertEquals("unexpected triggers for '" + action + "'", new HashSet<>(
			Arrays.asList(expected)), actual);
	}

	/**
	 * The real end to end check: a trigger coming from a keymap file must
	 * override the one hardcoded where the behaviour is installed.
	 */
	@Test
	public void testKeymapOverridesHardcodedTrigger() throws IOException {
		ij = new ImageJ();

		// Exactly what a keymap YAML file holds, parsed by the same reader the
		// KeymapManager uses when it loads <configDir>/keymaps/*.yaml
		final String keymapFile = "---\n" + "- !mapping\n" + "  action: " +
			SourceSelectorBehaviour.SOURCES_SELECTOR_TOGGLE_MAP + "\n" +
			"  contexts: [" + KeyConfigContexts.BIGDATAVIEWER + "]\n" +
			"  triggers: [shift E]\n";

		final InputTriggerConfig fromFile;
		try (Reader reader = new StringReader(keymapFile)) {
			fromFile = new InputTriggerConfig(YamlConfigIO.read(reader));
		}

		// Rebind the editor mode toggle before the window is built. The BIOP
		// windows follow the keymap of their own manager, not the one of the
		// other window styles.
		final Keymap keymap = BiopKeymapManager.getInstance()
			.getForwardSelectedKeymap();
		keymap.getConfig().set(fromFile);

		SourceServices.getBdvDisplayService().setDefaultBdvSupplier(
			new BiopBdvSupplier(BiopSerializableBdvOptions.options()));
		final BdvHandle bdvh = SourceServices.getBdvDisplayService().getNewBdv();

		// The window has to see the very keymap we just edited
		assertSame(keymap, BdvKeymapHelper.getKeymap(bdvh));

		final Set<InputTrigger> triggers = BdvKeymapHelper.getConfig(bdvh)
			.getInputs(SourceSelectorBehaviour.SOURCES_SELECTOR_TOGGLE_MAP,
				KeyConfigContexts.BIGDATAVIEWER);
		assertFalse("the toggle is not bound at all", triggers.isEmpty());
		assertEquals("the keymap did not override the hardcoded 'E'", "shift E",
			triggers.iterator().next().toString());

		assertEquals("the button label does not follow the keymap", "shift E",
			BdvKeymapHelper.getTriggerLabel(bdvh,
				SourceSelectorBehaviour.SOURCES_SELECTOR_TOGGLE_MAP,
				SourceSelectorBehaviour.SOURCES_SELECTOR_TOGGLE_KEYS));
	}

	private static boolean containsCommand(Map<Command, String> commands,
		String name)
	{
		return commands.keySet().stream().anyMatch(c -> c.getName().equals(name));
	}

	@After
	public void closeFiji() {
		if (ij != null) {
			TestHelper.closeFijiAndBdvs(ij);
			ij = null;
		}
	}
}
