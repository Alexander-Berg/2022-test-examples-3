package ru.yandex.market.sre.services.tms.eventdetector.service;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

import ru.yandex.market.sre.services.tms.eventdetector.dao.entity.ServiceIndicator;
import ru.yandex.market.sre.services.tms.eventdetector.model.core.Period;

public class EventDetectorTest {

    final TimeFormatterService timeFormatterService = new TimeFormatterService();

    @Test
    public void getPeriods() {
        ServiceIndicator indicator = new ServiceIndicator();
        DateTime dayAgo = DateTime.now().minusDays(1);
        List<Period> periods = new EventDetector(null, null, null, null, null, timeFormatterService, null)
                .getPeriods(indicator, dayAgo);
        periods.forEach(period -> System.out.println(timeFormatterService.format(period)));
    }

    @Test
    public void print() {
        System.out.println(timeFormatterService.format(new Period(1586784660L, 1586788260L)));
        System.out.println(timeFormatterService.format(new Period(1586788260L, 1586791860L)));
        System.out.println(timeFormatterService.format(new Period(1586788260L, 1586791860L)));
        System.out.println(timeFormatterService.format(new Period(1586791860L, 1586795460L)));
        System.out.println(timeFormatterService.format(new Period(1586791860L, 1586795460L)));
        System.out.println(timeFormatterService.format(new Period(1586795460L, 1586799060L)));
        System.out.println(timeFormatterService.format(new Period(1586799060L, 1586802660L)));
        System.out.println(timeFormatterService.format(new Period(1586802660L, 1586806260L)));
        System.out.println(timeFormatterService.format(new Period(1586806260L, 1586809860L)));
        System.out.println(timeFormatterService.format(new Period(1586809860L, 1586813460L)));
        System.out.println(timeFormatterService.format(new Period(1586813460L, 1586817060L)));
        System.out.println(timeFormatterService.format(new Period(1586817060L, 1586820660L)));
        System.out.println(timeFormatterService.format(new Period(1586820660L, 1586824260L)));
        System.out.println(timeFormatterService.format(new Period(1586824260L, 1586827860L)));
        System.out.println(timeFormatterService.format(new Period(1586795460L, 1586799060L)));
        System.out.println(timeFormatterService.format(new Period(1586827860L, 1586831460L)));
        System.out.println(timeFormatterService.format(new Period(1586831460L, 1586835060L)));
        System.out.println(timeFormatterService.format(new Period(1586799060L, 1586802660L)));
        System.out.println(timeFormatterService.format(new Period(1586835060L, 1586838660L)));
        System.out.println(timeFormatterService.format(new Period(1586802660L, 1586806260L)));
        System.out.println(timeFormatterService.format(new Period(1586838660L, 1586842260L)));
        System.out.println(timeFormatterService.format(new Period(1586806260L, 1586809860L)));
        System.out.println(timeFormatterService.format(new Period(1586842260L, 1586845860L)));
        System.out.println(timeFormatterService.format(new Period(1586809860L, 1586813460L)));
        System.out.println(timeFormatterService.format(new Period(1586845860L, 1586849460L)));
        System.out.println(timeFormatterService.format(new Period(1586813460L, 1586817060L)));
        System.out.println(timeFormatterService.format(new Period(1586849460L, 1586853060L)));
        System.out.println(timeFormatterService.format(new Period(1586817060L, 1586820660L)));
        System.out.println(timeFormatterService.format(new Period(1586820660L, 1586824260L)));
        System.out.println(timeFormatterService.format(new Period(1586853060L, 1586856660L)));
        System.out.println(timeFormatterService.format(new Period(1586824260L, 1586827860L)));
        System.out.println(timeFormatterService.format(new Period(1586856660L, 1586860260L)));
        System.out.println(timeFormatterService.format(new Period(1586827860L, 1586831460L)));
        System.out.println(timeFormatterService.format(new Period(1586860260L, 1586863860L)));
        System.out.println(timeFormatterService.format(new Period(1586831460L, 1586835060L)));
        System.out.println(timeFormatterService.format(new Period(1586835060L, 1586838660L)));
        System.out.println(timeFormatterService.format(new Period(1586863860L, 1586867460L)));
        System.out.println(timeFormatterService.format(new Period(1586838660L, 1586842260L)));
        System.out.println(timeFormatterService.format(new Period(1586867460L, 1586867760L)));
        System.out.println(timeFormatterService.format(new Period(1586842260L, 1586845860L)));
        System.out.println(timeFormatterService.format(new Period(1586867760L, 1586868060L)));
        System.out.println(timeFormatterService.format(new Period(1586845860L, 1586849460L)));
        System.out.println(timeFormatterService.format(new Period(1586868060L, 1586868360L)));
        System.out.println(timeFormatterService.format(new Period(1586868360L, 1586868660L)));
        System.out.println(timeFormatterService.format(new Period(1586849460L, 1586853060L)));
        System.out.println(timeFormatterService.format(new Period(1586868660L, 1586868960L)));
        System.out.println(timeFormatterService.format(new Period(1586853060L, 1586856660L)));
        System.out.println(timeFormatterService.format(new Period(1586868960L, 1586869260L)));
        System.out.println(timeFormatterService.format(new Period(1586856660L, 1586860260L)));
        System.out.println(timeFormatterService.format(new Period(1586869260L, 1586869560L)));
        System.out.println(timeFormatterService.format(new Period(1586860260L, 1586863860L)));
        System.out.println(timeFormatterService.format(new Period(1586869560L, 1586869860L)));
        System.out.println(timeFormatterService.format(new Period(1586863860L, 1586867460L)));
        System.out.println(timeFormatterService.format(new Period(1586869860L, 1586870160L)));
        System.out.println(timeFormatterService.format(new Period(1586870160L, 1586870460L)));
        System.out.println(timeFormatterService.format(new Period(1586867460L, 1586867760L)));
        System.out.println(timeFormatterService.format(new Period(1586867760L, 1586868060L)));
        System.out.println(timeFormatterService.format(new Period(1586868060L, 1586868360L)));
        System.out.println(timeFormatterService.format(new Period(1586868360L, 1586868660L)));
        System.out.println(timeFormatterService.format(new Period(1586868660L, 1586868960L)));
        System.out.println(timeFormatterService.format(new Period(1586868960L, 1586869260L)));
        System.out.println(timeFormatterService.format(new Period(1586869260L, 1586869560L)));
        System.out.println(timeFormatterService.format(new Period(1586869560L, 1586869860L)));
        System.out.println(timeFormatterService.format(new Period(1586869860L, 1586870160L)));
        System.out.println(timeFormatterService.format(new Period(1586870160L, 1586870460L)));
    }
}
