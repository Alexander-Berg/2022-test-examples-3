package ru.yandex.market.checkout.checkouter.order;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.common.report.model.OfferPicture;

/**
 * @author jkt on 02/01/2022.
 */
public class OrderPartialsTest extends AbstractWebTestBase {

    private static final Comparator<Object> DONT_COMPARE = (Object o1, Object o2) -> 0;

    private Order order;

    private static Stream<Arguments> identicalPartialsParametersForRecent() {
        return Stream.of(
                Arguments.of(OptionalOrderPart.DELIVERY, OptionalOrderPart.DELIVERY_PARCELS),
                Arguments.of(OptionalOrderPart.DELIVERY, OptionalOrderPart.DELIVERY_VERIFICATION_CODE)
        );
    }

    private static Stream<Arguments> identicalPartialsParametersForOrders() {
        return Stream.of(
                Arguments.of(OptionalOrderPart.DELIVERY, OptionalOrderPart.DELIVERY_PARCELS),
                Arguments.of(OptionalOrderPart.DELIVERY, OptionalOrderPart.DELIVERY_VERIFICATION_CODE),
                // кейсы ниже не нужны после https://st.yandex-team.ru/MARKETCHECKOUT-25335
                Arguments.of(OptionalOrderPart.DELIVERY, null),
                Arguments.of(OptionalOrderPart.ITEMS, null),
                Arguments.of(OptionalOrderPart.BUYER, null)
        );
    }

    @BeforeEach
    public void createOrder() {
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
    }

    @ParameterizedTest
    @EnumSource(value = OptionalOrderPart.class, names = {"DELIVERY", "ITEMS", "BUYER"}, mode = EnumSource.Mode.EXCLUDE)
    public void shouldReturnSameDataInDifferentPartialMethods(OptionalOrderPart partial) {

        Order orderRecentPartial = getOrderRecentPartial(
                partial,
                // потому что обычная ручка заказов всегда отдает эти три паршиала
                OptionalOrderPart.DELIVERY, OptionalOrderPart.ITEMS, OptionalOrderPart.BUYER
        );
        Order orderPartial = getOrderPartial(partial);

        compareOrdersWithoutPicturesAndItemsAndPromos(orderRecentPartial, orderPartial);
    }

    @Test // вообще это баг и надо пофиксить в проекте https://st.yandex-team.ru/MARKETCHECKOUT-25335
    public void shouldNotReturnIdenticalPromoPrices() {
        Order orderRecentPartial = getOrderRecentPartial();
        Order orderPartial = getOrderPartial();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(orderRecentPartial.getPromoPrices()).isNotEqualTo(orderPartial.getPromoPrices());
            softly.assertThat(orderRecentPartial.getPromoPrices().getBuyerTotalDiscount()).isNull();
            softly.assertThat(orderRecentPartial.getPromoPrices().getBuyerItemsTotalDiscount()).isNull();
            softly.assertThat(orderRecentPartial.getPromoPrices().getBuyerTotalBeforeDiscount()).isNull();
            softly.assertThat(orderRecentPartial.getPromoPrices().getBuyerItemsTotalBeforeDiscount()).isNull();
        });
    }

    @Test // вообще это баг и надо пофиксить в проекте https://st.yandex-team.ru/MARKETCHECKOUT-25335
    public void shouldNotReturnIdenticalItems() {
        Order orderRecentPartial = getOrderRecentPartial(OptionalOrderPart.ITEMS);
        Order orderPartial = getOrderPartial(OptionalOrderPart.ITEMS);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(getFirstItem(orderRecentPartial))
                    .usingComparatorForFields(DONT_COMPARE, "prices.buyerPriceBeforeDiscount")
                    .isEqualToComparingFieldByFieldRecursively(getFirstItem(orderPartial));
            softly.assertThat(getFirstItem(orderRecentPartial).getPrices())
                    .extracting(ItemPrices::getBuyerPriceBeforeDiscount)
                    .isNull();
        });
    }

    @ParameterizedTest
    @MethodSource("identicalPartialsParametersForRecent")
    public void shouldReturnSameRecentOrderWithDeliveryPartials(OptionalOrderPart firstIdenticalPartial,
                                                                OptionalOrderPart secondIdenticalPartial) {

        Order orderDelivery = getOrderRecentPartial(
                firstIdenticalPartial
        );

        Order orderDeliveryParcels = getOrderRecentPartial(
                secondIdenticalPartial
        );

        compareOrdersWithoutPicturesAndItemsAndPromos(orderDelivery, orderDeliveryParcels);
    }

    @ParameterizedTest
    @MethodSource("identicalPartialsParametersForOrders")
    public void shouldReturnSameOrderWithDeliveryPartials(OptionalOrderPart firstIdenticalPartial,
                                                          OptionalOrderPart secondIdenticalPartial) {

        Order orderDelivery = getOrderPartial(
                firstIdenticalPartial
        );

        Order orderDeliveryParcels = getOrderPartial(
                secondIdenticalPartial
        );

        compareOrdersWithoutPicturesAndItemsAndPromos(orderDelivery, orderDeliveryParcels);
    }


    private Order getOrderPartial(OptionalOrderPart... partials) {
        return orderService.getOrder(
                order.getId(), ClientInfo.SYSTEM, Arrays.stream(partials).collect(Collectors.toSet())
        );
    }

    private Order getOrderRecentPartial(OptionalOrderPart... partials) {
        return orderService.getOrdersPartial(
                OrderSearchRequest.builder()
                        .withOrderIds(new Long[]{order.getId()})
                        .withPartials(Arrays.stream(partials)
                                .filter(Objects::nonNull)
                                .toArray(OptionalOrderPart[]::new))
                        .build(),
                ClientInfo.SYSTEM
        ).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Ручка не выдала заказов. Дальше не проверить."));
    }

    @NotNull
    private OrderItem getFirstItem(Order order) {
        return order.getItems().stream()
                .sorted(Comparator.comparingLong(OrderItem::getId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Создался заказ без айтемов. Дальше не проверить"));
    }

    private ObjectAssert<Order> compareOrdersWithoutPicturesAndItemsAndPromos(Order actual, Order expected) {
        return Assertions.assertThat(actual)
                .usingComparatorForType(Comparator.comparing(OfferPicture::getUrl), OfferPicture.class)
                .usingComparatorForType(Comparator.comparing(OrderItem::getOfferId), OrderItem.class)
                .usingComparatorForFields(DONT_COMPARE, "itemsByFeedOffer")
                .usingComparatorForFields(DONT_COMPARE, "promoPrices")
                .isEqualToComparingFieldByFieldRecursively(expected);
    }
}
