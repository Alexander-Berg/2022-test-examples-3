package ru.yandex.chemodan.uploader.web;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;

/**
 * @author akirakozov
 */
public class WebGeneratePreviewMultiThreadTest extends TestBase {

    // Test for CHEMODAN-18209
    // TODO: test local web-server instead of kladun1e.dev.yandex.net and remove ignore
    @Ignore
    @Test
    public void multiThreadTest() throws InterruptedException {
        int nThreads = 100;
        final CountDownLatch endGate = new CountDownLatch(nThreads);
        final CountDownLatch startGate = new CountDownLatch(1);
        final AtomicInteger doneCount = new AtomicInteger();

        for (int i = 0; i < nThreads; i++) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        startGate.await();

                        BufferedImage img = ImageIO.read(new URL(
                                "http://kladun1e.dev.yandex.net:32450/generate-preview?api=0.2&"
                                + "mulca-id=1000004.yadisk:16011578.857919227202131246341959327055&size=200x200&crop=true"));
                        Assert.equals(img.getWidth(), 200);
                        Assert.equals(img.getHeight(), 200);
                        doneCount.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        endGate.countDown();
                    }
                }
            };
            t.start();
        }

        startGate.countDown();
        endGate.await();
        Assert.equals(doneCount.get(), nThreads);
    }
}
