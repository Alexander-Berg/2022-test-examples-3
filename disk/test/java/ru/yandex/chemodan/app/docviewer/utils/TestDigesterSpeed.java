package ru.yandex.chemodan.app.docviewer.utils;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author vlsergey
 * @author akirakozov
 */
public class TestDigesterSpeed {

    private static final Logger logger = LoggerFactory.getLogger(TestDigesterSpeed.class);

    private void testCalculateDigest(final String algorithm) {
        final UrlInputStreamSource source = new UrlInputStreamSource(
                TestResources.Adobe_Acrobat_1_5_114p);

        for (int i = 0; i < 100; i++) {
            Digester.calculateDigest(source, algorithm);
        }

        final int count = 100;

        Instant start = TimeUtils.now();
        for (int i = 0; i < count; i++) {
            Digester.calculateDigest(source, algorithm);
        }
        Instant end = TimeUtils.now();

        final long length = FileUtils.calculateLength(source);
        float mbPerSec = 100f * length * Duration.standardSeconds(1).getMillis()
                / new Duration(start, end).getMillis() / (1 << 20);

        logger.debug(algorithm + " calculated in " + TimeUtils.toDurationToNow(start) + " -- "
                + mbPerSec + " Mb/sec");
    }

    @Test
    public void testCalculateDigest_SHA1() {
        testCalculateDigest("SHA-1");
    }

    @Test
    public void testCalculateDigest_SHA256() {
        testCalculateDigest("SHA-256");
    }

    @Test
    public void testCalculateDigest_SHA512() {
        testCalculateDigest("SHA-512");
    }
}
