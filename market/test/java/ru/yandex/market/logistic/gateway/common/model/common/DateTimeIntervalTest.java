package ru.yandex.market.logistic.gateway.common.model.common;

import java.time.OffsetDateTime;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class DateTimeIntervalTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void fromFormattedValueSuccess() {

        String[] modifierTemplates = new String[]{"", "'T'HH", "'T'HH:mm", "'T'HH:mm:ss", "'T'HH:mm:ssxxx"};
        String[] fromModifiers = new String[]{"", "T12", "T12:23", "T12:23:09", "T12:23:09+05:00"};
        String[] toModifiers = new String[]{"", "T03", "T03:44", "T03:44:51", "T03:44:51+08:00"};

        OffsetDateTime[] fromDateTimes = new OffsetDateTime[] {
            OffsetDateTime.parse("2019-08-20T00:00:00+03:00"),
            OffsetDateTime.parse("2019-08-20T12:00:00+03:00"),
            OffsetDateTime.parse("2019-08-20T12:23:00+03:00"),
            OffsetDateTime.parse("2019-08-20T12:23:09+03:00"),
            OffsetDateTime.parse("2019-08-20T12:23:09+05:00")
        };

        OffsetDateTime[] toDateTimes = new OffsetDateTime[] {
            OffsetDateTime.parse("2019-08-21T00:00:00+03:00"),
            OffsetDateTime.parse("2019-08-21T03:00:00+03:00"),
            OffsetDateTime.parse("2019-08-21T03:44:00+03:00"),
            OffsetDateTime.parse("2019-08-21T03:44:51+03:00"),
            OffsetDateTime.parse("2019-08-21T03:44:51+08:00")
        };

        for (int i = 0; i < modifierTemplates.length; ++i) {
            for (int j = 0; j < modifierTemplates.length; ++j) {
                String formattedValue = String.format("2019-08-20%s/2019-08-21%s", fromModifiers[i], toModifiers[j]);

                softly.assertThat(DateTimeInterval.fromFormattedValue(formattedValue))
                    .as(String.format("yyyy-MM-dd%s/yyyy-MM-dd%s", modifierTemplates[i], modifierTemplates[j]))
                    .isEqualTo(new DateTimeInterval(fromDateTimes[i], toDateTimes[j]));
            }
        }
    }
}
