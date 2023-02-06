package ru.yandex.market.api.internal.report.parsers.json.outlet;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.OpenHoursV2;
import ru.yandex.market.api.domain.v2.OutletV2;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.report.parsers.json.OutletV2JsonParser;
import ru.yandex.market.api.matchers.BreakIntervalV2Matcher;
import ru.yandex.market.api.matchers.OpenHoursMatcher;
import ru.yandex.market.api.matchers.OutletMatcher;
import ru.yandex.market.api.matchers.PhoneMatcher;
import ru.yandex.market.api.util.ResourceHelpers;

public class OutletV2JsonParserTest extends UnitTestBase {
    @Test
    public void general() {
        OutletV2 outlet = parse("outlet-v2-general.json");

        Assert.assertThat(
            outlet,
            OutletMatcher.outlet(
                OutletMatcher.id("43"),
                OutletMatcher.name("test outlet"),
                OutletMatcher.type("post"),
                OutletMatcher.shopId(12)
            )
        );
    }

    @Test
    public void phone() {
        OutletV2 outlet = parse("outlet-v2-phones.json");

        Assert.assertThat(
            outlet,
            OutletMatcher.phones(
                cast(
                    Matchers.contains(
                        PhoneMatcher.phone("+7 (495) 3927427", "74953927427")
                    )
                )
            )
        );
    }

    @Test
    public void schedule() {
        OutletV2 outlet = parse("outlet-v2-schedule.json");

        Matcher<OpenHoursV2> openHours1 =
            OpenHoursMatcher.openHours(
                OpenHoursMatcher.daysFrom("1"),
                OpenHoursMatcher.daysTill("5"),
                OpenHoursMatcher.from("08:00"),
                OpenHoursMatcher.till("20:00"),
                OpenHoursMatcher.breaks(
                    cast(
                        Matchers.containsInAnyOrder(
                            BreakIntervalV2Matcher.interval(
                                BreakIntervalV2Matcher.from("13:00"),
                                BreakIntervalV2Matcher.till("14:00")
                            ),
                            BreakIntervalV2Matcher.interval(
                                BreakIntervalV2Matcher.from("19:00"),
                                BreakIntervalV2Matcher.till("20:00")
                            )
                        )
                    )
                )
            );

        Matcher<OpenHoursV2> openHours2 =
            OpenHoursMatcher.openHours(
                OpenHoursMatcher.daysFrom("6"),
                OpenHoursMatcher.daysTill("6"),
                OpenHoursMatcher.from("09:00"),
                OpenHoursMatcher.till("18:00"),
                OpenHoursMatcher.breaks(
                    cast(
                        Matchers.contains(
                            BreakIntervalV2Matcher.interval(
                                BreakIntervalV2Matcher.from("13:00"),
                                BreakIntervalV2Matcher.till("14:00")
                            )
                        )
                    )
                )
            );

        Assert.assertThat(
            outlet,
            OutletMatcher.schedule(
                cast(
                    Matchers.containsInAnyOrder(
                        openHours1,
                        openHours2
                    )
                )
            )
        );
    }


    private static OutletV2 parse(String filename) {
        return new OutletV2JsonParser().parse(ResourceHelpers.getResource(filename));
    }

    private static <T> Matcher<T> cast(Matcher<?> matcher) {
        return (Matcher<T>) matcher;
    }
}
