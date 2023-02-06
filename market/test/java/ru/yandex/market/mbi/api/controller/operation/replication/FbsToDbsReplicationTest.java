package ru.yandex.market.mbi.api.controller.operation.replication;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.balance.model.OrderInfo;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.delivery.model.ShopSelfDeliveryState;
import ru.yandex.market.core.delivery.repository.ShopSelfDeliveryDao;
import ru.yandex.market.core.state.event.BusinessChangesProtoLBEvent;
import ru.yandex.market.core.state.event.ContactChangesProtoLBEvent;
import ru.yandex.market.core.state.event.PartnerAppChangesProtoLBEvent;
import ru.yandex.market.core.state.event.PartnerChangesProtoLBEvent;
import ru.yandex.market.core.supplier.state.PartnerServiceLinkLogbrokerEvent;
import ru.yandex.market.core.tariff.TariffParamValue;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.GetOrCreateMarketIdRequest;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdPartner;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.model.ConfigureShopDto;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.PartnerActivationResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerBalanceReplicateRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicateFbsToDbsRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicateFbsToDbsResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.balance.BalanceConstants.SUPPLIER_BALANCE_ORDER_PRODUCT_ID;

public class FbsToDbsReplicationTest extends FunctionalTest {

    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceService balanceService;
    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceContactService balanceContactService;
    @Autowired
    private CheckouterShopApi checkouterShopApi;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private ShopSelfDeliveryDao shopSelfDeliveryDao;

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private LogbrokerEventPublisher<PartnerChangesProtoLBEvent> logbrokerPartnerChangesEventPublisher;

    @Autowired
    private LogbrokerEventPublisher<BusinessChangesProtoLBEvent> logbrokerBusinessChangesEventPublisher;

    @Autowired
    private LogbrokerEventPublisher<PartnerAppChangesProtoLBEvent> logbrokerPartnerAppChangesEventPublisher;

    @Autowired
    private LogbrokerEventPublisher<ContactChangesProtoLBEvent> logbrokerContactChangesEventPublisher;

    @Autowired
    private LogbrokerEventPublisher<PartnerServiceLinkLogbrokerEvent> partnerFfLinkLbProducer;

    @BeforeEach
    public void init() {
        when(logbrokerPartnerChangesEventPublisher.publishEventAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(logbrokerPartnerAppChangesEventPublisher.publishEventAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(logbrokerBusinessChangesEventPublisher.publishEventAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(logbrokerContactChangesEventPublisher.publishEventAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(partnerFfLinkLbProducer.publishEventAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    @DisplayName("Быстрое создание DBS на основе FBS без модерации")
    @DbUnitDataSet(before = "FbsToDbsReplicationTest.replicateFbsToDbsTest.before.csv",
            after = {"FbsToDbsReplicationTest.replicateFbsToDbsTest.donor.after.csv",
            "FbsToDbsReplicationTest.fastReplicateFbsToDbsTest.replica.after.csv"})
    public void fastReplicateFbsToDbsTest() {
        String partnerName = "DBS Магазин";
        long businessId = 11L;
        long uid = 123L;
        long clientId = 1000L;
        long marketId = 333;
        long donorPartnerId = 10L;
        long newWarehouseId = 10005;
        long contractId = 3601026;

        initMocks(clientId, businessId, marketId, partnerName, newWarehouseId);

        // регистрация партнера
        final ReplicateFbsToDbsRequest request = new ReplicateFbsToDbsRequest()
                .partnerDonorId(donorPartnerId)
                .regionId(213L)
                .newPartnerName(partnerName)
                .moderationEnabled(false);
        ReplicateFbsToDbsResponse response = getMbiOpenApiClient().replicateFbsToDbsPartner(uid, request);
        Assertions.assertNotNull(response.getPartnerId());

        long replicaPartnerId = response.getPartnerId();

        // регистрация в балансе
        getMbiOpenApiClient().requestPartnerBalanceCopy(uid,
                new PartnerBalanceReplicateRequest()
                        .sourcePartnerId(donorPartnerId)
                        .newPartnerId(replicaPartnerId));

        // создание дефолтного фида
        getMbiOpenApiClient().requestPartnerFeedDefault(uid, replicaPartnerId);

        // создание склада
        getMbiOpenApiClient().registerPartnerNesu(uid, replicaPartnerId, partnerName, null);

        //cоздания маппинга склада донора и нового созданного склада
        mbiApiClient.updatePartnerFulfillmentLink(replicaPartnerId, newWarehouseId);

        // копирование юр. инфо
        getMbiOpenApiClient().requestPartnerLegalInfoCopy(uid,
                new PartnerLegalInfoRequest()
                        .sourcePartnerId(donorPartnerId)
                        .newPartnerId(replicaPartnerId));

        // ожидаем, когда тарификатор вернет доставку
        var hasCourierDeliveryResponse = getMbiOpenApiClient().hasCourierDelivery(replicaPartnerId, uid);
        Assertions.assertNotNull(hasCourierDeliveryResponse.getHasCourierDelivery());
        Assertions.assertFalse(hasCourierDeliveryResponse.getHasCourierDelivery());

        // тарификатор возвращает доставку
        shopSelfDeliveryDao.saveShopSelfDeliveryState(
                ShopSelfDeliveryState.builder()
                        .setShopId(replicaPartnerId)
                        .setCourierRegions(List.of(213L))
                        .setHasCourierDelivery(true)
                        .setHasPickupDelivery(false)
                        .setHasPrepay(true)
                        .setLastEventMillis(0)
                        .build()
        );

        // ожидаем, что настройки доставки появились в mbi
        hasCourierDeliveryResponse = getMbiOpenApiClient().hasCourierDelivery(replicaPartnerId, uid);
        Assertions.assertNotNull(hasCourierDeliveryResponse.getHasCourierDelivery());
        Assertions.assertTrue(hasCourierDeliveryResponse.getHasCourierDelivery());

        // активируем фичу
        PartnerActivationResponse activationResponse = getMbiOpenApiClient().activatePartner(replicaPartnerId, uid);

        Assertions.assertNotNull(activationResponse.getPartnerActivated());
        Assertions.assertTrue(activationResponse.getPartnerActivated());

        verifyMocks(replicaPartnerId, partnerName, clientId, clientId, uid, 213, businessId, marketId, contractId, 2);
    }

    @Test
    @DisplayName("Создание DBS на основе FBS")
    @DbUnitDataSet(before = "FbsToDbsReplicationTest.replicateFbsToDbsTest.before.csv",
            after = {"FbsToDbsReplicationTest.replicateFbsToDbsTest.donor.after.csv",
                    "FbsToDbsReplicationTest.replicateFbsToDbsTest.replica.after.csv"})
    public void replicateFbsToDbsTest() {
        String partnerName = "DBS Магазин";
        long businessId = 11L;
        long uid = 123L;
        long clientId = 1000;
        long legalInfoClientId = 1001;
        long marketId = 333;
        long donorPartnerId = 10L;
        long legalInfoDonorPartnerId = 13L;
        long newWarehouseId = 10005;

        initMocks(clientId, businessId, marketId, partnerName, newWarehouseId);

        // регистрация партнера
        final ReplicateFbsToDbsRequest request = new ReplicateFbsToDbsRequest()
                .partnerDonorId(donorPartnerId)
                .regionId(213L)
                .newPartnerName(partnerName);
        ReplicateFbsToDbsResponse response = getMbiOpenApiClient().replicateFbsToDbsPartner(uid, request);
        Assertions.assertNotNull(response.getPartnerId());

        long replicaPartnerId = response.getPartnerId();

        // регистрация в балансе
        getMbiOpenApiClient().requestPartnerBalanceCopy(uid,
                new PartnerBalanceReplicateRequest()
                        .sourcePartnerId(donorPartnerId)
                        .newPartnerId(replicaPartnerId));

        // создание дефолтного фида
        getMbiOpenApiClient().requestPartnerFeedDefault(uid, replicaPartnerId);

        // создание склада
        getMbiOpenApiClient().registerPartnerNesu(uid, replicaPartnerId, partnerName, null);

        //cоздания маппинга склада донора и нового созданного склада
        mbiApiClient.updatePartnerFulfillmentLink(replicaPartnerId, newWarehouseId);

        // копирование юр. инфо
        getMbiOpenApiClient().requestPartnerLegalInfoCopy(uid,
                new PartnerLegalInfoRequest()
                        .sourcePartnerId(legalInfoDonorPartnerId)
                        .newPartnerId(replicaPartnerId));

        // ожидаем, когда тарификатор вернет доставку
        var hasCourierDeliveryResponse = getMbiOpenApiClient().hasCourierDelivery(replicaPartnerId, uid);
        Assertions.assertNotNull(hasCourierDeliveryResponse.getHasCourierDelivery());
        Assertions.assertFalse(hasCourierDeliveryResponse.getHasCourierDelivery());

        // тарификатор возвращает доставку
        shopSelfDeliveryDao.saveShopSelfDeliveryState(
                ShopSelfDeliveryState.builder()
                        .setShopId(replicaPartnerId)
                        .setCourierRegions(List.of(213L))
                        .setHasCourierDelivery(true)
                        .setHasPickupDelivery(false)
                        .setHasPrepay(true)
                        .setLastEventMillis(0)
                        .build()
        );

        // ожидаем, что настройки доставки появились в mbi
        hasCourierDeliveryResponse = getMbiOpenApiClient().hasCourierDelivery(replicaPartnerId, uid);
        Assertions.assertNotNull(hasCourierDeliveryResponse.getHasCourierDelivery());
        Assertions.assertTrue(hasCourierDeliveryResponse.getHasCourierDelivery());

        verifyMocks(replicaPartnerId, partnerName, clientId, legalInfoClientId, uid, 213, businessId, marketId, 3601036L, 1);
    }

    private void initMocks(long clientId, long businessId, long marketId, String warehouseName, long warehouseId) {
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo());
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);

        var businessWarehouse = BusinessWarehouseResponse.newBuilder()
                .businessId(businessId)
                .marketId(marketId)
                .name(warehouseName)
                .partnerType(PartnerType.DROPSHIP_BY_SELLER)
                .partnerStatus(PartnerStatus.ACTIVE)
                .partnerId(warehouseId);

        when(lmsClient.getBusinessWarehouseForPartner(any())).thenReturn(Optional.of(businessWarehouse.build()));

        mockMarketIdCreation(marketId);
    }

    private void mockMarketIdCreation(long marketId) {
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(marketId).build());
            marketAccountStreamObserver.onCompleted();
            return marketId;
        }).when(marketIdServiceImplBase).getOrCreateMarketId(any(), any());
        doAnswer(invocation -> {
            StreamObserver<MarketAccount> marketAccountStreamObserver = invocation.getArgument(1);
            marketAccountStreamObserver.onNext(MarketAccount.newBuilder().setMarketId(marketId).build());
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).confirmLegalInfo(any(), any());
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            var marketAccount = MarketAccount.newBuilder().setMarketId(marketId).build();
            var response = GetByPartnerResponse.newBuilder()
                    .setMarketId(marketAccount).setSuccess(true).build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(any(), any());
    }

    private void verifyMocks(
            long replicaPartnerId,
            String partnerName,
            long clientId,
            long legalInfoClientId,
            long uid,
            int regionId,
            long businessId,
            long marketId,
            long contractId,
            int times
    ) {
        // проверяем создание кампании в балансе
        verifyBalance(replicaPartnerId, clientId, uid, partnerName);

        // проверяем регистрацию marketId
        verifyRegisterMarketId(replicaPartnerId, uid, times);

        // проверяем регистрацию склада в nesu
        verifyNesuWarehouseCreation(replicaPartnerId, regionId, partnerName, businessId, marketId, legalInfoClientId, contractId, times);

        verifyNoMoreInteractions(nesuClient);
    }

    void verifyBalance(long replicaPartnerId,
                       long clientId,
                       long uid,
                       String partnerName) {
        verify(balanceService, times(1)).getClient(clientId);
        OrderInfo orderInfo = new OrderInfo(
                new CampaignInfo(
                        1,
                        replicaPartnerId,
                        clientId,
                        TariffParamValue.CLICKS.getTariffId(),
                        CampaignType.SHOP),
                SUPPLIER_BALANCE_ORDER_PRODUCT_ID,
                partnerName);
        orderInfo.setManagerUid(-2);
        verify(balanceService).createOrUpdateOrderByCampaign(orderInfo, uid);

        verifyNoMoreInteractions(balanceService, balanceContactService);
    }

    void verifyRegisterMarketId(long replicaPartnerId, long uid, int times) {
        verify(marketIdServiceImplBase).getOrCreateMarketId(eq(
                        GetOrCreateMarketIdRequest.newBuilder()
                                .setPartnerId(replicaPartnerId)
                                .setPartnerType("SHOP")
                                .setUid(uid)
                                .setLegalInfo(LegalInfo.newBuilder()
                                        .setLegalName("Варенцов Антон Владимирович")
                                        .setType("IP")
                                        .setRegistrationNumber("314525010700030")
                                        .setLegalAddress("607680, Нижегородская обл, Кстовский р-н, д Афонино, " +
                                                "Магистральная ул, д. 313, кв. 116")
                                        .setPhysicalAddress("")
                                        .build())
                                .build()),
                any());
        verify(marketIdServiceImplBase).confirmLegalInfo(any(), any());
        verify(marketIdServiceImplBase, times(times)).getByPartner(eq(
                GetByPartnerRequest.newBuilder()
                        .setPartner(MarketIdPartner.newBuilder()
                                .setPartnerId(replicaPartnerId)
                                .setPartnerType("SHOP")
                                .build())
                        .build()),
                any());
    }

    void verifyNesuWarehouseCreation(
            long replicaPartnerId,
            int regionId,
            String partnerName,
            long businessId,
            long marketId,
            long clientId,
            long contractId,
            int times
    ) {
        verify(nesuClient).registerShop(RegisterShopDto.builder()
                .id(replicaPartnerId)
                .regionId(regionId)
                .businessId(businessId)
                .name(partnerName)
                .role(ShopRole.DROPSHIP_BY_SELLER)
                .build());
        verify(nesuClient, times(times)).configureShop(replicaPartnerId, ConfigureShopDto.builder()
                .marketId(marketId)
                .balanceClientId(clientId)
                .balanceContractId(contractId)
                .balancePersonId(14764595L)
                .build());
    }
}
