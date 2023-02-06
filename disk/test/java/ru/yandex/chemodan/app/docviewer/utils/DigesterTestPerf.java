package ru.yandex.chemodan.app.docviewer.utils;

import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.TestManager;
import ru.yandex.misc.io.ClassPathResourceInputStreamSource;
import ru.yandex.misc.io.ClassPathResourceInputStreamSourceWithLength;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.time.Stopwatch;

/**
 * @author ssytnik
 */
public class DigesterTestPerf {
    private static final int PASSES = 10;


    @Test
    public void testFileIdCalcPerf() {
        String localPath = "test/pdf/render/mobilereglament.pdf";

        doTestFileIdCalcPerf("ISS", new ClassPathResourceInputStreamSource(
                TestManager.class, localPath));

        doTestFileIdCalcPerf("ISS with length", new ClassPathResourceInputStreamSourceWithLength(
                TestManager.class, localPath));

        doTestFileIdCalcPerf("File", ClassLoaderUtils.fileForResource(TestManager.class, localPath));

    }

    private void doTestFileIdCalcPerf(String testName, InputStreamSource source) {
        Digester digester = new Digester();
        digester.setDigestAlgorithm("SHA-512");

        Stopwatch watch = Stopwatch.createAndStart();
        for (int i = 1; i <= PASSES; i++) {
            digester.calculateDigestId(source);
        }
        watch.stop();

        System.out.println(testName + ": " + (watch.millisDuration() / PASSES) + "ms per pass");
    }

}
