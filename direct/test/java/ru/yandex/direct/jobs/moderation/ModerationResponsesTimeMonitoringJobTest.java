package ru.yandex.direct.jobs.moderation;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static ru.yandex.direct.jobs.moderation.ModerationResponsesTimeMonitoringJob.removeSuffix;

class ModerationResponsesTimeMonitoringJobTest {
    @Test
    void testRemoveSuffix() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(removeSuffix("234A")).isEqualTo("234");
            softly.assertThat(removeSuffix("234B")).isEqualTo("234");
            softly.assertThat(removeSuffix("A")).isEqualTo("");
            softly.assertThat(removeSuffix("123")).isEqualTo("123");
        });
    }
}
