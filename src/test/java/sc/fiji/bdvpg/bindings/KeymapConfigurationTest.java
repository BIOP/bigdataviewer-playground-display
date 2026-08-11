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
import sc.fiji.bdvpg.bdv.supplier.biop.BiopSerializableBdvOptions;
import sc.fiji.bdvpg.service.SourceServices;
import sc.fiji.bdvpg.viewer.bdv.config.BdvKeymapHelper;
import sc.fiji.bdvpg.viewer.behaviour.EditorBehaviourInstaller;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

		assertSame("the supplier did not share the playground keymap manager",
			BdvKeymapHelper.getKeymapManager(), manager);
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

		// Rebind the editor mode toggle before the window is built
		final Keymap keymap = BdvKeymapHelper.getKeymap();
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
