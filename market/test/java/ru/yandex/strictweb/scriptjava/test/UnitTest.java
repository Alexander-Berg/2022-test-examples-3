package ru.yandex.strictweb.scriptjava.test;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.strictweb.scriptjava.base.ExtendsNative;
import ru.yandex.strictweb.scriptjava.base.Native;
import ru.yandex.strictweb.scriptjava.base.ajax.Ajax;
import ru.yandex.strictweb.scriptjava.compiler.Compiler;
import ru.yandex.strictweb.scriptjava.plugins.AjaxServiceHelperCompilerPlugin;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileReader;

@Native
@ExtendsNative("String")
public class UnitTest {

    private static final Logger log = LoggerFactory.getLogger(UnitTest.class);

    public static void println(String msg) {
        log.info(msg);
    }

    @Test
    public void testCollections() throws Exception {
        boolean result = new TestCollections().test();
        Assert.assertTrue(result);
        testSingle(TestCollections.class);
    }

    @Test
    public void testTestObjectToXml() throws Exception {
        testSingle(TestObjectToXml.class);
    }

    private void testSingle(Class<?> testClass) throws Exception {
        log.info("Testing class " + testClass.getCanonicalName());

        Compiler compiler = new Compiler("");
        Ajax.prepareCompiler(compiler);

        File tempFile = File.createTempFile(testClass.getSimpleName(), ".js", new File("."));
        tempFile.deleteOnExit();

        compiler.addPlugin(new AjaxServiceHelperCompilerPlugin())
                .parseClass(this.getClass())
                .parseClass(testClass)
                .compileAndSave(tempFile.getCanonicalPath());

        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine jsEngine = mgr.getEngineByName("JavaScript");
        jsEngine.put("log", log);
        jsEngine.put("out", System.out);
        jsEngine.put("window", null);
        jsEngine.put("document", null);
        jsEngine.eval(
                "function UnitTest() {}; " +
                        "UnitTest.println = function(msg) { out.println(msg); }"
        );

        try (FileReader reader = new FileReader(tempFile)) {
            jsEngine.eval(reader);
            jsEngine.eval("UnitTest.println('Hello')");
            Object jsTestResult = jsEngine.eval("new " + testClass.getSimpleName() + "().test()");
            Assert.assertTrue("js return != true", (Boolean) jsTestResult);
        }
    }

}
