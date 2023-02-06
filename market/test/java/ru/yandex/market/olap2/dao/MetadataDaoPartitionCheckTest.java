package ru.yandex.market.olap2.dao;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.olap2.load.partitioning.PartitionType;
import ru.yandex.market.olap2.sla.ImportantCubesPaths;
import ru.yandex.market.olap2.sla.SlaCube;
import ru.yandex.market.olap2.sla.SlaCubesHolder;
import ru.yandex.market.olap2.step.model.StepEvent;
import ru.yandex.market.olap2.step.model.StepEventParams;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MetadataDaoPartitionCheckTest {

    private final LoggingJdbcTemplate template = Mockito.mock(LoggingJdbcTemplate.class);
    private final SlaCubesHolder holder = new SlaCubesHolder(ImmutableMap.of("cube_fact_name",
            new SlaCube("cube_fact_name", 1, true, false)));
    private final ImportantCubesPaths slaPaths = new ImportantCubesPaths(holder);
    private final MetadataDao dao = new MetadataDao(template, slaPaths);

    @Test
    public void checkOldPartitionNone() throws ParseException {
        StepEvent today = createEvent(null);
        assertFalse(dao.isOldPartition(today));
    }

    @Test
    public void checkOldPartitionYear() throws ParseException {
        LocalDate now = LocalDate.now();
        StepEvent thisYear = createEvent(String.valueOf(now.getYear()));
        StepEvent twoYearsAgo = createEvent(String.valueOf(now.getYear() - 2));
        assertFalse(dao.isOldPartition(thisYear));
        assertTrue(dao.isOldPartition(twoYearsAgo));
    }

    @Test
    public void checkOldPartitionMonth() throws ParseException {
        LocalDate now = LocalDate.now();
        StepEvent thisMonth = createEvent(now.format(DateTimeFormatter.ISO_DATE).substring(0, 7));
        StepEvent fourMonthAgo = createEvent(
                now.minusMonths(4).format(DateTimeFormatter.ISO_DATE).substring(0, 7));
        StepEvent moreThanFourMonthAgo = createEvent(
                now.minusMonths(5).format(DateTimeFormatter.ISO_DATE).substring(0, 7));
        assertFalse(dao.isOldPartition(thisMonth));
        assertFalse(dao.isOldPartition(fourMonthAgo));
        assertTrue(dao.isOldPartition(moreThanFourMonthAgo));
    }

    @Test
    public void checkOldPartitionMonthQuarter() {
        LocalDate now = LocalDate.now();
        StepEvent thisMonth = createEvent(toMonthQuarterPartition(now));
        StepEvent fourMonthAgo = createEvent(toMonthQuarterPartition(now.minusMonths(4)));
        StepEvent moreThanFourMonthAgo = createEvent(toMonthQuarterPartition(now.minusMonths(5)));
        assertFalse(dao.isOldPartition(thisMonth));
        assertFalse(dao.isOldPartition(fourMonthAgo));
        assertTrue(dao.isOldPartition(moreThanFourMonthAgo));
    }

    private String toMonthQuarterPartition(LocalDate date) {
        return date.format(DateTimeFormatter.ISO_DATE).substring(0, 8) + PartitionType.getMQPartitionNum(date);
    }

    @Test
    public void checkOldPartitionDay() {
        LocalDate now = LocalDate.now();
        StepEvent today = createEvent(now.format(DateTimeFormatter.ISO_DATE));
        StepEvent dayFourMonthAgo = createEvent(now.minusMonths(4).format(DateTimeFormatter.ISO_DATE));
        StepEvent dayMoreThanFourMonthAgo = createEvent(
                now.minusMonths(4).minusDays(1).format(DateTimeFormatter.ISO_DATE));
        assertFalse(dao.isOldPartition(dayFourMonthAgo));
        assertTrue(dao.isOldPartition(dayMoreThanFourMonthAgo));
        assertFalse(dao.isOldPartition(today));

    }

    private static StepEvent createEvent(String partition) {
        StepEventParams params = new StepEventParams();
        params.setPath("//a");
        params.setPartition(partition);
        StepEvent e = new StepEvent();
        e.setName("f");
        e.setStepEventParams(params);
        return e;
    }
}
