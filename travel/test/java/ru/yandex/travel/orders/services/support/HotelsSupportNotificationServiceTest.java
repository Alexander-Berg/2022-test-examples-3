package ru.yandex.travel.orders.services.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.travel.hotels.common.orders.BNovoHotelItinerary;
import ru.yandex.travel.hotels.common.orders.ConfirmationInfo;
import ru.yandex.travel.hotels.common.orders.ExpediaHotelItinerary;
import ru.yandex.travel.hotels.common.orders.Guest;
import ru.yandex.travel.hotels.common.orders.HotelItinerary;
import ru.yandex.travel.hotels.common.orders.OrderDetails;
import ru.yandex.travel.hotels.common.orders.TravellineHotelItinerary;
import ru.yandex.travel.orders.commons.proto.EServiceType;
import ru.yandex.travel.orders.entities.BNovoOrderItem;
import ru.yandex.travel.orders.entities.ExpediaOrderItem;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.HotelOrderItem;
import ru.yandex.travel.orders.entities.TravellineOrderItem;
import ru.yandex.travel.orders.entities.support.SuccessfulHotelOrderNotification;
import ru.yandex.travel.orders.repository.OrderRepository;
import ru.yandex.travel.orders.repository.support.SuccessfulHotelOrderNotificationRepository;
import ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static ru.yandex.travel.orders.commons.proto.EServiceType.PT_EXPEDIA_HOTEL;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class HotelsSupportNotificationServiceTest {
    @Autowired
    private OrderRepository orders;
    @Autowired
    private SuccessfulHotelOrderNotificationRepository successNotifications;
    @Autowired
    private PlatformTransactionManager txManager;
    @MockBean
    private JavaMailSenderImpl mailSender;
    @Autowired
    private HotelsSupportNotificationService hotelsSupportNotificationService;

    @Before
    public void setUp() {
        doInTx(() -> {
            successNotifications.deleteAll();
            orders.deleteAll();
            return null;
        });
    }

    @Test
    public void testCopyNewConfirmedOrders() {
        var since = Instant.parse("2007-12-03T11:15:30.00Z");
        var maxCheckinDate = LocalDate.now().plusDays(30);
        var maxOrdersForHotel = 4;

        List<HotelOrder> hotelOrders = doInTx(() -> List.of(
            // order#1 - not ok by creation date
            createOrder("2007-12-03T10:15:30.00Z", EHotelOrderState.OS_CONFIRMED, null, null),
            // order#2 - ok
            createOrder("2007-12-03T12:15:30.00Z", EHotelOrderState.OS_CONFIRMED, null, null),
            // order#3 - ok
            createOrder("2007-12-03T13:15:30.00Z", EHotelOrderState.OS_CONFIRMED, null, null),
            // order#4 - not ot by state
            createOrder("2007-12-03T13:15:30.00Z", EHotelOrderState.OS_WAITING_CONFIRMATION, null, null),
            // order#5 - not ok by check in date
            createOrder("2007-12-03T13:15:30.00Z", EHotelOrderState.OS_CONFIRMED, maxCheckinDate.plusDays(2), null)
        ));

        // first call
        var copied = hotelsSupportNotificationService.scheduleNewConfirmedOrders(
                since, maxCheckinDate, maxOrdersForHotel);
        assertThat(copied.size()).isEqualTo(2);
        checkOrderNotificationsExist(hotelOrders, List.of(false, true, true, false, false));

        // second call - no new orders copied
        copied = hotelsSupportNotificationService.scheduleNewConfirmedOrders(
                since, maxCheckinDate, maxOrdersForHotel);
        assertThat(copied.size()).isEqualTo(0);
        checkOrderNotificationsExist(hotelOrders, List.of(false, true, true, false, false));

        // change "since" -> order#1 becomes ok
        copied = hotelsSupportNotificationService.scheduleNewConfirmedOrders(
            since.minus(1, ChronoUnit.HOURS), maxCheckinDate, maxOrdersForHotel);
        assertThat(copied.size()).isEqualTo(1);
        checkOrderNotificationsExist(hotelOrders, List.of(true, true, true, false, false));

        // change check in date requirements -> order#5 becomes ok
        copied = hotelsSupportNotificationService.scheduleNewConfirmedOrders(
                since, maxCheckinDate.plusDays(4), maxOrdersForHotel);
        assertThat(copied.size()).isEqualTo(1);
        checkOrderNotificationsExist(hotelOrders, List.of(true, true, true, false, true));

        // order#4 becomes CONFIRMED, but it is still not ok because maxOrdersForHotel is already reached
        doInTx(() -> {
            var order = (HotelOrder) orders.findById(hotelOrders.get(3).getId()).get();
            order.setState(EHotelOrderState.OS_CONFIRMED);
            orders.save(order);
            return order;
        });
        copied = hotelsSupportNotificationService.scheduleNewConfirmedOrders(
                since, maxCheckinDate, maxOrdersForHotel);
        assertThat(copied.size()).isEqualTo(0);
        checkOrderNotificationsExist(hotelOrders, List.of(true, true, true, false, true));

        // changing permalink for order#4 ->
        // it is no longer subject to maxOrdersForHotel condition, so it becomes ok
        doInTx(() -> {
            var order = (HotelOrder) orders.findById(hotelOrders.get(3).getId()).get();
            var orderItem = (HotelOrderItem) order.getOrderItems().get(0);
            orderItem.getHotelItinerary().getOrderDetails().setPermalink(123123L);
            orders.save(order);
            return order;
        });
        copied = hotelsSupportNotificationService.scheduleNewConfirmedOrders(
                since, maxCheckinDate, maxOrdersForHotel);
        assertThat(copied.size()).isEqualTo(1);
        checkOrderNotificationsExist(hotelOrders, List.of(true, true, true, true, true));
    }

    @Test
    public void testCopyNewConfirmedOrdersOrdersLimitNotExceeded() {
        var since = Instant.parse("2007-12-03T11:15:30.00Z");
        var maxCheckinDate = LocalDate.now().plusDays(30);
        var maxOrdersForHotel = 2;

        Set<HotelOrder> hotelOrders = doInTx(() -> Set.of(
                createOrder("2007-12-03T12:15:30.00Z", EHotelOrderState.OS_CONFIRMED, null, null),
                createOrder("2007-12-03T13:15:30.00Z", EHotelOrderState.OS_CONFIRMED, null, null),
                createOrder("2007-12-03T14:15:30.00Z", EHotelOrderState.OS_CONFIRMED, null, null)
        ));

        var copied = hotelsSupportNotificationService.scheduleNewConfirmedOrders(
                since, maxCheckinDate, maxOrdersForHotel);
        assertThat(copied.size()).isEqualTo(2);
    }

    private void checkOrderNotificationsExist(List<HotelOrder> hotelOrders, List<Boolean> expected) {
        assertThat(hotelOrders.size()).isEqualTo(expected.size());
        for(int i = 0; i < hotelOrders.size(); i++) {
            var expression = assertThat(
                successNotifications.findById(hotelOrders.get(i).getId()).isPresent()).as(String.format("Order %s", i));

            if (expected.get(i)) {
                expression.isTrue();
            } else {
                expression.isFalse();
            }
        }
    }

    @Test
    public void successfulOrderNotificationFlow() {
        Set<UUID> orderIds = doInTx(() -> {
            clearNotifications();
            var o1 = createOrderWithNotification("YA-ORDER-1");
            var o2 = createOrderWithNotification("YA-ORDER-2");
            return Set.of(o1.getId(), o2.getId());
        });

        Set<UUID> schedulerIds = hotelsSupportNotificationService.findPendingSuccessfulHotelOrderNotifications();
        assertThat(schedulerIds).isEqualTo(orderIds);

        UUID id1 = schedulerIds.iterator().next();
        hotelsSupportNotificationService.sendSuccessfulHotelOrderNotification(id1);

        ArgumentCaptor<SimpleMailMessage> emailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(emailCaptor.capture());
        SimpleMailMessage email = emailCaptor.getValue();
        assertThat(email.getTo()).contains("hotels-support@unit.test");
        assertThat(email.getSubject()).contains("YA-ORDER-");
        assertThat(email.getText())
                .contains("Test Testov")
                .contains("71234567890")
                .contains("user1@unit.test")
                .contains("Confirmation-Id")
                .contains("Test Hotel")
                .contains("Партнёр: Expedia");

        schedulerIds = hotelsSupportNotificationService.findPendingSuccessfulHotelOrderNotifications();
        assertThat(schedulerIds).hasSize(1);
        assertThat(schedulerIds).doesNotContain(id1);
    }

    @Test
    public void successfulOrderNotificationRetries() {
        doThrow(new MailSendException("send mail error"))
                .doNothing() // then send successfully
                .when(mailSender).send((SimpleMailMessage) any());

        doInTx(() -> {
            clearNotifications();
            return createOrderWithNotification("YA-ORDER-2-1").getId();
        });

        Set<UUID> schedulerIds = hotelsSupportNotificationService.findPendingSuccessfulHotelOrderNotifications();
        assertThat(schedulerIds).hasSize(1);
        UUID id = schedulerIds.iterator().next();

        assertThatThrownBy(() -> hotelsSupportNotificationService.sendSuccessfulHotelOrderNotification(id))
                .isExactlyInstanceOf(MailSendException.class);
        assertThat(hotelsSupportNotificationService.findPendingSuccessfulHotelOrderNotifications())
                .hasSize(1);

        hotelsSupportNotificationService.sendSuccessfulHotelOrderNotification(id);
        assertThat(hotelsSupportNotificationService.findPendingSuccessfulHotelOrderNotifications())
                .hasSize(0);
    }

    private <T> T doInTx(Supplier<T> action) {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(s -> action.get());
    }

    private void clearNotifications() {
        successNotifications.deleteAll();
    }

    private HotelOrder createOrder(String date, EHotelOrderState state, LocalDate checkinDate, Long permalink) {
        return createOrder(date, state, PT_EXPEDIA_HOTEL, "", "", checkinDate, permalink);
    }

    private HotelOrder createOrder(EHotelOrderState state, String hotelName, String confirmId) {
        return createOrder("2007-12-03T10:15:30.00Z", state, PT_EXPEDIA_HOTEL, hotelName, confirmId, null, null);
    }

    private HotelOrder createOrder(
        String date, EHotelOrderState state,
        EServiceType orderItemType, String hotelName, String confirmId, LocalDate checkinDate, Long permalink
    ) {
        HotelOrder order = new HotelOrder();
        order.setId(UUID.randomUUID());
        order.setState(state);
        order.addOrderItem(createOrderItem(hotelName, confirmId, orderItemType, checkinDate, permalink));
        orders.saveAndFlush(order);

        // override default createdAt
        order.setCreatedAt(Instant.parse(date));
        orders.saveAndFlush(order);
        return order;
    }

    private HotelOrder createOrderWithNotification(String prettyId) {
        HotelOrder order = createOrder(
            EHotelOrderState.OS_CONFIRMED, "Test Hotel", "Confirmation-Id"
        );
        order.setPrettyId(prettyId);
        orders.saveAndFlush(order);

        SuccessfulHotelOrderNotification notification = new SuccessfulHotelOrderNotification();
        notification.setOrder(order);
        successNotifications.save(notification);

        return order;
    }

    static HotelOrderItem createOrderItem(String hotel, String confirmationId) {
        return createOrderItem(hotel, confirmationId, PT_EXPEDIA_HOTEL, null, null);
    }

    static HotelOrderItem createOrderItem(
            String hotel, String confirmationId,
            EServiceType orderItemType, LocalDate checkinDate, Long permalink
    ) {
        if (checkinDate == null) {
            checkinDate = LocalDate.now().plusDays(1);
        }
        if (permalink == null) {
            permalink = 424242L;
        }

        HotelOrderItem orderItem;
        Guest guest = new Guest();
        guest.setFirstName("Test");
        guest.setLastName("Testov");

        HotelItinerary itinerary;
        switch (orderItemType) {
            case PT_EXPEDIA_HOTEL:
                orderItem = new ExpediaOrderItem();
                itinerary = new ExpediaHotelItinerary();
                ((ExpediaOrderItem) orderItem).setItinerary((ExpediaHotelItinerary) itinerary);
                break;
            case PT_TRAVELLINE_HOTEL:
                orderItem = new TravellineOrderItem();
                itinerary = new TravellineHotelItinerary();
                ((TravellineOrderItem) orderItem).setItinerary((TravellineHotelItinerary) itinerary);
                break;
            case PT_BNOVO_HOTEL:
                orderItem = new BNovoOrderItem();
                itinerary = new BNovoHotelItinerary();
                ((BNovoOrderItem) orderItem).setItinerary((BNovoHotelItinerary) itinerary);
                break;
            default:
                throw new RuntimeException("Unsupported order item type: " + orderItemType);
        }

        itinerary.setGuests(List.of(guest));
        itinerary.setCustomerEmail("user1@unit.test");
        itinerary.setCustomerPhone("71234567890");
        itinerary.setOrderDetails(OrderDetails.builder()
                .hotelName(hotel)
                .checkinDate(checkinDate)
                .checkoutDate(checkinDate.plusDays(3))
                .hotelPhone("12345678")
                .ratePlanDetails("специальная инструкция")
                .permalink(permalink)
                .build());
        if (confirmationId != null) {
            ConfirmationInfo confirmation = new ConfirmationInfo();
            confirmation.setHotelConfirmationId(confirmationId);
            itinerary.setConfirmation(confirmation);
        }
        return orderItem;
    }
}
