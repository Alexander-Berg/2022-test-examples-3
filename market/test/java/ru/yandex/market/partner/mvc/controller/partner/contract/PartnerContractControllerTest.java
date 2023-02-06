package ru.yandex.market.partner.mvc.controller.partner.contract;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.id.service.MarketIdGrpcService;
import ru.yandex.market.core.state.event.PartnerAppChangesProtoLBEvent;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.data.PartnerAppDataOuterClass;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Функциональные тесты на {@link PartnerContractController}
 */
@DbUnitDataSet(before = "db/PartnerContractControllerTest.before.csv")
public class PartnerContractControllerTest extends FunctionalTest {
    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private MarketIdGrpcService marketIdGrpcService;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    private LogbrokerEventPublisher<PartnerAppChangesProtoLBEvent> logbrokerPartnerAppChangesEventPublisher;

    @BeforeEach
    private void before() {
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(1001L).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getOrCreateMarketId(any(), any());

        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(1001L).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).confirmLegalInfo(any(), any());
    }

    private static Stream<Arguments> getContractsTestParams() {
        return Stream.of(
                Arguments.of("Контракты поставщика с проставленной датой", 60L, "response/supplier.contract.withDates" +
                        ".json"),
                Arguments.of("Контракты поставщика с непроставленной датой", 70L, "response/supplier.contract" +
                        ".withoutDates.json"),
                Arguments.of("Контракты ДСБС", 100L, "response/shop.contract.withDates.json")
        );
    }

    @Test
    void testGetContractsWithDuplicate() {
        assertThatThrownBy(() -> getContracts(90))
                .isInstanceOfSatisfying(HttpServerErrorException.InternalServerError.class,
                        exception -> assertThat(exception.getResponseBodyAsString())
                                .contains("Only one active external id of type SPENDABLE is allowed for client 9." +
                                        " Found: 2"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "getContractsTestParams")
    void testGetContracts(String testDesc, long campaignId, String fileName) {
        ResponseEntity<String> response = getContracts(campaignId);

        JsonTestUtil.assertEquals(response, StringTestUtil.getString(getClass(), fileName));
    }

    private ResponseEntity<String> getContracts(long campaignId) {
        return FunctionalTestHelper.get(baseUrl + "/partners/" + campaignId + "/contracts");
    }

    @Test
    @DbUnitDataSet(after = "db/PartnerContractControllerTest.link.dbs.after.csv")
    void testPostContractsLinkDbs() {
        long campaignId = 110L;
        ResponseEntity<String> response =
                FunctionalTestHelper.post(baseUrl + "/campaigns/" + campaignId + "/contract/link?euid=11234",
                        JsonTestUtil.getJsonHttpEntity("{\"sourcePartnerId\":6}"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }


    @Test
    @DbUnitDataSet(after = "db/PartnerContractControllerTest.link.supplier.after.csv")
    void testPostContractsLinkSupplier() {
        long campaignId = 111L;
        willReturn(Optional.of(MarketAccount.newBuilder()
                .setMarketId(12345L)
                .build()
        )).given(marketIdGrpcService).findByPartner(eq(12L), eq(CampaignType.SUPPLIER));
        ResponseEntity<String> response =
                FunctionalTestHelper.post(baseUrl + "/campaigns/" + campaignId + "/contract/link?euid=11234",
                        JsonTestUtil.getJsonHttpEntity("{\"sourcePartnerId\":6}"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(nesuClient, times(1)).registerShop(any());
    }


    @ParameterizedTest
    @MethodSource("testPostContractsLinkSupplierWithDraftData")
    @DbUnitDataSet(after = "db/PartnerContractControllerTest.link.draft.after.csv")
    void testPostContractsLinkSupplierWithDraft(String body) {
        long campaignId = 113L;
        ResponseEntity<String> response =
                FunctionalTestHelper.post(baseUrl + "/campaigns/" + campaignId + "/contract/link?euid=11234",
                        JsonTestUtil.getJsonHttpEntity(body));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        //проверяем отправку эвента в логброкер
        ArgumentCaptor<PartnerAppChangesProtoLBEvent> captor =
                ArgumentCaptor.forClass(PartnerAppChangesProtoLBEvent.class);
        Mockito.verify(logbrokerPartnerAppChangesEventPublisher, times(3)).publishEventAsync(captor.capture());
        List<PartnerAppChangesProtoLBEvent> events = captor.getAllValues();
        Set<Long> closedRequestIds = new HashSet<>();
        Set<Long> closedPartnerIds = new HashSet<>();
        Set<Long> updatedRequestIds = new HashSet<>();
        Set<Long> updatedPartnerIds = new HashSet<>();
        events.forEach(e -> {
            PartnerAppDataOuterClass.PartnerAppData partnerAppData = e.getPayload();
            if (GeneralData.ActionType.DELETE.equals(partnerAppData.getGeneralInfo().getActionType())) {
                //удаляем INTERNAL_CLOSED
                assertThat(partnerAppData.getStatus())
                        .isEqualTo(PartnerAppDataOuterClass.PartnerAppStatus.INTERNAL_CLOSED);
                closedPartnerIds.addAll(partnerAppData.getPartnerIdsList());
                closedRequestIds.add(partnerAppData.getRequestId());
            } else {
                updatedPartnerIds.addAll(partnerAppData.getPartnerIdsList());
                updatedRequestIds.add(partnerAppData.getRequestId());
            }
        });
        assertThat(closedRequestIds).containsExactlyInAnyOrder(13L, 113L);
        assertThat(closedPartnerIds).containsExactlyInAnyOrder(13L);
        assertThat(updatedRequestIds).containsExactlyInAnyOrder(6L);
        assertThat(updatedPartnerIds).containsExactlyInAnyOrder(6L, 13L);
    }

    private static Stream<Arguments> testPostContractsLinkSupplierWithDraftData() {
        return Stream.of(Arguments.of("{\"sourcePartnerId\":6,\"contractId\":\"incomeContract6\"}"),
                Arguments.of("{\"sourcePartnerId\":6}"));
    }

    @Test
    void testPostContractsLinkSupplierWithContractId404() {
        long campaignId = 113L;
        assertThatThrownBy(() ->
                FunctionalTestHelper.post(baseUrl + "/campaigns/" + campaignId + "/contract/link?euid=11234",
                        JsonTestUtil.getJsonHttpEntity("{\"sourcePartnerId\":6,\"contractId\":\"incomeContract61\" }")))
                .isInstanceOfSatisfying(HttpClientErrorException.NotFound.class,
                        exception -> assertThat(exception.getResponseBodyAsString())
                                .contains("Prepay request for external contract id not found incomeContract61")
                );
    }

    @Test
    @DbUnitDataSet(after = "db/PartnerContractControllerTest.before.csv")
    void testPostContractsLinkDbs400() {
        long campaignId = 1000692905;
        assertThatThrownBy(() ->
                FunctionalTestHelper.post(baseUrl + "/campaigns/" + campaignId + "/contract/link?_user_id=67282295",
                        JsonTestUtil.getJsonHttpEntity("{\"sourcePartnerId\":10409084}")))
                .isInstanceOfSatisfying(HttpServerErrorException.InternalServerError.class,
                        exception -> assertThat(exception.getResponseBodyAsString())
                                .contains("Application in status COMPLETED already exists for partner 10383838"));
    }

    @Test
    @DbUnitDataSet(after = "db/PostContractsLinkSupplierActiveContracts.after.csv")
    void testPostContractsLinkSupplierActiveContracts() {
        long campaignId = 114L;
        willReturn(Optional.of(MarketAccount.newBuilder()
                .setMarketId(12345L)
                .build()
        )).given(marketIdGrpcService).findByPartner(eq(14L), eq(CampaignType.SUPPLIER));
        ResponseEntity<String> response =
                FunctionalTestHelper.post(baseUrl + "/campaigns/" + campaignId + "/contract/link?euid=11234",
                        JsonTestUtil.getJsonHttpEntity("{\"sourcePartnerId\":6}"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(nesuClient, times(1)).registerShop(any());
    }

    @Test
    @DbUnitDataSet(after = "db/PartnerContractControllerTest.before.csv")
    void testPostContractsLinkEverActivated() {
        long campaignId = 115L;
        assertThatThrownBy(() ->
                FunctionalTestHelper.post(baseUrl + "/campaigns/" + campaignId + "/contract/link?_user_id=11234",
                        JsonTestUtil.getJsonHttpEntity("{\"sourcePartnerId\":6}")))
                .isInstanceOfSatisfying(HttpClientErrorException.BadRequest.class,
                        exception -> assertThat(exception.getResponseBodyAsString())
                                .contains("Can't change application for partner 15 with activated program"));
    }
}
