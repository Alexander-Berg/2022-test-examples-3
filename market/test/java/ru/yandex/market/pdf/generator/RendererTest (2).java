package ru.yandex.market.pdf.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.yandex.market.pdf.generator.exceptions.TemplateRenderingException;

class RendererTest extends BaseTest {

    @TempDir
    File tempFile;

    private OutputStream os;
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
    void renderSimpleHtml() {
        new PdfRenderer("samples/simple").render("<html>HELLO!</html>", os);
        softly.assertThat(resultFile.length()).isGreaterThan("HELLO!".length());
    }

    @Test
    void tryRenderNotXhtml() {
        softly.assertThatThrownBy(
            () -> new PdfRenderer("samples/simple").render("<html><p>Hello, World!</html>", os)
        ).isInstanceOf(TemplateRenderingException.class);
    }

    @Test
    void renderToClosedOutputStream() throws IOException {
        os.close();
        softly.assertThatThrownBy(
            () -> new PdfRenderer("samples/simple").render("<html>HELLO!</html>", os)
        ).isInstanceOf(TemplateRenderingException.class);
    }

    @Test
    void passNullOutputStream() {
        softly.assertThatThrownBy(
            () -> new PdfRenderer("samples/simple").render("<html>HELLO!</html>", null)
        ).isInstanceOf(TemplateRenderingException.class);
    }

    @Test
    void passNullHtml() {
        softly.assertThatThrownBy(
            () -> new PdfRenderer("samples/simple").render(null, os)
        ).isInstanceOf(TemplateRenderingException.class);
    }

}
