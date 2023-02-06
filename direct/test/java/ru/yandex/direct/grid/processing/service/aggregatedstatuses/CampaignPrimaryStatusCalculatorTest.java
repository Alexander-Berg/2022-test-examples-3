package ru.yandex.direct.grid.processing.service.aggregatedstatuses;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignCounters;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignStatesEnum;
import ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatus;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignFilterStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ARCHIVED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.CAMPAIGN_ACTIVE;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.CAMPAIGN_HAS_NO_ADS_ELIGIBLE_FOR_SERVING;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.CAMPAIGN_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.DRAFT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.SUSPENDED_BY_USER;

@RunWith(Parameterized.class)
public class CampaignPrimaryStatusCalculatorTest {

    @Parameter
    public String testDescription;
    @Parameter(1)
    public AggregatedStatusCampaignData aggregatedStatusCampaignData;
    @Parameter(2)
    public GdCampaignPrimaryStatus expectStatus;
    @Parameter(3)
    public GdCampaignFilterStatus expectFilterStatus;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{

                // Без статуса
                {"null статус конвертируется в DRAFT",
                        null,
                        GdCampaignPrimaryStatus.DRAFT,
                        GdCampaignFilterStatus.DRAFT
                },
                {"(null status, ARCHIVED reason, ARCHIVED state) -> DRAFT",
                        new AggregatedStatusCampaignData(List.of(CampaignStatesEnum.ARCHIVED), new CampaignCounters(),
                                null, List.of(ARCHIVED)),
                        GdCampaignPrimaryStatus.DRAFT,
                        GdCampaignFilterStatus.DRAFT
                },
                // Архивные
                {"(ARCHIVED status, ARCHIVED reason, ARCHIVED state) -> ARCHIVED",
                        new AggregatedStatusCampaignData(List.of(CampaignStatesEnum.ARCHIVED), new CampaignCounters(),
                                GdSelfStatusEnum.ARCHIVED, List.of(ARCHIVED)),
                        GdCampaignPrimaryStatus.ARCHIVED,
                        GdCampaignFilterStatus.ARCHIVED
                },
                // На модерации
                {"(STOP_CRIT, CAMPAIGN_ON_MODERATION reason, DRAFT state) -> MODERATION",
                        new AggregatedStatusCampaignData(List.of(CampaignStatesEnum.DRAFT), new CampaignCounters(),
                                GdSelfStatusEnum.STOP_CRIT, List.of(CAMPAIGN_ON_MODERATION)),
                        GdCampaignPrimaryStatus.MODERATION,
                        GdCampaignFilterStatus.MODERATION
                },
                // Отклоненные
                {"(STOP_CRIT, CAMPAIGN_HAS_NO_ADS_ELIGIBLE_FOR_SERVING reason, PAYED state) -> MODERATION_DENIED",
                        new AggregatedStatusCampaignData(List.of(CampaignStatesEnum.PAYED), new CampaignCounters(),
                                GdSelfStatusEnum.STOP_CRIT, List.of(CAMPAIGN_HAS_NO_ADS_ELIGIBLE_FOR_SERVING)),
                        GdCampaignPrimaryStatus.MODERATION_DENIED,
                        GdCampaignFilterStatus.MODERATION_DENIED
                },
                // Остановленные
                {"(STOP_OK, SUSPENDED_BY_USER reason, SUSPENDED state) -> STOPPED",
                        new AggregatedStatusCampaignData(List.of(CampaignStatesEnum.SUSPENDED), new CampaignCounters(),
                                GdSelfStatusEnum.STOP_OK, List.of(SUSPENDED_BY_USER)),
                        GdCampaignPrimaryStatus.STOPPED,
                        GdCampaignFilterStatus.STOPPED
                },
                // Черновики
                {"(DRAFT, DRAFT reason, SUSPENDED state) -> DRAFT",
                        new AggregatedStatusCampaignData(List.of(CampaignStatesEnum.SUSPENDED), new CampaignCounters(),
                                GdSelfStatusEnum.DRAFT, List.of(DRAFT)),
                        GdCampaignPrimaryStatus.DRAFT,
                        GdCampaignFilterStatus.DRAFT
                },
                // Запущенные
                {"(RUN_WARN, CAMPAIGN_ACTIVE reason, PAYED state) -> ACTIVE",
                        new AggregatedStatusCampaignData(List.of(CampaignStatesEnum.PAYED), new CampaignCounters(),
                                GdSelfStatusEnum.RUN_WARN, List.of(CAMPAIGN_ACTIVE)),
                        GdCampaignPrimaryStatus.ACTIVE,
                        GdCampaignFilterStatus.RUN_WARN
                },
                {"(RUN_OK, CAMPAIGN_ACTIVE reason, PAYED state) -> ACTIVE",
                        new AggregatedStatusCampaignData(List.of(CampaignStatesEnum.PAYED), new CampaignCounters(),
                                GdSelfStatusEnum.RUN_OK, List.of(CAMPAIGN_ACTIVE)),
                        GdCampaignPrimaryStatus.ACTIVE,
                        GdCampaignFilterStatus.ACTIVE
                },
        });
    }

    @Test
    public void convertToCampaignPrimaryStatus() {
        GdCampaignPrimaryStatus status =
                CampaignPrimaryStatusCalculator.convertToPrimaryStatus(new GdiCampaign()
                        .withAggregatedStatus(aggregatedStatusCampaignData));
        assertThat(status).isEqualTo(expectStatus);
    }

    @Test
    public void convertToCampaignFilterStatus() {
        var status =
                CampaignPrimaryStatusCalculator.convertToFilterStatus(new GdiCampaign()
                        .withAggregatedStatus(aggregatedStatusCampaignData));
        assertThat(status).isEqualTo(expectFilterStatus);
    }
}
