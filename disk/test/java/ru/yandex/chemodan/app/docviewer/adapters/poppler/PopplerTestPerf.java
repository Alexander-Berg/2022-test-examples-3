package ru.yandex.chemodan.app.docviewer.adapters.poppler;

import java.io.PrintStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.docviewer.AbstractSpringAwareTest;
import ru.yandex.chemodan.app.docviewer.utils.pdf.PdfUtils;
import ru.yandex.misc.io.IoFunction1V;
import ru.yandex.misc.io.file.File2;

/**
 * @author ssytnik
 */
public class PopplerTestPerf extends AbstractSpringAwareTest {

    @Autowired
    PopplerAdapter popplerAdapter;


    @Test
    public void pdfinfoVsPdfboxLoad() throws Exception {

        try (PrintStream out = new PrintStream(File2.valueOf("~/popplerTestPerf.txt").getFile())) {
            long totalPdfInfoMs = 0L;
            long totalPdfBoxMs = 0L;
            int count = 0;
            int pdfBoxWasQuickerCount = 0;

            File2 pdfFolder = File2.valueOf("~/pdf");
            ListF<File2> pdfFiles = pdfFolder.listRegularFiles(); //.take(10);

            for (File2 pdfFile : pdfFiles) {
                long ts1 = System.currentTimeMillis();
                try {
                    pdfInfo(pdfFile);
                } catch (Exception e) {
                    System.out.println("pdfinfo error: " + e);
                }

                long ts2 = System.currentTimeMillis();
                try {
                    pdfBoxLoad(pdfFile);
                } catch (Exception e) {
                    System.out.println("pdfbox error: " + e);
                }

                long ts3 = System.currentTimeMillis();
                count++;

                long curPdfInfoMs = ts2 - ts1;
                totalPdfInfoMs += curPdfInfoMs;

                long curPdfBoxMs = ts3 - ts2;
                totalPdfBoxMs += curPdfBoxMs;

                if (curPdfInfoMs > curPdfBoxMs) {
                    pdfBoxWasQuickerCount++;
                }

                out.println(String.format("%s (length = %.2f Mb): pdfInfo = %d ms, pdfBox = %d ms",
                        pdfFile, pdfFile.length() / 1000000.0f, curPdfInfoMs, curPdfBoxMs));
            }

            if (count > 0) {
                out.println(String.format("average: pdfinfo = %d ms, pdfbox = %d ms",
                        totalPdfInfoMs / count, totalPdfBoxMs / count));
                out.println(String.format("pdfbox was quicker in %d of %d cases", pdfBoxWasQuickerCount, count));
            }

        }
    }


    private void pdfInfo(File2 pdfFile) {
        popplerAdapter.getSinglePageInfo(pdfFile, 1);
    }

    private void pdfBoxLoad(File2 pdfFile) {
        PdfUtils.withExistingDocument(pdfFile, true, (IoFunction1V<PDDocument>) document -> {
            PdfUtils.getSinglePageInfo(document, 1);
        });
    }

}
