package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;
import ru.yandex.market.antifraud.orders.web.entity.OrderItemChange;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.util.CastlingUtils;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public class ActualDeliveryRetryTest extends AbstractWebTestBase {

    @Test
    void shouldRetryOnStockStorageCountChange() {
        var params = BlueParametersProvider.defaultBlueOrderParameters();
        var item = params.getItems().iterator().next();
        CastlingUtils.castle(BigDecimal.valueOf(5), item);

        params.configuration().cart(params.getOrder()).setShouldMockStockStorageGetAmountResponse(false);
        params.turnOffErrorChecks();

        var ssItem = SSItemAmount.of(
                SSItem.of(item.getShopSku(), item.getSupplierId(), item.getWarehouseId()), 3);

        stockStorageConfigurer.mockGetAvailableCount(ssItem);

        var cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);
        var logs = reportConfigurer.findPlaceCall(MarketReportPlace.ACTUAL_DELIVERY);

        assertThat(cart, notNullValue());
        assertThat(cart.getCarts().get(0).getItem(item.getOfferItemKey()).getChanges(), hasItem(ItemChange.COUNT));
        assertThat(logs, hasSize(2));
    }

    @Test
    void shouldRetryOnAntifraudCountChange() {
        var params = BlueParametersProvider.defaultBlueOrderParameters();
        var item = params.getItems().iterator().next();
        CastlingUtils.castle(BigDecimal.valueOf(5), item);

        mstatAntifraudConfigurer.mockVerdict(verdict(item, 4));
        params.turnOffErrorChecks();

        var cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);
        var logs = reportConfigurer.findPlaceCall(MarketReportPlace.ACTUAL_DELIVERY);

        assertThat(cart, notNullValue());
        assertThat(cart.getCarts().get(0).getItem(item.getOfferItemKey()).getChanges(), hasItem(ItemChange.COUNT));
        assertThat(logs, hasSize(2));
    }

    @Test
    void shouldRetryOnPushApiCountChange() {
        var params = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        var item = params.getItems().iterator().next();
        CastlingUtils.castle(BigDecimal.valueOf(4), item);

        params.setMockPushApi(false);
        pushApiConfigurer.mockCart(params.getOrder(), params.getPushApiDeliveryResponses(), true);
        params.turnOffErrorChecks();

        CastlingUtils.castle(BigDecimal.valueOf(5), item);

        var cart = orderCreateHelper.multiCartActualizeWithMapToMultiCart(params);
        var logs = reportConfigurer.findPlaceCall(MarketReportPlace.ACTUAL_DELIVERY);

        assertThat(cart, notNullValue());
        assertThat(cart.getCarts().get(0).getItem(item.getOfferItemKey()).getChanges(), hasItem(ItemChange.COUNT));
        assertThat(logs, hasSize(2));
    }

    OrderVerdict verdict(@Nonnull OrderItem item, int count) {
        return new OrderVerdict(
                Set.of(new AntifraudCheckResult(
                        AntifraudAction.ORDER_ITEM_CHANGE,
                        "",
                        ""
                )),
                new OrderResponseDto(List.of(
                        new OrderItemResponseDto(null, item.getFeedId(), item.getOfferId(), null, count,
                                Set.of(OrderItemChange.COUNT))
                )),
                false
        );
    }
}
