package ru.yandex.market.mbi.bpmn.process.replication.data;

import java.util.Map;

import Market.DataCamp.API.CopyOffers;
import Market.DataCamp.DataCampOfferMeta;
import org.mockito.Mockito;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.bpmn.client.PartnerStatusServiceClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerResponse;
import ru.yandex.market.mbi.partner.status.client.model.ReplicationStatus;
import ru.yandex.market.mbi.partner.status.client.model.ReplicationStatusRequest;
import ru.yandex.market.mbi.partner.status.client.model.ReplicationType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class FbsReplicationForTestingData extends ReplicationForTestingData {
    private final PartnerStatusServiceClient partnerStatusServiceClient;

    public FbsReplicationForTestingData(MbiOpenApiClient client,
                                        MbiApiClient mbiApiClient,
                                        DataCampClient dataCampShopClient,
                                        PartnerStatusServiceClient partnerStatusServiceClient) {
        super(client, mbiApiClient, dataCampShopClient);
        this.partnerStatusServiceClient = partnerStatusServiceClient;
    }

    @Override
    public Map<String, Object> params() {
        return Map.of(
                "uid", uid,
                "partnerDonorId", partnerDonorId,
                "replicaPartnerId", partnerId,
                "warehouseId", String.valueOf(replicaWarehouseId),
                "acceptorPlacementType", PartnerPlacementType.FBS,
                "businessId", businessId,
                "operationId", operationId,
                "regionId", regionId
        );
    }

    public void mockAll() {
        mockReplicateFbsPartner();
        mockAwaitFfLink();
        mockRequestPartnerFeedDefault();
        mockRequestPartnerBalanceCopy();
        mockRequestPartnerLegalInfoCopy();
        mockStartCopyOffers();
        mockGetCopyTaskStatus();
        mockMappingGetCopyStatus();
        mockRequestPartnerFeatureCopy();
    }

    public void verifyAll() {
        verifyReplicateFbsPartner();
        verifyAwaitFfLink();
        verifyRequestPartnerFeedDefault();
        verifyRequestPartnerBalanceCopy();
        verifyRequestPartnerLegalInfoCopy();
        verifyStartCopyOffers(
                CopyOffers.OffersCopyTask.newBuilder()
                        .setBusinessId((int) businessId)
                        .addSrcShopIds((int) partnerDonorId)
                        .setDstShopId((int) partnerId)
                        .setDstWarehouseId((int) replicaWarehouseId)
                        .setCopyContentFromShop((int) partnerDonorId)
                        .setRgb(DataCampOfferMeta.MarketColor.BLUE)
                        .build()
        );
        verifyGetCopyTaskStatus();
        verifyMappingGetCopyStatus();
        verifyRequestPartnerFeatureCopy();
        verifyPartnerStatus();
    }

    private void mockReplicateFbsPartner() {
        when(client.replicatePartner(anyLong(), any(ReplicatePartnerRequest.class)))
                .thenReturn(new ReplicatePartnerResponse().partnerId(partnerId));
    }

    private void verifyReplicateFbsPartner() {
        Mockito.verify(client).replicatePartner(
                eq(uid),
                eq(new ReplicatePartnerRequest()
                        .partnerDonorId(partnerDonorId)
                        .replicaPartnerId(partnerId)
                        .regionId(regionId)
                        .acceptorPlacementType(PartnerPlacementType.FBS))
        );
    }

    private void verifyPartnerStatus() {
        Mockito.verify(partnerStatusServiceClient, times(1)).updateReplicationStatus(
                eq(partnerId),
                eq(new ReplicationStatusRequest()
                        .replicationType(ReplicationType.FBS_TO_FBS_REPLICATION)
                        .replicationStatus(ReplicationStatus.IN_PROGRESS)));
        Mockito.verify(partnerStatusServiceClient, times(1)).updateReplicationStatus(
                eq(partnerId),
                eq(new ReplicationStatusRequest()
                        .replicationType(ReplicationType.FBS_TO_FBS_REPLICATION)
                        .replicationStatus(ReplicationStatus.DONE)));
    }
}
