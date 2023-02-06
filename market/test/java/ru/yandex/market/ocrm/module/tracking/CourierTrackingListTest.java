package ru.yandex.market.ocrm.module.tracking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Result;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.ticket.test.ModuleTicketTestConfiguration;
import ru.yandex.market.ocrm.module.order.ModuleOrderTestConfiguration;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;
import ru.yandex.market.ocrm.module.tpl.CourierInfo;
import ru.yandex.market.ocrm.module.tpl.DeliveryInfo;
import ru.yandex.market.ocrm.module.tpl.MarketTplClient;
import ru.yandex.market.ocrm.module.tpl.Tracking;
import ru.yandex.market.ocrm.module.tpl.TrackingLink;

@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration(classes = CourierTrackingListTest.TestConfiguration.class)
public class CourierTrackingListTest {

    @Inject
    private EntityStorageService entityStorageService;
    @Inject
    private OrderTestUtils orderTestUtils;

    @Inject
    private MarketTplClient marketTplClient;

    @Inject
    CourierTrackingAvailabilityDetector courierTrackingAvailabilityDetector;

    @Test
    public void simple() {
        Order order = orderTestUtils.createOrder();
        final Long orderId = order.getOrderId();

        setupCheckIsCourierTrackingInfoAvailable(orderId, true);

        TrackingLink trackingLink = new TrackingLink(
                "trackingId123",
                "http://example.com/trackingLink",
                orderId
        );
        setupGetTrackingLink(orderId, trackingLink);

        Tracking tracking = new Tracking(
                trackingLink.getTrackingId(),
                new CourierInfo(
                        "courierName123",
                        "courierSurname123",
                        "+79123456789"
                ),
                new DeliveryInfo(
                        OffsetDateTime.of(
                                LocalDate.of(2020, 5, 20),
                                LocalTime.of(16, 10),
                                ZoneOffset.UTC
                        ),
                        OffsetDateTime.of(
                                LocalDate.of(2020, 5, 20),
                                LocalTime.of(17, 10),
                                ZoneOffset.UTC
                        )
                )
        );
        setupGetTracking(trackingLink.getTrackingId(), tracking);

        List<CourierTracking> payments = getCourierTracking(orderId);

        Assertions.assertEquals(1, payments.size());
        final CourierTracking actualCourierTracking = payments.get(0);

        Assertions.assertEquals(trackingLink.getLink(), actualCourierTracking.getTrackingLink());
        Assertions.assertEquals(tracking.getCourierInfo().getName(), actualCourierTracking.getCourierFirstName());
        Assertions.assertEquals(tracking.getCourierInfo().getSurname(), actualCourierTracking.getCourierLastName());
        Assertions.assertEquals(tracking.getCourierInfo().getPhone(),
                actualCourierTracking.getCourierPhone().getNormalized());

        Assertions.assertEquals(tracking.getDeliveryInfo().getExpectedTimeFrom(),
                actualCourierTracking.getExpectedDeliveryTimeFrom());
        Assertions.assertEquals(tracking.getDeliveryInfo().getExpectedTimeTo(),
                actualCourierTracking.getExpectedDeliveryTimeTo());

    }

    @Test
    public void courierInfoIsUnavailable__expectThereIsNoTrackingEntity() {
        Order order = orderTestUtils.createOrder();
        final Long orderId = order.getOrderId();

        setupCheckIsCourierTrackingInfoAvailable(orderId, false);


        List<CourierTracking> payments = getCourierTracking(orderId);

        Assertions.assertTrue(payments.isEmpty());
    }

    @Test
    public void courierInfoIsAvailableButTrackingLinkReturnsError__expectException() {
        Order order = orderTestUtils.createOrder();
        final Long orderId = order.getOrderId();

        setupCheckIsCourierTrackingInfoAvailable(orderId, true);

        final RuntimeException trackingLinkError = new RuntimeException("unknown tracking link exception");
        setupTrackingLinkError(orderId, trackingLinkError);

        try {
            getCourierTracking(orderId);
            Assertions.fail("exception is expected");
        } catch (Exception e) {
            Assertions.assertEquals(trackingLinkError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void courierInfoIsAvailableButTrackingReturnsError__expectException() {
        Order order = orderTestUtils.createOrder();
        final Long orderId = order.getOrderId();

        setupCheckIsCourierTrackingInfoAvailable(orderId, true);

        TrackingLink trackingLink = new TrackingLink(
                "trackingId123",
                "http://example.com/trackingLink",
                orderId
        );
        setupGetTrackingLink(orderId, trackingLink);

        RuntimeException trackingError = new RuntimeException("unknown tracking error");
        setupTrackingError(trackingLink.getTrackingId(), trackingError);

        try {
            getCourierTracking(orderId);
            Assertions.fail("exception is expected");
        } catch (Exception e) {
            Assertions.assertEquals(trackingError.getMessage(), e.getMessage());
        }
    }

    public void setupCheckIsCourierTrackingInfoAvailable(Long orderId,
                                                         boolean courierInfoIsAvailable) {
        Mockito.when(courierTrackingAvailabilityDetector.isCourierTrackingAvailable(
                Mockito.eq(orderId)
        ))
                .thenReturn(courierInfoIsAvailable);
    }

    public void setupGetTrackingLink(Long orderId,
                                     TrackingLink trackingLink) {
        Mockito.when(marketTplClient.getTrackingLink(
                Mockito.eq(orderId)
        ))
                .thenReturn(Result.newResult(trackingLink));
    }

    public void setupTrackingLinkError(Long orderId,
                                       RuntimeException error) {
        Mockito.when(marketTplClient.getTrackingLink(
                Mockito.eq(orderId)
        ))
                .thenReturn(Result.newError(error));
    }

    public void setupTrackingError(String trackingId,
                                   RuntimeException error) {
        Mockito.when(marketTplClient.getTracking(
                Mockito.eq(trackingId)
        ))
                .thenReturn(Result.newError(error));
    }

    public void setupGetTracking(String trackingId,
                                 Tracking tracking) {
        Mockito.when(marketTplClient.getTracking(
                Mockito.eq(trackingId)
        ))
                .thenReturn(Result.newResult(tracking));
    }

    private List<CourierTracking> getCourierTracking(Object order) {
        Query query = Query.of(CourierTracking.FQN)
                .withFilters(Filters.eq(CourierTracking.ORDERS, order));
        return entityStorageService.list(query);
    }

    @Import({
            ModuleCourierTrackingConfiguration.class,
            ModuleOrderTestConfiguration.class,
            ModuleTicketTestConfiguration.class
    })
    public static class TestConfiguration {

        @Bean
        public CourierTrackingAvailabilityDetector courierTrackingAvailabilityDetector() {
            return Mockito.mock(CourierTrackingAvailabilityDetector.class);
        }

        @Bean
        public MarketTplClient marketTplClient() {
            return Mockito.mock(MarketTplClient.class);
        }
    }
}
