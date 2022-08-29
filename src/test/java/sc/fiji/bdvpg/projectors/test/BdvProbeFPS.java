package sc.fiji.bdvpg.projectors.test;

import bdv.cache.CacheControl;
import bdv.util.BdvHandle;
import bdv.viewer.BasicViewerState;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerState;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.render.RenderTarget;
import bdv.viewer.render.awt.BufferedImageRenderResult;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.image.BufferedImage;

public class BdvProbeFPS {

    public static void acquireFrames(final int width, final int height, final int nFrames, ViewerPanel viewer) {
        final ViewerState renderState = new BasicViewerState( viewer.state().snapshot() );
        final int canvasW = viewer.getDisplay().getWidth();
        final int canvasH = viewer.getDisplay().getHeight();

        final AffineTransform3D affine = new AffineTransform3D();
        renderState.getViewerTransform( affine );
        affine.set( affine.get( 0, 3 ) - canvasW / 2.0, 0, 3 );
        affine.set( affine.get( 1, 3 ) - canvasH / 2.0, 1, 3 );
        affine.scale( ( double ) width / canvasW );
        affine.set( affine.get( 0, 3 ) + width / 2.0, 0, 3 );
        affine.set( affine.get( 1, 3 ) + height / 2.0, 1, 3 );
        renderState.setViewerTransform( affine );


        class MyTarget implements RenderTarget<BufferedImageRenderResult>
        {
            final BufferedImageRenderResult renderResult = new BufferedImageRenderResult();

            @Override
            public BufferedImageRenderResult getReusableRenderResult()
            {
                return renderResult;
            }

            @Override
            public BufferedImageRenderResult createRenderResult()
            {
                return new BufferedImageRenderResult();
            }

            @Override
            public void setRenderResult( final BufferedImageRenderResult renderResult )
            {}

            @Override
            public int getWidth()
            {
                return width;
            }

            @Override
            public int getHeight()
            {
                return height;
            }
        }
        final MyTarget target = new MyTarget();
        final MultiResolutionRenderer renderer = new MultiResolutionRenderer(
                target, () -> {}, new double[] { 1 }, 0, 1, null, false,
                viewer.getOptionValues().getAccumulateProjectorFactory(), new CacheControl.Dummy() );
        //progressWriter.setProgress( 0 );
        for ( int timepoint = 0; timepoint <= nFrames; ++timepoint )
        {
            //renderState.setCurrentTimepoint( timepoint );
            renderer.requestRepaint();
            renderer.paint( renderState );

            final BufferedImage bi = target.renderResult.getBufferedImage();
            /*if ( Prefs.showScaleBarInMovie() )
            {
                final Graphics2D g2 = bi.createGraphics();
                g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
                g2.setClip( 0, 0, width, height );
                scalebar.setViewerState( renderState );
                scalebar.paint( g2 );
            }

            ImageIO.write( bi, "png", new File( String.format( "%s/img-%03d.png", dir, timepoint ) ) );
            progressWriter.setProgress( ( double ) (timepoint - minTimepointIndex + 1) / (maxTimepointIndex - minTimepointIndex + 1) );*/
        }
    }

    public static double getStdMsPerFrame(BdvHandle bdvh) {
        int nRepetitions = 25;
        long timeIn = System.currentTimeMillis();
        BdvProbeFPS.acquireFrames(640, 480, nRepetitions, bdvh.getViewerPanel());
        long timeOut = System.currentTimeMillis();

        return ((timeOut-timeIn)/(double)nRepetitions);
    }
}
