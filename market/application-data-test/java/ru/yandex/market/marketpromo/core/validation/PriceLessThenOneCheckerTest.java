package ru.yandex.market.marketpromo.core.validation;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.marketpromo.core.validation.warning.PriceLessThenOneChecker;
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
import static org.hamcrest.Matchers.hasSize;

@ExtendWith(MockitoExtension.class)
public class PriceLessThenOneCheckerTest {

    private final PriceLessThenOneChecker checker = new PriceLessThenOneChecker();

    @Mock
    private LocalOffer offer;
    @Mock
    private Promo promo;
    @Mock
    private DirectDiscountOfferParticipation offerParticipation;

    @Test
    void shouldReturnWarningLessThenOne() {
        DirectDiscountOfferParticipation offerParticipation = DirectDiscountOfferParticipation
                .builder()
                .fixedBasePrice(BigDecimal.TEN)
                .fixedPrice(BigDecimal.ZERO)
                .offerPromoParticipation(
                        OfferPromoParticipation.builder()
                                .promoId("some promo")
                                .offerId(OfferId.of(IdentityUtils.hashId("some promo"), 12L))
                                .participate(true)
                                .build()
                )
                .build();

        Set<WarningCode> warningCodes = new HashSet<>();
        checker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, contains(WarningCode.PRICE_LESS_THEN_ONE));
    }

    @Test
    void shouldNotReturnWarningMoreThenOne() {
        DirectDiscountOfferParticipation offerParticipation = DirectDiscountOfferParticipation
                .builder()
                .fixedBasePrice(BigDecimal.TEN)
                .fixedPrice(BigDecimal.valueOf(5L))
                .offerPromoParticipation(
                        OfferPromoParticipation.builder()
                                .promoId("some promo")
                                .offerId(OfferId.of(IdentityUtils.hashId("some promo"), 12L))
                                .participate(true)
                                .build()
                )
                .build();

        Set<WarningCode> warningCodes = new HashSet<>();
        checker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, empty());
    }
    @Test
    void shouldNotReturnWarningNullPrice() {
        Set<WarningCode> warningCodes = Set.of(WarningCode.BASE_PRICE_NOT_EXISTS);
        checker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, hasSize(1));
        assertThat(warningCodes, contains(WarningCode.BASE_PRICE_NOT_EXISTS));
    }

}
