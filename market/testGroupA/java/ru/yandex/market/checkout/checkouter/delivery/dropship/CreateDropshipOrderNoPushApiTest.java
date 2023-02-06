package ru.yandex.market.checkout.checkouter.delivery.dropship;

import java.util.Calendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.LocalDeliveryOptionProvider;
import ru.yandex.market.checkout.util.pushApi.PushApiConfigurer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_STRAIGHT_DROPSHIP_FLOW;

public class CreateDropshipOrderNoPushApiTest extends AbstractWebTestBase {

    @Autowired
    protected PaymentService paymentService;
    @Autowired
    private PushApiConfigurer pushApiConfigurer;
    @Autowired
    private OrderPayHelper orderPayHelper;
    private Parameters parameters;
    private Boolean isStraightDropshipFlow;

    @BeforeEach
    public void setUp() throws Exception {
        isStraightDropshipFlow = checkouterFeatureReader.getBoolean(ENABLE_STRAIGHT_DROPSHIP_FLOW);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 18);
        calendar.set(Calendar.MINUTE, 18);
        setFixedTime(calendar.toInstant());
        trustMockConfigurer.resetRequests();
    }

    @AfterEach
    public void tearDown() {
        mixinLogicSelect(isStraightDropshipFlow);
    }

    private void mixinLogicSelect(boolean newEnabled) {
        checkouterFeatureWriter.writeValue(ENABLE_STRAIGHT_DROPSHIP_FLOW, newEnabled);
    }

    @Test
    @DisplayName("Создаём Dropship заказ несмотря на сломаный push-api")
    public void shouldCreateDropshipOrderWithoutPushApi() {
        mixinLogicSelect(true);

        parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        parameters.setMockPushApi(false);
        // создаём заказа даже не смотря на то, что accept не работает
        pushApiConfigurer.mockCart(parameters.getOrder(), parameters.getPushApiDeliveryResponses(), false);
        pushApiConfigurer.mockAcceptShopFailure(parameters.getOrder());

        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getRgb(), is(Color.BLUE));
        assertThat(order.getAcceptMethod(), is(OrderAcceptMethod.PUSH_API));
        assertThat(order.isFulfilment(), is(false));
        assertThat(order.getItems(), hasSize(1));
        assertThat(order.getItems().iterator().next().getWidth(), equalTo(DropshipDeliveryHelper.WIDTH));
        assertThat(order.getItems().iterator().next().getHeight(), equalTo(DropshipDeliveryHelper.HEIGHT));
        assertThat(order.getItems().iterator().next().getDepth(), equalTo(DropshipDeliveryHelper.DEPTH));
        assertThat(order.getDelivery().getDeliveryPartnerType(), is(DeliveryPartnerType.YANDEX_MARKET));
        assertThat(order.getDelivery().getDeliveryServiceId(),
                is(LocalDeliveryOptionProvider.DROPSHIP_DELIVERY_SERVICE_ID));
        assertThat(order.getDelivery().getParcels(), hasSize(1));
        assertThat(order.getDelivery().getParcels().get(0).getParcelItems(), hasSize(1));
    }


    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldMoveDropshipToProcessingAfterUnpaid(boolean newMardoMixinLogic) {
        mixinLogicSelect(newMardoMixinLogic);
        parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), is(newMardoMixinLogic ? OrderStatus.PENDING : OrderStatus.PROCESSING));
    }
}
