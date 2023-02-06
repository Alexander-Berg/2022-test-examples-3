package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.util.Collection;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.KeywordStatesEnum;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.ARCHIVED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.DRAFT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_OK;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_CRIT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_OK;

@RunWith(Parameterized.class)
public class SelfStatusCalculatorsKeywordTest {
    @Parameterized.Parameter()
    public Collection<KeywordStatesEnum> states;

    @Parameterized.Parameter(1)
    public GdSelfStatusEnum status;

    @Parameterized.Parameter(2)
    public List<GdSelfStatusReason> reasons;

    @Parameterized.Parameters(name = "states = {0} => status: {1}, reason: {2}")
    public static Object[][] params() {
        return new Object[][]{
                {List.of(KeywordStatesEnum.ARCHIVED),
                        ARCHIVED, null},

                {List.of(KeywordStatesEnum.DRAFT),
                        DRAFT, null},

                {List.of(KeywordStatesEnum.REJECTED),
                        STOP_CRIT, List.of(GdSelfStatusReason.REJECTED_ON_MODERATION)},

                {List.of(KeywordStatesEnum.SUSPENDED),
                        STOP_OK, List.of(GdSelfStatusReason.KEYWORD_SUSPENDED_BY_USER)},

                {List.of(),
                        RUN_OK, null},
        };
    }

    @Test
    public void test() {
        var calculatedSelfStatus = SelfStatusCalculators.calcKeywordSelfStatus(states);

        assertEquals("Status ok", status, calculatedSelfStatus.getStatus());

        if (reasons == null) {
            assertThat("Reason is null", calculatedSelfStatus.getReasons(), Matchers.nullValue());
        } else {
            assertThat("Reason count ok", calculatedSelfStatus.getReasons(), hasSize(reasons.size()));
            assertThat("Reason ok", calculatedSelfStatus.getReasons(), containsInAnyOrder(reasons.toArray()));
        }
    }
}
