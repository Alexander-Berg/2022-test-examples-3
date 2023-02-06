package ru.yandex.direct.grid.processing.service.aggregatedstatuses;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupCounters;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupPrimaryStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ACTIVE;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ADGROUP_BL_PROCESSING_WITH_OLD_VERSION_SHOWN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ADGROUP_HAS_ADS_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ADGROUP_REJECTED_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ADGROUP_SHOW_CONDITIONS_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.NOTHING;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.SUSPENDED_BY_USER;

@RunWith(Parameterized.class)
public class AdGroupPrimaryStatusCalculatorTest {

    @Parameter
    public String testDescription;
    @Parameter(1)
    public AggregatedStatusAdGroupData aggregatedStatusAdGroupData;
    @Parameter(2)
    public GdiGroupPrimaryStatus expectStatus;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{

                // Без статуса
                {"null статус конвертируется в DRAFT",
                        null,
                        GdiGroupPrimaryStatus.DRAFT},
                {"(null status, NOTHING reason, REJECTED state) -> DRAFT",
                        new AggregatedStatusAdGroupData(List.of(AdGroupStatesEnum.REJECTED), new AdGroupCounters(),
                                null, List.of(NOTHING)),
                        GdiGroupPrimaryStatus.DRAFT},
                // Архивные
                {"(ARCHIVED status, NOTHING reason, REJECTED state) -> ARCHIVED",
                        new AggregatedStatusAdGroupData(List.of(AdGroupStatesEnum.REJECTED), new AdGroupCounters(),
                                GdSelfStatusEnum.ARCHIVED, List.of(NOTHING)),
                        GdiGroupPrimaryStatus.ARCHIVED},
                // Черновики
                {"(DRAFT status, DRAFT reason, DRAFT state) -> DRAFT",
                        new AggregatedStatusAdGroupData(List.of(AdGroupStatesEnum.DRAFT), new AdGroupCounters(),
                                GdSelfStatusEnum.DRAFT, List.of(GdSelfStatusReason.DRAFT)),
                        GdiGroupPrimaryStatus.DRAFT},
                // На модерации
                {"(DRAFT, ADGROUP_HAS_ADS_ON_MODERATION reason, HAS_DRAFT_ON_MODERATION_ADS state) -> MODERATION",
                        new AggregatedStatusAdGroupData(List.of(AdGroupStatesEnum.HAS_DRAFT_ON_MODERATION_ADS),
                                new AdGroupCounters(), GdSelfStatusEnum.DRAFT, List.of(ADGROUP_HAS_ADS_ON_MODERATION)),
                        GdiGroupPrimaryStatus.MODERATION},
                {"(DRAFT status, ADGROUP_SHOW_CONDITIONS_ON_MODERATION reason, MODERATION state) -> MODERATION",
                        new AggregatedStatusAdGroupData(List.of(AdGroupStatesEnum.MODERATION), new AdGroupCounters(),
                                GdSelfStatusEnum.DRAFT, List.of(ADGROUP_SHOW_CONDITIONS_ON_MODERATION)),
                        GdiGroupPrimaryStatus.MODERATION},
                {"(STOP_WARN status, ADGROUP_SHOW_CONDITIONS_ON_MODERATION reason, MODERATION state) -> MODERATION",
                        new AggregatedStatusAdGroupData(List.of(AdGroupStatesEnum.MODERATION), new AdGroupCounters(),
                                GdSelfStatusEnum.STOP_WARN, List.of(ADGROUP_SHOW_CONDITIONS_ON_MODERATION)),
                        GdiGroupPrimaryStatus.MODERATION},
                // Отклоненные
                {"(STOP_CRIT status, ADGROUP_REJECTED_ON_MODERATION reason, REJECTED state) -> REJECTED",
                        new AggregatedStatusAdGroupData(List.of(AdGroupStatesEnum.REJECTED), new AdGroupCounters(),
                                GdSelfStatusEnum.STOP_CRIT, List.of(ADGROUP_REJECTED_ON_MODERATION)),
                        GdiGroupPrimaryStatus.REJECTED},
                // Остановленные
                {"(STOP_OK status, SUSPENDED_BY_USER reason, null state) -> STOPPED",
                        new AggregatedStatusAdGroupData(null, new AdGroupCounters(),
                                GdSelfStatusEnum.STOP_OK, List.of(SUSPENDED_BY_USER)),
                        GdiGroupPrimaryStatus.STOPPED},
                // Запущенные
                {"(STOP_OK status, ACTIVE reason, null state) -> ACTIVE",
                        new AggregatedStatusAdGroupData(null, new AdGroupCounters(),
                                GdSelfStatusEnum.RUN_OK, List.of(ACTIVE)),
                        GdiGroupPrimaryStatus.ACTIVE},
                // Обрабатывается
                {"(RUN_WARN status, ADGROUP_BL_PROCESSING_WITH_OLD_VERSION_SHOWN reason, null state) -> null",
                        new AggregatedStatusAdGroupData(null, new AdGroupCounters(), GdSelfStatusEnum.RUN_WARN,
                                List.of(ADGROUP_BL_PROCESSING_WITH_OLD_VERSION_SHOWN)),
                        null},
        });
    }

    @Test
    public void convertToRetargetingBaseStatus() {
        GdiGroupPrimaryStatus status =
                AdGroupPrimaryStatusCalculator.convertToPrimaryStatus(aggregatedStatusAdGroupData);
        assertThat(status).isEqualTo(expectStatus);
    }
}
