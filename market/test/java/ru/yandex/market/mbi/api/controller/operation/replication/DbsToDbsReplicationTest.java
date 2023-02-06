package ru.yandex.market.mbi.api.controller.operation.replication;

import java.util.List;
import java.util.Optional;

import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.common.balance.model.BalanceException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.balance.model.OrderInfo;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.delivery.model.ShopSelfDeliveryState;
import ru.yandex.market.core.delivery.repository.ShopSelfDeliveryDao;
import ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.program.partner.ProgramService;
import ru.yandex.market.core.program.partner.exception.MissedRequiredFieldsException;
import ru.yandex.market.core.program.partner.model.ProgramType;
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
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.model.ConfigureShopDto;
import ru.yandex.market.logistics.nesu.client.model.RegisterShopDto;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.PartnerBalanceReplicateRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicateRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.balance.BalanceConstants.SUPPLIER_BALANCE_ORDER_PRODUCT_ID;
import static ru.yandex.market.core.param.model.ParamType.CPA_IS_PARTNER_INTERFACE;

/**
 * Тестирует полную репликацию dbs в dbs. Тест вызывает те же ручки и в том же порядке, как в bmp-процессе.
 *
 * @author Vadim Lyalin
 * @see
 * <a href="https://a.yandex-team.ru/arc_vcs/market/mbi/mbi-bpmn/src/main/resources/processes/dbs_to_dbs_replication.bpmn">
 * bpmn процесс репликации dbs в dbs</a>
 */
public class DbsToDbsReplicationTest extends FunctionalTest {
    private static final long CLIENT_ID = 8387932;
    private static final long ACTOR_ID = 359155248;

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
    private PushApi pushApi;
    @Autowired
    private NesuClient nesuClient;
    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private ParamService paramService;
    @Autowired
    private ProgramService programService;
    @Autowired
    private ShopSelfDeliveryDao shopSelfDeliveryDao;
    @Autowired
    private PartnerNotificationClient partnerNotificationClient;

    /**
     * Проверяет репликацию подключенного dbs партнера, у которого программа в статусе SUCCESS без отправки на модерацию
     * Таблички before скопированы из прода для партнера 1161653
     */
    @Test
    @DbUnitDataSet(before = "DbsToDbsReplicationTest.before.csv",
            after = {"DbsToDbsReplicationTest.after.csv", "DbsToDbsReplicationTest.nomoder.after.csv"})
    void testSuccessReplicationWithoutModeration() {
        long replicaPartnerId =
                testSuccessReplication(/* вызов из копирования заявок на подключения и из копирования фич */ 2);
        verifyBalance(replicaPartnerId);
    }

    /**
     * Проверяет репликацию подключенного dbs партнера, у которого программа в статусе SUCCESS с отправкой на модерацию.
     * Также проверяет, что для менеджера, незарегистрированного в балансе, партнер создается с дефолтным менеджером.
     * Таблички before скопированы из прода для партнера 1161653
     */
    @Test
    @DbUnitDataSet(before = {"DbsToDbsReplicationTest.before.csv", "DbsToDbsReplicationTest.env.before.csv"},
            after = {"DbsToDbsReplicationTest.after.csv", "DbsToDbsReplicationTest.moderation.after.csv"})
    void testSuccessReplicationWithModeration() {
        MutableBoolean firstCall = new MutableBoolean(true);
        doAnswer(invocation -> {
            if (firstCall.booleanValue()) {
                firstCall.setValue(false);
                throw new BalanceException(new XmlRpcException("" +
                        "<error>\n" +
                        "    <msg>Invalid parameter for function: Manager uid=359155248 with manager_type=1 not " +
                        "found</msg>\n" +
                        "    <wo-rollback>0</wo-rollback>\n" +
                        "    <method>Balance2.CreateOffer</method>\n" +
                        "    <code>INVALID_PARAM</code>\n" +
                        "    <parent-codes>\n" +
                        "        <code>EXCEPTION</code>\n" +
                        "    </parent-codes>\n" +
                        "    <contents>Invalid parameter for function: Manager uid=1399962742 with manager_type=1 " +
                        "not found</contents>\n" +
                        "</error>"));
            }
            return null;
        })
                .when(balanceService)
                .createOrUpdateOrderByCampaign(any(OrderInfo.class), anyLong());

        PartnerNotificationApiServiceTest.setUpClient(partnerNotificationClient, () -> 1L);

        long replicaPartnerId = testSuccessReplication(/* вызов из копирования заявок */ 2);
        verifyBalanceForDefaultManager(replicaPartnerId);
        verify(partnerNotificationClient).sendNotification(any());
        verifyNoMoreInteractions(partnerNotificationClient);
    }

    /**
     * Проверяет успешную dbs-репликацию и отправку на модерацию (datasources_in_testing.status=12)
     * после настроек сисов и метода работы.
     */
    @Test
    @DbUnitDataSet(before = {"DbsToDbsReplicationTest.before.csv", "DbsToDbsReplicationTest.env.before.csv"},
            after = {"DbsToDbsReplicationTest.after.csv", "DbsToDbsReplicationTest.send-to-moder.after.csv",
                    "DbsToDbsReplicationTest.orderAutoAccept.after.csv"})
    void testSendToModeration() throws MissedRequiredFieldsException {
        // запускаем репликацию
        long replicaPartnerId = testSuccessReplication(2);
        // эмулируем выбор партнером метод работы ПИ
        paramService.setParam(new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, replicaPartnerId, true), 123);
        // эмулируем конфигурацию партнером способов доставки
        shopSelfDeliveryDao.saveShopSelfDeliveryState(ShopSelfDeliveryState.builder()
                .setHasCourierDelivery(true)
                .setCourierRegions(List.of(2L))
                .setShopId(replicaPartnerId)
                .build());
        // включаем программу
        programService.enable(replicaPartnerId, ProgramType.DROPSHIP_BY_SELLER, 123);
    }

    /**
     * @param lmsListenerInvocations ожидаемое количество вызовов LmsPartnerStateListener
     * @see ru.yandex.market.core.supplier.state.LmsPartnerStateListener
     */
    private long testSuccessReplication(int lmsListenerInvocations) {
        long donorPartnerId = 1161653;
        long businessId = 734139;
        String warehouseName = "Склад";
        String partnerWarehouseId = "Sklad";
        int regionId = 2;
        long replicaPartnerId;
        long marketId = 2009594;
        long newWarehouseId = 10005; //id склада который якобы был создан в несу

        var businessWarehouse = BusinessWarehouseResponse.newBuilder()
                .businessId(businessId)
                .marketId(marketId)
                .externalId(partnerWarehouseId)
                .name(warehouseName)
                .partnerType(PartnerType.DROPSHIP_BY_SELLER)
                .partnerStatus(PartnerStatus.ACTIVE)
                .partnerId(newWarehouseId);
        initMocks(CLIENT_ID, marketId, businessWarehouse);

        // базовое копирование
        var replicatePartnerResponse = getMbiOpenApiClient().replicateDbsPartner(ACTOR_ID,
                new ReplicatePartnerRequest()
                        .partnerDonorId(donorPartnerId)
                        .warehouseName(warehouseName)
                        .regionId((long) regionId));

        //noinspection ConstantConditions
        replicaPartnerId = replicatePartnerResponse.getPartnerId();
        ReplicateRequest replicateRequest = new ReplicateRequest()
                .sourcePartnerId(donorPartnerId)
                .newPartnerId(replicaPartnerId);

        // регистрация в балансе
        getMbiOpenApiClient().requestPartnerBalanceCopy(ACTOR_ID,
                new PartnerBalanceReplicateRequest()
                        .sourcePartnerId(donorPartnerId)
                        .newPartnerId(replicaPartnerId));

        // создание дефолтного фида
        getMbiOpenApiClient().requestPartnerFeedDefault(ACTOR_ID, replicaPartnerId);

        // создание склада
        getMbiOpenApiClient().registerPartnerNesu(ACTOR_ID, replicaPartnerId, warehouseName, partnerWarehouseId);

        //эмулируем поход несу к нас с только что созданным складом
        mbiApiClient.updatePartnerFulfillmentLink(replicaPartnerId, newWarehouseId);

        // копирование юр. инфо
        getMbiOpenApiClient().requestPartnerLegalInfoCopy(ACTOR_ID,
                new PartnerLegalInfoRequest()
                        .sourcePartnerId(donorPartnerId)
                        .newPartnerId(replicaPartnerId));

        // копирование расписания работы партнера
        getMbiOpenApiClient().requestPartnerScheduleCopy(ACTOR_ID, replicateRequest);

        // копирование правила расчета даты отгрузки
        getMbiOpenApiClient().requestPartnerShipmentDateCopy(ACTOR_ID, replicateRequest);

        // ассортимент копируется в dataCamp, mbi-api не участвует

        // копирование фич и катофов
        getMbiOpenApiClient().requestPartnerFeatureCopy(ACTOR_ID, replicateRequest);

        // проверяем регистрацию реплики в marketId
        verify(marketIdServiceImplBase).getOrCreateMarketId(eq(
                        GetOrCreateMarketIdRequest.newBuilder()
                                .setPartnerId(replicaPartnerId)
                                .setPartnerType("SHOP")
                                .setUid(ACTOR_ID)
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
        verify(marketIdServiceImplBase, times(lmsListenerInvocations)).getByPartner(eq(
                        GetByPartnerRequest.newBuilder()
                                .setPartner(MarketIdPartner.newBuilder()
                                        .setPartnerId(replicaPartnerId)
                                        .setPartnerType("SHOP")
                                        .build())
                                .build()),
                any());

        // проверяем регистрацию склада в nesu
        verify(nesuClient).registerShop(RegisterShopDto.builder()
                .id(replicaPartnerId)
                .regionId(regionId)
                .businessId(734139L)
                .name(warehouseName)
                .role(ShopRole.DROPSHIP_BY_SELLER)
                .externalId(partnerWarehouseId)
                .build());
        verify(nesuClient, times(lmsListenerInvocations)).configureShop(replicaPartnerId, ConfigureShopDto.builder()
                .marketId(marketId)
                .balanceClientId(94092207L)
                .balanceContractId(3601036L)
                .balancePersonId(14764595L)
                .build());

        verifyNoMoreInteractions(marketIdServiceImplBase, pushApi, nesuClient);

        return replicaPartnerId;
    }

    private void verifyBalance(long replicaPartnerId) {
        // проверяем создание кампании в балансе
        verify(balanceService).getClient(CLIENT_ID);
        OrderInfo orderInfo = new OrderInfo(
                new CampaignInfo(1, replicaPartnerId, CLIENT_ID,
                        TariffParamValue.CLICKS.getTariffId(),
                        CampaignType.SHOP),
                SUPPLIER_BALANCE_ORDER_PRODUCT_ID,
                "Склад");
        orderInfo.setManagerUid(45);
        verify(balanceService).createOrUpdateOrderByCampaign(orderInfo, ACTOR_ID);

        verifyNoMoreInteractions(balanceService, balanceContactService);
    }

    private void verifyBalanceForDefaultManager(long replicaPartnerId) {
        // проверяем создание кампании в балансе
        // пытаемся создать кампанию с менеджером
        verify(balanceService, times(2)).getClient(CLIENT_ID);
        OrderInfo orderInfo = new OrderInfo(
                new CampaignInfo(1, replicaPartnerId, CLIENT_ID,
                        TariffParamValue.CLICKS.getTariffId(),
                        CampaignType.SHOP),
                SUPPLIER_BALANCE_ORDER_PRODUCT_ID,
                "Склад");
        orderInfo.setManagerUid(45);
        verify(balanceService).createOrUpdateOrderByCampaign(orderInfo, ACTOR_ID);

        // создаем кампанию с дефолтным менеджером
        OrderInfo defaultManagerOrderInfo = new OrderInfo(
                // при этом campaign_id увеличивается, ну и ладно
                new CampaignInfo(2, replicaPartnerId, CLIENT_ID,
                        TariffParamValue.CLICKS.getTariffId(),
                        CampaignType.SHOP),
                SUPPLIER_BALANCE_ORDER_PRODUCT_ID,
                "Склад");
        defaultManagerOrderInfo.setManagerUid(-2);
        verify(balanceService).createOrUpdateOrderByCampaign(defaultManagerOrderInfo, ACTOR_ID);

        verifyNoMoreInteractions(balanceService, balanceContactService);
    }

    private void initMocks(long clientId, long marketId, BusinessWarehouseResponse.Builder businessWarehouse) {
        when(balanceService.getClient(clientId)).thenReturn(new ClientInfo());
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
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
        doAnswer(invocation -> Optional.of(businessWarehouse.build()))
                .when(lmsClient).getBusinessWarehouseForPartner(anyLong());
    }

    @Test
    @DbUnitDataSet(before = "DbsToDbsReplicationTest.setupApi.before.csv",
            after = {"DbsToDbsReplicationTest.setupApi.after.csv", "DbsToDbsReplicationTest.orderAutoAccept.after.csv"})
    public void testSuccessEnableWarehouseAfterSetupApi() throws Exception {
        initMocks(1, 1, null);
        paramService.setParam(new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, 1L, true),
                123L);
        programService.enable(1L, ProgramType.DROPSHIP_BY_SELLER, 123L);
    }
}
