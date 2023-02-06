package ru.yandex.direct.grid.processing.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.model.GdStatPreset;
import ru.yandex.direct.grid.model.GdStatRequirements;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class NormalizeStatRequirementsTest {
    private static final LocalDate TEST_DATE = LocalDate.parse("2018-01-09");
    private static final Instant TEST_INSTANT = TEST_DATE.atStartOfDay().toInstant(ZoneOffset.UTC);

    private static GdStatRequirements requirements() {
        return requirements(null);
    }

    private static GdStatRequirements requirements(GdStatPreset preset) {
        return requirements(preset, preset);
    }

    private static GdStatRequirements requirements(GdStatPreset preset, GdStatPreset statsByDaysPreset) {
        return new GdStatRequirements()
                .withPreset(preset)
                .withStatsByDaysPreset(statsByDaysPreset);
    }

    @Parameterized.Parameter
    public GdStatRequirements statRequirementsInput;

    @Parameterized.Parameter(1)
    public LocalDate expectedFrom;

    @Parameterized.Parameter(2)
    public LocalDate expectedTo;

    @Parameterized.Parameter(3)
    public LocalDate expectedStatsByDaysFrom;

    @Parameterized.Parameter(4)
    public LocalDate expectedStatsByDaysTo;

    @Parameterized.Parameters(name = "input = {0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        requirements().withFrom(TEST_DATE.minusDays(5)).withTo(TEST_DATE)
                                .withStatsByDaysFrom(TEST_DATE.minusDays(5)).withStatsByDaysTo(TEST_DATE),
                        TEST_DATE.minusDays(5), TEST_DATE,
                        TEST_DATE.minusDays(5), TEST_DATE,
                },
                {
                        requirements().withFrom(TEST_DATE.minusDays(5)).withTo(TEST_DATE)
                                .withStatsByDaysFrom(TEST_DATE.minusDays(10)).withStatsByDaysTo(TEST_DATE.plusDays(2)),
                        TEST_DATE.minusDays(5), TEST_DATE,
                        TEST_DATE.minusDays(10), TEST_DATE.plusDays(2),
                },
                {
                        requirements(GdStatPreset.TODAY),
                        TEST_DATE, TEST_DATE,
                        TEST_DATE, TEST_DATE,
                },
                {
                        requirements(GdStatPreset.YESTERDAY),
                        TEST_DATE.minusDays(1), TEST_DATE.minusDays(1),
                        TEST_DATE.minusDays(1), TEST_DATE.minusDays(1),
                },
                {
                        requirements(GdStatPreset.CURRENT_WEEK),
                        LocalDate.parse("2018-01-08"), LocalDate.parse("2018-01-14"),
                        LocalDate.parse("2018-01-08"), LocalDate.parse("2018-01-14"),
                },
                {
                        requirements(GdStatPreset.PREVIOUS_WEEK),
                        LocalDate.parse("2018-01-01"), LocalDate.parse("2018-01-07"),
                        LocalDate.parse("2018-01-01"), LocalDate.parse("2018-01-07"),
                },
                {
                        requirements(GdStatPreset.LAST_7DAYS),
                        LocalDate.parse("2018-01-03"), TEST_DATE,
                        LocalDate.parse("2018-01-03"), TEST_DATE,
                },
                {
                        requirements(GdStatPreset.CURRENT_MONTH),
                        LocalDate.parse("2018-01-01"), LocalDate.parse("2018-01-31"),
                        LocalDate.parse("2018-01-01"), LocalDate.parse("2018-01-31"),
                },
                {
                        requirements(GdStatPreset.PREVIOUS_MONTH),
                        LocalDate.parse("2017-12-01"), LocalDate.parse("2017-12-31"),
                        LocalDate.parse("2017-12-01"), LocalDate.parse("2017-12-31"),
                },
                {
                        requirements(GdStatPreset.LAST_30DAYS),
                        LocalDate.parse("2017-12-11"), TEST_DATE,
                        LocalDate.parse("2017-12-11"), TEST_DATE,
                },
                {
                        requirements(GdStatPreset.LAST_6MONTHS),
                        LocalDate.parse("2017-07-09"), TEST_DATE,
                        LocalDate.parse("2017-07-09"), TEST_DATE,
                },
                {
                        requirements().withFrom(TEST_DATE.minusDays(5)).withTo(TEST_DATE)
                                .withStatsByDaysPreset(GdStatPreset.TODAY),
                        TEST_DATE.minusDays(5), TEST_DATE,
                        TEST_DATE, TEST_DATE,
                },
                {
                        requirements().withPreset(GdStatPreset.TODAY)
                                .withStatsByDaysFrom(TEST_DATE.minusDays(5)).withStatsByDaysTo(TEST_DATE),
                        TEST_DATE, TEST_DATE,
                        TEST_DATE.minusDays(5), TEST_DATE,
                },
                {
                        requirements(GdStatPreset.TODAY, GdStatPreset.YESTERDAY),
                        TEST_DATE, TEST_DATE,
                        TEST_DATE.minusDays(1), TEST_DATE.minusDays(1),
                },
                {
                        requirements(null, GdStatPreset.TODAY),
                        null, null,
                        TEST_DATE, TEST_DATE,
                },
                {
                        requirements(GdStatPreset.TODAY, null),
                        TEST_DATE, TEST_DATE,
                        null, null,
                },
        });
    }


    @Test
    public void testRequirements() {
        GdStatRequirements requirements =
                StatHelper.normalizeStatRequirements(statRequirementsInput, TEST_INSTANT, null);

        assertThat(requirements)
                .isEqualTo(requirements()
                        .withFrom(expectedFrom)
                        .withTo(expectedTo)
                        .withStatsByDaysFrom(expectedStatsByDaysFrom)
                        .withStatsByDaysTo(expectedStatsByDaysTo));
    }

}
