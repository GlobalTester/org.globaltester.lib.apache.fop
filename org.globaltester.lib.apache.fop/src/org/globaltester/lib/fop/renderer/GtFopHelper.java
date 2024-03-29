package org.globaltester.lib.fop.renderer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.area.AreaTreeHandler;
import org.apache.fop.fo.FOElementMapping;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;


public class GtFopHelper {
	private static FopFactory fopFactory;

	public static FopFactory getFopFactory() {
		// check fopFactory and initialize it if not already done
		if (fopFactory == null) {
			// initialize the logging for FOP
			Logger rootLogger = Logger.getRootLogger();
			rootLogger.setLevel(Level.WARN);
			rootLogger.addAppender(new NullAppender());

			// create the fop factory
			FopFactory factory = FopFactory.newInstance();
			factory.addElementMapping(new FOElementMapping());
			fopFactory = factory;
		}

		return fopFactory;
	}

	/**
	 * Transform the given XML source in the given output file using PDF
	 * 
	 * @param src
	 * @param destFile
	 * @throws IOException 
	 * @throws PdfReportGenerationException 
	 * @throws FOPException 
	 * @throws TransformerException 
	 */
	public static void transformToPdf(Source src, File destFile, File styleSheet) throws IOException, PdfReportGenerationException {
		// output stream for destFile
		OutputStream out = new BufferedOutputStream(new FileOutputStream(
				destFile));

		try {
			
//			baseURL = baseURL.replaceAll("file:", "file://");
			//org.apache.fop.configuration.Configuration.put("baseDir",baseURL);
//			getFopFactory().setBaseURL(baseURL);
			
			FOUserAgent foUserAgent = getFopFactory().newFOUserAgent();
			if (destFile.toString().contains("%")) {
				throw new IllegalArgumentException("Path for report creation must not contain % sign");
			}
			String baseUrlString = destFile.getParentFile().toURI().toURL().toExternalForm();
			foUserAgent.setBaseURL(baseUrlString);

			

			// set up pdf renderer
			GTPDFRenderer pdfRenderer = new GTPDFRenderer();
			pdfRenderer.setUserAgent(foUserAgent);
			pdfRenderer.setUpGtDefaults(foUserAgent);
			foUserAgent.setRendererOverride(pdfRenderer);

			// set up event handler
			AreaTreeHandler handler = new AreaTreeHandler(foUserAgent,
					MimeConstants.MIME_PDF, out);
			foUserAgent.setFOEventHandlerOverride(handler);

			// create and transform
			Fop fop = getFopFactory().newFop(MimeConstants.MIME_PDF, foUserAgent, out);
			Source xslt = new StreamSource(styleSheet);
			TransformerFactory factory = TransformerFactory.newInstance(); //NOSONAR
			Transformer transformer = factory.newTransformer(xslt);
			transformer.setParameter("imgpath", destFile.getParent());

			Result res = new SAXResult(fop.getDefaultHandler());
			transformer.transform(src, res);
		} catch (Exception e) {
			throw new PdfReportGenerationException(e);
		} finally {
			out.close();
		}
	}
}
