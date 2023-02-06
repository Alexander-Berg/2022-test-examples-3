package ru.yandex.market.pdf.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.yandex.market.pdf.generator.exceptions.InvalidTemplateException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class PdfGenerationServiceTest {

    private OutputStream os;
    private File resultFile;


    @AfterEach
    public void closeOutputStream() throws IOException {
        os.close();
    }

    @Test
    public void basicScenario(@TempDir Path tempDir) throws FileNotFoundException {
        resultFile = tempDir.resolve("result.pdf").toFile();
        os = new FileOutputStream(resultFile);

        PdfGeneratorService service = new PdfGeneratorServiceImpl();
        service.generatePdf("samples.simple", "{\"name\":\"Thomas\"}", os);
        assertThat(resultFile.length(), greaterThan(0L));
    }

    @Test
    public void wrongTemplate(@TempDir Path tempDir) throws FileNotFoundException {
        resultFile = tempDir.resolve("result.pdf").toFile();
        os = new FileOutputStream(resultFile);

        Assertions.assertThrows(InvalidTemplateException.class, () -> {
            PdfGeneratorService service = new PdfGeneratorServiceImpl();
            service.generatePdf("samples.simple2", "{\"name\":\"Thomas\"}", os);
        });
    }

    @Test
    public void nullTemplate(@TempDir Path tempDir) throws FileNotFoundException {
        resultFile = tempDir.resolve("result.pdf").toFile();
        os = new FileOutputStream(resultFile);

        Assertions.assertThrows(InvalidTemplateException.class, () -> {
            PdfGeneratorService service = new PdfGeneratorServiceImpl();
            service.generatePdf(null, "{\"name\":\"Thomas\"}", os);
        });
    }

}
