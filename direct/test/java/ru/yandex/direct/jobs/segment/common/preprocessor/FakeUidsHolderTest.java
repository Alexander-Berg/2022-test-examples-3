package ru.yandex.direct.jobs.segment.common.preprocessor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.jobs.segment.common.metrica.UploadSegmentsService.MIN_SEGMENT_SIZE;

public class FakeUidsHolderTest {

    FakeUidsHolder fakeUidsHolder = new FakeUidsHolder();

    @Test
    public void holderContainsEnoughFakeUids() {
        assertThat(fakeUidsHolder.getFakeUids().size())
                .isGreaterThanOrEqualTo(MIN_SEGMENT_SIZE);
    }
}
