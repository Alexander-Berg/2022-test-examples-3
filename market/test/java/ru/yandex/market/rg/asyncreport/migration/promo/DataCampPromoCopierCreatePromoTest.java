package ru.yandex.market.rg.asyncreport.migration.promo;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampPromo;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeGenerationResponse;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.Mockito.doReturn;

public class DataCampPromoCopierCreatePromoTest extends FunctionalTest {

    private static final Long SECONDS_OF_EPOCH = 1634296613L;

    @Autowired
    private DataCampPromoCopier dataCampPromoCopier;
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;
    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        if (clock instanceof TestableClock) {
            ((TestableClock) clock).setFixed(Instant.ofEpochSecond(SECONDS_OF_EPOCH), ZoneId.systemDefault());
        }
    }

    @Test
    @DbUnitDataSet(before = "DataCampPromoCopierCreatePromoTest.before.csv",
                   after = "DataCampPromoCopierCreatePromoTest.after.csv")
    void createPromoTest() {
        var copyParams = new PromoCopierParams(100L, 101L, 200L, 201L, 300L);
        var donorPromo = generateDonorPromo();
        doReturn(PromocodeGenerationResponse.of("promoCode")).when(marketLoyaltyClient).generatePromocode();
        var createdPromo = dataCampPromoCopier.createPromo(copyParams, donorPromo);
        Assertions.assertEquals(generateExpectedPromo(), createdPromo);
    }

    private DataCampPromo.PromoDescription generateDonorPromo() {
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId("id")
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE)
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setEnabled(true)
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setSupplierRestriction(generateSupplierRestriction(100L))
                                .build()))
                .setLoyaltyKey(DataCampPromo.LoyaltyKey.newBuilder()
                        .setLoyaltyPromoId(499L)
                        .setLoyaltyPromoKey("oldCode")
                        .build())
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMarketPromocode(DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                .setPromoCode("oldCode")
                                .build())
                        .build())
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder().build())
                .build();
    }

    private DataCampPromo.PromoDescription generateExpectedPromo() {
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                        .setPromoId("101_promoCode_wh_migr")
                        .build())
                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                        .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE)
                        .build())
                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                        .setEnabled(true)
                        .addOffersMatchingRules(DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                                .setSupplierRestriction(generateSupplierRestriction(101L))
                                .build()))
                .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder().build())
                .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                        .setMarketPromocode(DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                .setPromoCode("promoCode")
                                .build())
                        .build())
                .setUpdateInfo(DataCampPromo.UpdateInfo.newBuilder()
                        .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                                .setSource(DataCampOfferMeta.DataSource.MARKET_MBI)
                                .setTimestamp(Timestamp.newBuilder()
                                        .setSeconds(SECONDS_OF_EPOCH)
                                        .build())
                                .build())
                        .setCreatedAt(SECONDS_OF_EPOCH)
                        .setUpdatedAt(SECONDS_OF_EPOCH)
                        .build())
                .build();
    }

    private DataCampPromo.PromoConstraints.OffersMatchingRule.SupplierRestriction generateSupplierRestriction(
            long supplierID) {
        return DataCampPromo.PromoConstraints.OffersMatchingRule.SupplierRestriction.newBuilder()
                .setSuppliers(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                        .addId(supplierID)
                        .build())
                .build();
    }
}
