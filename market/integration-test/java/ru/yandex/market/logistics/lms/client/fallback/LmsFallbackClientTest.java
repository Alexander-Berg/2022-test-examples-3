package ru.yandex.market.logistics.lms.client.fallback;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lms.client.models.FallbackClientLogTestInfo;
import ru.yandex.market.logistics.lms.client.utils.LmsLomClientLogUtils;
import ru.yandex.market.logistics.lom.entity.InternalVariable;
import ru.yandex.market.logistics.lom.entity.enums.InternalVariableType;
import ru.yandex.market.logistics.lom.lms.client.LmsFallbackClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ParametersAreNonnullByDefault
@DisplayName("Фолбек с походом в лмс")
class LmsFallbackClientTest extends AbstractFallbackWithLoggedCallsTest {

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LmsFallbackClient lmsFallbackClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Override
    void setWriteInClientLog(boolean loggingEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.WRITE_LMS_USAGE_IN_LOG)
                .setValue(String.valueOf(loggingEnabled))
        );
    }

    @Override
    void callClientAndVerifyAndCheckLog(FallbackClientLogTestInfo testInfo) {
        testInfo.getLmsDataClientCallingAndVerifying().run();
        softly.assertThat(backLogCaptor.getResults().toString().contains(
                LmsLomClientLogUtils.lmsClientCalling(testInfo.getMethodName(), testInfo.getParams())
            ))
            .isEqualTo(testInfo.isLoggingEnabled());
    }

    @Nonnull
    @Override
    FallbackClientLogTestInfo getLogisticsPointTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    lmsFallbackClient.getLogisticsPoint(1L);
                    verify(lmsClient).getLogisticsPoint(1L);
                }
            )
            .setMethodName("getLogisticsPointById")
            .setParams("id = 1");
    }

    @Nonnull
    @Override
    FallbackClientLogTestInfo getPartnerTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    lmsFallbackClient.getPartner(1L);
                    verify(lmsClient).getPartner(1L);
                }
            )
            .setMethodName("getPartnerById")
            .setParams("id = 1");
    }

    @Nonnull
    @Override
    FallbackClientLogTestInfo getPartnersTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    lmsFallbackClient.getPartners(Set.of(1L));
                    verify(lmsClient).searchPartners(SearchPartnerFilter.builder().setIds(Set.of(1L)).build());
                }
            )
            .setMethodName("getPartnersByIds")
            .setParams("[1]");
    }

    @Nonnull
    @Override
    FallbackClientLogTestInfo getLogisticsPointsTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
                        .ids(Set.of(1L))
                        .partnerIds(Set.of(2L))
                        .type(PointType.PICKUP_POINT)
                        .active(true)
                        .build();
                    lmsFallbackClient.getLogisticsPoints(filter);
                    verify(lmsClient).getLogisticsPoints(filter);
                }
            )
            .setMethodName("getLogisticsPointsByFilter")
            .setParams("ids = [1], partnerIds = [2], type = PICKUP_POINT, active = true");
    }

    @Nonnull
    @Override
    FallbackClientLogTestInfo getPartnerExternalParamsTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    Set<PartnerExternalParamType> types = Set.of(
                        PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA
                    );
                    lmsFallbackClient.getPartnerExternalParams(types);
                    verify(lmsClient).getPartnerExternalParams(types);
                }
            )
            .setMethodName("getPartnerExternalParamsByTypes")
            .setParams("types = [DISABLE_AUTO_CANCEL_AFTER_SLA]");
    }

    @Nonnull
    @Override
    FallbackClientLogTestInfo searchPartnerRelationWithCutoffsTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
                        .fromPartnersIds(Set.of(1L))
                        .toPartnersIds(Set.of(2L))
                        .build();
                    lmsFallbackClient.searchPartnerRelationWithCutoffs(partnerRelationFilter);
                    verify(lmsClient).searchPartnerRelation(partnerRelationFilter, new PageRequest(0, 1));
                }
            )
            .setMethodName("searchPartnerRelationWithCutoffs")
            .setParams("partnerFromIds = [1], partnerToIds = [2]");
    }

    @Nonnull
    @Override
    FallbackClientLogTestInfo searchPartnerRelationsWithReturnPartnersTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
                        .fromPartnersIds(Set.of(1L))
                        .build();
                    lmsFallbackClient.searchPartnerRelationsWithReturnPartners(partnerRelationFilter);
                    verify(lmsClient).searchPartnerRelation(partnerRelationFilter);
                }
            )
            .setMethodName("searchPartnerRelationsWithReturnPartners")
            .setParams("partnerFromIds = [1]");
    }

    @Nonnull
    @Override
    FallbackClientLogTestInfo searchInboundScheduleTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    LogisticSegmentInboundScheduleFilter filter = new LogisticSegmentInboundScheduleFilter()
                        .setFromPartnerId(11L)
                        .setToPartnerId(22L)
                        .setDeliveryType(DeliveryType.COURIER);
                    lmsFallbackClient.searchInboundSchedule(filter);
                    verify(lmsClient).searchInboundSchedule(filter);
                }
            )
            .setMethodName("searchInboundSchedule")
            .setParams("fromPartnerId = 11, toPartnerId = 22, deliveryType = COURIER");
    }

    @Nonnull
    @Override
    FallbackClientLogTestInfo searchPartnerApiSettingsMethodsTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    SettingsMethodFilter settingsMethodFilter = SettingsMethodFilter.newBuilder()
                        .partnerIds(Set.of(111L))
                        .methodTypes(Set.of("updateRecipient"))
                        .build();
                    lmsFallbackClient.searchPartnerApiSettingsMethods(settingsMethodFilter);
                    verify(lmsClient).searchPartnerApiSettingsMethods(settingsMethodFilter);
                }
            )
            .setMethodName("searchPartnerApiSettingsMethods")
            .setParams("partnerIds = [111], methodTypes = [updateRecipient]");
    }

    @Nonnull
    @Override
    FallbackClientLogTestInfo getScheduleDayTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    lmsFallbackClient.getScheduleDay(123L);
                    verify(lmsClient).getScheduleDay(123L);
                }
            )
            .setMethodName("getScheduleDayById")
            .setParams("id = 123");
    }
}
