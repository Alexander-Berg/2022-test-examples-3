package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ACTIVE;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.AD_IMAGE_REJECTED_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.AD_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.AD_SUSPENDED_BY_USER;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ARCHIVED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.CAMPAIGN_ACTIVE;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.DRAFT;

@RunWith(Parameterized.class)
public class StatusUtilsFilterTest {

    @Parameterized.Parameter
    public Collection<GdSelfStatusReason> reasons;

    @Parameterized.Parameter(1)
    public Collection<GdSelfStatusReason> filteredReasons;

    @Parameterized.Parameters
    public static Object[][] data() {
        return new Object[][] {
                {Collections.emptyList(), Collections.emptyList()},
                {null, Collections.emptyList()},
                {List.of(DRAFT, ACTIVE, ARCHIVED, CAMPAIGN_ACTIVE), Collections.emptyList()},
                {List.of(DRAFT, AD_SUSPENDED_BY_USER, CAMPAIGN_ACTIVE), List.of(AD_SUSPENDED_BY_USER)},
                {List.of(AD_ON_MODERATION, AD_SUSPENDED_BY_USER, AD_IMAGE_REJECTED_ON_MODERATION),
                        List.of(AD_ON_MODERATION, AD_SUSPENDED_BY_USER, AD_IMAGE_REJECTED_ON_MODERATION)},
        };
    }

    @Test
    public void filterMeaninglessReasons() {
        assertThat("filtered reasons match",
                StatusUtils.filterMeaninglessReasons(reasons), equalTo(filteredReasons));
    }
}
