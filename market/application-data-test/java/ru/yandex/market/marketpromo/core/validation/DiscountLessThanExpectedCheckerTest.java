package ru.yandex.market.marketpromo.core.validation;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.marketpromo.core.validation.warning.DiscountLessThanExpectedChecker;
import ru.yandex.market.marketpromo.model.CategoryIdWithDiscount;
import ru.yandex.market.marketpromo.model.DirectDiscountOfferParticipation;
import ru.yandex.market.marketpromo.model.DirectDiscountProperties;
import ru.yandex.market.marketpromo.model.LocalOffer;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.WarningCode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

@ExtendWith(MockitoExtension.class)
public class DiscountLessThanExpectedCheckerTest {

    private static final long CATEGORY_ID = 123L;

    private final DiscountLessThanExpectedChecker lessThanExpectedChecker = new DiscountLessThanExpectedChecker();

    @Mock
    private LocalOffer offer;
    @Mock
    private Promo promo;
    @Mock
    private DirectDiscountOfferParticipation offerParticipation;

    @Test
    void shouldNotGetWarningOnRightDiscount() {
        Mockito.when(offer.getBasePrice()).thenReturn(BigDecimal.valueOf(120));
        Mockito.when(offer.getPrice()).thenReturn(BigDecimal.valueOf(100));
        Mockito.when(offer.getCategoryId()).thenReturn(CATEGORY_ID);
        Mockito.when(promo.getMechanicsPropertiesAs(DirectDiscountProperties.class))
                .thenReturn(Optional.of(DirectDiscountProperties.builder()
                        .build()));
        Mockito.when(promo.getCategoriesWithDiscounts()).thenReturn(
                List.of(CategoryIdWithDiscount.of(CATEGORY_ID, BigDecimal.valueOf(10))));

        Set<WarningCode> warningCodes = new HashSet<>();
        lessThanExpectedChecker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, empty());
    }

    @Test
    void shouldNotGetWarningIfNoPricesPresented() {
        Set<WarningCode> warningCodes = Set.of(WarningCode.BASE_PRICE_NOT_EXISTS);
        lessThanExpectedChecker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, hasSize(1));
        assertThat(warningCodes, contains(WarningCode.BASE_PRICE_NOT_EXISTS));
    }

    @Test
    void shouldGetWarningOnDiscountLessThanExpectedOnOfferPricesWithCategoryThreshold() {
        Mockito.when(offer.getBasePrice()).thenReturn(BigDecimal.valueOf(120));
        Mockito.when(offer.getPrice()).thenReturn(BigDecimal.valueOf(100));
        Mockito.when(offer.getCategoryId()).thenReturn(CATEGORY_ID);
        Mockito.when(promo.getMechanicsPropertiesAs(DirectDiscountProperties.class))
                .thenReturn(Optional.of(DirectDiscountProperties.builder()
                        .build()));
        Mockito.when(promo.getCategoriesWithDiscounts()).thenReturn(
                List.of(CategoryIdWithDiscount.of(CATEGORY_ID, BigDecimal.valueOf(30))));

        Set<WarningCode> warningCodes = new HashSet<>();
        lessThanExpectedChecker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, contains(WarningCode.DISCOUNT_LESS_THAN_EXPECTED));
    }

    @Test
    void shouldGetWarningOnDiscountLessThanExpectedOnOfferPricesWithDefaultThreshold() {
        Mockito.when(offer.getBasePrice()).thenReturn(BigDecimal.valueOf(120));
        Mockito.when(offer.getPrice()).thenReturn(BigDecimal.valueOf(100));
        Mockito.when(promo.getMechanicsPropertiesAs(DirectDiscountProperties.class))
                .thenReturn(Optional.of(DirectDiscountProperties.builder()
                        .minimalDiscountPercentSize(BigDecimal.valueOf(30))
                        .build()));

        Set<WarningCode> warningCodes = new HashSet<>();
        lessThanExpectedChecker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, contains(WarningCode.DISCOUNT_LESS_THAN_EXPECTED));
    }

    @Test
    void shouldGetWarningOnDiscountLessThanExpectedOnFixedPricesWithCategoryThreshold() {
        Mockito.when(offerParticipation.getFixedBasePrice()).thenReturn(BigDecimal.valueOf(120));
        Mockito.when(offerParticipation.getFixedPrice()).thenReturn(BigDecimal.valueOf(100));
        Mockito.when(offer.getCategoryId()).thenReturn(CATEGORY_ID);
        Mockito.when(promo.getMechanicsPropertiesAs(DirectDiscountProperties.class))
                .thenReturn(Optional.of(DirectDiscountProperties.builder().build()));
        Mockito.when(promo.getCategoriesWithDiscounts()).thenReturn(
                List.of(CategoryIdWithDiscount.of(CATEGORY_ID, BigDecimal.valueOf(30))));

        Set<WarningCode> warningCodes = new HashSet<>();
        lessThanExpectedChecker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, contains(WarningCode.DISCOUNT_LESS_THAN_EXPECTED));
    }

    @Test
    void shouldGetWarningOnDiscountLessThanExpectedOnFixedPricesWithDefaultThreshold() {
        Mockito.when(offerParticipation.getFixedBasePrice()).thenReturn(BigDecimal.valueOf(120));
        Mockito.when(offerParticipation.getFixedPrice()).thenReturn(BigDecimal.valueOf(100));
        Mockito.when(promo.getMechanicsPropertiesAs(DirectDiscountProperties.class))
                .thenReturn(Optional.of(DirectDiscountProperties.builder()
                        .minimalDiscountPercentSize(BigDecimal.valueOf(30))
                        .build()));

        Set<WarningCode> warningCodes = new HashSet<>();
        lessThanExpectedChecker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, contains(WarningCode.DISCOUNT_LESS_THAN_EXPECTED));
    }

    @Test
    void shouldGetWarningOnDiscountLessThanExpectedOnMixedPricesWithCategoryThreshold() {
        Mockito.when(offer.getBasePrice()).thenReturn(BigDecimal.valueOf(120));
        Mockito.when(offerParticipation.getFixedPrice()).thenReturn(BigDecimal.valueOf(100));
        Mockito.when(offer.getCategoryId()).thenReturn(CATEGORY_ID);
        Mockito.when(promo.getMechanicsPropertiesAs(DirectDiscountProperties.class))
                .thenReturn(Optional.of(DirectDiscountProperties.builder().build()));
        Mockito.when(promo.getCategoriesWithDiscounts()).thenReturn(
                List.of(CategoryIdWithDiscount.of(CATEGORY_ID, BigDecimal.valueOf(30))));

        Set<WarningCode> warningCodes = new HashSet<>();
        lessThanExpectedChecker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, contains(WarningCode.DISCOUNT_LESS_THAN_EXPECTED));
    }

    @Test
    void shouldGetWarningOnDiscountLessThanExpectedOnMixedPricesWithDefaultThreshold() {
        Mockito.when(offerParticipation.getFixedBasePrice()).thenReturn(BigDecimal.valueOf(120));
        Mockito.when(offer.getPrice()).thenReturn(BigDecimal.valueOf(100));
        Mockito.when(promo.getMechanicsPropertiesAs(DirectDiscountProperties.class))
                .thenReturn(Optional.of(DirectDiscountProperties.builder()
                        .minimalDiscountPercentSize(BigDecimal.valueOf(30))
                        .build()));

        Set<WarningCode> warningCodes = new HashSet<>();
        lessThanExpectedChecker.check(offer, promo, offerParticipation, warningCodes);
        assertThat(warningCodes, contains(WarningCode.DISCOUNT_LESS_THAN_EXPECTED));
    }
}
