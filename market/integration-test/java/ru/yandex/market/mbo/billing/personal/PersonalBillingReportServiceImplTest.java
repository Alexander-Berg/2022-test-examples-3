package ru.yandex.market.mbo.billing.personal;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.mbo.billing.report.personal.StatsPayments;
import ru.yandex.market.mbo.billing.report.personal.PersonalBilling;
import ru.yandex.market.mbo.billing.report.personal.PersonalBillingFilter;
import ru.yandex.market.mbo.billing.report.personal.PersonalBillingReportServiceImpl;
import ru.yandex.market.mbo.configs.TestConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.market.mbo.billing.report.personal.PersonalBillingFilter.Grouping.BY_TASK;
import static ru.yandex.market.mbo.billing.report.personal.PersonalBillingFilter.Grouping.NONE;
import static ru.yandex.market.mbo.billing.report.personal.PersonalBillingFilter.OrderedColumn.FINISHED_TIME;

@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class})
public class PersonalBillingReportServiceImplTest {

    @Autowired
    private PersonalBillingReportServiceImpl service;

    @Test
    public void findByFilterGrouping() {
        for (PersonalBillingFilter.Grouping grouping : PersonalBillingFilter.Grouping.values()) {
            PersonalBilling personalBilling = findByFilter(grouping, FINISHED_TIME);
            assertEquals(Long.valueOf(546502620), personalBilling.getUserId());
            assertEquals(LocalDate.of(2020, 12, 1), personalBilling.getFinishedTime());
        }
    }

    @Test
    public void findByFilterOrdered() {
        for (PersonalBillingFilter.OrderedColumn column : PersonalBillingFilter.OrderedColumn.values()) {
            PersonalBilling personalBilling = findByFilter(NONE, column);
            assertEquals(Long.valueOf(546502620), personalBilling.getUserId());
        }
    }

    @Test
    public void findByFilterWithoutCategory() {
        final PersonalBillingFilter filter = getPersonalBillingFilter(NONE, null)
            .setCategoryId(-1L);
        final List<PersonalBilling> byFilters = service.findByFilters(filter);
        byFilters.forEach(personalBilling ->
            assertTrue(personalBilling.getGuruCategoryId().equals(-1L)
                || personalBilling.getGuruCategoryId().equals(0L)));
    }

    private PersonalBilling findByFilter(PersonalBillingFilter.Grouping grouping,
                                         PersonalBillingFilter.OrderedColumn column) {
        PersonalBillingFilter filter = getPersonalBillingFilter(grouping, column);
        List<PersonalBilling> byFilters = service.findByFilters(filter);
        assertEquals(1, byFilters.size());
        return byFilters.get(0);
    }

    private PersonalBillingFilter getPersonalBillingFilter(PersonalBillingFilter.Grouping grouping,
                                                           PersonalBillingFilter.OrderedColumn column) {
        return new PersonalBillingFilter()
            .setOperatorId(546502620L)
            .setFromDate(LocalDate.of(2020, 12, 1))
            .setToDate(LocalDate.of(2020, 12, 23))
            .setGrouping(grouping)
            .setOrderedColumn(column)
            .setLimit(1);
    }

    @Test
    public void countByFilter() {
        PersonalBillingFilter filter = new PersonalBillingFilter()
            .setOperatorId(546502620L)
            .setFromDate(LocalDate.of(2020, 12, 1))
            .setToDate(LocalDate.of(2020, 12, 23))
            .setGrouping(BY_TASK);
        int count = service.countByFilter(filter);
        assertTrue(count > 0); // don't use exact number
    }

    @Test
    public void statsForPayments() {
        StatsPayments statsPayments = service.statsForPayments(546502620L);
        assertNotNull(statsPayments);
    }

    @Test
    public void calcPaymentByFilter() {
        PersonalBillingFilter filter = new PersonalBillingFilter()
            .setOperatorId(546502620L)
            .setFromDate(LocalDate.of(2020, 12, 1))
            .setToDate(LocalDate.of(2020, 12, 23))
            .setGrouping(BY_TASK);
        double payment = service.calcPaymentByFilter(filter);
        assertNotEquals(0, payment, 0.001);
    }

    @Test
    public void calcPaymentByFilterZero() {
        PersonalBillingFilter filter = new PersonalBillingFilter()
            .setOperatorId(54652620L)
            .setFromDate(LocalDate.of(2019, 12, 1))
            .setToDate(LocalDate.of(2019, 12, 23))
            .setGrouping(BY_TASK);
        double payment = service.calcPaymentByFilter(filter);
        System.out.println(payment);
        assertEquals(0, payment, 0.001);
    }
}
