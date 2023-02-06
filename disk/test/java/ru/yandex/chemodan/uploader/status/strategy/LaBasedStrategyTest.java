package ru.yandex.chemodan.uploader.status.strategy;

import java.lang.management.OperatingSystemMXBean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.misc.test.Assert;

import static org.mockito.Mockito.when;

/**
 * @author nshmakov
 */
public class LaBasedStrategyTest {

    private LaBasedStrategy sut = new LaBasedStrategy();

    @Mock
    private OperatingSystemMXBean operatingSystemMXBeanMock;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        sut.setOperatingSystemMXBean(operatingSystemMXBeanMock);
    }

    @Test
    public void shouldComputeLaBasedLoadingStatus() {
        when(operatingSystemMXBeanMock.getSystemLoadAverage()).thenReturn(0.43);

        long actual = sut.compute();

        Assert.equals(43l, actual);
    }
}
