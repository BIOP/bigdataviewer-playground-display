
package sc.fiji.bdvpg.bdv.supplier.biop;

import bdv.util.AxisOrder;
import bdv.util.BdvOptions;
import bdv.viewer.render.AccumulateProjectorARGB;
import bdv.viewer.render.AccumulateProjectorFactory;
import net.imglib2.type.numeric.ARGBType;

public class BiopSerializableBdvOptions {

	public int width = -1;

	public int height = -1;

	public double[] screenScales = new double[] { 1, 0.75, 0.5, 0.25, 0.125 };

	public long targetRenderNanos = 30 * 1000000L;

	public int numRenderingThreads = 3;

	public int numSourceGroups = 10;

	public String frameTitle = "BigDataViewer";

	public boolean is2D = false;

	public AxisOrder axisOrder = AxisOrder.DEFAULT;

	// Extra for the playground
	public boolean interpolate = false;

	public int numTimePoints = 1;

	public AccumulateProjectorFactory<ARGBType> accumulateProjectorFactory =
		AccumulateProjectorARGB.factory;
	public int fontSize = 18;

	public String font = "Courier";

	public static BiopSerializableBdvOptions options() {
		return new BiopSerializableBdvOptions();
	}

	public BdvOptions getBdvOptions() {
		BdvOptions o = BdvOptions.options().screenScales(this.screenScales)
			.targetRenderNanos(this.targetRenderNanos).numRenderingThreads(
				this.numRenderingThreads).numSourceGroups(this.numSourceGroups)
			.axisOrder(this.axisOrder).preferredSize(this.width, this.height)
			.frameTitle(this.frameTitle);
		if (this.accumulateProjectorFactory != null) {
			o = o.accumulateProjectorFactory(this.accumulateProjectorFactory);
		}
		if (this.is2D) o = o.is2D();

		return o;
	}

}
