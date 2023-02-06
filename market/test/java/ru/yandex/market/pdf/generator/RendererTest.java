package ru.yandex.market.pdf.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.yandex.market.pdf.generator.exceptions.TemplateRenderingException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class RendererTest {


    private OutputStream os;
    private File resultFile;


    @AfterEach
    public void closeOutputStream() throws IOException {
        os.close();
    }

    @Test
    public void renderSimpleHtml(@TempDir Path tempDir) throws FileNotFoundException {
        resultFile = tempDir.resolve("result.pdf").toFile();
        os = new FileOutputStream(resultFile);

        new PdfRenderer("samples/simple").render("<html>HELLO!</html>", os);
        assertThat(resultFile.length(), greaterThan((long) "HELLO!".length()));
    }

    @Test
    public void tryRenderNotXhtml(@TempDir Path tempDir) throws FileNotFoundException {
        resultFile = tempDir.resolve("result.pdf").toFile();
        os = new FileOutputStream(resultFile);

        Assertions.assertThrows(TemplateRenderingException.class, () -> {
            new PdfRenderer("samples/simple").render("<html><p>Hello, World!</html>", os);
        });
    }

    @Test
    public void renderToClosedOutputStream(@TempDir Path tempDir) throws FileNotFoundException {
        resultFile = tempDir.resolve("result.pdf").toFile();
        os = new FileOutputStream(resultFile);

        Assertions.assertThrows(TemplateRenderingException.class, () -> {
            os.close();
            new PdfRenderer("samples/simple").render("<html>HELLO!</html>", os);
        });
    }

    @Test
    public void passNullOutputStream(@TempDir Path tempDir) throws FileNotFoundException {
        resultFile = tempDir.resolve("result.pdf").toFile();
        os = new FileOutputStream(resultFile);

        Assertions.assertThrows(TemplateRenderingException.class, () -> {
            new PdfRenderer("samples/simple").render("<html>HELLO!</html>", null);
        });
    }

    @Test
    public void passNullHtml(@TempDir Path tempDir) throws FileNotFoundException {
        resultFile = tempDir.resolve("result.pdf").toFile();
        os = new FileOutputStream(resultFile);

        Assertions.assertThrows(TemplateRenderingException.class, () -> {
            new PdfRenderer("samples/simple").render(null, os);
        });
    }

}
