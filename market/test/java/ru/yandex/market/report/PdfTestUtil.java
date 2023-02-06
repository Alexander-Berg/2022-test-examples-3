package ru.yandex.market.report;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Assertions;

/**
 * Утилита для проверки корректности pdf файлов.
 *
 * @author stani on 23.05.18.
 */
public final class PdfTestUtil {

    private PdfTestUtil() {
        throw new UnsupportedOperationException();
    }

    public static void assertPdfTextEqualsFile(
            byte[] pdfFileBytes, String expectedText, Integer numberOfPages) throws IOException {
        PDDocument document = PDDocument.load(pdfFileBytes);
        if (numberOfPages != null) {
            Assertions.assertEquals(numberOfPages.intValue(), document.getNumberOfPages());
        }
        expectedText = expectedText.replace(
                "${sysdate}", new SimpleDateFormat("dd.MM.yyyy").format(Date.from(Instant.now())));
        Assertions.assertEquals(expectedText, new PDFTextStripper().getText(document));
    }
}
