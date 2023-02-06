package ru.yandex.market.pdf.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.yandex.market.pdf.generator.exceptions.InvalidTemplateException;

class PdfGenerationServiceTest extends BaseTest {

    private OutputStream os;
    @TempDir
    File tempFile;
    private File resultFile;

    @BeforeEach
    void createOutputStream() throws IOException {
        resultFile = new File(tempFile, "result.pdf");
        os = new FileOutputStream(resultFile);
    }

    @AfterEach
    void closeOutputStream() throws IOException {
        os.close();
    }

    @Test
    void basicScenario() {
        PdfGeneratorService service = new PdfGeneratorServiceImpl();
        service.generatePdf("samples.simple", "{\"name\":\"Thomas\"}", os);
        softly.assertThat(resultFile.length()).isGreaterThan(0L);
    }

    @Test
    void wrongTemplate() {
        PdfGeneratorService service = new PdfGeneratorServiceImpl();
        softly.assertThatThrownBy(
            () -> service.generatePdf("samples.simple2", "{\"name\":\"Thomas\"}", os)
        ).isInstanceOf(InvalidTemplateException.class);
    }

    @Test
    void nullTemplate() {
        PdfGeneratorService service = new PdfGeneratorServiceImpl();
        softly.assertThatThrownBy(
            () -> service.generatePdf(null, "{\"name\":\"Thomas\"}", os)
        ).isInstanceOf(InvalidTemplateException.class);
    }

}
