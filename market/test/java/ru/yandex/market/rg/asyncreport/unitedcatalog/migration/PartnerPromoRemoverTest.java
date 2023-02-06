package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.io.IOException;
import java.util.List;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.asyncreport.model.PartnerPromoMigrationParams;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;
import ru.yandex.market.rg.asyncreport.unitedcatalog.migration.delete.PartnerPromosRemover;
import ru.yandex.market.rg.config.FunctionalTest;

import static Market.DataCamp.DataCampPromo.BusinessMigrationInfo.Status.NEW_BUSINESS;
import static Market.DataCamp.DataCampPromo.BusinessMigrationInfo.Status.NORMAL;
import static Market.DataCamp.DataCampPromo.BusinessMigrationInfo.Status.OLD_BUSINESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PartnerPromoRemoverTest extends FunctionalTest {
    @Autowired
    DataCampClient dataCampMigrationClient;
    @Autowired
    PartnerPromosRemover partnerPromosRemover;

    @Test
    public void noPartnerPromosTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance()).when(dataCampMigrationClient).getPromos(any(PromoDatacampRequest.class));
        partnerPromosRemover.generate("1", objectMapper.readValue("{\"entityId\":101,\"partnerId\":101," +
                "\"sourceBusinessId\":\"99\",\"targetBusinessId\":\"98\"}", PartnerPromoMigrationParams.class));
        verify(dataCampMigrationClient, times(0)).addPromo(any(SyncGetPromo.UpdatePromoBatchRequest.class), anyLong());
        verify(dataCampMigrationClient, times(0)).deletePromo(any(SyncGetPromo.DeletePromoBatchRequest.class));
    }

    @Test
    public void removerTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        int sourceBusinessId = 99;
        int targetBusinessId = 98;
        DataCampPromo.PromoDescription promo1 = createPartnerPromo("promo1", sourceBusinessId, OLD_BUSINESS);
        DataCampPromo.PromoDescription promo2 = createPartnerPromo("promo2", sourceBusinessId, OLD_BUSINESS);
        SyncGetPromo.GetPromoBatchResponse firstPageOld = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addAllPromo(List.of(promo1, promo2))
                )
                .build();
        doReturn(firstPageOld).when(dataCampMigrationClient).getPromos((PromoDatacampRequest) argThat(request -> ((PromoDatacampRequest) request).getBusinessId() == sourceBusinessId));
        DataCampPromo.PromoDescription promo3 = createPartnerPromo("promo3", targetBusinessId, NEW_BUSINESS);
        DataCampPromo.PromoDescription promo4 = createPartnerPromo("promo4", targetBusinessId, NEW_BUSINESS);
        SyncGetPromo.GetPromoBatchResponse firstPageNew = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addAllPromo(List.of(promo3, promo4))
                )
                .build();
        doReturn(firstPageNew).when(dataCampMigrationClient).getPromos((PromoDatacampRequest) argThat(request -> ((PromoDatacampRequest) request).getBusinessId() == targetBusinessId));
        doReturn(SyncGetPromo.DeletePromoBatchResponse.getDefaultInstance()).when(dataCampMigrationClient).deletePromo(any());
        partnerPromosRemover.generate("1", objectMapper.readValue("{\"entityId\":101,\"partnerId\":101," +
                "\"sourceBusinessId\":\"99\",\"targetBusinessId\":\"98\"}", PartnerPromoMigrationParams.class));
        ArgumentCaptor<SyncGetPromo.DeletePromoBatchRequest> requestOld = ArgumentCaptor.forClass(SyncGetPromo.DeletePromoBatchRequest.class);
        verify(dataCampMigrationClient, Mockito.times(1)).deletePromo(requestOld.capture());
        List<SyncGetPromo.PromoIdentifiers> oldPromoList = requestOld.getValue().getIdentifiersList();
        assertThat(oldPromoList).hasSize(2);
        assertThat(oldPromoList).map(promoDescription -> promoDescription.getPrimaryKey().getBusinessId()).containsExactly(sourceBusinessId, sourceBusinessId);

        ArgumentCaptor<SyncGetPromo.UpdatePromoBatchRequest> newPromos = ArgumentCaptor.forClass(SyncGetPromo.UpdatePromoBatchRequest.class);
        verify(dataCampMigrationClient, Mockito.times(1)).addPromo(newPromos.capture(), anyLong());

        List<DataCampPromo.PromoDescription> newPromosList = newPromos.getValue().getPromos().getPromoList();
        assertThat(newPromosList).hasSize(2);
        assertThat(newPromosList).map(promoDescription -> promoDescription.getPrimaryKey().getBusinessId())
                .containsExactly(targetBusinessId, targetBusinessId);
        assertThat(newPromosList).map(promoDescription -> promoDescription.getBusinessMigrationInfo().getStatus())
                .containsExactly(NORMAL, NORMAL);
        assertThat(newPromosList).map(promoDescription -> promoDescription.getBusinessMigrationInfo().getLastMigrationInfo().getSourceBusinessId())
                .containsExactly(sourceBusinessId, sourceBusinessId);
    }

    private DataCampPromo.PromoDescription createPartnerPromo(String promoId, long businessId, DataCampPromo.BusinessMigrationInfo.Status status) {
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId(promoId)
                        .setBusinessId(Math.toIntExact(businessId))
                        .setSource(Promo.ESourceType.PARTNER_SOURCE)
                        .build())
                .setBusinessMigrationInfo(DataCampPromo.BusinessMigrationInfo.newBuilder()
                        .setStatus(status)
                )
                .build();
    }
}
