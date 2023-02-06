package ru.yandex.market.core.supplier.promo.service;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.supplier.promo.dao.PromoOffersValidationParamsDao;
import ru.yandex.market.core.supplier.promo.model.PromoType;
import ru.yandex.market.core.supplier.promo.model.ChangedOffer;
import ru.yandex.market.core.supplier.promo.model.context.CheapestAsGiftTemplateContext;
import ru.yandex.market.core.supplier.promo.model.offer.xls.CheapestAsGiftXlsPromoOffer;
import ru.yandex.market.core.supplier.promo.model.validation.PromoOfferValidationStats;
import ru.yandex.market.core.supplier.promo.model.validation.PromoOfferValidationStatus;
import ru.yandex.market.core.supplier.promo.model.validation.ValidationUpload;
import ru.yandex.market.core.supplier.promo.model.validation.request.PromoOfferXlsValidationRequest;
import ru.yandex.market.core.supplier.promo.model.validation.strategy.xls.CheapestAsGiftOfferXlsValidationStrategy;

public class PromoOfferValidatinInfoServiceFunctionalTest extends FunctionalTest {
    @Autowired
    private PromoOffersValidationParamsDao promoOffersValidationParamsDao;
    @Autowired
    private CheapestAsGiftTemplateContext cheapestAsGiftTemplateContext;

    @Test
    @DbUnitDataSet(before = "create-validation-test.before.csv")
    public void createValidationTest() {
        createValidation();
        PromoOfferValidationStats stats = promoOffersValidationParamsDao.getValidationStats("id1");
        PromoOfferValidationStats expectedStats = new PromoOfferValidationStats.Builder()
                .withValidationId("id1")
                .withPromoId("#1234")
                .withStatus(PromoOfferValidationStatus.NEW)
                .build();
        assertThat(stats).usingRecursiveComparison().isEqualTo(expectedStats);
    }

    @Test
    @DbUnitDataSet(before = "full-validation-stats-uncommitted.before.csv")
    public void getStatsTest() {
        PromoOfferValidationStats stats = promoOffersValidationParamsDao.getValidationStats("id1");
        PromoOfferValidationStats expectedStats = new PromoOfferValidationStats.Builder()
                .withValidationId("id1")
                .withPromoId("#1234")
                .withValidatedUpload(new ValidationUpload("id1", "validatedUrl1", "validatedS3key"))
                .withStatus(PromoOfferValidationStatus.COMPLETE)
                .addTotalOffers(20L)
                .addCorrectSelectedOffers(13L)
                .addInvalidOffers(7L)
                .addParticipatingInOtherPromos(4L)
                .withEligibleS3Key("eligibleS3key")
                .withChangedOffer(new ChangedOffer("offer1", 147, 2, true))
                .withHost("validationHost")
                .withMaxCashbackNominal(25)
                .withMinCashbackNominal(3)
                .withMinMarketTariff(0.3)
                .withMaxMarketTariff(1.5)
                .withMarketTariffsVersionId(0)
                .build();
        assertThat(stats).usingRecursiveComparison().isEqualTo(expectedStats);
    }

    @Test
    @DbUnitDataSet(before = "full-validation-stats-uncommitted.before.csv")
    public void commitTest() {
        promoOffersValidationParamsDao.commitValidatedOffers("id1");
        PromoOfferValidationStats stats = promoOffersValidationParamsDao.getValidationStats("id1");
        assertTrue(stats.getCommitted());
    }

    @Test
    @DbUnitDataSet(before = "create-validation-test.before.csv")
    public void addS3keyTest() {
        String eligibleS3key = "eligibleS3key";
        createValidation();
        PromoOfferValidationStats statsForUpdate =
                new PromoOfferValidationStats.Builder()
                        .withValidationId("id1")
                        .withEligibleS3Key(eligibleS3key)
                        .build();
        promoOffersValidationParamsDao.updateValidation(statsForUpdate);
        PromoOfferValidationStats stats = promoOffersValidationParamsDao.getValidationStats("id1");
        assertEquals(stats.getEligibleS3Key(), eligibleS3key);
    }

    @Test
    @DbUnitDataSet(before = "full-validation-stats-uncommitted.before.csv")
    public void getLastValidatedUploadUrlsTest() {
        Map<String, String> lastUploadUrls = promoOffersValidationParamsDao.getLastValidatedUploadUrls(2, List.of("#1234", "#4211", "not_existed"));
        assertThat(lastUploadUrls.values()).containsExactlyInAnyOrder("validatedUrl1", "validatedUrl2");
    }

    private void createValidation() {
        PromoOfferXlsValidationRequest<CheapestAsGiftXlsPromoOffer> validationRequest =
                new PromoOfferXlsValidationRequest.Builder<CheapestAsGiftXlsPromoOffer>()
                        .withTemplateContext(cheapestAsGiftTemplateContext)
                        .withValidationStrategy(new CheapestAsGiftOfferXlsValidationStrategy())
                        .withSupplierId(2L)
                        .withPromoId("#1234")
                        .withPromoType(PromoType.CHEAPEST_AS_GIFT)
                        .withOriginalUpload(new ValidationUpload("id1", "originalUrl", "originalS3key"))
                        .withValidationId("id1")
                        .build();
        promoOffersValidationParamsDao.createValidation(validationRequest);
    }
}
