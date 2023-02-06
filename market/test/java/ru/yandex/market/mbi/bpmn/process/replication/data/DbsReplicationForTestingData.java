package ru.yandex.market.mbi.bpmn.process.replication.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import Market.DataCamp.API.CopyOffers;
import Market.DataCamp.DataCampOfferMeta;
import org.mockito.Mockito;

import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinkDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinksDTO;
import ru.yandex.market.mbi.bpmn.client.PartnerStatusServiceClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.PartnerBalanceReplicateRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerBalanceReplicateResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerResponse;
import ru.yandex.market.mbi.open.api.client.model.ReplicateRequest;
import ru.yandex.market.mbi.open.api.client.model.ScheduleReplicationResponse;
import ru.yandex.market.mbi.open.api.client.model.ShipmentDateReplicationResponse;
import ru.yandex.market.mbi.partner.status.client.model.ReplicationStatus;
import ru.yandex.market.mbi.partner.status.client.model.ReplicationStatusRequest;
import ru.yandex.market.mbi.partner.status.client.model.ReplicationType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Для моков процесса заведения складов для ДБС.
 */
public class DbsReplicationForTestingData extends ReplicationForTestingData {
    private final PartnerStatusServiceClient partnerStatusServiceClient;

    public DbsReplicationForTestingData(MbiOpenApiClient client,
                                        MbiApiClient mbiApiClient,
                                        DataCampClient dataCampShopClient,
                                        PartnerStatusServiceClient partnerStatusServiceClient) {
        super(client, mbiApiClient, dataCampShopClient);
        this.partnerStatusServiceClient = partnerStatusServiceClient;
    }

    public DbsReplicationForTestingData(MbiOpenApiClient client,
                                        MbiApiClient mbiApiClient,
                                        DataCampClient dataCampShopClient,
                                        PartnerStatusServiceClient partnerStatusServiceClient,
                                        long businessId) {
        super(client, mbiApiClient, dataCampShopClient);
        this.businessId = businessId;
        this.partnerStatusServiceClient = partnerStatusServiceClient;
    }

    @Override
    public Map<String, Object> params() {
        return Map.of("uid", uid,
                "partnerDonorId", partnerDonorId,
                "legalInfoDonorPartnerId", legalInfoDonorPartnerId,
                "regionId", regionId,
                "warehouseName", warehouseName,
                "partnerWarehouseId", partnerWarehouseId,
                "businessId", businessId);
    }

    public void mockAll() {
        mockReplicateDbsPartner();
        mockRequestPartnerLegalInfoCopy();
        mockGetPartnerFulfillments();
        mockRequestPartnerBalanceCopy();
        mockRequestPartnerFeedDefault();
        mockRequestPartnerFeatureCopy();
        mockRequestPartnerScheduleCopy();
        mockRequestPartnerShipmentDateCopy();
        mockStartCopyOffers();
        mockGetCopyTaskStatus();
    }

    public void mockWaitBalance() {
        mockReplicateDbsPartner();
        when(client.requestPartnerBalanceCopy(anyLong(), any()))
                .thenAnswer(a -> {
                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {
                    }
                    return new PartnerBalanceReplicateResponse().campaignId(campaignId);
                });
    }

    public void verifyAll() {
        verifyReplicateDbsPartner();
        verifyRequestPartnerLegalCopyWithDonorLegalInfo();
        verifyRegisterPartnerNesu();
        verifyGetPartnerFulfillments();
        verifyRequestPartnerBalanceCopy();
        verifyRequestPartnerFeedDefault();
        verifyRequestPartnerFeatureCopy();
        verifyRequestPartnerScheduleCopy();
        verifyRequestPartnerShipmentDateCopy();
        verifyStartCopyOffers(
                CopyOffers.OffersCopyTask.newBuilder()
                        .setBusinessId((int) businessId)
                        .addSrcShopIds((int) partnerDonorId)
                        .setDstShopId((int) partnerId)
                        .setDstWarehouseId((int) warehouseId)
                        .setCopyContentFromShop((int) partnerDonorId)
                        .setRgb(DataCampOfferMeta.MarketColor.WHITE)
                        .build()
        );
        verifyGetCopyTaskStatus();
        verifyPartnerStatus();
    }

    public void mockReplicateDbsPartner() {
        when(client.replicateDbsPartner(anyLong(), any(ReplicatePartnerRequest.class)))
                .thenReturn(new ReplicatePartnerResponse().partnerId(partnerId));
    }

    public void verifyReplicateDbsPartner() {
        Mockito.verify(client).replicateDbsPartner(eq(uid), eq(new ReplicatePartnerRequest()
                .partnerDonorId(partnerDonorId)
                .regionId(regionId)
                .warehouseName(warehouseName)
                .partnerWarehouseId(partnerWarehouseId)));
    }

    public void verifyRegisterPartnerNesu() {
        verify(client).registerPartnerNesu(eq(uid), eq(partnerId),
                eq(warehouseName), eq(partnerWarehouseId));
    }

    public void mockGetPartnerFulfillments() {
        when(mbiApiClient.getPartnerFulfillments(anyLong(), any())).thenReturn(
                new PartnerFulfillmentLinksDTO(
                        List.of(new PartnerFulfillmentLinkDTO(partnerId, warehouseId,
                                feedId, DeliveryServiceType.DROPSHIP_BY_SELLER))
                )
        );
    }

    public void verifyGetPartnerFulfillments() {
        verify(mbiApiClient).getPartnerFulfillments(eq(partnerId), eq(Set.of(DeliveryServiceType.CROSSDOCK,
                DeliveryServiceType.DROPSHIP, DeliveryServiceType.DROPSHIP_BY_SELLER)));
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

    public void verifyRequestPartnerLegalCopyWithDonorLegalInfo() {
        verify(client).requestPartnerLegalInfoCopy(eq(uid), eq(new PartnerLegalInfoRequest()
                .sourcePartnerId(legalInfoDonorPartnerId)
                .newPartnerId(partnerId)));
    }

    public void mockRequestPartnerScheduleCopy() {
        when(client.requestPartnerScheduleCopy(anyLong(), any())).thenReturn(
                new ScheduleReplicationResponse()
        );
    }

    public void verifyRequestPartnerScheduleCopy() {
        verify(client).requestPartnerScheduleCopy(eq(uid),
                eq(new ReplicateRequest()
                        .newPartnerId(partnerId)
                        .sourcePartnerId(partnerDonorId)
                ));
    }

    public void mockRequestPartnerShipmentDateCopy() {
        when(client.requestPartnerShipmentDateCopy(anyLong(), any())).thenReturn(
                new ShipmentDateReplicationResponse().copied(true)
        );
    }

    public void verifyRequestPartnerShipmentDateCopy() {
        verify(client).requestPartnerShipmentDateCopy(eq(uid),
                eq(new ReplicateRequest()
                        .newPartnerId(partnerId)
                        .sourcePartnerId(partnerDonorId)
                ));
    }

    private void verifyPartnerStatus() {
        Mockito.verify(partnerStatusServiceClient, times(1)).updateReplicationStatus(
                eq(partnerId),
                eq(new ReplicationStatusRequest()
                        .replicationType(ReplicationType.DBS_TO_DBS_REPLICATION)
                        .replicationStatus(ReplicationStatus.IN_PROGRESS)));
        Mockito.verify(partnerStatusServiceClient, times(1)).updateReplicationStatus(
                eq(partnerId),
                eq(new ReplicationStatusRequest()
                        .replicationType(ReplicationType.DBS_TO_DBS_REPLICATION)
                        .replicationStatus(ReplicationStatus.DONE)));
    }

    public MbiOpenApiClient getClient() {
        return client;
    }

    public MbiApiClient getMbiApiClient() {
        return mbiApiClient;
    }

    public DataCampClient getDataCampShopClient() {
        return dataCampShopClient;
    }
}
