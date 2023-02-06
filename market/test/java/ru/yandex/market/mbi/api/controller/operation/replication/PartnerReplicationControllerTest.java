package ru.yandex.market.mbi.api.controller.operation.replication;

import java.util.OptionalLong;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.common.balance.model.BalanceException;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.balance.model.OrderInfo;
import ru.yandex.market.core.id.service.MarketIdGrpcService;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.DefaultFeedResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerBalanceReplicateRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerBalanceReplicateResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType;
import ru.yandex.market.mbi.open.api.client.model.PartnerReplicateVatRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerReplicateVatResponse;
import ru.yandex.market.mbi.open.api.client.model.ReplicateFbsToDbsRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicateFbsToDbsResponse;
import ru.yandex.market.mbi.open.api.client.model.ReplicateFeatureResponse;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicateRequest;
import ru.yandex.market.mbi.open.api.client.model.ScheduleReplicationResponse;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PartnerReplicationControllerTest extends FunctionalTest {

    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceService balanceService;
    @Autowired
    private NesuClient nesuClient;
    @Autowired
    private CheckouterShopApi checkouterShopApi;
    @Autowired
    private CheckouterAPI checkouterClient;
    @Autowired
    private MarketIdGrpcService marketIdGrpcService;

    @Test
    void testShouldThrowNotFoundPartner() {
        PartnerLegalInfoRequest request = new PartnerLegalInfoRequest();
        request.setSourcePartnerId(123L);
        request.setNewPartnerId(321L);
        MbiOpenApiClientResponseException exception = Assertions.assertThrows(MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().requestPartnerLegalInfoCopy(123L, request));
        assertThat(exception.getApiError().getMessage(), containsString("Partner with id 123 was not found"));
        Assertions.assertEquals(exception.getHttpErrorCode(), 404);
    }

    @Test
    @DbUnitDataSet(before = "PartnerReplicationController.testCopyApplication.before.csv",
            after = "PartnerReplicationController.testCopyApplication.after.csv")
    void testShouldCopyOnlyLegalInfo() {
        long marketId = 333;
        PartnerLegalInfoRequest request = new PartnerLegalInfoRequest();
        request.setSourcePartnerId(123L);
        request.setNewPartnerId(321L);
        doReturn(OptionalLong.of(marketId)).when(marketIdGrpcService).getOrCreateMarketId(321, true);

        PartnerLegalInfoResponse partnerLegalInfoResponse = getMbiOpenApiClient()
                .requestPartnerLegalInfoCopy(123L, request);
        Assertions.assertEquals(1L, partnerLegalInfoResponse.getPartnerApplicationId());
        Assertions.assertEquals(false, partnerLegalInfoResponse.getVatCopied());
        Assertions.assertEquals(true, partnerLegalInfoResponse.getReturnContactCopied());
    }

    @Test
    @DbUnitDataSet(before = "PartnerReplicationController.testCopyReturnContact.before.csv")
    void testCopyLegalInfoShouldThrowNoPrepayRequest() {
        PartnerLegalInfoRequest request = new PartnerLegalInfoRequest();
        request.setSourcePartnerId(123L);
        request.setNewPartnerId(321L);
        MbiOpenApiClientResponseException exception = Assertions.assertThrows(MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().requestPartnerLegalInfoCopy(123L, request));
        assertThat(exception.getApiError().getMessage(), containsString("Prepay request not found for source partner:" +
                " 123"));
        Assertions.assertEquals(exception.getHttpErrorCode(), 404);
    }

    @Test
    @DbUnitDataSet(before = "PartnerReplicationController.testCopyAllLegalInfo.before.csv",
            after = "PartnerReplicationController.testCopyAllLegalInfo.after.csv")
    void testShouldCopyAllLegalInfo() {
        long marketId = 333;
        PartnerLegalInfoRequest request = new PartnerLegalInfoRequest();
        request.setSourcePartnerId(123L);
        request.setNewPartnerId(321L);

        doReturn(OptionalLong.of(marketId)).when(marketIdGrpcService).getOrCreateMarketId(321, true);

        PartnerLegalInfoResponse partnerLegalInfoResponse = getMbiOpenApiClient()
                .requestPartnerLegalInfoCopy(123L, request);
        Assertions.assertEquals(1L, partnerLegalInfoResponse.getPartnerApplicationId());
        Assertions.assertEquals(true, partnerLegalInfoResponse.getVatCopied());
        Assertions.assertEquals(true, partnerLegalInfoResponse.getReturnContactCopied());

        verify(marketIdGrpcService).getOrCreateMarketId(321, true);
        verify(marketIdGrpcService).linkOrCreateMarketId(321, 1, null, 1);
        verifyNoMoreInteractions(marketIdGrpcService);
    }

    @Test
    @DbUnitDataSet(before = "PartnerReplicationControllerTest.testReturnExistedLegalInfo.before.csv",
            after = "PartnerReplicationControllerTest.testReturnExistedLegalInfo.before.csv")
    void testReturnExistedLegalInfo() {
        long marketId = 333;
        long existedReplicaApplicationId = 2;

        PartnerLegalInfoRequest request = new PartnerLegalInfoRequest();
        request.setSourcePartnerId(123L);
        request.setNewPartnerId(321L);
        doReturn(OptionalLong.of(marketId)).when(marketIdGrpcService).getOrCreateMarketId(321, true);

        PartnerLegalInfoResponse partnerLegalInfoResponse = getMbiOpenApiClient()
                .requestPartnerLegalInfoCopy(123L, request);
        org.assertj.core.api.Assertions.assertThat(partnerLegalInfoResponse)
                .returns(existedReplicaApplicationId, PartnerLegalInfoResponse::getPartnerApplicationId)
                .returns(false, PartnerLegalInfoResponse::getVatCopied)
                .returns(false, PartnerLegalInfoResponse::getReturnContactCopied);
    }

    @Test
    @DbUnitDataSet(before = "PartnerReplicationController.testCopyApplication.before.csv",
            after = "PartnerReplicationController.testCopySelfEmployedParam.after.csv")
    void testShouldCopySelfEmployedParamType() {
        long marketId = 333;
        PartnerLegalInfoRequest request = new PartnerLegalInfoRequest();
        request.setSourcePartnerId(123L);
        request.setNewPartnerId(321L);
        doReturn(OptionalLong.of(marketId)).when(marketIdGrpcService).getOrCreateMarketId(321, true);

        PartnerLegalInfoResponse partnerLegalInfoResponse = getMbiOpenApiClient()
                .requestPartnerLegalInfoCopy(123L, request);
        Assertions.assertEquals(1L, partnerLegalInfoResponse.getPartnerApplicationId());
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerReplicationController.testCopyVat.before.csv",
            after = "PartnerReplicationController.testCopyVat.after.csv"
    )
    void testCopyVat() {
        PartnerReplicateVatRequest request = new PartnerReplicateVatRequest()
                .sourcePartnerId(123L)
                .newPartnerId(321L);
        PartnerReplicateVatResponse partnerLegalInfoResponse = getMbiOpenApiClient()
                .requestPartnerVatCopy(123L, request);
        Assertions.assertTrue(partnerLegalInfoResponse.getVatCopied());
    }

    @Test
    @DbUnitDataSet(before = "PartnerReplicationController.testCopyApplication.before.csv")
    void testShouldThrowNotFoundCampaign() {
        PartnerBalanceReplicateRequest request = new PartnerBalanceReplicateRequest();
        request.setSourcePartnerId(123L);
        request.setNewPartnerId(321L);
        MbiOpenApiClientResponseException exception = Assertions.assertThrows(MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().requestPartnerBalanceCopy(123L, request));
        assertThat(exception.getApiError().getMessage(), containsString("Not found source campaign"));
        Assertions.assertEquals(exception.getHttpErrorCode(), 404);
    }

    @Test
    @DbUnitDataSet(before = "PartnerReplicationController.testCopyCampaign.before.csv",
            after = "PartnerReplicationController.testCopyCampaign.after.csv")
    void testShouldCopyCampaign() {
        long sourcesPartnerId = 123;
        long newPartnerId = 321;
        Mockito.when(balanceService.getClient(999L)).thenReturn(new ClientInfo());
        PartnerBalanceReplicateRequest request = new PartnerBalanceReplicateRequest();
        request.setSourcePartnerId(sourcesPartnerId);
        request.setNewPartnerId(newPartnerId);
        PartnerBalanceReplicateResponse response = getMbiOpenApiClient().requestPartnerBalanceCopy(123L, request);
        Assertions.assertNotNull(response.getCampaignId());
        ArgumentCaptor<OrderInfo> argument = ArgumentCaptor.forClass(OrderInfo.class);
        verify(balanceService).createOrUpdateOrderByCampaign(argument.capture(), anyLong());
        Assertions.assertEquals(newPartnerId, argument.getValue().getCampaignInfo().getDatasourceId());
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerReplicationController.testCopySupplierCampaign.before.csv",
            after = "PartnerReplicationController.testCopySupplierCampaign.after.csv"
    )
    void testCopySupplierCampaign() {
        Mockito.when(balanceService.getClient(100L)).thenReturn(new ClientInfo(100L, ClientType.OOO));
        var request = new PartnerBalanceReplicateRequest()
                .sourcePartnerId(1L)
                .newPartnerId(2L);
        var response = getMbiOpenApiClient().requestPartnerBalanceCopy(111L, request);
        Assertions.assertEquals(20L, response.getCampaignId());
        ArgumentCaptor<OrderInfo> argument = ArgumentCaptor.forClass(OrderInfo.class);
        verify(balanceService).createOrUpdateOrderByCampaign(argument.capture(), anyLong());
        Assertions.assertEquals(2L, argument.getValue().getCampaignInfo().getDatasourceId());
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerReplicationController.testManagerNotFound.before.csv",
            after = "PartnerReplicationController.testManagerNotFound.after.csv"
    )
    void testManagerNotFound() {
        Mockito.when(balanceService.getClient(100L)).thenReturn(new ClientInfo(100L, ClientType.OOO));
        MutableBoolean firstCall = new MutableBoolean(true);
        doAnswer(invocation -> {
            if (firstCall.booleanValue()) {
                firstCall.setValue(false);
                throw new BalanceException(new XmlRpcException("" +
                        "<error>\n" +
                        "    <msg>Manager for uid 1494833601 was not found</msg>\n" +
                        "    <wo-rollback>0</wo-rollback>\n" +
                        "    <method>_process_order</method>\n" +
                        "    <code>MANAGER_NOT_FOUND</code>\n" +
                        "    <parent-codes>\n" +
                        "        <code>EXCEPTION</code>\n" +
                        "        <code>NOT_FOUND</code>\n" +
                        "    </parent-codes>\n" +
                        "    <contents>Manager for uid 1494833601 was not found</contents>\n" +
                        "</error>"));
            }
            return null;
        })
                .when(balanceService)
                .createOrUpdateOrderByCampaign(any(OrderInfo.class), anyLong());

        var request = new PartnerBalanceReplicateRequest()
                .sourcePartnerId(1L)
                .newPartnerId(2L);
        var response = getMbiOpenApiClient().requestPartnerBalanceCopy(111L, request);
        Assertions.assertEquals(2L, response.getCampaignId());
        ArgumentCaptor<OrderInfo> argument = ArgumentCaptor.forClass(OrderInfo.class);
        verify(balanceService, times(2)).createOrUpdateOrderByCampaign(argument.capture(), anyLong());
        Assertions.assertEquals(2L, argument.getValue().getCampaignInfo().getDatasourceId());
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerReplicationController.testCopySupplierCampaign.before.csv",
            after = "PartnerReplicationController.testCopySupplierCampaign.after.csv"
    )
    void testCopyCampaignSeveralTimes() {
        Mockito.when(balanceService.getClient(100L)).thenReturn(new ClientInfo(100L, ClientType.OOO));
        var request = new PartnerBalanceReplicateRequest()
                .sourcePartnerId(1L)
                .newPartnerId(2L);
        var response = getMbiOpenApiClient().requestPartnerBalanceCopy(111L, request);
        var secondResponse = getMbiOpenApiClient().requestPartnerBalanceCopy(111L, request);

        Assertions.assertEquals(response, secondResponse);
        Assertions.assertEquals(20L, response.getCampaignId());
        ArgumentCaptor<OrderInfo> argument = ArgumentCaptor.forClass(OrderInfo.class);
        verify(balanceService).createOrUpdateOrderByCampaign(argument.capture(), anyLong());
        Assertions.assertEquals(2L, argument.getValue().getCampaignInfo().getDatasourceId());
    }

    @Test
    @DbUnitDataSet(before = "PartnerReplicationController.testReturnExistingFeed.before.csv",
            after = "PartnerReplicationController.testReturnExistingFeed.after.csv")
    void testShouldReturnExistingDefaultFeedForPartner() {
        long newPartnerId = 321L;
        long existingFeedId = 1234567L;
        DefaultFeedResponse response = getMbiOpenApiClient().requestPartnerFeedDefault(123L, newPartnerId);
        Assertions.assertEquals(response.getFeedId(), existingFeedId);
        Assertions.assertEquals(response.getPartnerId(), newPartnerId);
    }

    @Test
    @DbUnitDataSet(before = "PartnerReplicationController.testDefaultFeed.before.csv",
            after = "PartnerReplicationController.testDefaultFeed.after.csv")
    void testShouldCreateDefaultFeedForPartner() {
        long newPartnerId = 321L;
        DefaultFeedResponse response = getMbiOpenApiClient().requestPartnerFeedDefault(123L, newPartnerId);
        Assertions.assertNotNull(response.getFeedId());
        Assertions.assertEquals(response.getPartnerId(), newPartnerId);
    }

    /**
     * Проверяет регистрацию партнера в nesu.
     */
    @Test
    @DbUnitDataSet(before = "PartnerReplicationController.testNesuRegistration.before.csv")
    void testNesuRegistration() {
        long partnerId = 123;

        getMbiOpenApiClient().registerPartnerNesu(1L, partnerId, "склад", "123");

        verify(nesuClient).registerShop(RegisterShopDto.builder()
                .id(partnerId)
                .regionId(215)
                .name("склад")
                .externalId("123")
                .role(ShopRole.DROPSHIP_BY_SELLER)
                .build());
        verifyNoMoreInteractions(nesuClient);
    }

    @Test
    @DbUnitDataSet(before = "PartnerReplicationController.testFeatureCopy.before.csv",
            after = "PartnerReplicationController.testFeatureCopy.after.csv")
    void testShouldCopyFeatureFromSourcePartner() {
        long sourcesPartnerId = 123L;
        long newPartnerId = 321L;
        ReplicateRequest request = new ReplicateRequest();
        request.setSourcePartnerId(sourcesPartnerId);
        request.setNewPartnerId(newPartnerId);
        ReplicateFeatureResponse response = getMbiOpenApiClient().requestPartnerFeatureCopy(123L, request);
        Assertions.assertEquals(Boolean.TRUE, response.getFeatureCopied());
    }

    /**
     * Проверяет репликацию расписания, когда у донора расписание отсутствует
     */
    @Test
    @DbUnitDataSet(before = "PartnerReplicationController.testFeatureCopy.before.csv")
    void testScheduleReplicationNoSchedule() {
        ScheduleReplicationResponse response = getMbiOpenApiClient().requestPartnerScheduleCopy(
                1L, new ReplicateRequest().sourcePartnerId(123L).newPartnerId(321L));

        //noinspection ConstantConditions
        assertFalse(response.getCopied());
    }

    @Test
    @DbUnitDataSet(before = {"PartnerReplicationController.testFeatureCopy.before.csv",
            "PartnerReplicationController.testScheduleReplication.before.csv"},
            after = "PartnerReplicationController.testScheduleReplication.after.csv")
    void testScheduleReplication() {
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
        doReturn(mock(ShopMetaData.class)).when(checkouterShopApi).getShopData(eq(321L));

        ScheduleReplicationResponse response = getMbiOpenApiClient().requestPartnerScheduleCopy(
                1L, new ReplicateRequest().sourcePartnerId(123L).newPartnerId(321L));

        //noinspection ConstantConditions
        assertTrue(response.getCopied());
        verify(checkouterShopApi).getShopData(321L);
        verify(checkouterShopApi).pushSchedules(Mockito.argThat(map -> {
            org.assertj.core.api.Assertions.assertThat(map)
                    .returns(1, MultiMap::keyCount);
            return true;
        }));
        verifyNoMoreInteractions(checkouterShopApi);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerReplicationController.testReplicationFbyPartner.before.csv",
            after = "PartnerReplicationController.testReplicationFbyPartner.after.csv"
    )
    void testReplicationFbsToFbyPartner() {
        Mockito.when(balanceService.getClient(1000L)).thenReturn(new ClientInfo(1000L, ClientType.OAO));
        var request = new ReplicatePartnerRequest();
        request.setPartnerDonorId(10L);
        request.setAcceptorPlacementType(PartnerPlacementType.FBY);
        getMbiOpenApiClient().replicatePartner(1000L, request);
    }


    @Test
    @DbUnitDataSet(
            before = "PartnerReplicationController.testReplicateFbsToDbsWithIgnoreStocks.before.csv",
            after = "PartnerReplicationController.testReplicateFbsToDbsWithIgnoreStocks.after.csv"
    )
    void testReplicateFbsToDbsWithIgnoreStocks() {
        long partnerDonorId = 10L;
        long regionId = 213L;
        long uid = 1000L;
        String partnerName = "ДБСик";

        when(checkouterClient.shops()).thenReturn(checkouterShopApi);

        final ReplicateFbsToDbsRequest request = new ReplicateFbsToDbsRequest()
                .partnerDonorId(partnerDonorId)
                .regionId(regionId)
                .newPartnerName(partnerName)
                .ignoreStocks(true)
                .moderationEnabled(false);
        ReplicateFbsToDbsResponse response = getMbiOpenApiClient().replicateFbsToDbsPartner(uid, request);

        Assertions.assertNotNull(response.getPartnerId());
    }

    @Test
    @DbUnitDataSet(before = {
            "PartnerReplicationController.testReplicateFbsToDbsWithIgnoreStocks.before.csv",
            "PartnerReplicationController.enableBpmn.before.csv"
    })
    void testNoInteractionsWithCpaIsPartnerInterfaceListenerWhileReplicatingFbsToDbs() {

        long partnerDonorId = 10L;
        long regionId = 213L;
        long uid = 1000L;
        String partnerName = "ДБСик";

        final ReplicateFbsToDbsRequest request = new ReplicateFbsToDbsRequest()
                .partnerDonorId(partnerDonorId)
                .regionId(regionId)
                .newPartnerName(partnerName)
                .ignoreStocks(false)
                .moderationEnabled(false);

        getMbiOpenApiClient().replicateFbsToDbsPartner(uid, request);

        Mockito.verifyNoInteractions(checkouterShopApi);
        Mockito.verifyNoInteractions(checkouterClient);
        Mockito.verifyNoInteractions(nesuClient);
    }
}
