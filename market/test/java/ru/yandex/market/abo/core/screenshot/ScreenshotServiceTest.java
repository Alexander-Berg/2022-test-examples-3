package ru.yandex.market.abo.core.screenshot;

import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.abo.core.screenshot.ScreenshotSource.TICKET;
import static ru.yandex.market.abo.core.screenshot.ScreenshotSource.TICKET_PROBLEM;

/**
 * @author kukabara
 */
public class ScreenshotServiceTest extends EmptyTest {
    @Autowired
    ScreenshotService screenshotService;
    @Autowired
    private ScreenshotMdsClient screenshotMdsClient;
    @Autowired
    MdsS3Client mdsS3Client;

    @Test
    public void testAddScreenshot() throws Exception {
        Screenshot s = new Screenshot(1L, 1L, 1);
        byte[] data = "aaaa".getBytes();
        Screenshot saved = screenshotService.addScreenshot(s, data);
        byte[] savedData = screenshotMdsClient.download(saved.getId());
        assertArrayEquals(data, savedData);
    }

    @Test
    public void testAddScreenshotWithError() throws Exception {
        byte[] data = null;

        int entityTypeId = TICKET_PROBLEM.getId();
        long entityId = RND.nextLong();
        Screenshot s = new Screenshot(1L, entityId, entityTypeId);
        assertThrows(Exception.class, () ->
                screenshotService.addScreenshot(s, data));
    }

    @Test
    public void testReattachScreenshots() {
        long hypId = 1;
        long problemId = 11;
        LongStream.range(1, 3).mapToObj(id -> new Screenshot(id, hypId, TICKET.getId())).
                forEach(screenshot -> screenshotService.addScreenshot(screenshot, new byte[]{1, 2, 3}));

        screenshotService.reattachExistingScreenshots(hypId, TICKET, problemId, TICKET_PROBLEM);
        List<Screenshot> problemScreenshots = screenshotService.getScreenshotsByEntity(TICKET_PROBLEM, problemId);
        assertEquals(2, problemScreenshots.size());
        assertTrue(problemScreenshots.stream().allMatch(screenshot -> screenshot.getHash() != null));
        assertEquals(0, screenshotService.getScreenshotsByEntity(TICKET, hypId).size());
    }
}
