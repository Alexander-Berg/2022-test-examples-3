package ru.yandex.travel.orders.repository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.orders.entities.PaymentSchedule;
import ru.yandex.travel.orders.entities.PaymentScheduleItem;
import ru.yandex.travel.orders.entities.PendingInvoice;
import ru.yandex.travel.orders.workflow.payments.proto.EPaymentState;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class PaymentScheduleRepositoryTest {
    @Autowired
    private PaymentScheduleRepository scheduleRepository;

    @Test
    public void testFindExpiring() {
        Instant now = Instant.now();
        Instant future = now.plusSeconds(1);
        UUID id = UUID.randomUUID();
        PaymentSchedule schedule = PaymentSchedule.builder()
                .id(id)
                .expired(false)
                .state(EPaymentState.PS_PARTIALLY_PAID)
                .build();
        schedule.setItems(List.of(
                PaymentScheduleItem.builder()
                        .id(UUID.randomUUID())
                        .schedule(schedule)
                        .paymentEndsAt(now)
                        .pendingInvoice(PendingInvoice.builder()
                                .id(UUID.randomUUID())
                                .state(EPaymentState.PS_INVOICE_PENDING)
                                .build())
                        .build()
        ));
        scheduleRepository.saveAndFlush(schedule);


        var res = scheduleRepository.findNonExpiredSchedulesInGivenStatesExpiringBeforeInstant(
                Set.of(EPaymentState.PS_PARTIALLY_PAID),
                Set.of(EPaymentState.PS_INVOICE_PENDING), future, Collections.emptySet(), Pageable.unpaged());
        assertThat(res).isNotEmpty();
        var count =
                scheduleRepository.countNonExpiredSchedulesInGivenStatesExpiringBeforeInstant(Set.of(EPaymentState.PS_PARTIALLY_PAID),
                        Set.of(EPaymentState.PS_INVOICE_PENDING), future, Collections.emptySet());
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testDoNotFindExpired() {
        Instant now = Instant.now();
        Instant future = now.plusSeconds(1);
        UUID id = UUID.randomUUID();
        PaymentSchedule schedule = PaymentSchedule.builder()
                .id(id)
                .expired(true)
                .state(EPaymentState.PS_PARTIALLY_PAID)
                .build();
        schedule.setItems(List.of(
                PaymentScheduleItem.builder()
                        .id(UUID.randomUUID())
                        .schedule(schedule)
                        .paymentEndsAt(now)
                        .pendingInvoice(PendingInvoice.builder()
                                .id(UUID.randomUUID())
                                .state(EPaymentState.PS_INVOICE_PENDING)
                                .build())
                        .build()
        ));
        scheduleRepository.saveAndFlush(schedule);


        var res = scheduleRepository.findNonExpiredSchedulesInGivenStatesExpiringBeforeInstant(
                Set.of(EPaymentState.PS_PARTIALLY_PAID),
                Set.of(EPaymentState.PS_INVOICE_PENDING), future, Collections.emptySet(), Pageable.unpaged());
        assertThat(res).isEmpty();
        var count =
                scheduleRepository.countNonExpiredSchedulesInGivenStatesExpiringBeforeInstant(Set.of(EPaymentState.PS_PARTIALLY_PAID),
                        Set.of(EPaymentState.PS_INVOICE_PENDING), future, Collections.emptySet());
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void testDoNotFindDrafts() {
        Instant now = Instant.now();
        Instant future = now.plusSeconds(1);
        UUID id = UUID.randomUUID();
        PaymentSchedule schedule = PaymentSchedule.builder()
                .id(id)
                .state(EPaymentState.PS_FULLY_PAID)
                .build();
        schedule.setItems(List.of(
                PaymentScheduleItem.builder()
                        .id(UUID.randomUUID())
                        .schedule(schedule)
                        .paymentEndsAt(now)
                        .pendingInvoice(PendingInvoice.builder()
                                .id(UUID.randomUUID())
                                .state(EPaymentState.PS_DRAFT)
                                .build())
                        .build()
        ));
        scheduleRepository.saveAndFlush(schedule);


        var res = scheduleRepository.findNonExpiredSchedulesInGivenStatesExpiringBeforeInstant(
                Set.of(EPaymentState.PS_PARTIALLY_PAID),
                Set.of(EPaymentState.PS_INVOICE_PENDING), future, Collections.emptySet(), Pageable.unpaged());
        assertThat(res).isEmpty();
        var count = scheduleRepository.countNonExpiredSchedulesInGivenStatesExpiringBeforeInstant(
                Set.of(EPaymentState.PS_PARTIALLY_PAID),
                Set.of(EPaymentState.PS_INVOICE_PENDING), future, Collections.emptySet());
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void testDoNotFindNonExpired() {
        Instant now = Instant.now();
        Instant future = now.plusSeconds(1);
        UUID id = UUID.randomUUID();
        PaymentSchedule schedule = PaymentSchedule.builder()
                .id(id)
                .state(EPaymentState.PS_PARTIALLY_PAID)
                .build();
        schedule.setItems(List.of(
                PaymentScheduleItem.builder()
                        .id(UUID.randomUUID())
                        .schedule(schedule)
                        .paymentEndsAt(future)
                        .pendingInvoice(PendingInvoice.builder()
                                .id(UUID.randomUUID())
                                .state(EPaymentState.PS_INVOICE_PENDING)
                                .build())
                        .build()
        ));
        scheduleRepository.saveAndFlush(schedule);


        var res = scheduleRepository.findNonExpiredSchedulesInGivenStatesExpiringBeforeInstant(
                Set.of(EPaymentState.PS_PARTIALLY_PAID),
                Set.of(EPaymentState.PS_INVOICE_PENDING), now, Collections.emptySet(), Pageable.unpaged());
        assertThat(res).isEmpty();
        var count = scheduleRepository.countNonExpiredSchedulesInGivenStatesExpiringBeforeInstant(
                Set.of(EPaymentState.PS_PARTIALLY_PAID),
                Set.of(EPaymentState.PS_INVOICE_PENDING), now, Collections.emptySet());
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void testDoNotFindActive() {
        Instant now = Instant.now();
        Instant future = now.plusSeconds(1);
        UUID id = UUID.randomUUID();
        PaymentSchedule schedule = PaymentSchedule.builder()
                .id(id)
                .state(EPaymentState.PS_PARTIALLY_PAID)
                .build();
        schedule.setItems(List.of(
                PaymentScheduleItem.builder()
                        .id(UUID.randomUUID())
                        .schedule(schedule)
                        .paymentEndsAt(now)
                        .pendingInvoice(PendingInvoice.builder()
                                .id(UUID.randomUUID())
                                .state(EPaymentState.PS_INVOICE_PENDING)
                                .build())
                        .build()
        ));
        scheduleRepository.saveAndFlush(schedule);


        var res = scheduleRepository.findNonExpiredSchedulesInGivenStatesExpiringBeforeInstant(
                Set.of(EPaymentState.PS_PARTIALLY_PAID),
                Set.of(EPaymentState.PS_INVOICE_PENDING), future, Set.of(id), Pageable.unpaged());
        assertThat(res).isEmpty();
        var count = scheduleRepository.countNonExpiredSchedulesInGivenStatesExpiringBeforeInstant(
                Set.of(EPaymentState.PS_PARTIALLY_PAID),
                Set.of(EPaymentState.PS_INVOICE_PENDING), future, Set.of(id));
        assertThat(count).isEqualTo(0);
    }
}
