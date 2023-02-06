package ru.yandex.market.adv.promo.mvc.promo.common.controller;

import java.time.Instant;

import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.adv.promo.utils.CommonTestUtils.getResource;

public class PastPromosControllerTest extends FunctionalTest {

    private final static String RESULT_JSON = "pastPromos_response.json";

    @Autowired
    private DataCampClient dataCampClient;

    @Test
    @DbUnitDataSet(before = "PastPromosControllerTest/getPastPromosTest/before.csv")
    @DisplayName("Проверка корректности получения прошедших акций")
    void getPastPromosTest() {
        long partnerId = 12345;
        long businessId = 789;

        doReturn(createGetPromoBatchResponse())
                .when(dataCampClient).getPromos(any(GetPromoBatchRequestWithFilters.class));

        ResponseEntity<String> response = getPastPromos(partnerId, businessId);

        String expected = getResource(this.getClass(), RESULT_JSON);
        JSONAssert.assertEquals(expected, response.getBody(), false);
    }

    private SyncGetPromo.GetPromoBatchResponse createGetPromoBatchResponse() {
        DataCampPromo.PromoDescription promo1 = createPromo(
                "12345_promo_test", false, 1_600_500_000, Promo.ESourceType.PARTNER_SOURCE);
        DataCampPromo.PromoDescription promo2 = createPromo(
                "#11111", false, 1_600_700_000, Promo.ESourceType.ANAPLAN);
        DataCampPromo.PromoDescription promo3 = createPromo(
                "12345_test", true, 0, Promo.ESourceType.PARTNER_SOURCE);

        DataCampPromo.PromoDescriptionBatch batch = DataCampPromo.PromoDescriptionBatch.newBuilder()
                .addPromo(promo1)
                .addPromo(promo2)
                .addPromo(promo3)
                .build();

        return SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(batch)
                .build();
    }

    private DataCampPromo.PromoDescription createPromo(
            String promoId,
            boolean enabled,
            long disablingDate,
            Promo.ESourceType eSourceType
    ) {
        DataCampPromo.PromoDescriptionIdentifier identifier = DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                .setPromoId(promoId)
                .setSource(eSourceType)
                .build();

        DataCampPromo.PromoConstraints.Builder constraintsBuilder =
                DataCampPromo.PromoConstraints.newBuilder()
                        .setEnabled(enabled);

        if (!enabled) {
            if (disablingDate != 0) {
                constraintsBuilder.setDisablingDate(disablingDate);
            } else {
                DataCampOfferMeta.UpdateMeta updatedMeta =
                        DataCampOfferMeta.UpdateMeta.newBuilder()
                                .setTimestamp(DateTimes.toTimestamp(Instant.ofEpochSecond(disablingDate)))
                                .build();
                constraintsBuilder.setMeta(updatedMeta);
            }
        }

        DataCampPromo.PromoConstraints constraints = constraintsBuilder.build();

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(identifier)
                .setConstraints(constraints)
                .build();
    }

    ResponseEntity<String> getPastPromos(long partnerId, long businessId) {
        return FunctionalTestHelper.get(baseUrl() + "/partner/promo/description/past-promos?" +
                "partnerId=" + partnerId + "&businessId=" + businessId);
    }
}
