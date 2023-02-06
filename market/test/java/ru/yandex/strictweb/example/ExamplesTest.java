package ru.yandex.strictweb.example;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.strictweb.example.helloworld.HelloWorldCompile;
import ru.yandex.strictweb.example.sampleajax.SampleAjaxCompile;
import ru.yandex.strictweb.example.simples.Compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author amaslak
 */
public class ExamplesTest {

    private static final Logger log = LoggerFactory.getLogger(ExamplesTest.class);

    private Path tempDirectory;

    @Before
    public void prepare() throws IOException {
        tempDirectory = Files.createTempDirectory(Paths.get("."), "www-root");
        tempDirectory.toFile().deleteOnExit();
    }

    @Test
    public void testCompiler() throws Exception {
        Path jsPath = tempDirectory.resolve(Paths.get(Compiler.JS));
        Compiler.compile(jsPath);
    }

    @Test
    public void testSampleAjaxCompile() throws Exception {
        Path jsPath = tempDirectory.resolve(Paths.get(SampleAjaxCompile.JS));
        SampleAjaxCompile.compile(jsPath);
    }

    @Test
    public void testHelloWorldCompile() throws Exception {
        Path jsPath = tempDirectory.resolve(Paths.get(HelloWorldCompile.JS));
        HelloWorldCompile.compile(jsPath);
    }

}
