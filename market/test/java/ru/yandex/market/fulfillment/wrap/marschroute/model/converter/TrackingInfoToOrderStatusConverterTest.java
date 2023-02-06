package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.DeliveryId;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking.TrackingInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking.TrackingStatus;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.fulfillment.wrap.marschroute.service.order.status.TrackingInfoToOrderStatusConverter;
import ru.yandex.market.fulfillment.wrap.marschroute.service.order.status.mapping.TrackingStatusToOrderStatusTypeMapping;
import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatus;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteDateTimes.marschrouteDateTime;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.TrackingInfos.trackingInfo;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import({TrackingInfoToOrderStatusConverter.class,
    TrackingStatusToOrderStatusTypeMapping.class})
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class TrackingInfoToOrderStatusConverterTest extends BaseIntegrationTest {

    @Autowired
    private TrackingInfoToOrderStatusConverter converter;

    @Test
    void testConversion() throws Exception {
        TrackingInfo trackingInfo = trackingInfo(
            "Отменен",
            marschrouteDateTime(LocalDate.of(1970, 1, 1).atStartOfDay()),
            TrackingStatus.CANCELLED_0);

        List<OrderStatus> orderStatuses = converter.toOrderStatusList(trackingInfo, DeliveryId.POST_RF);

        softly.assertThat(orderStatuses)
            .as("Order should have two cancelled statuses")
            .hasSize(2);

        OrderStatus fulfillmentOrderStatus = orderStatuses.get(0);

        softly.assertThat(fulfillmentOrderStatus.getMessage())
            .as("Asserting message value")
            .isEqualTo(trackingInfo.getStatus());

        softly.assertThat(fulfillmentOrderStatus.getSetDate().getOffsetDateTime())
            .as("Asserting date time value")
            .isEqualTo(trackingInfo.getDate().getOffsetDateTime());

        softly.assertThat(fulfillmentOrderStatus.getStatusCode())
            .as("Asserting that status code is CANCELLED")
            .isEqualTo(OrderStatusType.ORDER_CANCELLED_FF);

        OrderStatus deliveryOrderStatus = orderStatuses.get(1);

        softly.assertThat(deliveryOrderStatus.getMessage())
            .as("Asserting message value")
            .isEqualTo(trackingInfo.getStatus());

        softly.assertThat(deliveryOrderStatus.getSetDate().getOffsetDateTime())
            .as("Asserting date time value")
            .isEqualTo(trackingInfo.getDate().getOffsetDateTime());

        softly.assertThat(deliveryOrderStatus.getStatusCode())
            .as("Asserting that status code is ORDER_CANCELLED_DS")
            .isEqualTo(OrderStatusType.ORDER_CANCELLED_DS);
    }
}
