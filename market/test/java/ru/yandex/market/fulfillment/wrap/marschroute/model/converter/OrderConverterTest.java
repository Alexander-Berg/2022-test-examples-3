package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.MarschrouteOrder;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteDeliveryInterval;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteDimensions;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschroutePaymentType;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Delivery;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.Location;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.PaymentType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.TimeInterval;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({OrderConverter.class, LocationConverter.class})
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class OrderConverterTest extends BaseIntegrationTest {

    @Autowired
    private OrderConverter orderConverter;

    @MockBean
    private GeoInformationProvider geoInformationProvider;

    @BeforeEach
    void init() {
        when(geoInformationProvider.findWithKladr(anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void testConversion() throws Exception {
        Order initialOrder = createOrder();
        MarschrouteOrder convertedOrder = orderConverter.convert(initialOrder);

        softly.assertThat(convertedOrder.getId())
                .as("Asserting order id value")
                .isEqualTo(initialOrder.getOrderId().getYandexId());

        softly.assertThat(convertedOrder.getDeliverySum())
                .as("Asserting delivery sum value is equal to delivery cost with rounding")
                .isEqualTo(226);

        softly.assertThat(convertedOrder.getPaymentType())
                .as("Asserting payment type")
                .isEqualTo(MarschroutePaymentType.CASH);

        softly.assertThat(convertedOrder.getSendDate())
                .as("Asserting send date")
                .isNull();

        softly.assertThat(convertedOrder.getTimeFrom())
                .as("Asserting time from is null")
                .isNull();

        softly.assertThat(convertedOrder.getDeliveryInterval())
                .as("Asserting delivery interval")
                .isEqualTo(MarschrouteDeliveryInterval.WORKING_TIME);

        softly.assertThat(convertedOrder.getWeight())
                .as("Asserting weight value was converted with rounding")
                .isEqualTo(9112);

        MarschrouteDimensions dimensions = convertedOrder.getDimensions();

        softly.assertThat(dimensions.getWidth())
                .as("Asserting width value")
                .isEqualTo(50);

        softly.assertThat(dimensions.getHeight())
                .as("Asserting height value")
                .isEqualTo(100);

        softly.assertThat(dimensions.getDepth())
                .as("Asserting depth value")
                .isEqualTo(150);

        softly.assertThat(convertedOrder.getPickupPointCode())
                .as("Asserting pickup point code")
                .isEqualTo(initialOrder.getPickupPointCode());

        softly.assertThat(convertedOrder.getComment())
                .as("Asserting comment value")
                .isEqualTo("/overflow " + initialOrder.getComment());
    }


    private Order createOrder() {
        Korobyte korobyte = new Korobyte.KorobyteBuiler(5, 10, 15, BigDecimal.valueOf(9.1111)).build();

        return new Order.OrderBuilder(
                new ResourceId("yandexId", "fulfillmentId"),
                createLocation("8 symbol", "8 symbol", "overflow"),
                null,
                korobyte,
                null,
                null,
                PaymentType.CASH,
                createOrderDelivery(),
                null,
                BigDecimal.valueOf(225.1),
                null,
                null,
                null,
                null,
                null,
                null
        )
                .setDeliveryDate(new DateTime("2017-09-10T10:30"))
                .setDeliveryInterval(new TimeInterval("12:00:00/13:00:00"))
                .setPickupPointCode("code")
                .setComment("comment")
                .build();
    }

    private Delivery createOrderDelivery() {
        return new Delivery(
                new ResourceId("yandexId", "deliveryId"),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Location createLocation(String house, String building, String housing) {
        return new Location(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            house,
            building,
            housing,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            1
        );
    }
}
