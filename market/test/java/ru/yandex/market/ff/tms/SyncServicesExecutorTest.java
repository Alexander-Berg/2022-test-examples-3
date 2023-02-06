package ru.yandex.market.ff.tms;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.service.FulfillmentInfoService;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.legalInfo.LegalInfoResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Интеграционный тест для {@link SyncServicesExecutor}.
 *
 * @author avetokhin 03.08.18.
 * @author anton19979 03.12.19.
 */
class SyncServicesExecutorTest extends IntegrationTest {

    @Autowired
    private FulfillmentInfoService fulfillmentInfoService;

    private SyncServicesExecutor executor;

    @BeforeEach
    void init() {
        executor = new SyncServicesExecutor(lmsClient, fulfillmentInfoService);
    }

    @Test
    @DatabaseSetup("classpath:tms/sync-ff-services/before.xml")
    @ExpectedDatabase(value = "classpath:tms/sync-ff-services/after.xml", assertionMode = NON_STRICT)
    @SuppressWarnings("checkstyle:MethodLength")
    void testSync() {
        List<PartnerResponse> ffPartnerResponses = List.of(
                PartnerResponse.newBuilder().id(1L).name("service11").status(PartnerStatus.ACTIVE)
                        .partnerType(PartnerType.FULFILLMENT).marketId(11L).build()
        );

        List<PartnerResponse> dcPartnerResponses = Arrays.asList(
                PartnerResponse.newBuilder().id(4L).name("service44").status(PartnerStatus.FROZEN)
                        .partnerType(PartnerType.DISTRIBUTION_CENTER).marketId(44L).build(),
                PartnerResponse.newBuilder().id(5L).name("service55").status(PartnerStatus.FROZEN)
                        .partnerType(PartnerType.DISTRIBUTION_CENTER).marketId(55L).build(),
                PartnerResponse.newBuilder().id(6L).name("service66").status(PartnerStatus.FROZEN)
                        .partnerType(PartnerType.DISTRIBUTION_CENTER).marketId(66L).build()
        );

        when(lmsClient.searchPartners(eq(SearchPartnerFilter.builder().setTypes(Set.of(PartnerType.FULFILLMENT))
                .build()))).thenReturn(ffPartnerResponses);
        when(lmsClient.searchPartners(eq(SearchPartnerFilter.builder().setTypes(Set.of(PartnerType.DISTRIBUTION_CENTER))
                .build()))).thenReturn(dcPartnerResponses);

        when(lmsClient.getLogisticsPoints(
                refEq(LogisticsPointFilter.newBuilder()
                        .partnerIds(Collections.singleton(1L))
                        .type(PointType.WAREHOUSE)
                        .build())))
                .thenReturn(Collections.singletonList(LogisticsPointResponse.newBuilder()
                        .address(Address.newBuilder()
                                .addressString("А это ваш адрес")
                                .build())
                        .build()));
        when(lmsClient.getLogisticsPoints(
                refEq(LogisticsPointFilter.newBuilder()
                        .partnerIds(Collections.singleton(4L))
                        .type(PointType.WAREHOUSE)
                        .build())))
                .thenReturn(Collections.singletonList(LogisticsPointResponse.newBuilder()
                        .address(Address.newBuilder()
                                .settlement("Москва")
                                .street("Кремль")
                                .house("1")
                                .build())
                        .build()));
        when(lmsClient.getLogisticsPoints(
                refEq(LogisticsPointFilter.newBuilder()
                        .partnerIds(Collections.singleton(5L))
                        .type(PointType.WAREHOUSE)
                        .build())))
                .thenReturn(Collections.singletonList(LogisticsPointResponse.newBuilder()
                        .address(Address.newBuilder()
                                .settlement("Москва")
                                .street("Кремль")
                                .house("1")
                                .build())
                        .build()));
        when(lmsClient.getLogisticsPoints(
                refEq(LogisticsPointFilter.newBuilder()
                        .partnerIds(Collections.singleton(6L))
                        .type(PointType.WAREHOUSE)
                        .build())))
                .thenReturn(Collections.emptyList());

        when(lmsClient.getPartnerLegalInfo(eq(5L))).thenReturn(Optional.empty());
        when(lmsClient.getPartnerLegalInfo(eq(6L))).thenReturn(Optional.empty());
        when(lmsClient.getPartnerLegalInfo(eq(1L))).thenReturn(Optional.of(getLegalInfoResponse(
                "ООО \"Ваш поставщик 1\"", "А это ваш юридический адрес")));
        when(lmsClient.getPartnerLegalInfo(eq(4L))).thenReturn(Optional.of(getLegalInfoResponse(
                "ООО \"Ваш поставщик 3\"", "А это тоже не ваш юридический адрес")));

        executor.doJob(null);

        verify(lmsClient).searchPartners(eq(SearchPartnerFilter.builder().setTypes(
                Set.of(PartnerType.FULFILLMENT))
                .build()));
        verify(lmsClient).searchPartners(eq(SearchPartnerFilter.builder().setTypes(
                        Set.of(PartnerType.DISTRIBUTION_CENTER))
                .build()));

        verify(lmsClient).getLogisticsPoints(
                refEq(LogisticsPointFilter.newBuilder()
                        .partnerIds(Collections.singleton(1L))
                        .type(PointType.WAREHOUSE)
                        .build()));
        verify(lmsClient).getLogisticsPoints(
                refEq(LogisticsPointFilter.newBuilder()
                        .partnerIds(Collections.singleton(4L))
                        .type(PointType.WAREHOUSE)
                        .build()));
        verify(lmsClient).getLogisticsPoints(
                refEq(LogisticsPointFilter.newBuilder()
                        .partnerIds(Collections.singleton(5L))
                        .type(PointType.WAREHOUSE)
                        .build()));
        verify(lmsClient).getLogisticsPoints(
                refEq(LogisticsPointFilter.newBuilder()
                        .partnerIds(Collections.singleton(6L))
                        .type(PointType.WAREHOUSE)
                        .build()));

        verify(lmsClient).getPartnerLegalInfo(eq(4L));
        verify(lmsClient).getPartnerLegalInfo(eq(5L));
        verify(lmsClient).getPartnerLegalInfo(eq(6L));

        verify(lmsClient).getPartnerLegalInfo(eq(1L));
        verify(lmsClient).getPartnerLegalInfo(eq(4L));
        verify(lmsClient).getPartnerLegalInfo(eq(5L));
        verify(lmsClient).getPartnerLegalInfo(eq(6L));

        verifyNoMoreInteractions(lmsClient);
    }


    @Test
    @DatabaseSetup("classpath:tms/sync-ff-services/before.xml")
    void testSyncWithError() {
        List<PartnerResponse> ffPartnerResponses = List.of(
                PartnerResponse.newBuilder().id(1L).name("service11").status(PartnerStatus.ACTIVE)
                        .partnerType(PartnerType.FULFILLMENT).marketId(11L).build()
        );

        List<PartnerResponse> dcPartnerResponses = List.of(
                PartnerResponse.newBuilder().id(2L).name("service2").status(PartnerStatus.INACTIVE)
                        .partnerType(PartnerType.DISTRIBUTION_CENTER).marketId(22L).build()
        );

        when(lmsClient.searchPartners(eq(SearchPartnerFilter.builder().setTypes(Set.of(PartnerType.FULFILLMENT))
                .build()))).thenReturn(ffPartnerResponses);
        when(lmsClient.searchPartners(eq(SearchPartnerFilter.builder().setTypes(Set.of(PartnerType.DISTRIBUTION_CENTER))
                .build()))).thenReturn(dcPartnerResponses);

        when(lmsClient.getLogisticsPoints(
                refEq(LogisticsPointFilter.newBuilder()
                        .partnerIds(Collections.singleton(1L))
                        .type(PointType.WAREHOUSE)
                        .build())))
                .thenThrow(new RuntimeException("Http Error"));

        when(lmsClient.getLogisticsPoints(
                refEq(LogisticsPointFilter.newBuilder()
                        .partnerIds(Collections.singleton(2L))
                        .type(PointType.WAREHOUSE)
                        .build())))
                .thenReturn(Collections.emptyList());

        try {
            executor.doJob(null);
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains("success: 1"));
        }

        verify(lmsClient).searchPartners(eq(SearchPartnerFilter.builder().setTypes(
                        Set.of(PartnerType.FULFILLMENT))
                .build()));

        verify(lmsClient).getLogisticsPoints(
                refEq(LogisticsPointFilter.newBuilder()
                        .partnerIds(Collections.singleton(2L))
                        .type(PointType.WAREHOUSE)
                        .build()));


    }

    @Nonnull
    private LegalInfoResponse getLegalInfoResponse(String inc, String address) {
        return new LegalInfoResponse(
                null,
                null,
                inc,
                null, null,
                null,
                null,
                null,
                Address.newBuilder().addressString(address).build(),
                null,
                null,
                null,
                null,
                null
        );
    }
}
