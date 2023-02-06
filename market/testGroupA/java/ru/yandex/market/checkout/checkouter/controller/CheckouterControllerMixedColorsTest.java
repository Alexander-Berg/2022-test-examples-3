package ru.yandex.market.checkout.checkouter.controller;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.configuration.CheckoutOptionParameters;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider.OrderItemBuilder;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.dsbsOrderItem;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.shopDeliveryOrder;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;

public class CheckouterControllerMixedColorsTest extends AbstractWebTestBase {

    private OrderItemBuilder dbsItem = dsbsOrderItem()
            .offer("some dbs offer");

    private OrderItemBuilder ffItem = orderItemWithSortingCenter()
            .offer("some ff offer");

    @Test
    void shouldCreateMultiorderWithDifferentOrderColors() {
        var dbsOrder = shopDeliveryOrder()
                .itemBuilder(dbsItem)
                .build();
        var ffOrder = orderWithYandexDelivery()
                .itemBuilder(ffItem)
                .build();
        var multicolorCart = MultiCartProvider.createBuilder()
                .order(dbsOrder)
                .order(ffOrder)
                .buyer(BuyerProvider.getBuyer())
                .build();

        var props = new Parameters(multicolorCart);

        props.setApiSettings(ApiSettings.PRODUCTION);
        props.setPaymentMethod(PaymentMethod.YANDEX);

        assertThat(dbsOrder.getShopId(), notNullValue());
        assertThat(ffOrder.getShopId(), notNullValue());

        props.addShopMetaData(
                dbsOrder.getShopId(),
                ShopSettingsHelper.getDsbsShopPrepayMeta()
        );

        props.addShopMetaData(
                ffOrder.getShopId(),
                ShopSettingsHelper.getDefaultMeta()
        );

        props.configuration().cart().mocks(dbsOrder.getLabel())
                .setPushApiDeliveryResponses(
                        List.of(DeliveryProvider.createFrom(dbsOrder.getDelivery())
                                .buildResponse(DeliveryResponse::new)));

        props.configuration().checkout().mocks(dbsOrder.getLabel()).setPushApiDeliveryResponses(
                List.of(DeliveryProvider.createFrom(dbsOrder.getDelivery())
                        .buildResponse(DeliveryResponse::new))
        );

        var offers = List.of(
                FoundOfferBuilder.createFrom(ffItem.build())
                        .shopId(ffOrder.getShopId())
                        .build(),
                FoundOfferBuilder.createFrom(dbsItem.build())
                        .shopId(dbsOrder.getShopId())
                        .build()
        );

        for (Order cart : multicolorCart.getCarts()) {
            var conf = props.configuration().cart().mocks(cart.getLabel());
            conf.getReportParameters().setOffers(offers);
            conf.getReportParameters().setActualDelivery(
                    ActualDeliveryProvider.builder()
                            .addDelivery(DeliveryProvider.createFrom(cart.getDelivery())
                                    .buildActualDeliveryOption())
                            .build()
            );
            var option = new CheckoutOptionParameters();
            option.setDeliveryType(cart.getDelivery().getType());
            option.setDeliveryPartnerType(cart.getDelivery().getDeliveryPartnerType());

            props.configuration().checkout()
                    .addOrderOptions(cart.getLabel(), option);
        }

        var multiOrder = orderCreateHelper.createMultiOrder(props);

        assertThat(multiOrder, notNullValue());
    }

}
