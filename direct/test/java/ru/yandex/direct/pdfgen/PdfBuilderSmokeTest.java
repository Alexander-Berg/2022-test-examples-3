package ru.yandex.direct.pdfgen;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class PdfBuilderSmokeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfBuilderSmokeTest.class);

    @Test
    public void buildPdfTest() throws IOException {
        PdfBuilder builder = new PdfBuilder();

        TemplateParameters parameters = new TemplateParameters(2228274L, "мир");
        byte[] pdfContent = builder.buildPdf("test.fo.thyme", parameters);
        assertThat(pdfContent.length, greaterThan(0));

        // for manual tests only
        //writeTestFile(pdfContent);
    }

    private void writeTestFile(byte[] pdfContent) throws IOException {
        String filename = System.getProperty("user.dir") + File.separator + "test.pdf";
        FileUtils.writeByteArrayToFile(new File(filename), pdfContent);
        LOGGER.info("output file: {}", filename);
    }

    public static class TemplateParameters {
        private final long id;
        private final String recipient;

        TemplateParameters(long id, String recipient) {
            this.id = id;
            this.recipient = recipient;
        }

        public long getId() {
            return id;
        }

        public String getRecipient() {
            return recipient;
        }
    }
}
