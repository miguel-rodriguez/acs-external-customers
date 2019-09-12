package com.alfresco.support.alfrescoCMIS.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
 
import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Docx2PdfConversion {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public void convert(String inFile, String outFile) {
		 try (InputStream is = new FileInputStream(new File(inFile));
			 OutputStream out = new FileOutputStream(new File(outFile));) {
			 long start = System.currentTimeMillis();
			 // 1) Load DOCX into XWPFDocument
			 XWPFDocument document = new XWPFDocument(is);
			 // 2) Prepare Pdf options
			 PdfOptions options = PdfOptions.create();
			 // 3) Convert XWPFDocument to Pdf
			 PdfConverter.getInstance().convert(document, out, options);
			 logger.debug(inFile + " was converted to a PDF file in :: "
			 + (System.currentTimeMillis() - start) + " milli seconds");
		 } catch (Throwable e) {
		 	e.printStackTrace();
		 }
	 }
}
