package ru.yandex.market.logistics.management.util;

import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.domain.entity.Calendar;
import ru.yandex.market.logistics.management.domain.entity.DeliveryInterval;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.repository.CalendarRepository;
import ru.yandex.market.logistics.management.repository.PartnerRepository;

@TestComponent
@AllArgsConstructor
public class TransactionalUtils {

    private final PartnerRepository repository;
    private final CalendarRepository calendarRepository;

    @Transactional
    public List<Partner> loadFullPartners() {
        return repository.findAll().stream()
            .map(this::initialize)
            .collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Partner loadFullPartner(Long partnerId) {
        return repository.findById(partnerId)
            .map(this::initialize)
            .orElse(null);
    }

    private Partner initialize(Partner partner) {
        Hibernate.initialize(partner.getDeliveryIntervals());
        partner.getDeliveryIntervals().forEach(this::initialize);
        Hibernate.initialize(partner.getPartnerRoutes());
        Hibernate.initialize(partner.getShops());
        Hibernate.initialize(partner.getExternalParamValues());
        Hibernate.initialize(partner.getForbiddenCargoTypes());
        Hibernate.initialize(partner.getCourierSchedule());
        Hibernate.initialize(partner.getIntakeSchedule());
        return partner;
    }

    private void initialize(DeliveryInterval interval) {
        Hibernate.initialize(interval.getSchedule());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Partner saveInTransaction(Partner partner) {
        return repository.saveAndFlush(partner);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveInTransaction(Calendar calendar) {
        calendarRepository.saveAndFlush(calendar);
    }

    public static void startTransaction() {
        TestTransaction.start();
    }

    public static void commit() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }

    public static void rollback() {
        TestTransaction.flagForRollback();
        TestTransaction.end();
    }

    public static void doWithinTransaction(Runnable action) {
        startTransaction();
        try {
            action.run();
        } catch (Throwable e) {
            rollback();
            throw e;
        }
        commit();
    }

    public static void doWithinRollbackTransaction(Runnable action) {
        startTransaction();
        try {
            action.run();
        } finally {
            rollback();
        }
    }
}
