package ru.yandex.market.marketpromo.core.validation;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.marketpromo.core.test.generator.Offers;
import ru.yandex.market.marketpromo.core.validation.warning.BasePriceExistsChecker;
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
public class BasePriceExistsCheckerTest {

    private final BasePriceExistsChecker checker = new BasePriceExistsChecker();

    @Mock
    private Promo promo;

    @Test
    void shouldNotReturnWarningOnExistingBasePrice() {
        LocalOffer offer = Offers.localOffer(
                Offers.shopSku("etu.200327"),
                Offers.basePrice(100L)
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
        assertThat(warningCodes, empty());
    }

    @Test
    void shouldNotReturnWarningOnExistingFixedBasePrice() {
        LocalOffer offer = Offers.localOffer(
                Offers.shopSku("etu.200327"),
                Offers.basePrice(100L)
        );

        DirectDiscountOfferParticipation offerParticipation = DirectDiscountOfferParticipation
                .builder()
                .fixedBasePrice(BigDecimal.valueOf(100))
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
        assertThat(warningCodes, empty());
    }

    @Test
    void shouldReturnWarningOnNotExistingBase() {
        LocalOffer offer = Offers.localOffer(
                Offers.shopSku("etu.200327")
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
    void shouldNotReturnWarningOnNotExistingBaseNotParticipate() {
        LocalOffer offer = Offers.localOffer(
                Offers.shopSku("etu.200327"),
                Offers.basePrice(100L)
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
