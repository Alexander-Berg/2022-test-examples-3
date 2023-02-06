package ru.yandex.market.fulfillment.wrap.marschroute.service.tracking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
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
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static java.time.ZoneOffset.ofHours;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteDateTimes.marschrouteDateTime;
import static ru.yandex.market.fulfillment.wrap.marschroute.factory.TrackingInfos.trackingInfo;
import static ru.yandex.market.logistic.api.model.common.OrderStatusCategory.FULFILLMENT;
import static ru.yandex.market.logistic.api.model.common.OrderStatusCategory.OTHER;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(value = {
    OrderHistoryFactory.class,
    TrackingInfoToOrderStatusConverter.class,
    TrackingStatusToOrderStatusTypeMapping.class})
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class OrderHistoryFactoryTest extends BaseIntegrationTest {

    @Autowired
    private OrderHistoryFactory orderHistoryFactory;

    /**
     * Проверяет, что фабрика возвращает историю в виде массива статусов в убывающем порядке по setDate.
     */
    @Test
    void testOrderHistoryIsBeingSortedInDescOrder() throws Exception {
        LocalDateTime now = LocalDate.now().atStartOfDay();

        List<TrackingInfo> tracking = Arrays.asList(
            trackingInfo("NEW", marschrouteDateTime(now), TrackingStatus.NEW),
            trackingInfo("PROCESSED", marschrouteDateTime(now.plusHours(1)), TrackingStatus.PROCESSED),
            trackingInfo("PACKED", marschrouteDateTime(now.plusHours(2)), TrackingStatus.PACKED)
        );

        List<OrderStatus> history = orderHistoryFactory.produce(
            new ResourceId(null, null),
            tracking,
            DeliveryId.POST_RF,
            Arrays.asList(FULFILLMENT, OTHER))
            .getHistory();

        softly.assertThat(history)
            .as("Asserting history size")
            .hasSize(3);

        softly.assertThat(history.get(0).getSetDate().getOffsetDateTime())
            .as("Asserting first order status happened later than second")
            .isAfter(history.get(1).getSetDate().getOffsetDateTime());

        softly.assertThat(history.get(1).getSetDate().getOffsetDateTime())
            .as("Asserting second order status happened later than third")
            .isAfter(history.get(2).getSetDate().getOffsetDateTime());
    }

    /**
     * Проверяет, что несколько последовательных идентичных FF статусов схлопываются в один с самой ранней датой.
     */
    @Test
    void testFactoryRemovesNonUniqueStatuses() throws Exception {
        LocalDateTime now = LocalDate.now().atStartOfDay();

        List<TrackingInfo> tracking = Arrays.asList(
            trackingInfo("CANCELLED_0", marschrouteDateTime(now.plusHours(1)), TrackingStatus.CANCELLED_0),
            trackingInfo("CANCELLED_1", marschrouteDateTime(now.plusHours(2)), TrackingStatus.CANCELLED_1),
            trackingInfo("CANCELLED_2", marschrouteDateTime(now.plusHours(3)), TrackingStatus.CANCELLED_2),
            trackingInfo("LOST", marschrouteDateTime(now.plusHours(4)), TrackingStatus.LOST)
        );

        List<OrderStatus> history = orderHistoryFactory.produce(
            new ResourceId(null, null),
            tracking,
            DeliveryId.POST_RF,
            Arrays.asList(FULFILLMENT, OTHER))
            .getHistory();
        softly.assertThat(history.size())
            .as("Asserting that history has total of two items")
            .isEqualTo(2);

        softly.assertThat(history.get(0).getStatusCode())
            .as("Asserting that first status is lost ")
            .isEqualTo(OrderStatusType.ORDER_IS_LOST);

        softly.assertThat(history.get(1).getStatusCode())
            .as("Asserting that second status is cancelled")
            .isEqualTo(OrderStatusType.ORDER_CANCELLED_FF);

        softly.assertThat(history.get(1).getMessage())
            .as("Asserting that second status message is combined message of 3 tracking infos")
            .isEqualTo(tracking.get(0).getStatus() + "/" + tracking.get(1).getStatus() + "/" + tracking.get(2).getStatus());

        softly.assertThat(history.get(1).getSetDate())
            .as("Asserting that second status has earliest date time among all three other statuses")
            .isEqualTo(DateTime.fromOffsetDateTime(now.plusHours(1).atOffset(ofHours(3))));
    }


    /**
     * Проверяет, что несколько последовательных идентичных FF статусов схлопываются в один с самой ранней датой.
     */
    @Test
    void testFactoryRemovedDuplicatesWhenNeededAndDontWhenNotNeeded() throws Exception {
        LocalDateTime now = LocalDate.now().atStartOfDay();

        List<TrackingInfo> tracking = Arrays.asList(
            trackingInfo("CANCELLED_0", marschrouteDateTime(now), TrackingStatus.CANCELLED_0),
            trackingInfo("LOST", marschrouteDateTime(now.plusHours(1)), TrackingStatus.LOST),
            trackingInfo("CANCELLED_1", marschrouteDateTime(now.plusHours(2)), TrackingStatus.CANCELLED_1),
            trackingInfo("CANCELLED_2", marschrouteDateTime(now.plusHours(3)), TrackingStatus.CANCELLED_2),
            trackingInfo("CANCELLED_3", marschrouteDateTime(now.plusHours(9)), TrackingStatus.CANCELLED_3),
            trackingInfo("CANCELLED_4", marschrouteDateTime(now.plusHours(12)), TrackingStatus.CANCELLED_4)
        );

        List<OrderStatus> history = orderHistoryFactory.produce(
            new ResourceId(null, null),
            tracking,
            DeliveryId.POST_RF,
            Arrays.asList(FULFILLMENT, OTHER))
            .getHistory();
        softly.assertThat(history.size())
            .as("Asserting that history has total of five items")
            .isEqualTo(5);

        softly.assertThat(history.get(0).getStatusCode())
            .as("Asserting that status is cancelled with the latest date from patch")
            .isEqualTo(OrderStatusType.ORDER_CANCELLED_FF);

        softly.assertThat(history.get(0).getSetDate().getOffsetDateTime().toLocalDateTime())
            .as("Asserting that status is cancelled with the latest date from patch")
            .isEqualTo(now.plusHours(12));

        softly.assertThat(history.get(1).getStatusCode())
            .as("Asserting that status is cancelled which is after 6 hours")
            .isEqualTo(OrderStatusType.ORDER_CANCELLED_FF);

        softly.assertThat(history.get(1).getSetDate().getOffsetDateTime().toLocalDateTime())
            .as("Asserting that status is cancelled with the latest date from patch")
            .isEqualTo(now.plusHours(9));

        softly.assertThat(history.get(2).getStatusCode())
            .as("Asserting that status is cancelled squashed ")
            .isEqualTo(OrderStatusType.ORDER_CANCELLED_FF);

        softly.assertThat(history.get(2).getSetDate().getOffsetDateTime().toLocalDateTime())
            .as("Asserting that status is cancelled with the latest date from patch")
            .isEqualTo(now.plusHours(2));

        softly.assertThat(history.get(3).getStatusCode())
            .as("Asserting that second status is lost")
            .isEqualTo(OrderStatusType.ORDER_IS_LOST);

        softly.assertThat(history.get(3).getSetDate().getOffsetDateTime().toLocalDateTime())
            .as("Asserting that status is cancelled with the latest date from patch")
            .isEqualTo(now.plusHours(1));

        softly.assertThat(history.get(4).getStatusCode())
            .as("Asserting that earliest status is cancelled")
            .isEqualTo(OrderStatusType.ORDER_CANCELLED_FF);

        softly.assertThat(history.get(4).getSetDate().getOffsetDateTime().toLocalDateTime())
            .as("Asserting that status is cancelled with the latest date from patch")
            .isEqualTo(now);

    }
}
