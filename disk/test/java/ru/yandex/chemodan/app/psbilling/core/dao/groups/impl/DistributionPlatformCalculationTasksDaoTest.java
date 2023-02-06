package ru.yandex.chemodan.app.psbilling.core.dao.groups.impl;

import java.util.UUID;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.DistributionPlatformCalculationDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.CalculationStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.DistributionServiceTransactionCalculation;
import ru.yandex.misc.test.Assert;

public class DistributionPlatformCalculationTasksDaoTest extends AbstractPsBillingCoreTest {
    @Autowired
    private DistributionPlatformCalculationDao dao;
    private final UUID taskId0 = UUID.randomUUID();
    private final UUID taskId1 = UUID.randomUUID();
    private final UUID taskId2 = UUID.randomUUID();
    private final UUID taskId3 = UUID.randomUUID();
    private final UUID taskId4 = UUID.randomUUID();
    private final UUID taskId5 = UUID.randomUUID();


    @Test

    public void saveBazingaJobId() {
        DistributionServiceTransactionCalculation calculation = dao.initCalculation(LocalDate.now(), 1L, taskId1,
                "jobId");
        Assert.equals(taskId1, calculation.getTaskId());
        Assert.equals(LocalDate.now().withDayOfMonth(1), calculation.getBillingDate());
        Assert.equals(CalculationStatus.STARTING, calculation.getStatus());
        Assert.equals(1L, calculation.getClientId());
        Assert.equals("jobId", calculation.getBazingaJobId());
        Assert.notNull(calculation.getUpdatedAt());
    }

    @Test
    public void constraints() {
        dao.initCalculation(LocalDate.now(), 1L, taskId1, null);
        Assert.assertThrows(() -> dao.initCalculation(LocalDate.now(), 1L, taskId1, null), Exception.class);
    }

    @Test
    public void lock() {
        dao.initCalculation(LocalDate.now(), 1L, taskId1, null);
        dao.initCalculation(LocalDate.now(), 2L, taskId2, null);
        dao.initCalculation(LocalDate.now().minusMonths(3), 3L, taskId3, null);
        ListF<DistributionServiceTransactionCalculation> transactions = dao.lock(LocalDate.now());
        Assert.equals(2, transactions.length());
        Assert.notEmpty(transactions.find(x -> x.getTaskId().equals(taskId1)));
        Assert.notEmpty(transactions.find(x -> x.getTaskId().equals(taskId2)));

        dao.setCalculationObsolete(LocalDate.now());
        transactions = dao.lock(LocalDate.now());
        Assert.equals(0, transactions.length());
    }

    @Test
    public void updateStatus() {
        dao.initCalculation(LocalDate.now(), 1L, taskId1, null);
        dao.initCalculation(LocalDate.now(), 1L, taskId2, null);
        boolean updated = dao.updateStatus(taskId2, CalculationStatus.STARTING, CalculationStatus.STARTED);
        Assert.isTrue(updated);
        assertStatus(taskId1, CalculationStatus.STARTING);
        assertStatus(taskId2, CalculationStatus.STARTED);
    }

    @Test
    public void setCalculationObsolete() {
        dao.initCalculation(LocalDate.now(), 1L, taskId0, null);
        dao.initCalculation(LocalDate.now(), 1L, taskId1, null);
        dao.initCalculation(LocalDate.now(), 1L, taskId2, null);
        dao.initCalculation(LocalDate.now(), 1L, taskId3, null);
        dao.initCalculation(LocalDate.now(), 1L, taskId4, null);
        dao.initCalculation(LocalDate.now().minusMonths(1), 1L, taskId5, null);

        dao.updateStatus(taskId1, CalculationStatus.STARTING, CalculationStatus.STARTED);
        dao.updateStatus(taskId2, CalculationStatus.STARTING, CalculationStatus.COMPLETED);
        dao.updateStatus(taskId3, CalculationStatus.STARTING, CalculationStatus.DUMPING);
        dao.updateStatus(taskId4, CalculationStatus.STARTING, CalculationStatus.OBSOLETE);

        dao.setCalculationObsolete(LocalDate.now());
        assertStatus(taskId0, CalculationStatus.OBSOLETE);
        assertStatus(taskId1, CalculationStatus.OBSOLETE);
        assertStatus(taskId2, CalculationStatus.OBSOLETE);
        assertStatus(taskId3, CalculationStatus.DUMPING);
        assertStatus(taskId4, CalculationStatus.OBSOLETE);
        assertStatus(taskId5, CalculationStatus.STARTING);
    }

    private void assertStatus(UUID taskId, CalculationStatus status) {
        DistributionServiceTransactionCalculation job = dao.find(taskId);
        Assert.equals(status, job.getStatus());
    }
}
