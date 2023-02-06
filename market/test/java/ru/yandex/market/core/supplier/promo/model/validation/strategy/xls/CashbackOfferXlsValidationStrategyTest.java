package ru.yandex.market.core.supplier.promo.model.validation.strategy.xls;

import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampPromo;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.supplier.promo.model.offer.xls.CashbackXlsPromoOffer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.supplier.promo.model.validation.PromoOfferValidationError.OFFER_NOT_ELIGIBLE_FOR_PROMO;

public class CashbackOfferXlsValidationStrategyTest extends AbstractXlsValidationStrategyTest {
    @Test
    public void testOfferWithoutCategory() {
        String offerId = "offerId";
        CashbackOfferXlsValidationStrategy strategy = new CashbackOfferXlsValidationStrategy(true);
        DataCampPromo.PromoDescription cashbackPromo = DataCampPromo.PromoDescription.newBuilder().build();
        DataCampOffer.Offer offerWithoutCategory = DataCampOffer.Offer.newBuilder().build();
        List<CashbackXlsPromoOffer> fileOffers = List.of(new CashbackXlsPromoOffer.Builder()
                .withShopSku(offerId)
                .build()
        );
        List<CashbackXlsPromoOffer> validatedOffers = strategy.apply(cashbackPromo, fileOffers, Map.of(offerId, offerWithoutCategory));
        assertThat(validatedOffers).hasSize(1);
        assertThat(validatedOffers).singleElement()
                .satisfies(
                        offer -> assertThat(offer.getSortedErrorsAsString())
                                .isEqualTo("1. " + OFFER_NOT_ELIGIBLE_FOR_PROMO.getErrorMessage())
                );
    }

    @Test
    public void testOfferWithCategory() {
        String offerId = "offerId";
        int categoryId = 12345;
        CashbackOfferXlsValidationStrategy strategy = new CashbackOfferXlsValidationStrategy(true);
        DataCampPromo.PromoDescription cashbackPromo = DataCampPromo.PromoDescription.newBuilder().build();
        DataCampOffer.Offer offerWithCategory = DataCampOffer.Offer.newBuilder()
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                        .setMarket(DataCampOfferContent.MarketContent.newBuilder()
                                .setCategoryId(categoryId)
                        )
                )
                .build();
        List<CashbackXlsPromoOffer> fileOffers = List.of(new CashbackXlsPromoOffer.Builder()
                .withShopSku(offerId)
                .build()
        );
        List<CashbackXlsPromoOffer> validatedOffers = strategy.apply(cashbackPromo, fileOffers, Map.of(offerId, offerWithCategory));
        assertThat(validatedOffers).hasSize(1);
        assertThat(validatedOffers).singleElement().satisfies(offer -> assertThat(offer.getSortedErrorsAsString()).isEmpty());
    }
}
