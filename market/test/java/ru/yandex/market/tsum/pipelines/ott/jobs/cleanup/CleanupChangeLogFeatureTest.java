package ru.yandex.market.tsum.pipelines.ott.jobs.cleanup;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.common.io.CharStreams;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CleanupChangeLogFeatureTest {
    CleanupChangeLogFeature feature = new CleanupChangeLogFeature();

    @Test
    public void releaseChangeLogTest() {
        String release = loadFile("../resources/releaseChangelog.md");
        String expected = loadFile("../resources/initialChangelog.md");
        String updated = feature.updateChangeLog(release, "release_22220101_OTT-77777");
        Assert.assertEquals(expected, updated);
    }

    @Test
    public void previousHotfixChangeLogTest() {
        String release = loadFile("../resources/prevHotfixChangelog.md");
        String expected = loadFile("../resources/initialChangelog.md");
        String updated = feature.updateChangeLog(release, "release_20200512_OTT-11017_hf3");
        Assert.assertEquals(expected, updated);
    }

    @Test
    public void lastHotfixChangeLogTest() {
        String release = loadFile("../resources/lastHotfixChangelog.md");
        String expected = loadFile("../resources/initialChangelog.md");
        String updated = feature.updateChangeLog(release, "release_20200526_OTT-11436_hf1");
        Assert.assertEquals(expected, updated);
    }

    private static String loadFile(String path) {
        try {
            return CharStreams.toString(
                new InputStreamReader(CleanupChangeLogFeatureTest.class.getResourceAsStream(path),
                    StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
