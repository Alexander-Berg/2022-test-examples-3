package ru.yandex.market.db.monitoring;

import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.assertj.core.internal.RecursiveFieldByFieldComparator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.application.monitoring.ComplexMonitoring.Result;

import static org.assertj.core.internal.TypeComparators.defaultTypeComparators;
import static ru.yandex.market.application.monitoring.MonitoringStatus.CRITICAL;
import static ru.yandex.market.application.monitoring.MonitoringStatus.OK;
import static ru.yandex.market.application.monitoring.MonitoringStatus.WARNING;

public class DbMonitoringTest extends BaseDbMonitoringTest {
    @Resource
    protected DbMonitoringRepository dbMonitoringRepository;

    private DbMonitoring dbMonitoring;

    public static ListAssert<DbMonitoringUnit> assertThat(List<DbMonitoringUnit> list) {
        RecursiveFieldByFieldComparator recursiveFieldByFieldComparator =
                new RecursiveFieldByFieldComparator(Collections.emptyMap(), defaultTypeComparators());
        return Assertions.assertThat(list)
            .usingComparatorForElementFieldsWithNames(recursiveFieldByFieldComparator, "exception")
            .usingElementComparatorIgnoringFields("saveAction", "lastUpdateTs");
    }

    public static AbstractObjectAssert<?, DbMonitoringUnit> assertThat(DbMonitoringUnit unit) {
        RecursiveFieldByFieldComparator recursiveFieldByFieldComparator =
                new RecursiveFieldByFieldComparator(Collections.emptyMap(), defaultTypeComparators());
        return Assertions.assertThat(unit)
            .usingComparatorForFields(recursiveFieldByFieldComparator, "exception");
    }

    @Before
    public void setUp() {
        dbMonitoring = new DbMonitoring(dbMonitoringRepository);
    }

    @Test
    public void testCreateUnit() {
        DbMonitoringUnit unit1 = dbMonitoring.getOrCreateUnit("unit1");

        List<DbMonitoringUnit> all = dbMonitoringRepository.findAll();
        assertThat(all).isEmpty();
    }

    @Test
    public void testCreateUpdateUnit() {
        DbMonitoringUnit unit1 = dbMonitoring.getOrCreateUnit("unit1");

        List<DbMonitoringUnit> all = dbMonitoringRepository.findAll();
        assertThat(all).isEmpty();

        unit1.warning("My warning");

        all = dbMonitoringRepository.findAll();
        assertThat(all).containsExactly(unit1);

        unit1.critical("My critical", new IllegalArgumentException("My argument exception"));

        all = dbMonitoringRepository.findAll();
        assertThat(all).containsExactly(unit1);

        unit1.ok();

        all = dbMonitoringRepository.findAll();
        assertThat(all).containsExactly(unit1);
    }

    @Test
    public void testSeveralUnits() {
        DbMonitoringUnit unit1 = dbMonitoring.getOrCreateUnit("unit1");
        DbMonitoringUnit unit2 = dbMonitoring.getOrCreateUnit("unit2");
        DbMonitoringUnit unit3 = dbMonitoring.getOrCreateUnit("unit3");
        DbMonitoringUnit unit4 = dbMonitoring.getOrCreateUnit("unit4");

        List<DbMonitoringUnit> all = dbMonitoringRepository.findAll();
        assertThat(all).isEmpty();

        unit1.ok();
        unit2.warning("My warning");
        unit3.critical("My critical");

        all = dbMonitoringRepository.findAll();
        assertThat(all).containsExactlyInAnyOrder(unit1, unit2, unit3);
    }

    @Test
    public void testCriticalWithInnerException() {
        DbMonitoringUnit unit1 = dbMonitoring.getOrCreateUnit("unit1");

        try {
            Integer.parseInt("String");
        } catch (Exception e) {
            unit1.critical("Parse error", new RuntimeException("Parse exception", e));
        }

        List<DbMonitoringUnit> all = dbMonitoringRepository.findAll();
        assertThat(all).containsExactlyInAnyOrder(unit1);
    }

    @Test
    public void testGetOrCreateUnit() {
        DbMonitoringUnit unit1 = dbMonitoring.getOrCreateUnit("unit");
        DbMonitoringUnit unit2 = dbMonitoring.getOrCreateUnit("unit");

        Assert.assertEquals(unit1, unit2);
    }

    @Test
    public void testFindByName() {
        DbMonitoringUnit unit1 = dbMonitoring.getOrCreateUnit("unit1");
        unit1.ok();

        DbMonitoringUnit actualUnit1 = dbMonitoringRepository.findByName("unit1");
        DbMonitoringUnit actualUnit2 = dbMonitoringRepository.findByName("unit2");

        assertThat(actualUnit1).isEqualToIgnoringGivenFields(unit1, "saveAction", "lastUpdateTs");
        assertThat(actualUnit2).isNull();
    }

    @Test
    public void testGetResultByName() {
        DbMonitoringUnit unit1 = dbMonitoring.getOrCreateUnit("unit1");
        DbMonitoringUnit unit2 = dbMonitoring.getOrCreateUnit("unit2");
        DbMonitoringUnit unit3 = dbMonitoring.getOrCreateUnit("unit3");
        DbMonitoringUnit unit4 = dbMonitoring.getOrCreateUnit("unit4");

        unit2.ok();
        unit3.warning("My warn", new Throwable("My warn exception"));
        unit4.critical("My crit");

        Result result1 = dbMonitoring.getOrFetchResult("unit1");
        Result result2 = dbMonitoring.getOrFetchResult("unit2");
        Result result3 = dbMonitoring.getOrFetchResult("unit3");
        Result result4 = dbMonitoring.getOrFetchResult("unit4");
        Result result5 = dbMonitoring.getOrFetchResult("unit5");
        Assertions.assertThat(result1)
            .isEqualToComparingFieldByFieldRecursively(new Result(OK, "OK"));
        Assertions.assertThat(result2)
            .isEqualToComparingFieldByFieldRecursively(new Result(OK, "OK"));
        Assertions.assertThat(result3)
            .isEqualToComparingFieldByFieldRecursively(new Result(WARNING, "unit3: My warn"));
        Assertions.assertThat(result4)
            .isEqualToComparingFieldByFieldRecursively(new Result(CRITICAL, "unit4: My crit"));
        Assertions.assertThat(result5)
            .isEqualToComparingFieldByFieldRecursively(new Result(WARNING, "Unknown monitoring name: unit5"));
    }

    @Test
    public void testGetTotalResult() {
        DbMonitoringUnit unit1 = dbMonitoring.getOrCreateUnit("unit1");
        DbMonitoringUnit unit2 = dbMonitoring.getOrCreateUnit("unit2");
        DbMonitoringUnit unit3 = dbMonitoring.getOrCreateUnit("unit3");
        DbMonitoringUnit unit4 = dbMonitoring.getOrCreateUnit("unit4");

        unit2.ok();
        unit3.warning("My warn", new Throwable("My warn exception"));
        unit4.critical("My crit");

        Result result = dbMonitoring.fetchTotalResult();
        Assertions.assertThat(result)
            .isEqualToComparingFieldByFieldRecursively(new Result(CRITICAL,
                "CRIT {unit4: My crit} WARN {unit3: My warn}"));
    }

    /**
     * Тест моделирует работу, когда у нас несколько независимых DbMonitoring и все они синхронизируются через
     * базу данных.
     */
    @Test
    public void testWithDbMonitoringInDifferentInstances() {
        DbMonitoring dbMonitoring1 = new DbMonitoring(dbMonitoringRepository);
        DbMonitoring dbMonitoring2 = new DbMonitoring(dbMonitoringRepository);
        DbMonitoring dbMonitoring3 = new DbMonitoring(dbMonitoringRepository);

        DbMonitoringUnit unit11 = dbMonitoring1.getOrCreateUnit("unit11");
        DbMonitoringUnit unit12 = dbMonitoring1.getOrCreateUnit("unit12");
        DbMonitoringUnit unit21 = dbMonitoring2.getOrCreateUnit("unit21");
        DbMonitoringUnit unit22 = dbMonitoring2.getOrCreateUnit("unit22");
        DbMonitoringUnit unit31 = dbMonitoring3.getOrCreateUnit("unit31");

        unit11.critical("unit11");
        unit12.critical("unit12");
        unit21.critical("unit21");
        unit22.critical("unit22");
        unit31.critical("unit31");

        Result result0 = dbMonitoring.getOrFetchResult("unit21");
        Assertions.assertThat(result0)
            .isEqualToComparingFieldByFieldRecursively(new Result(CRITICAL, "unit21: unit21"));

        Result result = dbMonitoring.fetchTotalResult();
        Result result1 = dbMonitoring1.fetchTotalResult();
        Result result2 = dbMonitoring2.fetchTotalResult();
        Result result3 = dbMonitoring3.fetchTotalResult();
        Assertions.assertThat(result)
            .isEqualToComparingFieldByFieldRecursively(new Result(CRITICAL,
                "CRIT {unit11: unit11, unit12: unit12, unit21: unit21, unit22: unit22, unit31: unit31}"));
        Assertions.assertThat(result1)
            .isEqualToComparingFieldByFieldRecursively(new Result(CRITICAL,
                "CRIT {unit11: unit11, unit12: unit12, unit21: unit21, unit22: unit22, unit31: unit31}"));
        Assertions.assertThat(result2)
            .isEqualToComparingFieldByFieldRecursively(new Result(CRITICAL,
                "CRIT {unit11: unit11, unit12: unit12, unit21: unit21, unit22: unit22, unit31: unit31}"));
        Assertions.assertThat(result3)
            .isEqualToComparingFieldByFieldRecursively(new Result(CRITICAL,
                "CRIT {unit11: unit11, unit12: unit12, unit21: unit21, unit22: unit22, unit31: unit31}"));
    }
}
