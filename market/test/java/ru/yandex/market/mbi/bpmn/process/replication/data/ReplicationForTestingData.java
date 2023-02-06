package ru.yandex.market.mbi.bpmn.process.replication.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import Market.DataCamp.API.CopyOffers;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponseStatus;
import ru.yandex.market.mbi.api.client.entity.operation.ExternalOperationResult;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatistics;
import ru.yandex.market.mbi.api.client.entity.operation.OperationStatus;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinkDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinksDTO;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.DefaultFeedResponse;
import ru.yandex.market.mbi.open.api.client.model.HasMappingsResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerApplicationRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerBalanceReplicateRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerBalanceReplicateResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerReplicateVatRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerReplicateVatResponse;
import ru.yandex.market.mbi.open.api.client.model.ReplicateFeatureResponse;
import ru.yandex.market.mbi.open.api.client.model.ReplicateRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class ReplicationForTestingData {

    protected long uid = 444L;
    protected long partnerDonorId = 123L;
    protected String partnerDonorName = "Партнер донор";
    protected long legalInfoDonorPartnerId = 666;
    protected long partnerId = 111L;
    protected long regionId = 213L;
    protected String warehouseName = "Склад";
    protected String partnerWarehouseId = "sklad-123";
    protected long warehouseId = 5L;
    protected long replicaWarehouseId = 200L;
    protected long businessId = 999L;
    protected long partnerApplicationId = 1L;
    protected long feedId = 115L;
    protected int taskId = 888;
    protected long campaignId = 1111L;
    protected String operationId = "op-id";


    protected final PartnerNotificationClient partnerNotificationClient;
    protected final MbiOpenApiClient client;
    protected final MbiApiClient mbiApiClient;
    protected final DataCampClient dataCampShopClient;

    public ReplicationForTestingData(
            MbiOpenApiClient client,
            MbiApiClient mbiApiClient,
            DataCampClient dataCampShopClient
    ) {
        this(client, mbiApiClient, dataCampShopClient, null);
    }

    public ReplicationForTestingData(
            MbiOpenApiClient client,
            MbiApiClient mbiApiClient,
            DataCampClient dataCampShopClient,
            PartnerNotificationClient partnerNotificationClient
    ) {
        this.client = client;
        this.mbiApiClient = mbiApiClient;
        this.dataCampShopClient = dataCampShopClient;
        this.partnerNotificationClient = partnerNotificationClient;
    }

    public abstract Map<String, Object> params();

    public void mockRequestPartnerLegalInfoCopy() {
        when(client.requestPartnerLegalInfoCopy(anyLong(), any(PartnerLegalInfoRequest.class)))
                .thenReturn(new PartnerLegalInfoResponse()
                        .newPartnerId(partnerId)
                        .sourcePartnerId(partnerDonorId)
                        .partnerApplicationId(partnerApplicationId)
                        .returnContactCopied(true)
                        .vatCopied(true)
                );
    }

    public void verifyRequestPartnerLegalInfoCopy() {
        verify(client).requestPartnerLegalInfoCopy(eq(uid), eq(new PartnerLegalInfoRequest()
                .sourcePartnerId(partnerDonorId)
                .newPartnerId(partnerId)));
    }

    public void mockRequestPartnerVatCopy() {
        when(client.requestPartnerVatCopy(anyLong(), any(PartnerReplicateVatRequest.class)))
                .thenReturn(new PartnerReplicateVatResponse()
                        .newPartnerId(partnerId)
                        .sourcePartnerId(partnerDonorId)
                        .vatCopied(true)
                );
    }

    public void verifyRequestPartnerVatCopy() {
        verify(client).requestPartnerVatCopy(eq(uid), eq(new PartnerReplicateVatRequest()
                .sourcePartnerId(partnerDonorId)
                .newPartnerId(partnerId)
        ));
    }

    public void mockRequestPartnerBalanceCopy() {
        when(client.requestPartnerBalanceCopy(anyLong(), any()))
                .thenReturn(new PartnerBalanceReplicateResponse().campaignId(campaignId));
    }

    public void verifyRequestPartnerBalanceCopy() {
        verify(client).requestPartnerBalanceCopy(eq(uid), eq(new PartnerBalanceReplicateRequest()
                .newPartnerId(partnerId)
                .sourcePartnerId(partnerDonorId)
        ));
    }

    public void verifyPartnerApplication() {
        verify(client).createPartnerApplicationRequest(eq(uid), eq(partnerId), eq(new PartnerApplicationRequest()));
    }

    public void verifyRegisterPartnerNesu() {
        verify(client).registerPartnerNesu(uid, partnerId, warehouseName, partnerWarehouseId);
    }

    public void mockRequestPartnerFeedDefault() {
        when(client.requestPartnerFeedDefault(anyLong(), anyLong())).thenReturn(
                new DefaultFeedResponse().partnerId(partnerId).feedId(feedId)
        );
    }

    public void verifyRequestPartnerFeedDefault() {
        verify(client).requestPartnerFeedDefault(eq(uid), eq(partnerId));
    }

    public void mockStartCopyOffers() {
        when(dataCampShopClient.startCopyOffers(any(CopyOffers.OffersCopyTask.class))).thenReturn(
                CopyOffers.OffersCopyTaskStatus.newBuilder()
                        .setId(taskId)
                        .setStatus(CopyOffers.OffersCopyTaskStatus.EStatus.STARTED)
                        .build()
        );
    }

    public void verifyStartCopyOffers(CopyOffers.OffersCopyTask task) {
        verify(dataCampShopClient).startCopyOffers(eq(task));
    }

    public void mockGetCopyTaskStatus() {
        when(dataCampShopClient.getCopyTaskStatus(anyLong(), anyLong(), anyInt())).thenReturn(
                CopyOffers.OffersCopyTaskStatus.newBuilder()
                        .setId(taskId)
                        .setStatus(CopyOffers.OffersCopyTaskStatus.EStatus.FINISHED)
                        .build()
        );
    }

    public void verifyGetCopyTaskStatus() {
        verify(dataCampShopClient).getCopyTaskStatus(eq(businessId),
                eq(partnerId), eq(taskId));
    }

    public void mockMbiNotify() {
        when(mbiApiClient.updateOperationStatus(any()))
                .thenReturn(new GenericCallResponse(GenericCallResponseStatus.OK, "ok"));
    }

    public void verifyMbiNotify(String processInstanceId) {
        verify(mbiApiClient).updateOperationStatus(
                eq(new ExternalOperationResult(
                        processInstanceId,
                        OperationStatus.OK,
                        new OperationStatistics(Collections.emptyMap())
                ))
        );
    }

    public void mockRequestPartnerFeatureCopy() {
        when(client.requestPartnerFeatureCopy(anyLong(), any())).thenReturn(
                new ReplicateFeatureResponse().partnerId(partnerId).featureCopied(true)
        );
    }

    public void verifyRequestPartnerFeatureCopy() {
        verify(client).requestPartnerFeatureCopy(eq(uid),
                eq(new ReplicateRequest()
                        .newPartnerId(partnerId)
                        .sourcePartnerId(partnerDonorId)
                ));
    }

    protected void mockMappingGetCopyStatus() {
        when(client.hasMappings(anyLong(), anyLong()))
                .thenReturn(new HasMappingsResponse().hasMappings(true));
    }

    protected void verifyMappingGetCopyStatus() {
        verify(client).hasMappings(eq(uid), eq(partnerId));
    }

    protected void mockAwaitFfLink() {
        when(mbiApiClient.getPartnerFulfillments(eq(partnerId), any()))
                .thenReturn(new PartnerFulfillmentLinksDTO(
                        List.of(new PartnerFulfillmentLinkDTO(partnerId, replicaWarehouseId, feedId,
                                DeliveryServiceType.DROPSHIP))));
    }

    protected void verifyAwaitFfLink() {
        verify(mbiApiClient).getPartnerFulfillments(eq(partnerId), any());
    }

    public long getUid() {
        return uid;
    }

    public ReplicationForTestingData setUid(long uid) {
        this.uid = uid;
        return this;
    }

    public long getPartnerDonorId() {
        return partnerDonorId;
    }

    public ReplicationForTestingData setPartnerDonorId(long partnerDonorId) {
        this.partnerDonorId = partnerDonorId;
        return this;
    }

    public long getPartnerId() {
        return partnerId;
    }

    public ReplicationForTestingData setPartnerId(long partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public long getRegionId() {
        return regionId;
    }

    public ReplicationForTestingData setRegionId(long regionId) {
        this.regionId = regionId;
        return this;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public ReplicationForTestingData setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
        return this;
    }

    public long getWarehouseId() {
        return warehouseId;
    }

    public ReplicationForTestingData setWarehouseId(long warehouseId) {
        this.warehouseId = warehouseId;
        return this;
    }

    public long getBusinessId() {
        return businessId;
    }

    public ReplicationForTestingData setBusinessId(long businessId) {
        this.businessId = businessId;
        return this;
    }

    public long getPartnerApplicationId() {
        return partnerApplicationId;
    }

    public ReplicationForTestingData setPartnerApplicationId(long partnerApplicationId) {
        this.partnerApplicationId = partnerApplicationId;
        return this;
    }

    public long getFeedId() {
        return feedId;
    }

    public ReplicationForTestingData setFeedId(long feedId) {
        this.feedId = feedId;
        return this;
    }

    public int getTaskId() {
        return taskId;
    }

    public ReplicationForTestingData setTaskId(int taskId) {
        this.taskId = taskId;
        return this;
    }

    public long getCampaignId() {
        return campaignId;
    }

    public ReplicationForTestingData setCampaignId(long campaignId) {
        this.campaignId = campaignId;
        return this;
    }

    public String getOperationId() {
        return operationId;
    }

    public ReplicationForTestingData setOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }
}
