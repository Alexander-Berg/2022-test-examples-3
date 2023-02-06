package ru.yandex.direct.grid.processing.service.aggregatedstatuses;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.retargeting.AggregatedStatusRetargetingData;
import ru.yandex.direct.grid.core.entity.retargeting.model.GdiRetargetingBaseStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ACTIVE;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ARCHIVED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.SUSPENDED_BY_USER;
import static ru.yandex.direct.core.entity.aggregatedstatuses.retargeting.RetargetingStatesEnum.SUSPENDED;

@RunWith(Parameterized.class)
public class RetargetingBaseStatusCalculatorTest {

    @Parameterized.Parameter
    public String testDescription;
    @Parameterized.Parameter(1)
    public AggregatedStatusRetargetingData aggregatedStatusRetargetingData;
    @Parameterized.Parameter(2)
    public GdiRetargetingBaseStatus expectStatus;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // Без статуса
                {"null статус конвертируется в ACTIVE",
                        null,
                        GdiRetargetingBaseStatus.ACTIVE},
                {"(null status, SUSPENDED_BY_USER reason, SUSPENDED state) -> ACTIVE",
                        new AggregatedStatusRetargetingData(Set.of(SUSPENDED), null, List.of(SUSPENDED_BY_USER)),
                        GdiRetargetingBaseStatus.ACTIVE},
                // Остановленные
                {"(STOP_OK status, SUSPENDED_BY_USER reason, SUSPENDED state) -> SUSPENDED",
                        new AggregatedStatusRetargetingData(Set.of(SUSPENDED), GdSelfStatusEnum.STOP_OK,
                                List.of(SUSPENDED_BY_USER)),
                        GdiRetargetingBaseStatus.SUSPENDED},
                // Активные
                {"(RUN_OK status, ACTIVE reason, null state) -> ACTIVE",
                        new AggregatedStatusRetargetingData(null, GdSelfStatusEnum.RUN_OK, List.of(ACTIVE)),
                        GdiRetargetingBaseStatus.ACTIVE},
                // Архивные
                {"(ARCHIVED status, ARCHIVED reason, null state) -> null",
                        new AggregatedStatusRetargetingData(null, GdSelfStatusEnum.ARCHIVED, List.of(ARCHIVED)),
                        null},
        });
    }

    @Test
    public void convertToRetargetingBaseStatus() {
        GdiRetargetingBaseStatus status =
                RetargetingBaseStatusCalculator.convertToRetargetingBaseStatus(aggregatedStatusRetargetingData);
        assertThat(status).isEqualTo(expectStatus);
    }
}
