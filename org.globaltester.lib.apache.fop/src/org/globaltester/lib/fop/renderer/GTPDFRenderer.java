package org.globaltester.lib.fop.renderer;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.datatypes.URISpecification;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.pdf.PDFXObject;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.pdf.PDFImageHandler;
import org.apache.fop.render.pdf.PDFImageHandlerRegistry;
import org.apache.fop.render.pdf.PDFImageHandlerRenderedImage;
import org.apache.fop.render.pdf.PDFRenderer;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.ImageInfo;
import org.apache.xmlgraphics.image.loader.ImageManager;
import org.apache.xmlgraphics.image.loader.ImageSessionContext;
import org.apache.xmlgraphics.image.loader.impl.PreloaderGIF;
import org.apache.xmlgraphics.image.loader.impl.imageio.ImageLoaderFactoryImageIO;
import org.apache.xmlgraphics.image.loader.spi.ImageImplRegistry;
import org.apache.xmlgraphics.image.loader.util.ImageUtil;

/**
 * Override PDFRenderer class to make imageHandlerRegistry visible to allow
 * adding of ImageHandlers by external code
 * 
 * @author Alexander May
 * 
 */
public class GTPDFRenderer extends PDFRenderer {

	/** Image handler registry */
	private PDFImageHandlerRegistry imageHandlerRegistry = new PDFImageHandlerRegistry();

	public PDFImageHandlerRegistry getImageHandlerRegistry() {
		return imageHandlerRegistry;
	}
	
	/**
     * Adds a PDF XObject (a bitmap or form) to the PDF that will later be referenced.
     * @param uri URL of the bitmap
     * @param pos Position of the bitmap
     * @param foreignAttributes foreign attributes associated with the image
     */
	@SuppressWarnings("rawtypes")
	@Override
	protected void putImage(String uri, Rectangle2D pos, Map foreignAttributes) {
        Rectangle posInt = new Rectangle(
                (int)pos.getX(),
                (int)pos.getY(),
                (int)pos.getWidth(),
                (int)pos.getHeight());

        uri = URISpecification.getURL(uri);
        PDFXObject xobject = pdfDoc.getXObject(uri);
        if (xobject != null) {
            float w = (float) pos.getWidth() / 1000f;
            float h = (float) pos.getHeight() / 1000f;
            placeImage((float)pos.getX() / 1000f,
                       (float)pos.getY() / 1000f, w, h, xobject);
            return;
        }
        Point origin = new Point(currentIPPosition, currentBPPosition);
        int x = origin.x + posInt.x;
        int y = origin.y + posInt.y;

        ImageManager manager = getUserAgent().getFactory().getImageManager();
        ImageInfo info = null;
        try {
            ImageSessionContext sessionContext = getUserAgent().getImageSessionContext();
            info = manager.getImageInfo(uri, sessionContext);
            
            
            
            Map hints = ImageUtil.getDefaultHints(sessionContext);
            org.apache.xmlgraphics.image.loader.Image img = manager.getImage(
                        info, imageHandlerRegistry.getSupportedFlavors(), hints, sessionContext);
            
            //First check for a dynamically registered handler
            PDFImageHandler handler = imageHandlerRegistry.getHandler(img);
            if (handler != null) {
            // TODO #833 Use proper logging mechanism
//                if (log.isDebugEnabled()) {
//                    log.debug("Using PDFImageHandler: " + handler.getClass().getName());
//                }
                try {
                    RendererContext context = createRendererContext(
                            x, y, posInt.width, posInt.height, foreignAttributes);
                    handler.generateImage(context, img, origin, posInt);
                } catch (IOException ioe) {
                	// TODO #833 Use proper logging mechanism
                	ioe.printStackTrace();
//                  log.error("I/O error while handling image: " + info, ioe);
                    return;
                }
            } else {
                throw new UnsupportedOperationException(
                        "No PDFImageHandler available for image: "
                            + info + " (" + img.getClass().getName() + ")");
            }
        } catch (ImageException ie) {
        // TODO #833 Use proper logging mechanism
        	ie.printStackTrace();
//            log.error("Error while processing image: "
//                    + (info != null ? info.toString() : uri), ie);
        } catch (FileNotFoundException fnfe) {
        // TODO #833 Use proper logging mechanism
        	fnfe.printStackTrace();
//            log.error(fnfe.getMessage());
        } catch (IOException ioe) {
        // TODO #833 Use proper logging mechanism
        	ioe.printStackTrace();
//            log.error("I/O error while processing image: "
//                    + (info != null ? info.toString() : uri), ioe);
        }

        // output new data
        try {
            this.pdfDoc.output(ostream);
		} catch (IOException ioe) {
			// TODO #833 Use proper logging mechanism
			ioe.printStackTrace();
//			log.error("I/O error while processing image: "
//					+ (info != null ? info.toString() : uri), ioe);
		}
    }
    
	public void setUpGtDefaults(FOUserAgent foUserAgent){
    	//set up inclusion of GIF images
		ImageImplRegistry imageRegistry = foUserAgent.getFactory()
				.getImageManager().getRegistry();
		imageRegistry.registerPreloader(new PreloaderGIF());
		imageRegistry.registerLoaderFactory(new ImageLoaderFactoryImageIO());
		this.getImageHandlerRegistry().addHandler(
				new PDFImageHandlerRenderedImage());

		//set up the font info
		FontInfo fontInfo = new FontInfo();
		this.setupFontInfo(fontInfo);
    }
}
