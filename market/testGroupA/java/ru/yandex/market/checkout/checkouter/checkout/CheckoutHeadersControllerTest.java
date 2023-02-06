package ru.yandex.market.checkout.checkouter.checkout;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.storage.jooq.OrderBuyerRecordMapper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkouter.jooq.tables.records.OrderBuyerRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_YANDEX_ICOOKIE;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_YANDEX_I_COOKIE;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_BUYER;

public class CheckoutHeadersControllerTest extends AbstractWebTestBase {

    public static final String I_AM_ICOOKIE = "I am icookie!!!";

    @Autowired
    private DSLContext dsl;

    @Test
    public void shouldSaveICookieToBuyer() throws Exception {
        checkouterFeatureWriter.writeValue(ENABLE_YANDEX_I_COOKIE, true);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        HttpHeaders headers = new HttpHeaders();
        headers.set(X_YANDEX_ICOOKIE, I_AM_ICOOKIE);
        MultiOrder checkout = orderCreateHelper.checkout(multiCart, parameters, headers);

        Order order = orderService.getOrder(checkout.getOrders().get(0).getId());
        assertEquals(I_AM_ICOOKIE, order.getBuyer().getICookie());
    }

    @Test
    public void shouldSaveICookieToBuyerIfOrderBuyerExists() throws Exception {
        checkouterFeatureWriter.writeValue(ENABLE_YANDEX_I_COOKIE, true);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        MultiCart multiCart = orderCreateHelper.cart(parameters);

        // выставляем на уровне конкретного заказа копию байера
        multiCart.getCarts().get(0).setBuyer(new Buyer(multiCart.getBuyer()));

        HttpHeaders headers = new HttpHeaders();
        headers.set(X_YANDEX_ICOOKIE, I_AM_ICOOKIE);
        orderCreateHelper.checkout(multiCart, parameters, headers);

        // В БД
        OrderBuyerRecord record = dsl.selectFrom(ORDER_BUYER).fetchSingle();
        assertEquals(I_AM_ICOOKIE, record.getIcookie());

        Buyer buyer = OrderBuyerRecordMapper.toBuyer(record);
        assertEquals(I_AM_ICOOKIE, buyer.getICookie());
    }

}
