package ru.yandex.market.mbi.api.controller.operation.replication;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.balance.model.OrderInfo;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.tariff.TariffParamValue;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.GetOrCreateMarketIdRequest;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdPartner;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.type.ExtendedShipmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.model.ConfigureShopDto;
import ru.yandex.market.logistics.nesu.client.model.ShopWithSendersDto;
import ru.yandex.market.logistics.nesu.client.model.filter.ShopWithSendersFilter;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.HasMappingsResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerBalanceReplicateRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicateRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.balance.BalanceConstants.SUPPLIER_BALANCE_ORDER_PRODUCT_ID;

/**
 * Тестирует полную репликацию fbs в fbs. Тест вызывает те же ручки и в том же порядке, как в bmp-процессе.
 * Предполагается, что ранее партнер был созддан через лайтовую регистрацию
 * {@link ru.yandex.market.partner.mvc.controller.partner.replication.PartnerReplicationController#registerForReplication},
 * для него был создан склад в nesu и у нас создана фф-линка.
 *
 * @author Vadim Lyalin
 * @see
 * <a href="https://a.yandex-team.ru/arc_vcs/market/mbi/mbi-bpmn/src/main/resources/processes/fbs_to_fbs_replication.bpmn">
 * bpmn процесс репликации fbs в fbs</a>
 */
public class FbsToFbsReplicationTest extends FunctionalTest {
    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceService balanceService;
    @Autowired
    @Qualifier("impatientBalanceService")
    private BalanceContactService balanceContactService;
    @Autowired
    private PushApi pushApi;
    @Autowired
    private NesuClient nesuClient;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    long donorPartnerId = 582562;
    long replicaPartnerId = 11;
    long actorId = 222;
    long regionId = 21639;
    long regionIdNearestCity = 10748;
    long marketId = 2015622;
    long clientId = 63304240;
    long replicaWarehouseId = 110;
    long businessId = 834448L;

    /**
     * Проверяет репликацию подключенного fbs партнера, у которого программа в статусе SUCCESS.
     * Таблички before скопированы из прода для партнера 582562 (размещается через API)
     */
    @Test
    @DbUnitDataSet(before = "FbsToFbsReplicationTest.before.csv", after = "FbsToFbsReplicationTest.after.csv")
    void testSuccessReplication() {
        testReplication();
        verifyMocks();
    }

    void testReplication() {
        initOtherMocks();

        ReplicateRequest replicateRequest = new ReplicateRequest()
                .sourcePartnerId(donorPartnerId)
                .newPartnerId(replicaPartnerId);

        //процесс реликации запускается с фронта

        // nesu привязывает склад к партнеру
        mbiApiClient.updatePartnerFulfillmentLink(replicaPartnerId, replicaWarehouseId);

        // регистрация партнера
        getMbiOpenApiClient().replicatePartner(actorId, new ReplicatePartnerRequest()
                .partnerDonorId(donorPartnerId)
                .replicaPartnerId(replicaPartnerId)
                .regionId(regionId)
                .acceptorPlacementType(PartnerPlacementType.FBS));

        // создание дефолтного фида
        getMbiOpenApiClient().requestPartnerFeedDefault(actorId, replicaPartnerId);

        // регистрация в балансе
        getMbiOpenApiClient().requestPartnerBalanceCopy(actorId,
                new PartnerBalanceReplicateRequest()
                        .sourcePartnerId(donorPartnerId)
                        .newPartnerId(replicaPartnerId));

        // копирование юр. инфо
        getMbiOpenApiClient().requestPartnerLegalInfoCopy(actorId,
                new PartnerLegalInfoRequest()
                        .sourcePartnerId(donorPartnerId)
                        .newPartnerId(replicaPartnerId));

        // донастройка lms партнера
        getMbiOpenApiClient().confirmLogisticPartner(actorId, replicaPartnerId);

        // ассортимент копируется в dataCamp, mbi-api не участвует

        // ожидание прорастания маппингов и их загрузка
        HasMappingsResponse hasMappingsResponse = getMbiOpenApiClient().hasMappings(actorId, replicaPartnerId);

        // копирование фич и катофов
        getMbiOpenApiClient().requestPartnerFeatureCopy(actorId, replicateRequest);

        assertThat(hasMappingsResponse)
                .returns(true, HasMappingsResponse::getHasMappings);
    }

    private void verifyMocks() {

        // проверяем создание кампании в балансе
        verify(balanceService).getClient(clientId);
        OrderInfo orderInfo = new OrderInfo(
                new CampaignInfo(11, replicaPartnerId, clientId,
                        TariffParamValue.CLICKS.getTariffId(),
                        CampaignType.SUPPLIER),
                SUPPLIER_BALANCE_ORDER_PRODUCT_ID,
                "Склад Москва");
        verify(balanceService).createOrUpdateOrderByCampaign(orderInfo, actorId);

        // проверяем регистрацию реплики в marketId
        verify(marketIdServiceImplBase).getOrCreateMarketId(eq(
                GetOrCreateMarketIdRequest.newBuilder()
                        .setPartnerId(replicaPartnerId)
                        .setPartnerType("SUPPLIER")
                        .setUid(1002269535)
                        .setLegalInfo(LegalInfo.newBuilder()
                                .setLegalName("ООО \"ЛАБОРАТОРИЯ ИННОВАЦИЙ\"")
                                .setType("OOO")
                                .setRegistrationNumber("1187746551123")
                                .setLegalAddress("127473, г. Москва, Краснопролетарская улица, дом 16, " +
                                        "строение 1, " +
                                        "помещение II, комната 32")
                                .setPhysicalAddress("127473, г. Москва, Краснопролетарская улица, дом 16, " +
                                        "строение 1, подъезд 5, офис 1-305")
                                .setInn("7720431412")
                                .setKpp("770701001")
                                .build())
                        .build()),
                any());
        verify(marketIdServiceImplBase).confirmLegalInfo(any(), any());
        verify(marketIdServiceImplBase, times(2)).getByPartner(eq(
                GetByPartnerRequest.newBuilder()
                        .setPartner(MarketIdPartner.newBuilder()
                                .setPartnerId(replicaPartnerId)
                                .setPartnerType("SUPPLIER")
                                .build())
                        .build()),
                any());

        // проверяем настройку склада в LMS
        verify(lmsClient, times(1)).getBusinessWarehouseForPartner(replicaWarehouseId);
        verify(lmsClient, times(5)).getPartner(replicaWarehouseId);
        verify(lmsClient, times(2)).changePartnerStatus(replicaWarehouseId, PartnerStatus.INACTIVE);
        verify(lmsClient).changePartnerStatus(replicaWarehouseId, PartnerStatus.ACTIVE);
        verify(lmsClient).updatePartnerSettings(replicaWarehouseId, PartnerSettingDto.newBuilder()
                .stockSyncEnabled(true)
                .autoSwitchStockSyncEnabled(false)
                .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                .korobyteSyncEnabled(false)
                .build());


        //1 создание ff-линки
        //2 юр инфо
        //3 копирование фич
        InOrder inOrder = Mockito.inOrder(nesuClient);
        inOrder.verify(nesuClient, times(3)).setStockSyncStrategy(replicaWarehouseId, replicaPartnerId, true);

        // проверяем донастройку магазина в nesu (привязку к маркетИд).
        // TODO: MBI-82651 (убирает times(2))
        verify(nesuClient, times(2)).configureShop(replicaPartnerId, ConfigureShopDto.builder()
                .marketId(marketId)
                .balancePersonId(8062296L)
                .balanceClientId(63313544L)
                .balanceContractId(899974L)
                .build());

        verifyNoMoreInteractions(balanceService, marketIdServiceImplBase, pushApi, nesuClient, lmsClient);
    }

    private void initOtherMocks() {
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo(clientId, ClientType.OOO));

        when(lmsClient.getBusinessWarehouseForPartner(replicaWarehouseId)).thenReturn(Optional.of(
                BusinessWarehouseResponse.newBuilder()
                        .partnerStatus(PartnerStatus.ACTIVE)
                        .partnerType(PartnerType.DROPSHIP)
                        .partnerId(replicaWarehouseId)
                        .name("DropShip_Москва")
                        .readableName("Склад Москва")
                        .externalId("639d4d4a-9d59-4e27-8587-4c1c205f1f88")
                        .locationId((int) regionIdNearestCity)
                        .businessId(businessId)
                        .shipmentType(ExtendedShipmentType.IMPORT)
                        .address(Address.newBuilder()
                                .settlement("Москва")
                                .build())
                        .build()));
        when(lmsClient.getPartner(replicaWarehouseId)).thenReturn(Optional.of(PartnerResponse.newBuilder()
                .id(replicaWarehouseId)
                .build()));

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
        doAnswer(new Answer<List<ShopWithSendersDto>>() {
            private boolean isMarketIdSet = false;

            @Override
            public List<ShopWithSendersDto> answer(InvocationOnMock invocation) {
                if (isMarketIdSet) {
                    return List.of(ShopWithSendersDto.builder().id(replicaPartnerId).marketId(marketId).build());
                } else {
                    isMarketIdSet = true;
                    return List.of();
                }
            }
        }).when(nesuClient).searchShopWithSenders(eq(ShopWithSendersFilter.builder()
                .shopIds(Collections.singleton(replicaPartnerId)).build()));
    }

    /**
     * Проверяет репликацию подключенного fbs партнера, у которого программа в статусе SUCCESS.
     * Таблички before скопированы из прода для партнера 582562 (размещается через API),
     * и к ним добавлена фича B2B_SELLER и фича B2C_SELLER. При копировании фичи B2C_SELLER со значением DONT_WANT
     * на нее вешается катофф PARTNER
     */
    @Test
    @DbUnitDataSet(before = "FbsToFbsReplicationWithB2BSellerTest.before.csv", after = "FbsToFbsReplicationWithB2BSellerTest.after.csv")
    void testSuccessReplicationB2BSeller() {
        testReplication();
        verifyMocks();
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "FbsToFbsReplicationTest.before.csv",
                    "PartnerReplicationController.enableBpmn.before.csv"
            },
            after = "FbsToFbsReplicationTest.after.csv"
    )
    void testNoInteractionsWithCpaIsPartnerInterfaceListenerWhileReplicatingDbs() {
        testReplication();
        verifyMocks();
    }
}
