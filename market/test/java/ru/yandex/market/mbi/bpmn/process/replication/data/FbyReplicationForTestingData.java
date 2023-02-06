package ru.yandex.market.mbi.bpmn.process.replication.data;

import java.util.Map;
import java.util.Set;

import Market.DataCamp.API.CopyOffers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStatus;
import org.mockito.Mockito;

import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.SaasDocType;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferFilter;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerLegalInfoResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerResponse;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Для моков процесса репликации в FBY.
 */
public class FbyReplicationForTestingData extends ReplicationForTestingData {

    private final SaasService saasService;

    public FbyReplicationForTestingData(MbiOpenApiClient client, MbiApiClient mbiApiClient,
                                        DataCampClient dataCampShopClient, SaasService saasService) {
        super(client, mbiApiClient, dataCampShopClient);
        this.saasService = saasService;
    }

    @Override
    public Map<String, Object> params() {
        return Map.of(
                "uid", uid,
                "partnerDonorId", partnerDonorId,
                "acceptorPlacementType", PartnerPlacementType.FBY,
                "businessId", businessId,
                "operationId", operationId
        );
    }

    public void mockAll() {
        reset(client);
        mockReplicateFbsPartner();
        mockRequestPartnerBalanceCopy();
        mockRequestPartnerVatCopy();
        mockRequestPartnerFeedDefault();
        mockStartCopyOffers();
        mockGetCopyTaskStatus();
        mockSaaS();
    }

    public void verifyAll(String processInstanceId) {
        verifyReplicateFbsPartner();
        verifyRequestPartnerBalanceCopy();
        verifyRequestPartnerVatCopy();
        verifyRequestPartnerFeedDefault();
        verifyPartnerApplication();
        verifyStartCopyOffers(
                CopyOffers.OffersCopyTask
                        .newBuilder()
                        .setBusinessId((int) businessId)
                        .setDstShopId((int) partnerId)
                        .setCopyContentFromShop((int) partnerDonorId)
                        .addSrcShopIds((int) partnerDonorId)
                        .setRgb(DataCampOfferMeta.MarketColor.BLUE)
                        .build()
        );
        verifyGetCopyTaskStatus();
        verifySaaS();
    }

    public void mockReplicateFbsPartner() {
        when(client.replicatePartner(anyLong(), any(ReplicatePartnerRequest.class)))
                .thenReturn(new ReplicatePartnerResponse().partnerId(partnerId));
    }

    public void mockFailReplicateFbsPartner() {
        when(client.replicatePartner(anyLong(), any(ReplicatePartnerRequest.class)))
                .thenThrow(new MbiOpenApiClientResponseException("bad request", 400,
                        new ru.yandex.market.mbi.open.api.client.model.ApiError().code(1).message("very bad user")));
    }

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

    public void verifyReplicateFbsPartner() {
        Mockito.verify(client).replicatePartner(
                eq(uid),
                eq(new ReplicatePartnerRequest()
                        .partnerDonorId(partnerDonorId)
                        .acceptorPlacementType(PartnerPlacementType.FBY))
        );
    }

    public void mockSaaS() {
        when(saasService.searchBusinessOffers(eq(
                SaasOfferFilter.newBuilder()
                        .setPrefix(businessId)
                        .setBusinessId(businessId)
                        .addShopId(partnerDonorId)
                        .setDocType(SaasDocType.OFFER)
                        .setPageRequest(SeekSliceRequest.firstN(0))
                        .build()
        ))).thenReturn(
                SaasSearchResult.builder()
                        .setTotalCount(10)
                        .build()
        );
        when(saasService.searchBusinessOffers(eq(
                SaasOfferFilter.newBuilder()
                        .setPrefix(businessId)
                        .setBusinessId(businessId)
                        .addShopId(partnerId)
                        .setDocType(SaasDocType.OFFER)
                        .setPageRequest(SeekSliceRequest.firstN(0))
                        .addResultOfferStatuses(
                                partnerId,
                                Set.of(
                                        DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_NO_STOCKS,
                                        DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_FINISH_PS_CHECK,
                                        DataCampOfferStatus.OfferStatus.ResultStatus.NOT_PUBLISHED_PARTNER_IS_DISABLED
                                )
                        ).build()
        ))).thenReturn(
                SaasSearchResult.builder()
                        .setTotalCount(10)
                        .build()
        );
    }

    public void verifySaaS() {
        Mockito.verify(saasService, times(0)).searchBusinessOffers(any());
    }

    public MbiOpenApiClient getClient() {
        return client;
    }
}
