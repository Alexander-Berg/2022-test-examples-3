package ru.yandex.direct.grid.processing.service.aggregatedstatuses;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AggregatedStatusAdData;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerPrimaryStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ACTIVE;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ADGROUP_REJECTED_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ADGROUP_SHOW_CONDITIONS_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.AD_DISPLAY_HREF_REJECTED_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.AD_IMAGE_REJECTED_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.AD_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.AD_SUSPENDED_BY_USER;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.AD_TURBOLANDING_REJECTED_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.AD_VCARD_REJECTED_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.AD_VIDEO_ADDITION_REJECTED_ON_MODERATION;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ARCHIVED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.DRAFT;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.REJECTED_ON_MODERATION;

@RunWith(Parameterized.class)
public class AdPrimaryStatusCalculatorTest {

    @Parameterized.Parameter
    public String testDescription;
    @Parameterized.Parameter(1)
    public AggregatedStatusAdData aggregatedStatusAdData;
    @Parameterized.Parameter(2)
    public GdiBannerPrimaryStatus expectStatus;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{

                // Без статуса
                {"null статус конвертируется в DRAFT",
                        null,
                        GdiBannerPrimaryStatus.DRAFT},
                {"(null status, ARCHIVED reason, ARCHIVED state) -> DRAFT",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.ARCHIVED), null, List.of(ARCHIVED)),
                        GdiBannerPrimaryStatus.DRAFT},
                // Архивные
                {"(ARCHIVED status, ARCHIVED reason, ARCHIVED state) -> ARCHIVED",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.ARCHIVED), GdSelfStatusEnum.ARCHIVED,
                                List.of(ARCHIVED)),
                        GdiBannerPrimaryStatus.ARCHIVED},
                // Черновики
                {"(DRAFT status, DRAFT reason, DRAFT state) -> DRAFT",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.DRAFT), GdSelfStatusEnum.DRAFT, List.of(DRAFT)),
                        GdiBannerPrimaryStatus.DRAFT},
                // На модерации
                {"(DRAFT status, AD_ON_MODERATION reason, DRAFT_ON_MODERATION state) -> MODERATION",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.DRAFT_ON_MODERATION), GdSelfStatusEnum.DRAFT,
                                List.of(AD_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION},
                {"(STOP_OK status, ADGROUP_SHOW_CONDITIONS_ON_MODERATION reason, null state) -> MODERATION",
                        new AggregatedStatusAdData(null, GdSelfStatusEnum.STOP_OK,
                                List.of(ADGROUP_SHOW_CONDITIONS_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION},
                {"(STOP_WARN status, ADGROUP_SHOW_CONDITIONS_ON_MODERATION reason, null state) -> MODERATION",
                        new AggregatedStatusAdData(null, GdSelfStatusEnum.STOP_WARN,
                                List.of(ADGROUP_SHOW_CONDITIONS_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION},
                {"(STOP_CRIT status, ADGROUP_SHOW_CONDITIONS_ON_MODERATION reason, null state) -> MODERATION",
                        new AggregatedStatusAdData(null, GdSelfStatusEnum.STOP_CRIT,
                                List.of(ADGROUP_SHOW_CONDITIONS_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION},
                // Отклоненные
                {"(STOP_CRIT status, AD_VCARD_REJECTED_ON_MODERATION reason, null state) -> MODERATION_REJECTED",
                        new AggregatedStatusAdData(null, GdSelfStatusEnum.STOP_CRIT,
                                List.of(AD_VCARD_REJECTED_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION_REJECTED},
                {"(STOP_CRIT status, REJECTED_ON_MODERATION reason, REJECTED state) -> MODERATION_REJECTED",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.REJECTED), GdSelfStatusEnum.STOP_CRIT,
                                List.of(REJECTED_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION_REJECTED},
                {"(RUN_WARN status, AD_VCARD_REJECTED_ON_MODERATION reason, null state) -> MODERATION_REJECTED",
                        new AggregatedStatusAdData(null, GdSelfStatusEnum.RUN_WARN,
                                List.of(AD_VCARD_REJECTED_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION_REJECTED},
                {"(RUN_WARN, AD_VIDEO_ADDITION_REJECTED_ON_MODERATION, REJECTED_VIDEO_ADDITION) -> MODERATION_REJECTED",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.REJECTED_VIDEO_ADDITION),
                                GdSelfStatusEnum.RUN_WARN, List.of(AD_VIDEO_ADDITION_REJECTED_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION_REJECTED},
                {"(RUN_WARN, AD_IMAGE_REJECTED_ON_MODERATION reason, REJECTED_IMAGE state) -> MODERATION_REJECTED",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.REJECTED_IMAGE), GdSelfStatusEnum.RUN_WARN,
                                List.of(AD_IMAGE_REJECTED_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION_REJECTED},
                {"(RUN_WARN, AD_VIDEO_ADDITION_REJECTED_ON_MODERATION reason, null state) -> MODERATION_REJECTED",
                        new AggregatedStatusAdData(null, GdSelfStatusEnum.RUN_WARN,
                                List.of(AD_DISPLAY_HREF_REJECTED_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION_REJECTED},
                {"(RUN_WARN status, AD_TURBOLANDING_REJECTED_ON_MODERATION reason, null state) -> MODERATION_REJECTED",
                        new AggregatedStatusAdData(null, GdSelfStatusEnum.RUN_WARN,
                                List.of(AD_TURBOLANDING_REJECTED_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION_REJECTED},
                {"(DRAFT status, ADGROUP_REJECTED_ON_MODERATION reason, null state) -> MODERATION_REJECTED",
                        new AggregatedStatusAdData(null, GdSelfStatusEnum.DRAFT,
                                List.of(ADGROUP_REJECTED_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION_REJECTED},
                {"(STOP_CRIT status, ADGROUP_REJECTED_ON_MODERATION reason, null state) -> MODERATION_REJECTED",
                        new AggregatedStatusAdData(null, GdSelfStatusEnum.STOP_CRIT,
                                List.of(ADGROUP_REJECTED_ON_MODERATION)),
                        GdiBannerPrimaryStatus.MODERATION_REJECTED},
                // Остановленные
                {"(STOP_OK status, AD_SUSPENDED_BY_USER reason, SUSPENDED state) -> MANUALLY_SUSPENDED",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.SUSPENDED), GdSelfStatusEnum.STOP_OK,
                                List.of(AD_SUSPENDED_BY_USER)),
                        GdiBannerPrimaryStatus.MANUALLY_SUSPENDED},
                {"(DRAFT status, ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER reason, DRAFT state) -> MANUALLY_SUSPENDED",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.DRAFT), GdSelfStatusEnum.DRAFT,
                                List.of(ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER)),
                        GdiBannerPrimaryStatus.MANUALLY_SUSPENDED},
                {"(STOP_CRIT, ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER reason, DRAFT state) -> MANUALLY_SUSPENDED",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.DRAFT), GdSelfStatusEnum.STOP_CRIT,
                                List.of(ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER)),
                        GdiBannerPrimaryStatus.MANUALLY_SUSPENDED},
                {"(STOP_WARN, ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER reason, DRAFT state) -> MANUALLY_SUSPENDED",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.DRAFT), GdSelfStatusEnum.STOP_WARN,
                                List.of(ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER)),
                        GdiBannerPrimaryStatus.MANUALLY_SUSPENDED},
                {"(STOP_OK, ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER reason, DRAFT state) -> MANUALLY_SUSPENDED",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.DRAFT), GdSelfStatusEnum.STOP_OK,
                                List.of(ADGROUP_SHOW_CONDITIONS_SUSPENDED_BY_USER)),
                        GdiBannerPrimaryStatus.MANUALLY_SUSPENDED},
                // Запущенные
                {"(RUN_OK status, ACTIVE reason, PREACCEPTED state) -> ACTIVE",
                        new AggregatedStatusAdData(Set.of(AdStatesEnum.PREACCEPTED), GdSelfStatusEnum.RUN_OK,
                                List.of(ACTIVE)),
                        GdiBannerPrimaryStatus.ACTIVE},
        });
    }

    @Test
    public void convertToRetargetingBaseStatus() {
        GdiBannerPrimaryStatus status = AdPrimaryStatusCalculator.convertToPrimaryStatus(aggregatedStatusAdData);
        assertThat(status).isEqualTo(expectStatus);
    }
}
