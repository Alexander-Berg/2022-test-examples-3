package ru.yandex.market.antifraud.filter.generators;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ForShowsFiltersTest {
    @Test
    public void smokeTest() {
        ForShowsFilters gen = new ForShowsFilters(DateTime.parse("2010-10-28T01:30:45"));
        assertTrue(gen.generateForAllFilters().size() > 0);
        assertTrue(gen.generateFor12Filter().getShows().size() > 0);
        assertTrue(gen.generateFor12Filter().getClicks().size() > 0);
    }

    @Test
    public void smokeTest2() {
        ForShowsFilters g = new ForShowsFilters(DateTime.parse("2010-10-28T01:30:45"));
        assertTrue(g.generateForNoneFilter().size() > 0);
        assertTrue(g.generateFor02Filter().size() > 0);
        assertTrue(g.generateFor04Filter().size() > 0);
        assertTrue(g.generateFor08Filter().size() > 0);
        assertTrue(g.generateFor08FilterEmptyCookie().size() > 0);
        assertTrue(g.generateFor09Filter().size() > 0);
        assertTrue(g.generateFor13FilterInstead9EmptyCookie().size() > 0);
        assertTrue(g.generateFor10Filter().size() > 0);
        assertTrue(g.generateFor13FilterInsteadOf10().size() > 0);
        assertTrue(g.generateFor11Filter().size() > 0);
        assertTrue(g.generateFor13FilterInsteadOf11().size() > 0);
        assertTrue(g.generateFor13Filter().size() > 0);
    }
}
