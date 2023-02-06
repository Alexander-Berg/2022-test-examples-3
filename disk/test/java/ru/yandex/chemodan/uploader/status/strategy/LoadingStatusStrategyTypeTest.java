package ru.yandex.chemodan.uploader.status.strategy;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

import static ru.yandex.chemodan.uploader.status.strategy.LoadingStatusStrategyType.CONTENT;
import static ru.yandex.chemodan.uploader.status.strategy.LoadingStatusStrategyType.DISK_IO;
import static ru.yandex.chemodan.uploader.status.strategy.LoadingStatusStrategyType.LA;
import static ru.yandex.chemodan.uploader.status.strategy.LoadingStatusStrategyType.QUEUE_SIZE_USER;

/**
 * @author nshmakov
 */
public class LoadingStatusStrategyTypeTest {

    @Test
    public void fromString() {
        String types = "la,disk_io,content,queue_size_user";

        List<LoadingStatusStrategyType> actual = LoadingStatusStrategyType.fromString(types);

        List<LoadingStatusStrategyType> expected = Arrays.asList(LA, DISK_IO, CONTENT, QUEUE_SIZE_USER);
        Assert.equals(expected, actual);
    }
}
