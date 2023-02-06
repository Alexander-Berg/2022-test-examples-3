package ru.yandex.chemodan.uploader.registry.record.status;

import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.misc.test.Assert;

/**
 * @author Lev Tolmachev
 */
public class PreviewVideoStatusTest {

    @Test
    public void previewTimes() {
        Assert.isEmpty(PreviewVideoStatus.getMultiplePreviewTimes(Duration.standardSeconds(1)));

        Assert.sizeIs(PreviewVideoStatus.PREVIEW_COUNT,
                PreviewVideoStatus.getMultiplePreviewTimes(Duration.standardSeconds(100)));

        Assert.equals(Cf.list(
                Duration.standardSeconds(1),
                Duration.standardSeconds(2),
                Duration.standardSeconds(3),
                Duration.standardSeconds(4),
                Duration.standardSeconds(5)
            ),
            PreviewVideoStatus.getMultiplePreviewTimes(Duration.standardSeconds(6)));
    }

}
