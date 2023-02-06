package ru.yandex.market.logshatter.reader;

import junit.framework.Assert;
import org.junit.Test;
import ru.yandex.market.logshatter.reader.file.FileWatcherService;

public class FileWatcherServiceTest {
    @Test
    public void testNumber() throws Exception {
        Assert.assertEquals(4, FileWatcherService.getFileNumber("access.log.4"));
        Assert.assertEquals(0, FileWatcherService.getFileNumber("access.log."));
        Assert.assertEquals(4, FileWatcherService.getFileNumber("access.log.4.gz"));
        Assert.assertEquals(0, FileWatcherService.getFileNumber("access.log"));

    }
}