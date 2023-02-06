package ru.yandex.market.mbi.bpmn.process.replication.fbs2dbs;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import Market.DataCamp.API.CopyOffers;
import Market.DataCamp.DataCampOfferMeta;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.PartnerPlacementProgramType;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.DeliveryTariffDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopRegionGroupsDto;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinkDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinksDTO;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.client.RetryableTarifficatorClient;
import ru.yandex.market.mbi.bpmn.util.CamundaTestUtil;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.DefaultFeedResponse;
import ru.yandex.market.mbi.open.api.client.model.HasCourierDeliveryResponse;
import ru.yandex.market.mbi.open.api.client.model.NotificationPartnerInfoDTO;
import ru.yandex.market.mbi.open.api.client.model.NotificationPartnerInfoRequest;
import ru.yandex.market.mbi.open.api.client.model.NotificationPartnerInfoResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerActivationResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerBalanceReplicateResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoResponse;
import ru.yandex.market.mbi.open.api.client.model.ReplicateFbsToDbsResponse;
import ru.yandex.market.partner.notification.client.model.DestinationDTO;
import ru.yandex.market.partner.notification.client.model.SendNotificationRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.bpmn.model.enums.ProcessType.FBS_TO_DBS_REPLICATION;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FbsToDbsReplicationProcessTest extends FunctionalTest {
    @Autowired
    public MbiOpenApiClient client;

    @Autowired
    public MbiApiClient mbiApiClient;

    @Autowired
    public DataCampClient dataCampShopClient;

    @Autowired
    private RetryableTarifficatorClient tarifficatorClient;

    @Autowired
    private PartnerNotificationClient notificationClient;

    protected long uid = 444L;
    protected Long partnerDonorId = 123L;
    protected String partnerDonorName = "Исходный FBS";
    protected String partnerLegalInfoDonorName = "Донор юр. данных";
    protected long legalInfoDonorPartnerId = 456L;
    protected long partnerId = 111L;
    protected String warehouseName = "Склад";
    protected String partnerWarehouseId = "";
    protected long warehouseId = 5L;
    protected long businessId = 999L;
    protected long partnerApplicationId = 1L;
    protected long feedId = 115L;
    protected int taskId = 888;
    protected long campaignId = 1111L;
    protected String operationId = "op-id";
    private final Integer regionId = 2;

    @Test
    @DisplayName("Проверяет успешное выполнение быстрой репликации")
    public void testSuccessFastProcess() throws InterruptedException {
        mockCommonSteps();

        //Копирование юр. данных
        when(client.requestPartnerLegalInfoCopy(anyLong(), any(PartnerLegalInfoRequest.class)))
                .thenReturn(new PartnerLegalInfoResponse()
                        .newPartnerId(partnerId)
                        .sourcePartnerId(partnerDonorId)
                        .partnerApplicationId(partnerApplicationId)
                        .returnContactCopied(true)
                        .vatCopied(true)
                );

        //Активация партнера
        when(mbiOpenApiClient.activatePartner(anyLong(), anyLong()))
                .thenReturn(new PartnerActivationResponse().partnerActivated(true));

        RuntimeService runtimeService = processEngine.getRuntimeService();
        var processInstance = runtimeService.startProcessInstanceByKey(
                FBS_TO_DBS_REPLICATION.getId(),
                TEST_BUSINESS_KEY,
                Map.of("uid", uid,
                        "partnerDonorId", partnerDonorId,
                        "warehouseName", warehouseName,
                        "businessId", businessId,
                        "regionId", regionId,
                        "skipModeration", true
                )
        );

        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                processInstance.getProcessInstanceId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, processInstance);

        verifyCommonSteps();

        verify(client).replicateFbsToDbsPartner(eq(uid), argThat(
                request -> partnerDonorId.equals(request.getPartnerDonorId()) &&
                        warehouseName.equals(request.getNewPartnerName()) &&
                        regionId.equals(request.getRegionId().intValue()) &&
                        Boolean.TRUE.equals(request.getIgnoreStocks()) &&
                        Boolean.FALSE.equals(request.getModerationEnabled())
        ));

        verify(tarifficatorClient).saveShopMetaData(eq(partnerId), eq(uid), argThat(shopMetaData ->
                Long.valueOf(regionId).equals(shopMetaData.getLocalRegion()) &&
                        shopMetaData.getCurrency() == Currency.RUR &&
                        List.of(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                                .equals(shopMetaData.getPlacementPrograms())));

        verify(client).requestPartnerLegalInfoCopy(eq(uid), argThat(
                request -> request.getSourcePartnerId().equals(partnerDonorId) &&
                        request.getNewPartnerId().equals(partnerId)));

        verify(mbiOpenApiClient).activatePartner(partnerId, uid);

        verifyNoMoreInteractions(mbiOpenApiClient, client, dataCampShopClient, tarifficatorClient, notificationClient);

    }

    @Test
    @DisplayName("Проверяет успешное выполнение репликации с двумя донорами")
    public void testSuccessFullProcess() throws InterruptedException {
        mockCommonSteps();

        //Копирование юр. данных
        when(client.requestPartnerLegalInfoCopy(anyLong(), any(PartnerLegalInfoRequest.class)))
                .thenReturn(new PartnerLegalInfoResponse()
                        .newPartnerId(partnerId)
                        .sourcePartnerId(legalInfoDonorPartnerId)
                        .partnerApplicationId(partnerApplicationId)
                        .returnContactCopied(true)
                        .vatCopied(true)
                );

        //Получение имени донора
        when(client.providePartnerInfo(eq(new NotificationPartnerInfoRequest().partnerIds(List.of(partnerDonorId)))))
                .thenReturn(new NotificationPartnerInfoResponse().partnerInfos(
                        List.of(new NotificationPartnerInfoDTO().partnerName(partnerDonorName))
                ));

        //Получение имени донора юр. данных
        when(client.providePartnerInfo(eq(new NotificationPartnerInfoRequest()
                .partnerIds(List.of(legalInfoDonorPartnerId)))))
                .thenReturn(new NotificationPartnerInfoResponse().partnerInfos(
                        List.of(new NotificationPartnerInfoDTO().partnerName(partnerLegalInfoDonorName))
                ));

        //Получение имени реплики
        when(client.providePartnerInfo(eq(new NotificationPartnerInfoRequest().partnerIds(List.of(partnerId)))))
                .thenReturn(new NotificationPartnerInfoResponse().partnerInfos(
                        List.of(new NotificationPartnerInfoDTO().partnerName(warehouseName))
                ));

        RuntimeService runtimeService = processEngine.getRuntimeService();
        var processInstance = runtimeService.startProcessInstanceByKey(
                FBS_TO_DBS_REPLICATION.getId(),
                TEST_BUSINESS_KEY,
                Map.of("uid", uid,
                        "partnerDonorId", partnerDonorId,
                        "warehouseName", warehouseName,
                        "businessId", businessId,
                        "regionId", regionId,
                        "legalInfoDonorPartnerId", legalInfoDonorPartnerId
                )
        );

        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                processInstance.getProcessInstanceId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, processInstance);

        verifyCommonSteps();

        verify(client).replicateFbsToDbsPartner(eq(uid), argThat(
                request -> partnerDonorId.equals(request.getPartnerDonorId()) &&
                        warehouseName.equals(request.getNewPartnerName()) &&
                        regionId.equals(request.getRegionId().intValue()) &&
                        Boolean.TRUE.equals(request.getIgnoreStocks()) &&
                        Boolean.TRUE.equals(request.getModerationEnabled())
        ));

        verify(tarifficatorClient).saveShopMetaData(eq(partnerId), eq(uid), argThat(shopMetaData ->
                Long.valueOf(regionId).equals(shopMetaData.getLocalRegion()) &&
                        shopMetaData.getCurrency() == Currency.RUR &&
                        List.of(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                                .equals(shopMetaData.getPlacementPrograms())));

        verify(client).requestPartnerLegalInfoCopy(eq(uid), argThat(
                request -> request.getSourcePartnerId().equals(legalInfoDonorPartnerId) &&
                        request.getNewPartnerId().equals(partnerId)));

        verify(mbiOpenApiClient).providePartnerInfo(
                argThat(argument -> List.of(partnerDonorId).equals(argument.getPartnerIds())));
        verify(mbiOpenApiClient).providePartnerInfo(
                argThat(argument -> List.of(legalInfoDonorPartnerId).equals(argument.getPartnerIds())));
        verify(mbiOpenApiClient).providePartnerInfo(
                argThat(argument -> List.of(partnerId).equals(argument.getPartnerIds())));

        ArgumentCaptor<SendNotificationRequest> requestCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(notificationClient).sendNotification(requestCaptor.capture());

        SendNotificationRequest request = requestCaptor.getValue();
        Assertions.assertEquals(Boolean.FALSE, request.getRenderOnly());
        Assertions.assertEquals(1654676084L, request.getTypeId());
        Assertions.assertEquals(new DestinationDTO().shopId(partnerDonorId), request.getDestination());
        Assertions.assertEquals("<data><donorWarehouseName>" + partnerDonorName + "</donorWarehouseName>" +
                "<donorShopId>" + partnerDonorId + "</donorShopId>" +
                "<legalDonorWarehouseName>" + partnerLegalInfoDonorName + "</legalDonorWarehouseName>" +
                "<legalDonorShopId>" + legalInfoDonorPartnerId + "</legalDonorShopId>" +
                "<newWarehouseName>" + warehouseName + "</newWarehouseName>" +
                "<warehouseId>" + warehouseId + "</warehouseId>" +
                "<newShopId>" + partnerId + "</newShopId>" +
                "<newCampaignId>" + campaignId + "</newCampaignId></data>", request.getData());

        verifyNoMoreInteractions(mbiOpenApiClient, client, dataCampShopClient, tarifficatorClient, notificationClient);
    }

    Stream<Map<String, Object>> singleDonorData() {
        return Stream.of(
                Map.of("uid", uid,
                        "partnerDonorId", partnerDonorId,
                        "warehouseName", warehouseName,
                        "businessId", businessId,
                        "regionId", regionId
                ), // legalInfoDonorPartnerId не задан
                Map.of("uid", uid,
                        "partnerDonorId", partnerDonorId,
                        "warehouseName", warehouseName,
                        "businessId", businessId,
                        "regionId", regionId,
                        "legalInfoDonorPartnerId", partnerDonorId
                ) //legalInfoDonorPartnerId совпадает с partnerDonorId
        );
    }

    @ParameterizedTest(name = "{index}: {displayName}}")
    @MethodSource("singleDonorData")
    @DisplayName("Проверяет успешное выполнение репликации с одним донором")
    public void testSuccessFullWithSingleDonorProcess(Map<String, Object> data) throws InterruptedException {
        mockCommonSteps();

        //Копирование юр. данных
        when(client.requestPartnerLegalInfoCopy(anyLong(), any(PartnerLegalInfoRequest.class)))
                .thenReturn(new PartnerLegalInfoResponse()
                        .newPartnerId(partnerId)
                        .sourcePartnerId(partnerDonorId)
                        .partnerApplicationId(partnerApplicationId)
                        .returnContactCopied(true)
                        .vatCopied(true)
                );

        //Получение имени донора
        when(client.providePartnerInfo(eq(new NotificationPartnerInfoRequest().partnerIds(List.of(partnerDonorId)))))
                .thenReturn(new NotificationPartnerInfoResponse().partnerInfos(
                        List.of(new NotificationPartnerInfoDTO().partnerName(partnerDonorName))
                ));

        //Получение имени реплики
        when(client.providePartnerInfo(eq(new NotificationPartnerInfoRequest().partnerIds(List.of(partnerId)))))
                .thenReturn(new NotificationPartnerInfoResponse().partnerInfos(
                        List.of(new NotificationPartnerInfoDTO().partnerName(warehouseName))
                ));

        RuntimeService runtimeService = processEngine.getRuntimeService();
        var processInstance = runtimeService.startProcessInstanceByKey(
                FBS_TO_DBS_REPLICATION.getId(),
                TEST_BUSINESS_KEY,
                data
        );

        assertTrue(CamundaTestUtil.waitUntilNoActiveJobs(
                processEngine,
                processInstance.getProcessInstanceId()
        ));

        CamundaTestUtil.checkIncidents(processEngine, processInstance);

        verifyCommonSteps();

        verify(client).replicateFbsToDbsPartner(eq(uid), argThat(
                request -> partnerDonorId.equals(request.getPartnerDonorId()) &&
                        warehouseName.equals(request.getNewPartnerName()) &&
                        regionId.equals(request.getRegionId().intValue()) &&
                        Boolean.TRUE.equals(request.getIgnoreStocks()) &&
                        Boolean.TRUE.equals(request.getModerationEnabled())
        ));

        verify(tarifficatorClient).saveShopMetaData(eq(partnerId), eq(uid), argThat(shopMetaData ->
                Long.valueOf(regionId).equals(shopMetaData.getLocalRegion()) &&
                        shopMetaData.getCurrency() == Currency.RUR &&
                        List.of(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                                .equals(shopMetaData.getPlacementPrograms())));

        verify(client).requestPartnerLegalInfoCopy(eq(uid), argThat(
                request -> request.getSourcePartnerId().equals(partnerDonorId) &&
                        request.getNewPartnerId().equals(partnerId)));

        verify(mbiOpenApiClient).providePartnerInfo(
                argThat(argument -> List.of(partnerDonorId).equals(argument.getPartnerIds())));
        verify(mbiOpenApiClient).providePartnerInfo(
                argThat(argument -> List.of(partnerId).equals(argument.getPartnerIds())));

        ArgumentCaptor<SendNotificationRequest> requestCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(notificationClient).sendNotification(requestCaptor.capture());

        SendNotificationRequest request = requestCaptor.getValue();
        Assertions.assertEquals(Boolean.FALSE, request.getRenderOnly());
        Assertions.assertEquals(1654676084L, request.getTypeId());
        Assertions.assertEquals(new DestinationDTO().shopId(partnerDonorId), request.getDestination());
        Assertions.assertEquals("<data><donorWarehouseName>" + partnerDonorName + "</donorWarehouseName>" +
                "<donorShopId>" + partnerDonorId + "</donorShopId>" +
                "<newWarehouseName>" + warehouseName + "</newWarehouseName>" +
                "<warehouseId>" + warehouseId + "</warehouseId>" +
                "<newShopId>" + partnerId + "</newShopId>" +
                "<newCampaignId>" + campaignId + "</newCampaignId></data>", request.getData());

        verifyNoMoreInteractions(mbiOpenApiClient, client, dataCampShopClient, tarifficatorClient, notificationClient);
    }

    private void mockCommonSteps() {
        when(client.replicateFbsToDbsPartner(any(), any())).thenReturn(
                new ReplicateFbsToDbsResponse().partnerId(partnerId)
        );

        when(client.requestPartnerFeedDefault(anyLong(), anyLong())).thenReturn(
                new DefaultFeedResponse().partnerId(partnerId).feedId(feedId)
        );

        //Ожидание создания склада
        when(mbiApiClient.getPartnerFulfillments(anyLong(), any())).thenReturn(
                new PartnerFulfillmentLinksDTO(
                        List.of(new PartnerFulfillmentLinkDTO(partnerId, warehouseId,
                                feedId, DeliveryServiceType.DROPSHIP_BY_SELLER))
                )
        );

        //регистрация в балансе
        when(mbiOpenApiClient.requestPartnerBalanceCopy(any(), any()))
                .thenReturn(new PartnerBalanceReplicateResponse()
                        .campaignId(campaignId));

        //Заполнение региональных настроек доставки
        when(tarifficatorClient.getRegionGroups(anyLong()))
                .thenReturn(new ShopRegionGroupsDto()
                        .regionsGroups(List.of(new RegionGroupDto()
                                .selfRegion(true)
                                .id(999L))));

        //Запуск копирования ассортимента
        when(dataCampShopClient.startCopyOffers(any(CopyOffers.OffersCopyTask.class))).thenReturn(
                CopyOffers.OffersCopyTaskStatus.newBuilder()
                        .setId(taskId)
                        .setStatus(CopyOffers.OffersCopyTaskStatus.EStatus.STARTED)
                        .build()
        );

        //Ожидание копирования ассортимента
        when(dataCampShopClient.getCopyTaskStatus(anyLong(), anyLong(), anyInt()))
                .thenReturn(CopyOffers.OffersCopyTaskStatus.newBuilder()
                        .setId(taskId)
                        .setStatus(CopyOffers.OffersCopyTaskStatus.EStatus.FINISHED)
                        .build());

        //Ожидание копирования настроек доставки
        when(mbiOpenApiClient.hasCourierDelivery(anyLong(), anyLong()))
                .thenReturn(new HasCourierDeliveryResponse().hasCourierDelivery(true));
    }

    private void verifyCommonSteps() {
        verify(client).requestPartnerFeedDefault(uid, partnerId);

        verify(mbiOpenApiClient).registerPartnerNesu(uid, partnerId, warehouseName, partnerWarehouseId);

        verify(mbiApiClient).getPartnerFulfillments(eq(partnerId), argThat(
                types -> types.contains(DeliveryServiceType.DROPSHIP_BY_SELLER)));

        verify(mbiOpenApiClient).requestPartnerBalanceCopy(eq(uid), argThat(argument ->
                argument.getNewPartnerId().equals(partnerId) &&
                        argument.getSourcePartnerId().equals(partnerDonorId)));

        verify(mbiOpenApiClient).confirmLogisticPartner(uid, partnerId);

        verify(tarifficatorClient).getRegionGroups(partnerId);

        verify(tarifficatorClient).createTariff(eq(partnerId), eq(uid), eq(999L), any(DeliveryTariffDto.class));

        verify(dataCampShopClient).startCopyOffers(argThat(
                task -> task.getBusinessId() == businessId &&
                        task.getDstShopId() == partnerId &&
                        task.getSrcShopIdsCount() == 1 &&
                        task.getSrcShopIds(0) == partnerDonorId &&
                        task.getDstWarehouseId() == warehouseId &&
                        task.getCopyContentFromShop() == partnerDonorId &&
                        task.getRgb() == DataCampOfferMeta.MarketColor.WHITE));
        verify(dataCampShopClient).getCopyTaskStatus(businessId, partnerId, taskId);
        verify(mbiOpenApiClient).hasCourierDelivery(partnerId, uid);
    }
}
