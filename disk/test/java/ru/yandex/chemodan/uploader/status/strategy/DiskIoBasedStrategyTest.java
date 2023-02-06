package ru.yandex.chemodan.uploader.status.strategy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;

import static org.mockito.Mockito.when;

/**
 * @author nshmakov
 */
public class DiskIoBasedStrategyTest {

    private DiskIoBasedStrategy sut = new DiskIoBasedStrategy();
    private final String deviceName = "md1";

    private File2 statFileMock;

    @Before
    public void init() {
        statFileMock = Mockito.mock(File2.class);
        sut.setStatFile(statFileMock);
        sut.setDeviceName(deviceName);
        sut.setSectorSize(512);
    }

    @Test
    public void shouldReturnZeroStatAfterFirstRefresh() {
        when(statFileMock.readLines()).thenReturn(diskStats(1));

        sut.refresh();
        long actual = sut.compute();

        Assert.equals(0L, actual);
    }

    @Test
    public void shouldComputeDifferenceBetweenTwoCalls() {
        when(statFileMock.readLines())
                .thenReturn(diskStats(1))
                .thenReturn(diskStats(2));

        sut.refresh();  // first read
        sut.refresh();  // second read
        long actual = sut.compute();

        long expected = 768000; // ((210008758272 + 44628309534) - (210008757272 + 44628309034)) * 512
        Assert.equals(expected, actual);
    }

    private ListF<String> diskStats(int num) {
        return ClassLoaderUtils.streamSourceForResource(getClass(), "diskstats" + num + ".txt").readLines();
    }
}
