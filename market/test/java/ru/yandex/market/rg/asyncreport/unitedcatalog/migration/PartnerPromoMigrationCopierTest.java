package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;


import java.io.IOException;
import java.util.List;
import java.util.Objects;

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
import ru.yandex.market.rg.config.FunctionalTest;

import static Market.DataCamp.DataCampPromo.BusinessMigrationInfo.Status.NEW_BUSINESS;
import static Market.DataCamp.DataCampPromo.BusinessMigrationInfo.Status.OLD_BUSINESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class PartnerPromoMigrationCopierTest extends FunctionalTest {
    @Autowired
    DataCampClient dataCampMigrationClient;
    @Autowired
    PartnerPromosCopier partnerPromosCopier;

    @Test
    public void noPartnerPromosTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance()).when(dataCampMigrationClient).getPromos(any(PromoDatacampRequest.class));
        partnerPromosCopier.generate("1", objectMapper.readValue("{\"entityId\":101,\"partnerId\":101," +
                "\"sourceBusinessId\":\"99\",\"targetBusinessId\":\"98\"}", PartnerPromoMigrationParams.class));
        verify(dataCampMigrationClient, Mockito.times(0)).addPromo(any(SyncGetPromo.UpdatePromoBatchRequest.class), anyLong());
    }

    @Test
    public void onePageCopierTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        int sourceBusinessId = 99;
        int targetBusinessId = 98;
        DataCampPromo.PromoDescription promo1 = createPartnerPromo("promo1", sourceBusinessId);
        DataCampPromo.PromoDescription promo2 = createPartnerPromo("promo2", sourceBusinessId);
        SyncGetPromo.GetPromoBatchResponse firstPage = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addAllPromo(List.of(promo1, promo2))
                )
                .build();
        doReturn(firstPage).when(dataCampMigrationClient).getPromos(any(PromoDatacampRequest.class));
        partnerPromosCopier.generate("1", objectMapper.readValue("{\"entityId\":101,\"partnerId\":101," +
                "\"sourceBusinessId\":\"99\",\"targetBusinessId\":\"98\"}", PartnerPromoMigrationParams.class));
        ArgumentCaptor<SyncGetPromo.UpdatePromoBatchRequest> request = ArgumentCaptor.forClass(SyncGetPromo.UpdatePromoBatchRequest.class);
        verify(dataCampMigrationClient, Mockito.times(2)).addPromo(request.capture(), anyLong());
        List<DataCampPromo.PromoDescription> oldPromoList = request.getAllValues().get(0).getPromos().getPromoList();
        assertThat(oldPromoList).hasSize(2);
        assertThat(oldPromoList).map(promoDescription -> promoDescription.getPrimaryKey().getBusinessId()).containsExactly(sourceBusinessId, sourceBusinessId);
        assertThat(oldPromoList).map(promoDescription -> promoDescription.getBusinessMigrationInfo().getStatus())
                .containsExactly(OLD_BUSINESS, OLD_BUSINESS);

        List<DataCampPromo.PromoDescription> newPromoList = request.getAllValues().get(1).getPromos().getPromoList();
        assertThat(newPromoList).hasSize(2);
        assertThat(newPromoList).map(promoDescription -> promoDescription.getPrimaryKey().getBusinessId()).containsExactly(targetBusinessId, targetBusinessId);
        assertThat(newPromoList).map(promoDescription -> promoDescription.getBusinessMigrationInfo().getStatus())
                .containsExactly(NEW_BUSINESS, NEW_BUSINESS);
    }

    @Test
    public void twoPagesCopierTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        int sourceBusinessId = 99;
        int targetBusinessId = 98;
        DataCampPromo.PromoDescription promo1 = createPartnerPromo("promo1", sourceBusinessId);
        DataCampPromo.PromoDescription promo2 = createPartnerPromo("promo2", sourceBusinessId);
        DataCampPromo.PromoDescription promo3 = createPartnerPromo("promo3", sourceBusinessId);

        SyncGetPromo.GetPromoBatchResponse firstPage = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addAllPromo(List.of(promo1, promo2))
                )
                .setNextPagePosition("promo3")
                .build();
        SyncGetPromo.GetPromoBatchResponse secondPage = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addAllPromo(List.of(promo3))
                )
                .build();
        doReturn(firstPage).when(dataCampMigrationClient).getPromos((PromoDatacampRequest) argThat(request -> ((PromoDatacampRequest) request).getPosition() == null));
        doReturn(secondPage).when(dataCampMigrationClient).getPromos((PromoDatacampRequest) argThat(request -> Objects.equals(((PromoDatacampRequest) request).getPosition(), "promo3")));
        ArgumentCaptor<SyncGetPromo.UpdatePromoBatchRequest> request = ArgumentCaptor.forClass(SyncGetPromo.UpdatePromoBatchRequest.class);

        partnerPromosCopier.generate("1", objectMapper.readValue("{\"entityId\":101,\"partnerId\":101," +
                "\"sourceBusinessId\":\"99\",\"targetBusinessId\":\"98\"}", PartnerPromoMigrationParams.class));
        verify(dataCampMigrationClient, Mockito.times(4)).addPromo(request.capture(), anyLong());

        List<DataCampPromo.PromoDescription> oldPromoList = request.getAllValues().get(0).getPromos().getPromoList();
        assertThat(oldPromoList).hasSize(2);
        assertThat(oldPromoList).map(promoDescription -> promoDescription.getPrimaryKey().getBusinessId())
                .containsExactly(sourceBusinessId, sourceBusinessId);
        assertThat(oldPromoList).map(promoDescription -> promoDescription.getBusinessMigrationInfo().getStatus())
                .containsExactly(OLD_BUSINESS, OLD_BUSINESS);

        List<DataCampPromo.PromoDescription> newPromoList = request.getAllValues().get(1).getPromos().getPromoList();
        assertThat(newPromoList).hasSize(2);
        assertThat(newPromoList).map(promoDescription -> promoDescription.getPrimaryKey().getBusinessId())
                .containsExactly(targetBusinessId, targetBusinessId);
        assertThat(newPromoList).map(promoDescription -> promoDescription.getBusinessMigrationInfo().getStatus())
                .containsExactly(NEW_BUSINESS, NEW_BUSINESS);

        List<DataCampPromo.PromoDescription> oldPromoList1 = request.getAllValues().get(2).getPromos().getPromoList();
        assertThat(oldPromoList1).hasSize(1);
        assertThat(oldPromoList1).map(promoDescription -> promoDescription.getPrimaryKey().getBusinessId())
                .containsExactly(sourceBusinessId);
        assertThat(oldPromoList1).map(promoDescription -> promoDescription.getBusinessMigrationInfo().getStatus())
                .containsExactly(OLD_BUSINESS);

        List<DataCampPromo.PromoDescription> newPromoList1 = request.getAllValues().get(3).getPromos().getPromoList();
        assertThat(newPromoList1).hasSize(1);
        assertThat(newPromoList1).map(promoDescription -> promoDescription.getPrimaryKey().getBusinessId())
                .containsExactly(targetBusinessId);
        assertThat(newPromoList1).map(promoDescription -> promoDescription.getBusinessMigrationInfo().getStatus())
                .containsExactly(NEW_BUSINESS);
    }

    private DataCampPromo.PromoDescription createPartnerPromo(String promoId, long businessId) {
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId(promoId)
                        .setBusinessId(Math.toIntExact(businessId))
                        .setSource(Promo.ESourceType.PARTNER_SOURCE)
                        .build())
                .build();
    }
}
