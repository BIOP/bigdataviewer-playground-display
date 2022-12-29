
package sc.fiji.bdvpg.bdv.supplier;

import bdv.ui.SourcesTransferable;
import bdv.util.BdvHandle;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.behaviour.EditorBehaviourInstaller;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.BdvTransferHandler;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class BdvSupplierHelper {

	public static void addSourcesDragAndDrop(BdvHandle bdvh) {
		bdvh.getViewerPanel().setTransferHandler(new BdvTransferHandler());
	}

	public static SourceSelectorBehaviour addEditorMode(BdvHandle bdvh,
		String pathToBindings)
	{

		// Adds selection mode triggered by E

		// Set up a source selection mode with a trigger input key that toggles it
		// on and off
		SourceSelectorBehaviour ssb = new SourceSelectorBehaviour(bdvh, "E");

		// Stores the associated selector to the display
		SourceAndConverterServices.getBdvDisplayService().setDisplayMetadata(bdvh,
			SourceSelectorBehaviour.class.getSimpleName(), ssb);

		new EditorBehaviourInstaller(ssb, pathToBindings).run();

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
				"drag-selected-sources", new String[] { "alt button1" });
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

}
