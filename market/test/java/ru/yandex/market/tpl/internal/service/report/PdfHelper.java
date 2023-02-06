package ru.yandex.market.tpl.internal.service.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.assertj.core.api.Assertions.assertThat;

@UtilityClass
public class PdfHelper {

    public void assertPdf(
            final InputStream is, String fileName, String truncateAfter, Map<String, String> replacements
    ) throws IOException {
        final PDFParser parser = new PDFParser(new RandomAccessBuffer(is));
        parser.parse();

        final PDDocument document = parser.getPDDocument();

        final PDFTextStripper stripper = new PDFTextStripper();
        final String text = stripper.getText(document);
        final String truncatedText = text.substring(0, text.lastIndexOf(truncateAfter) + truncateAfter.length());

        final InputStream expectedIS = getSystemResourceAsStream("report/" + fileName);
        String expectedText = IOUtils.toString(expectedIS, StandardCharsets.UTF_8);

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            expectedText = expectedText.replace(entry.getKey(), entry.getValue());
        }

        assertThat(truncatedText).isEqualToIgnoringWhitespace(expectedText);
    }


    public void writeTextFile(String truncatedText) throws IOException {
        FileUtils.writeStringToFile(new File(""), truncatedText, StandardCharsets.UTF_8);
    }

    public void writePdfFile(InputStream is) throws IOException {
        IOUtils.copy(is, new FileOutputStream(new File("file.pdf")));
    }
}
