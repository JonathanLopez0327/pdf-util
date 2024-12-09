package com.testautomationguru.utility;

/*
 * Copyright [2015] [www.testautomationguru.com]

Licensed under the Apache License, Version 2.0 (the "License")
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * <h1>PDF Utility</h1>
 * A simple pdf utility using apache pdfbox to get the text,
 * compare files using plain text or pixel by pixel comparison, extract all the images from the pdf
 *
 * @author www.testautomationguru.com
 * @version 1.0
 * @since 2015-06-13
 */

public class PDFUtil {

    private static final Logger logger = LogManager.getLogger(PDFUtil.class);
    private String imageDestinationPath;
    private boolean bTrimWhiteSpace;
    private boolean bHighlightPdfDifference;
    private Color imgColor;
    private PDFTextStripper stripper;
    private boolean bCompareAllPages;
    private CompareMode compareMode;
    private String[] excludePattern;
    private int startPage = 1;
    private int endPage = -1;

    public PDFUtil() {
        this.bTrimWhiteSpace = true;
        this.bHighlightPdfDifference = false;
        this.imgColor = Color.MAGENTA;
        this.bCompareAllPages = false;
        this.compareMode = CompareMode.TEXT_MODE;
        Configurator.setLevel(logger.getName(), org.apache.logging.log4j.Level.OFF);
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
    }

    public void enableLog() {
        Configurator.setLevel(logger.getName(), org.apache.logging.log4j.Level.INFO);
    }

    public void setCompareMode(CompareMode mode) {
        this.compareMode = mode;
    }

    public CompareMode getCompareMode() {
        return this.compareMode;
    }

    public void setLogLevel(org.apache.logging.log4j.Level level) {
        Configurator.setLevel(logger.getName(), level);
    }

    public void trimWhiteSpace(boolean flag) {
        this.bTrimWhiteSpace = flag;
    }

    public String getImageDestinationPath() {
        return this.imageDestinationPath;
    }

    public void setImageDestinationPath(String path) {
        this.imageDestinationPath = path;
    }

    public void highlightPdfDifference(boolean flag) {
        this.bHighlightPdfDifference = flag;
    }

    public void highlightPdfDifference(Color colorCode) {
        this.bHighlightPdfDifference = true;
        this.imgColor = colorCode;
    }

    public void compareAllPages(boolean flag) {
        this.bCompareAllPages = flag;
    }

    public void useStripper(PDFTextStripper stripper) {
        this.stripper = stripper;
    }

    public static PDDocument loadDocument(String filePath) throws IOException {
        return Loader.loadPDF(new File(filePath));
    }

    public int getPageCount(String file) throws IOException {
        logger.info("file : {}", file);
        PDDocument doc = loadDocument(file);
        int pageCount = doc.getNumberOfPages();
        logger.info("pageCount : {}", pageCount);
        doc.close();
        return pageCount;
    }

    public String getText(String file) throws IOException {
        return this.getPDFText(file, -1, -1);
    }

    public String getText(String file, int startPage) throws IOException {
        return this.getPDFText(file, startPage, -1);
    }

    public String getText(String file, int startPage, int endPage) throws IOException {
        return this.getPDFText(file, startPage, endPage);
    }

    private String getPDFText(String file, int startPage, int endPage) throws IOException {
        logger.info("file : {}", file);
        logger.info("startPage : {}", startPage);
        logger.info("endPage : {}", endPage);

        PDDocument doc = loadDocument(file);

        PDFTextStripper localStripper = new PDFTextStripper();
        if (null != this.stripper) {
            localStripper = this.stripper;
        }

        this.updateStartAndEndPages(file, startPage, endPage);
        localStripper.setStartPage(this.startPage);
        localStripper.setEndPage(this.endPage);

        String txt = localStripper.getText(doc);
        logger.info("PDF Text before trimming : {}", txt);
        if (this.bTrimWhiteSpace) {
            txt = txt.trim().replaceAll("\\s+", " ").trim();
            logger.info("PDF Text after trimming : {}", txt);
        }

        doc.close();
        return txt;
    }

    public void excludeText(String... regexs) {
        this.excludePattern = regexs;
    }

    public boolean compare(String file1, String file2) throws IOException {
        return this.comparePdfFiles(file1, file2, -1, -1);
    }

    public boolean compare(String file1, String file2, int startPage, int endPage) throws IOException {
        return this.comparePdfFiles(file1, file2, startPage, endPage);
    }

    public boolean compare(String file1, String file2, int startPage) throws IOException {
        return this.comparePdfFiles(file1, file2, startPage, -1);
    }

    private boolean comparePdfFiles(String file1, String file2, int startPage, int endPage) throws IOException {
        if (CompareMode.TEXT_MODE == this.compareMode)
            return comparePdfFilesWithTextMode(file1, file2, startPage, endPage);
        else
            return comparePdfByImage(file1, file2, startPage, endPage);
    }

    private boolean comparePdfFilesWithTextMode(String file1, String file2, int startPage, int endPage) throws IOException {
        String file1Txt = this.getPDFText(file1, startPage, endPage).trim();
        String file2Txt = this.getPDFText(file2, startPage, endPage).trim();

        if (null != this.excludePattern && this.excludePattern.length > 0) {
            for (String pattern : this.excludePattern) {
                file1Txt = file1Txt.replaceAll(pattern, "");
                file2Txt = file2Txt.replaceAll(pattern, "");
            }
        }

        logger.info("File 1 Txt : {}", file1Txt);
        logger.info("File 2 Txt : {}", file2Txt);

        boolean result = file1Txt.equalsIgnoreCase(file2Txt);

        if (!result) {
            logger.warn("PDF content does not match");
        }

        return result;
    }

    public List<String> savePdfAsImage(String file, int startPage) throws IOException {
        return this.saveAsImage(file, startPage, -1);
    }

    public List<String> savePdfAsImage(String file, int startPage, int endPage) throws IOException {
        return this.saveAsImage(file, startPage, endPage);
    }

    public List<String> savePdfAsImage(String file) throws IOException {
        return this.saveAsImage(file, -1, -1);
    }

    private List<String> saveAsImage(String file, int startPage, int endPage) throws IOException {
        logger.info("file : {}", file);
        logger.info("startPage : {}", startPage);
        logger.info("endPage : {}", endPage);

        ArrayList<String> imgNames = new ArrayList<>();

        try {
            File sourceFile = new File(file);
            this.createImageDestinationDirectory(file);
            this.updateStartAndEndPages(file, startPage, endPage);

            String fileName = sourceFile.getName().replace(".pdf", "");

            PDDocument document = loadDocument(file);
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int iPage = this.startPage - 1; iPage < this.endPage; iPage++) {
                logger.info("Page No : {}", (iPage + 1));
                String fname = this.imageDestinationPath + fileName + "_" + (iPage + 1) + ".png";
                BufferedImage image = pdfRenderer.renderImageWithDPI(iPage, 300, ImageType.RGB);
                ImageIOUtil.writeImage(image, fname, 300);
                imgNames.add(fname);
                logger.info("PDF Page saved as image : {}", fname);
            }
            document.close();
        } catch (Exception e) {
            logger.error("Error while saving pdf as image : {}", e.getMessage());
        }
        return imgNames;
    }

    public boolean compare(String file1, String file2, int startPage, int endPage, boolean highlightImageDifferences, boolean showAllDifferences) throws IOException {
        this.compareMode = CompareMode.VISUAL_MODE;
        this.bHighlightPdfDifference = highlightImageDifferences;
        this.bCompareAllPages = showAllDifferences;
        return this.comparePdfByImage(file1, file2, startPage, endPage);
    }

    private boolean comparePdfByImage(String file1, String file2, int startPage, int endPage) throws IOException {
        logger.info("file1 : {}", file1);
        logger.info("file2 : {}", file2);

        int pgCount1 = this.getPageCount(file1);
        int pgCount2 = this.getPageCount(file2);

        if (pgCount1 != pgCount2) {
            logger.warn("files page counts do not match - returning false");
            return false;
        }

        if (this.bHighlightPdfDifference)
            this.createImageDestinationDirectory(file2);

        this.updateStartAndEndPages(file1, startPage, endPage);

        return this.convertToImageAndCompare(file1, file2, this.startPage, this.endPage);
    }

    private boolean convertToImageAndCompare(String file1, String file2, int startPage, int endPage) throws IOException {
        boolean result = true;

        try (PDDocument doc1 = loadDocument(file1); PDDocument doc2 = loadDocument(file2)) {
            PDFRenderer pdfRenderer1 = new PDFRenderer(doc1);
            PDFRenderer pdfRenderer2 = new PDFRenderer(doc2);

            for (int iPage = startPage - 1; iPage < endPage; iPage++) {
                String fileName = new File(file1).getName().replace(".pdf", "_") + (iPage + 1);
                fileName = this.getImageDestinationPath() + "/" + fileName + "_diff.png";

                logger.info("Comparing Page No : {}", (iPage + 1));
                BufferedImage image1 = pdfRenderer1.renderImageWithDPI(iPage, 300, ImageType.RGB);
                BufferedImage image2 = pdfRenderer2.renderImageWithDPI(iPage, 300, ImageType.RGB);
                result = ImageUtil.compareAndHighlight(image1, image2, fileName, this.bHighlightPdfDifference, this.imgColor.getRGB()) && result;
                if (!this.bCompareAllPages && !result) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error while comparing pdf files : {}", e.getMessage());
        }
        return result;
    }

    public List<String> extractImages(String file, int startPage) {
        return this.extractImagesFromPdf(file, startPage, -1);
    }

    public List<String> extractImages(String file, int startPage, int endPage) {
        return this.extractImagesFromPdf(file, startPage, endPage);
    }

    public List<String> extractImages(String file) {
        return this.extractImagesFromPdf(file, -1, -1);
    }

    private List<String> extractImagesFromPdf(String file, int startPage, int endPage) {
        logger.info("file : {}", file);
        logger.info("startPage : {}", startPage);
        logger.info("endPage : {}", endPage);

        ArrayList<String> imgNames = new ArrayList<>();
        boolean bImageFound = false;
        try {
            this.createImageDestinationDirectory(file);
            String fileName = this.getFileName(file).replace(".pdf", "_resource");

            PDDocument document = loadDocument(file);
            PDPageTree list = document.getPages();

            this.updateStartAndEndPages(file, startPage, endPage);

            int totalImages = 1;
            for (int iPage = this.startPage - 1; iPage < this.endPage; iPage++) {
                logger.info("Page No : {}", (iPage + 1));
                PDResources pdResources = list.get(iPage).getResources();
                for (COSName c : pdResources.getXObjectNames()) {
                    PDXObject o = pdResources.getXObject(c);
                    if (o instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                        bImageFound = true;
                        String fname = this.imageDestinationPath + "/" + fileName + "_" + totalImages + ".png";
                        ImageIO.write(((org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) o).getImage(), "png", new File(fname));
                        imgNames.add(fname);
                        totalImages++;
                    }
                }
            }
            document.close();
            if (bImageFound)
                logger.info("Images are saved @ {}", this.imageDestinationPath);
            else
                logger.info("No images were found in the PDF");
        } catch (Exception e) {
            logger.error("Error while extracting images from pdf : {}", e.getMessage());
        }
        return imgNames;
    }

    private void createImageDestinationDirectory(String file) throws IOException {
        if (null == this.imageDestinationPath) {
            File sourceFile = new File(file);
            String destinationDir = sourceFile.getParent() + "/temp/";
            this.imageDestinationPath = destinationDir;
            this.createFolder(destinationDir);
        }
    }

    private boolean createFolder(String dir) throws IOException {
        FileUtils.deleteDirectory(new File(dir));
        return new File(dir).mkdir();
    }

    private String getFileName(String file) {
        return new File(file).getName();
    }

    private void updateStartAndEndPages(String file, int start, int end) throws IOException {
        PDDocument document = loadDocument(file);
        int pagecount = document.getNumberOfPages();
        logger.info("Page Count : {}", pagecount);
        logger.info("Given start page: {}", start);
        logger.info("Given end page: {}", end);

        if ((start > 0 && start <= pagecount)) {
            this.startPage = start;
        } else {
            this.startPage = 1;
        }
        if ((end > 0 && end >= start && end <= pagecount)) {
            this.endPage = end;
        } else {
            this.endPage = pagecount;
        }
        document.close();
        logger.info("Updated start page: {}", this.startPage);
        logger.info("Updated end page: {}", this.endPage);
    }
}