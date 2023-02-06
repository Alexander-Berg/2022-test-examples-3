package ru.yandex.chemodan.uploader.status.strategy;

import org.joda.time.Duration;
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
public class NetworkIoBasedStrategyTest {

    private File2 statFileMock = Mockito.mock(File2.class);
    private NetworkIoLoadingStatusDelayHolder delayHolderMock = Mockito.mock(NetworkIoLoadingStatusDelayHolder.class);
    private NetworkIoBasedStrategy sut = new NetworkIoBasedStrategy();

    @Before
    public void init() {
        sut.setDelayHolder(delayHolderMock);
        sut.setStatFile(statFileMock);
        sut.setNetworkInterface("eth0");
    }

    @Test
    public void shouldReturnZeroAfterFirstCall() {
        when(statFileMock.readLines()).thenReturn(netStats(1));
        when(delayHolderMock.getDelay()).thenReturn(Duration.standardSeconds(1));

        sut.refresh();
        long actual = sut.compute();

        Assert.equals(0L, actual);
    }

    @Test
    public void shouldComputeDifferenceBetweenTwoCalls() {
        when(delayHolderMock.getDelay()).thenReturn(Duration.standardSeconds(1));
        long actual = computeDifferenceBetweenTwoCalls();

        long expected = 2000; // (128729048016045 - 128729048015045) + (126897311768212 - 126897311767212)
        Assert.equals(expected, actual);
    }

    @Test
    public void statShouldBeTwoTimesGreaterWhenDelayIsHalfSecond() {
        when(delayHolderMock.getDelay()).thenReturn(Duration.millis(500));

        long actual = computeDifferenceBetweenTwoCalls();

        long expected = 4000;
        Assert.equals(expected, actual);
    }

    @Test
    public void statShouldBeTwoTimesSmallerWhenDelayIsTwoSeconds() {
        when(delayHolderMock.getDelay()).thenReturn(Duration.standardSeconds(2));

        long actual = computeDifferenceBetweenTwoCalls();

        long expected = 1000;
        Assert.equals(expected, actual);
    }

    private long computeDifferenceBetweenTwoCalls() {
        when(statFileMock.readLines())
                .thenReturn(netStats(1))
                .thenReturn(netStats(2));
        sut.refresh();
        sut.refresh();
        return sut.compute();
    }

    private ListF<String> netStats(int num) {
        return ClassLoaderUtils.streamSourceForResource(getClass(), "netstats" + num + ".txt").readLines();
    }
}
