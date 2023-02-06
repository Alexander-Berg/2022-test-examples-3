package ru.yandex.travel.orders.services.support;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.support.UnsuccessfulHotelOrderNotification;
import ru.yandex.travel.orders.repository.OrderRepository;
import ru.yandex.travel.orders.repository.support.UnsuccessfulHotelOrderNotificationRepository;
import ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
public class HotelsSupportNotificationServiceUnsuccessfulOrdersTest {
    @Autowired
    private OrderRepository orders;
    @Autowired
    private WorkflowRepository workflows;
    @Autowired
    private UnsuccessfulHotelOrderNotificationRepository failureNotifications;
    @Autowired
    private PlatformTransactionManager txManager;
    @MockBean
    private JavaMailSenderImpl mailSender;
    @Autowired
    private HotelsSupportNotificationService hotelsSupportNotificationService;

    @Ignore
    @Test
    public void sendUnsuccessfulOrderGroupNotification() {
        List<UUID> orderIds = doInTx(() -> {
            clearNotifications();
            var o1 = createNotification("YA-ORDER-UON-1", "p1", "", "Hotel1", 10);
            var o2 = createNotification("YA-ORDER-UON-2", "p1", "", "Hotel2", 5);
            return List.of(o1.getId(), o2.getId());
        });

        Collection<List<UUID>> scheduledGroups = hotelsSupportNotificationService.findPendingUnsuccessfulHotelOrderNotifications();
        assertThat(new ArrayList<>(scheduledGroups)).isEqualTo(List.of(orderIds));

        List<UUID> group1 = scheduledGroups.iterator().next();
        hotelsSupportNotificationService.sendUnsuccessfulHotelOrderNotification(group1);

        ArgumentCaptor<SimpleMailMessage> emailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(emailCaptor.capture());
        SimpleMailMessage email = emailCaptor.getValue();
        assertThat(email.getTo()).contains("hotels-support@unit.test");
        assertThat(email.getSubject()).contains("YA-ORDER-UON-1").contains("YA-ORDER-UON-2");
        assertThat(email.getText())
                .contains("YA-ORDER-UON-1").contains("YA-ORDER-UON-2")
                .contains("Test Testov")
                .contains("71234567890")
                .contains("user1@unit.test")
                .contains("Hotel1", "Hotel2")
                .contains("Партнёр: Expedia");

        assertThat(hotelsSupportNotificationService.findPendingUnsuccessfulHotelOrderNotifications()).hasSize(0);
    }

    @Test
    @Transactional
    public void findMultipleAuthGroups() {
        clearNotifications();
        var o1 = createNotification("YA-ORDER-UON-2-1", "p1", "", "Hotel1", 5);
        var o2 = createNotification("YA-ORDER-UON-2-2", "p1", "", "Hotel1", 10);
        var o3 = createNotification("YA-ORDER-UON-2-3", "p2", "", "Hotel1", 5);
        var o4 = createNotification("YA-ORDER-UON-2-4", "", "s1", "Hotel1", 5);
        var o5 = createNotification("YA-ORDER-UON-2-5", "", "s2", "Hotel1", 5);

        Collection<List<UUID>> scheduledGroups = hotelsSupportNotificationService.findPendingUnsuccessfulHotelOrderNotifications();
        assertThat(scheduledGroups).hasSize(4)
                .contains(List.of(o2.getId(), o1.getId()))
                .contains(List.of(o3.getId()))
                .contains(List.of(o4.getId()))
                .contains(List.of(o5.getId()));
    }

    private <T> T doInTx(Supplier<T> action) {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(s -> action.get());
    }

    private void clearNotifications() {
        failureNotifications.deleteAll();
    }

    private HotelOrder createNotification(String prettyId, String passportId, String sessionKey, String hotel, int minuteAgo) {
        HotelOrder order = new HotelOrder();
        order.setId(UUID.randomUUID());
        order.setState(EHotelOrderState.OS_CONFIRMED);
        order.setPrettyId(prettyId);
        order.addOrderItem(HotelsSupportNotificationServiceTest.createOrderItem(hotel, null));
        order = orders.saveAndFlush(order);
        order.setCreatedAt(Instant.now().minus(Duration.ofMinutes(minuteAgo)));
        order = orders.save(order);
        workflows.save(Workflow.createWorkflowForEntity(order));
        UnsuccessfulHotelOrderNotification notification = new UnsuccessfulHotelOrderNotification();
        notification.setOrder(order);
        notification.setPassportId(passportId);
        notification.setSessionKey(sessionKey);
        failureNotifications.save(notification);
        return order;
    }
}
