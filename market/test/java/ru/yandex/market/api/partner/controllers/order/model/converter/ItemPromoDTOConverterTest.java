package ru.yandex.market.api.partner.controllers.order.model.converter;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.api.partner.controllers.order.model.ItemPromoDTO;
import ru.yandex.market.api.partner.controllers.order.model.PromoTypeDTO;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Тесты для {@link ItemPromoDTOConverter}.
 */
class ItemPromoDTOConverterTest {

    /**
     * Если в  {@link PromoType} добавлен тип без маппинга => {@link ItemPromoDTOConverter#buildFrom} кинет исключение.
     * Добавьте его в IGNORED_PROMO_TYPES или поправьте конвертацию.
     */
    @ParameterizedTest
    @EnumSource(PromoType.class)
    void test_promoTypeDto_should_containMappingsForAllKnownTypes(final PromoType promoType) {
        final PromoDefinition definition = PromoDefinition.builder()
                .type(promoType)
                .build();
        final ItemPromo itemPromo = new ItemPromo(definition, null, null, null);
        ItemPromoDTOConverter.buildFrom(itemPromo);
    }

    /**
     * Защищаемся от указания null в типе промо.
     */
    @Test
    void test_buildFrom_when_nullPromoType_shouldThrow() {
        final NullPointerException ex = Assertions.assertThrows(NullPointerException.class,
                () -> ItemPromoDTOConverter.buildFrom(
                        new ItemPromo(new PromoDefinition(null, null, null, null),
                                null, null, null) {
                            @Override
                            public PromoType getType() {
                                return super.getType();
                            }
                        }
                ));
        assertThat(ex.getMessage(), equalTo("PromoType must not be null"));
    }

    /**
     * Конвертация промо с типом PromoType.MARKET_COUPON.
     */
    @Test
    void test_buildFrom_when_correctCoupon_should_translateCorrectly() {
        ItemPromoDTO promoDTO = ItemPromoDTOConverter.buildFrom(
                ItemPromo.createWithSubsidy(PromoDefinition.marketCouponPromo(), BigDecimal.TEN)
        );
        assertThat(promoDTO, notNullValue());
        assertThat(promoDTO.getSubsidy(), is(BigDecimal.TEN));
        assertThat(promoDTO.getType(), is(PromoTypeDTO.MARKET_COUPON));
    }

    /**
     * Конвертация промо с типом PromoType.MARKET_PROMOCODE.
     */
    @Test
    void test_buildFrom_when_correctPromocode_should_translateCorrectly() {
        ItemPromoDTO promoDTO = ItemPromoDTOConverter.buildFrom(
                ItemPromo
                        .createWithSubsidy(PromoDefinition
                                        .marketPromocodePromo("PromoId", "LPromoId", 123L),
                                BigDecimal.TEN)
        );
        assertThat(promoDTO, notNullValue());
        assertThat(promoDTO.getSubsidy(), is(BigDecimal.TEN));
        assertThat(promoDTO.getType(), is(PromoTypeDTO.MARKET_PROMOCODE));
    }

    /**
     * Конвертация промо с типом PromoType.MARKET_DEAL.
     */
    @Test
    void test_buildFrom_when_correctDeal_should_translateCorrectly() {
        ItemPromoDTO promoDTO = ItemPromoDTOConverter.buildFrom(
                ItemPromo.createWithSubsidy(PromoDefinition.marketDealPromo("someId"), BigDecimal.TEN)
        );
        assertThat(promoDTO, notNullValue());
        assertThat(promoDTO.getSubsidy(), is(BigDecimal.TEN));
        assertThat(promoDTO.getType(), is(PromoTypeDTO.MARKET_DEAL));
        assertThat(promoDTO.getMarketPromoId(), is("someId"));
    }

    /**
     * Конвертация игнорируемого промо с типом {@link PromoType#MARKET_BLUE}
     */
    @Test
    void test_buildFrom_when_correctMarketBlue_should_returnNUll() {
        ItemPromoDTO promoDTO = ItemPromoDTOConverter.buildFrom(
                ItemPromo.createWithSubsidy(PromoDefinition.blueMarketPromo(), BigDecimal.TEN)
        );
        assertThat(promoDTO, nullValue());
    }

    /**
     * Конвертация игнорируемого промо с типом {@link PromoType#CASHBACK}
     */
    @Test
    void test_buildFrom_when_correctCashback_should_returnNUll() {
        ItemPromoDTO promoDTO = ItemPromoDTOConverter.buildFrom(
                ItemPromo.createWithSubsidy(PromoDefinition.cashbackPromo(null, null, null, null), BigDecimal.TEN)
        );
        assertThat(promoDTO, nullValue());
    }

    @Test
    void test_buildFrom_when_passedNull_should_returnNull() {
        assertThat(ItemPromoDTOConverter.buildFrom(null), nullValue());
    }

}
