package ru.yandex.market.mbi.bpmn.process.replication.data;

import java.util.List;
import java.util.Map;

import Market.DataCamp.API.CopyOffers;
import Market.DataCamp.DataCampOfferMeta;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.NotificationPartnerInfoDTO;
import ru.yandex.market.mbi.open.api.client.model.NotificationPartnerInfoRequest;
import ru.yandex.market.mbi.open.api.client.model.NotificationPartnerInfoResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerActivationResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerResponse;
import ru.yandex.market.partner.notification.client.model.DestinationDTO;
import ru.yandex.market.partner.notification.client.model.SendNotificationRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DbsToFbsReplicationForTestingData extends ReplicationForTestingData {

    private final Long notificationType = 1651654260L;

    private final PartnerNotificationClient partnerNotificationClient;

    public DbsToFbsReplicationForTestingData(MbiOpenApiClient client, MbiApiClient mbiApiClient,
                                             DataCampClient dataCampShopClient,
                                             PartnerNotificationClient partnerNotificationClient) {
        super(client, mbiApiClient, dataCampShopClient);
        this.partnerNotificationClient = partnerNotificationClient;
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
        mockReplicatePartner();
        mockRequestPartnerFeedDefault();
        mockRequestPartnerBalanceCopy();
        mockRequestPartnerLegalInfoCopy();
        mockStartCopyOffers();
        mockGetCopyTaskStatus();
        mockGetPartnerName(partnerDonorId, partnerDonorName);
        mockGetPartnerName(partnerId, warehouseName);
        mockActivatePartner();
    }

    public void verifyAll() {
        verifyReplicatePartner();
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
        verifyGetPartnerName(partnerDonorId);
        verifyGetPartnerName(partnerId);
        verifySendNotification();
        verifyActivatePartner();

        verifyNoMoreInteractions(client);
        verifyNoMoreInteractions(mbiApiClient);
        verifyNoMoreInteractions(dataCampShopClient);
        verifyNoMoreInteractions(partnerNotificationClient);
    }

    private void mockReplicatePartner() {
        when(client.replicatePartner(anyLong(), any(ReplicatePartnerRequest.class)))
                .thenReturn(new ReplicatePartnerResponse().partnerId(partnerId));
    }

    private void verifyReplicatePartner() {
        Mockito.verify(client).replicatePartner(
                eq(uid),
                eq(new ReplicatePartnerRequest()
                        .partnerDonorId(partnerDonorId)
                        .replicaPartnerId(partnerId)
                        .regionId(regionId)
                        .acceptorPlacementType(PartnerPlacementType.FBS))
        );
    }

    public void mockGetPartnerName(long partnerId, String partnerName) {
        when(client.providePartnerInfo(
                eq(new NotificationPartnerInfoRequest().partnerIds(List.of(partnerId))))
        )
                .thenReturn(new NotificationPartnerInfoResponse().partnerInfos(
                        List.of(new NotificationPartnerInfoDTO().partnerName(partnerName))
                ));
    }

    private void verifyGetPartnerName(long partnerId) {
        verify(client).providePartnerInfo(
                argThat(argument -> List.of(partnerId).equals(argument.getPartnerIds())));
    }

    public void mockActivatePartner() {
        when(client.activatePartner(eq(partnerId), eq(uid)))
                .thenReturn(new PartnerActivationResponse().partnerActivated(true));
    }

    private void verifyActivatePartner() {
        Mockito.verify(client).activatePartner(
                eq(partnerId),
                eq(uid)
        );
    }

    private void verifySendNotification() {
        ArgumentCaptor<SendNotificationRequest> requestCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(partnerNotificationClient).sendNotification(requestCaptor.capture());

        SendNotificationRequest request = requestCaptor.getValue();
        Assertions.assertEquals(Boolean.FALSE, request.getRenderOnly());
        Assertions.assertEquals(notificationType, request.getTypeId());
        Assertions.assertEquals(new DestinationDTO().shopId(partnerDonorId), request.getDestination());
        Assertions.assertEquals("<data><donorWarehouseName>" + partnerDonorName + "</donorWarehouseName>" +
                "<donorShopId>" + partnerDonorId + "</donorShopId>" +
                "<newWarehouseName>" + warehouseName + "</newWarehouseName>" +
                "<newShopId>" + partnerId + "</newShopId>" +
                "<newCampaignId>" + campaignId + "</newCampaignId></data>", request.getData());
    }
}
