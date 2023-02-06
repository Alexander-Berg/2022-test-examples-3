package ru.yandex.market.logistic.gateway.service.converter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;

import ru.yandex.market.delivery.mdbclient.model.fulfillment.ItemPlace;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.Korobyte;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.Place;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.ResourceId;
import ru.yandex.market.delivery.mdbclient.model.fulfillment.UnitId;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrderResult;
import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Order;
import ru.yandex.market.logistic.gateway.exceptions.MdbConverterException;
import ru.yandex.market.logistic.gateway.utils.fulfillment.DtoFactory;

public class MdbFulfillmentConverterTest extends BaseTest {

    @Test
    public void testConvertGetOrderResultToMdbSuccess() {
        Order order = DtoFactory.createOrder();
        Partner partner = new Partner(145L);

        Optional<GetOrderResult> getOrderResultOptional =
            MdbFulfillmentConverter.convertGetOrderResultToMdb(order, partner);

        assertions.assertThat(getOrderResultOptional)
            .as("Asserting the result is present")
            .isPresent();
        assertions.assertThat(getOrderResultOptional.get())
            .as("Asserting that converted MDB GetOrderResult instance is valid")
            .usingRecursiveComparison()
            .isEqualTo(createMdbGetOrderResult());
    }

    @Test
    public void testConvertGetOrderResultToMdbSuccessNull() {
        Optional<GetOrderResult> getOrderResultOptional =
            MdbFulfillmentConverter.convertGetOrderResultToMdb(null, null);

        assertions.assertThat(getOrderResultOptional)
            .as("Asserting the result is empty")
            .isEmpty();
    }

    @Test(expected = MdbConverterException.class)
    public void testConvertGetOrderResultToMdbFailed() {
        Order order = new Order.OrderBuilder(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null).build();
        Partner partner = new Partner(null);

        MdbFulfillmentConverter.convertGetOrderResultToMdb(order, partner);
    }

    private GetOrderResult createMdbGetOrderResult() {
        return new GetOrderResult("111", 145L, "Zakaz", Collections.singletonList(createMdbPlace()));
    }

    private Place createMdbPlace() {
        return new Place(createMdbPlaceId(), createMdbKorobyte(), Collections.singletonList(createMdbItemPlace()));
    }

    private ResourceId createMdbPlaceId() {
        return new ResourceId("111", "Zakaz");
    }

    private Korobyte createMdbKorobyte() {
        return new Korobyte(45, 16, 21, BigDecimal.valueOf(3.2), BigDecimal.valueOf(2), BigDecimal.valueOf(1.2));
    }

    private ItemPlace createMdbItemPlace() {
        return new ItemPlace(createMdbUnitId(), 1);
    }

    private UnitId createMdbUnitId() {
        return new UnitId("123id", 0L, "75690200345480.Checkouter-test-20");
    }
}
