
package sc.fiji.bdvpg.bdv.supplier;

import bdv.KeyConfigContexts;
import bdv.KeyConfigScopes;
import bdv.ui.SourcesTransferable;
import bdv.util.BdvHandle;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.viewer.bdv.config.BdvKeymapHelper;
import sc.fiji.bdvpg.viewer.behaviour.EditorBehaviourInstaller;
import sc.fiji.bdvpg.scijava.service.tree.swingdnd.BdvTransferHandler;
import sc.fiji.bdvpg.service.SourceServices;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class BdvSupplierHelper {

	/**
	 * Command name and default trigger of the drag and drop of the selected
	 * sources, editable in the keymap page of the BDV preferences dialog.
	 */
	public static final String DRAG_SELECTED_SOURCES = "drag-selected-sources";

	public static final String[] DRAG_SELECTED_SOURCES_KEYS = new String[] {
		"alt button1" };

	public static void addSourcesDragAndDrop(BdvHandle bdvh) {
		bdvh.getViewerPanel().setTransferHandler(new BdvTransferHandler());
	}

	/**
	 * @param bdvh the window to install the editor mode on
	 * @param pathToBindings ignored, kept for backwards compatibility. Bindings
	 *          come from the keymap of the window, see {@link BdvKeymapHelper}.
	 * @return the source selector installed on this window
	 * @deprecated use {@link #addEditorMode(BdvHandle)}
	 */
	@Deprecated
	public static SourceSelectorBehaviour addEditorMode(BdvHandle bdvh,
		String pathToBindings)
	{
		return addEditorMode(bdvh);
	}

	/**
	 * Adds the source selection ("editor") mode to a BDV window. All the
	 * triggers involved, starting with the {@code E} key which toggles between
	 * the editor and the navigation mode, are read from the keymap of the window
	 * and can be changed by the user in the keymap page of the BDV preferences
	 * dialog.
	 *
	 * @param bdvh the window to install the editor mode on
	 * @return the source selector installed on this window
	 */
	public static SourceSelectorBehaviour addEditorMode(BdvHandle bdvh) {

		final InputTriggerConfig config = BdvKeymapHelper.getConfig(bdvh);

		// Set up a source selection mode with a trigger input key that toggles it
		// on and off, 'E' unless the user rebound it
		SourceSelectorBehaviour ssb = new SourceSelectorBehaviour(bdvh, config,
			SourceSelectorBehaviour.SOURCES_SELECTOR_TOGGLE_KEYS[0]);

		// Stores the associated selector to the display
		SourceServices.getBdvDisplayService().setDisplayMetadata(bdvh,
			SourceSelectorBehaviour.class.getSimpleName(), ssb);

		new EditorBehaviourInstaller(ssb).run();

		// Custom Drag support
		if (bdvh.getViewerPanel()
			.getTransferHandler() instanceof BdvTransferHandler)
		{
			System.out.println("Dragging support enabled");
			BdvTransferHandler handler = (BdvTransferHandler) bdvh.getViewerPanel()
				.getTransferHandler();
			handler.setTransferableFunction(c -> new SourcesTransferable(ssb
				.getSelectedSources()));
			ssb.addBehaviour(new DragNDSourcesBehaviour(bdvh),
				DRAG_SELECTED_SOURCES, DRAG_SELECTED_SOURCES_KEYS);
		}
		return ssb;
	}

	static class DragNDSourcesBehaviour implements DragBehaviour {

		final BdvHandle bdvh;

		public DragNDSourcesBehaviour(BdvHandle bdvh) {
			this.bdvh = bdvh;
		}

		@Override
		public void init(int x, int y) {
			bdvh.getViewerPanel().getTransferHandler().exportAsDrag(bdvh
				.getViewerPanel(), new MouseEvent(bdvh.getViewerPanel(), 0, 0, 0, 100,
					100, 1, false), TransferHandler.MOVE);
		}

		@Override
		public void drag(int x, int y) {

		}

		@Override
		public void end(int x, int y) {

		}
	}

	/**
	 * Lists the drag and drop of the selected sources in the keymap page of the
	 * BDV preferences dialog.
	 */
	@Plugin(type = CommandDescriptionProvider.class)
	public static class Descriptions extends CommandDescriptionProvider {

		public Descriptions() {
			super(KeyConfigScopes.BIGDATAVIEWER, KeyConfigContexts.BIGDATAVIEWER);
		}

		@Override
		public void getCommandDescriptions(final CommandDescriptions descriptions) {
			descriptions.add(DRAG_SELECTED_SOURCES, DRAG_SELECTED_SOURCES_KEYS,
				"Drag the selected sources out of the viewer, to drop them onto another window. Editor mode only.");
		}
	}

}
