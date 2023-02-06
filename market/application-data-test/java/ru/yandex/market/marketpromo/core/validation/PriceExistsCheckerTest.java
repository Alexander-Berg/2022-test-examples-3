package ru.yandex.market.marketpromo.core.validation;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.marketpromo.core.test.generator.Offers;
import ru.yandex.market.marketpromo.core.validation.warning.PriceExistsChecker;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.LocalOffer;
import ru.yandex.market.marketpromo.model.OfferId;
import ru.yandex.market.marketpromo.model.OfferPromoParticipation;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.WarningCode;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

@ExtendWith(MockitoExtension.class)
public class PriceExistsCheckerTest {

    private final PriceExistsChecker checker = new PriceExistsChecker();

    @Mock
    private Promo promo;

    @Test
    void shouldNotReturnWarningOnExistingPrice() {
        LocalOffer offer = Offers.localOffer(
                Offers.shopSku("etu.200327"),
                Offers.price(100L)
        );

        DirectDiscountOfferParticipation offerParticipation = DirectDiscountOfferParticipation
                .builder()
                .offerPromoParticipation(
                        OfferPromoParticipation.builder()
                                .promoId("some promo")
                                .offerId(OfferId.of(IdentityUtils.hashId("etu.200327"), 12L))
                                .participate(true)
                                .build()
                )
                .fixedPrice(BigDecimal.TEN)
                .build();

        Set<WarningCode> warningCodes = new HashSet<>();
        checker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, empty());
    }

    @Test
    void shouldReturnWarningOnNotExistingPrice() {
        LocalOffer offer = Offers.localOffer(
                Offers.shopSku("etu.200327"),
                Offers.price(BigDecimal.TEN)
        );

        DirectDiscountOfferParticipation offerParticipation = DirectDiscountOfferParticipation
                .builder()
                .offerPromoParticipation(
                        OfferPromoParticipation.builder()
                                .promoId("some promo")
                                .offerId(OfferId.of(IdentityUtils.hashId("etu.200327"), 12L))
                                .participate(true)
                                .build()
                )
                .build();

        Set<WarningCode> warningCodes = new HashSet<>();
        checker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, contains(WarningCode.BASE_PRICE_NOT_EXISTS));
    }

    @Test
    void shouldNotReturnWarningIfNotParticipate() {
        LocalOffer offer = Offers.localOffer(
                Offers.shopSku("etu.200327"),
                Offers.price(100L)
        );

        DirectDiscountOfferParticipation offerParticipation = DirectDiscountOfferParticipation
                .builder()
                .offerPromoParticipation(
                        OfferPromoParticipation.builder()
                                .promoId("some promo")
                                .offerId(OfferId.of(IdentityUtils.hashId("etu.200327"), 12L))
                                .participate(false)
                                .build()
                )
                .build();

        Set<WarningCode> warningCodes = new HashSet<>();
        checker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, empty());
    }
}
