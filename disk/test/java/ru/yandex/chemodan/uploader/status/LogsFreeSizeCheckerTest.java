package ru.yandex.chemodan.uploader.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;

import static org.mockito.Mockito.when;

/**
 * @author nshmakov
 */
@RunWith(MockitoJUnitRunner.class)
public class LogsFreeSizeCheckerTest {

    private LogsFreeSizeChecker sut = new LogsFreeSizeChecker();

    @Mock
    private File2 fileMock;
    private final DataSize minFreeSpace = DataSize.fromMegaBytes(100);

    @Before
    public void init() {
        sut.setLocalFilesDir(fileMock);
        sut.setMinFreeSpace(minFreeSpace);
    }

    @Test
    public void shouldBeNotAvailableWhenNotEnoughSpace() {
        when(fileMock.getFreeSpaceSize()).thenReturn(minFreeSpace.min(DataSize.fromMegaBytes(1)));
        Assert.isFalse(sut.isAvailable());
    }

    @Test
    public void shouldBeAvailableWhenAllConditionsAreMet() {
        when(fileMock.getFreeSpaceSize()).thenReturn(minFreeSpace.plus(DataSize.fromMegaBytes(1)));
        Assert.isTrue(sut.isAvailable());
    }
}
