package ru.yandex.market.checkout.checkouter.delivery;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryServiceCustomerInfoList;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryServiceCustomerInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.TrackOrderSource;
import ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.ANOTHER_MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class DeliveryInfoControllerTest extends AbstractWebTestBase {

    private static final long ANOTHER_DELIVERY_SERVICE_ID = 123L;
    private static final DeliveryServiceCustomerInfo EXPECTED_DELIVERY_SERVICE_INFO_1 =
            new DeliveryServiceCustomerInfo(
                    "MOCK_DELIVERY_SERVICE",
                    List.of("+7-(912)-345-67-89", "+7-(912)-345-67-88"),
                    "www.partner100501-site.ru",
                    TrackOrderSource.ORDER_NO,
                    DeliveryServiceSubtype.CONTRACT_COURIER);
    private static final DeliveryServiceCustomerInfo EXPECTED_DELIVERY_SERVICE_INFO_2 =
            new DeliveryServiceCustomerInfo(
                    "ANOTHER_MOCK_DELIVERY_SERVICE",
                    List.of("+7-(912)-345-67-80"),
                    "www.partner2-site.ru",
                    TrackOrderSource.ORDER_NO,
                    DeliveryServiceSubtype.MARKET_COURIER);

    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;

    @Test
    void getDeliveryServiceInfoSingle() throws Exception {
        DeliveryServiceCustomerInfoList deliveryServiceInfo =
                orderDeliveryHelper.getDeliveryServiceInfo(List.of(MOCK_DELIVERY_SERVICE_ID));
        assertThat(deliveryServiceInfo.getDeliveryServiceCustomerInfos(), contains(EXPECTED_DELIVERY_SERVICE_INFO_1));
    }

    @Test
    void getDeliveryServiceInfoEmpty() throws Exception {
        DeliveryServiceCustomerInfoList deliveryServiceInfo =
                orderDeliveryHelper.getDeliveryServiceInfo(List.of(ANOTHER_DELIVERY_SERVICE_ID));

        assertThat(deliveryServiceInfo.getDeliveryServiceCustomerInfos().size(), is(1));
        assertNull(deliveryServiceInfo.getDeliveryServiceCustomerInfos().get(0));
    }

    @Test
    void getDeliveryServiceInfoMultipleWithNull() throws Exception {
        DeliveryServiceCustomerInfoList deliveryServiceInfo =
                orderDeliveryHelper.getDeliveryServiceInfo(List.of(MOCK_DELIVERY_SERVICE_ID,
                        ANOTHER_DELIVERY_SERVICE_ID));

        assertThat(deliveryServiceInfo.getDeliveryServiceCustomerInfos(), contains(EXPECTED_DELIVERY_SERVICE_INFO_1,
                null));
    }

    @Test
    void getDeliveryServiceInfoMultiple() throws Exception {
        DeliveryServiceCustomerInfoList deliveryServiceInfo =
                orderDeliveryHelper.getDeliveryServiceInfo(List.of(MOCK_DELIVERY_SERVICE_ID,
                        ANOTHER_MOCK_DELIVERY_SERVICE_ID));
        assertThat(deliveryServiceInfo.getDeliveryServiceCustomerInfos(), contains(EXPECTED_DELIVERY_SERVICE_INFO_1,
                EXPECTED_DELIVERY_SERVICE_INFO_2));
    }

}
