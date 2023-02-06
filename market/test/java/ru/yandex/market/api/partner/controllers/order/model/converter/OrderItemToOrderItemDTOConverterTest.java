package ru.yandex.market.api.partner.controllers.order.model.converter;

import java.math.BigDecimal;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.controllers.order.model.ItemPromoDTO;
import ru.yandex.market.api.partner.controllers.order.model.OrderItemDTO;
import ru.yandex.market.api.partner.controllers.order.model.OrderItemInstanceResponseDTO;
import ru.yandex.market.api.partner.controllers.order.model.PromoTypeDTO;
import ru.yandex.market.api.partner.controllers.order.model.VatTypeDTO;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.util.OrderItemInstancesUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class OrderItemToOrderItemDTOConverterTest {

    @Test
    void test_buildFrom_when_passedCorrect_should_translateCorrectly() {
        OrderItem source = new OrderItem();
        source.setId(123L);
        source.setFeedId(456L);
        source.setFeedCategoryId("someFeedCatId");
        source.setOfferId("someOfferId");
        source.setOfferName("someOfferName");
        source.setVat(VatType.VAT_0);
        source.setCount(5);
        source.setPrice(BigDecimal.TEN);
        source.setPromos(ImmutableSet.of(ItemPromo.createWithSubsidy(PromoDefinition.marketDealPromo("somePromoId"),
                BigDecimal.TEN)));
        OrderItemInstance orderItemInstance = new OrderItemInstance();
        orderItemInstance.setCis("testCis");
        orderItemInstance.setCisFull("testCisFull");
        source.setInstances(OrderItemInstancesUtil.convertToNode(List.of(orderItemInstance)));

        OrderItemDTO orderItemDTO = Preconditions.checkNotNull(OrderItemToOrderItemDTOConverter.buildFrom(source));

        assertThat(orderItemDTO.getId(), is(123L));
        assertThat(orderItemDTO.getFeedId(), is(456L));
        assertThat(orderItemDTO.getFeedCategoryId(), is("someFeedCatId"));
        assertThat(orderItemDTO.getOfferId(), is("someOfferId"));
        assertThat(orderItemDTO.getOfferName(), is("someOfferName"));
        assertThat(orderItemDTO.getVat(), is(VatTypeDTO.VAT_0));
        assertThat(orderItemDTO.getCount(), is(5));
        assertThat(orderItemDTO.getPrice(), is(BigDecimal.TEN));

        assertThat(orderItemDTO.getPromos(), hasSize(1));
        ItemPromoDTO itemPromoDTO = orderItemDTO.getPromos().get(0);
        assertThat(itemPromoDTO.getType(), is(PromoTypeDTO.MARKET_DEAL));
        assertThat(itemPromoDTO.getMarketPromoId(), is("somePromoId"));
        assertThat(itemPromoDTO.getSubsidy(), is(BigDecimal.TEN));

        OrderItemInstanceResponseDTO instance = Iterables.getOnlyElement(orderItemDTO.getInstances());
        assertThat(instance.getCis(), is("testCis"));
        assertThat(instance.getCisFull(), is("testCisFull"));
    }

    @Test
    void test_buildFrom_when_passedNull_should_returnNull() {
        assertThat(OrderItemToOrderItemDTOConverter.buildFrom(null), nullValue());
    }

    @Test
    void test_buildFrom_when_passedIgnorableItems_should_returnNullNotEmpty() {
        OrderItem itemWithIgnoredPromos = new OrderItem();
        itemWithIgnoredPromos.setPromos(ImmutableSet.of(
                ItemPromo.createWithSubsidy(PromoDefinition.blueMarketPromo(), BigDecimal.TEN)));

        OrderItemDTO convertedItemWithAbsentPromos =
                Preconditions.checkNotNull(OrderItemToOrderItemDTOConverter.buildFrom(itemWithIgnoredPromos));
        assertThat(convertedItemWithAbsentPromos.getPromos(), nullValue());
    }

    @Test
    void test_buildFrom_when_passedWithEmptyPromos_should_convertAsNull() {
        OrderItem itemWithAbsentPromos = new OrderItem();
        itemWithAbsentPromos.setPromos(null);
        OrderItemDTO convertedItemWithAbsentPromos =
                Preconditions.checkNotNull(OrderItemToOrderItemDTOConverter.buildFrom(itemWithAbsentPromos));
        assertThat(convertedItemWithAbsentPromos.getPromos(), nullValue());

        OrderItem itemWithEmptyPromos = new OrderItem();
        itemWithEmptyPromos.setPromos(null);
        convertedItemWithAbsentPromos =
                Preconditions.checkNotNull(OrderItemToOrderItemDTOConverter.buildFrom(itemWithEmptyPromos));
        assertThat(convertedItemWithAbsentPromos.getPromos(), nullValue());
    }
}
