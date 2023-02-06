package ru.yandex.direct.useractionlog.schema;

import java.time.LocalDateTime;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

@ParametersAreNonnullByDefault
public class RecordSourceTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void testEquals() {
        RecordSource sample =
                new RecordSource(RecordSource.RECORD_SOURCE_DAEMON, LocalDateTime.of(2000, 1, 1, 0, 0));
        softly.assertThat(new RecordSource(sample.getType(), sample.getTimestamp()))
                .isEqualTo(sample);
        softly.assertThat(new RecordSource(RecordSource.RECORD_SOURCE_MANUAL, sample.getTimestamp()))
                .isNotEqualTo(sample);
        softly.assertThat(new RecordSource(sample.getType(), LocalDateTime.of(2000, 1, 1, 0, 1)))
                .isNotEqualTo(sample);
        softly.assertThat(new RecordSource(RecordSource.RECORD_SOURCE_MANUAL, LocalDateTime.of(2000, 1, 1, 0, 1)))
                .isNotEqualTo(sample);
    }

    @Test
    public void testComparison() {
        RecordSource sample =
                new RecordSource(RecordSource.RECORD_SOURCE_DAEMON, LocalDateTime.of(2000, 1, 1, 0, 0));
        softly.assertThat(new RecordSource(sample.getType(), sample.getTimestamp().minusMinutes(1)))
                .isLessThan(sample);
        softly.assertThat(new RecordSource(sample.getType(), sample.getTimestamp().plusMinutes(1)))
                .isGreaterThan(sample);
        softly.assertThat(new RecordSource(RecordSource.RECORD_SOURCE_MANUAL, sample.getTimestamp().minusMinutes(1)))
                .isGreaterThan(sample);
    }
}
