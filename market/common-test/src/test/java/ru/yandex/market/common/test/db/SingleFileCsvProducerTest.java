package ru.yandex.market.common.test.db;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.common.util.IOUtils;

/**
 * Тесты для {@link SingleFileCsvProducer}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SampleTestConfig.class)
@DbUnitDataSet(before = "csvProducerTest.before.csv", nonTruncatedTables = "MY.DICT")
public class SingleFileCsvProducerTest extends DbUnitTest {


    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DataSource dataSource;

    @DisplayName("Дата, округленная до первого числа месяца")
    @Test
    public void testFunctionsTruncatedSysdateToUnitMonths() {
        Instant instant = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant();
        Timestamp timestamp = SingleFileCsvProducer.Functions.truncedsysdatetounit("MONTHS");
        Assertions.assertEquals(instant, timestamp.toInstant());
    }

    @DisplayName("Дата, округленная до года")
    @Test
    public void testFunctionsTruncatedSysdateToUnitYear() {
        Instant instant = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).withMonth(1).withDayOfMonth(1).toInstant();
        Timestamp timestamp = SingleFileCsvProducer.Functions.truncedsysdatetounit("YEARS");
        Assertions.assertEquals(instant, timestamp.toInstant());
    }

    @DisplayName("Дата, округленная до дня")
    @Test
    public void testFunctionsTruncatedSysdateToUnitDays() {
        Instant instant = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant();
        Timestamp timestamp = SingleFileCsvProducer.Functions.truncedsysdatetounit("DAYS");
        Assertions.assertEquals(instant, timestamp.toInstant());
    }

    @DisplayName("Дата, округленная до первого числа месяца чтение из файла")
    @Test
    public void testFunctionsTruncatedSysdateToUnitMonthsCsv() {
        Instant instant = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant();
        Timestamp dt = jdbcTemplate.queryForObject("select DT from MY.DT_TABLE where id=1", Timestamp.class);
        Assertions.assertEquals(instant, dt.toInstant());
    }

    @DisplayName("Дата, округленная до года чтение из файла")
    @Test
    public void testFunctionsTruncatedSysdateToUnitYearCsv() {
        Instant instant =
                ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).withMonth(Month.JANUARY.getValue()).withDayOfMonth(1).toInstant();
        Timestamp dt = jdbcTemplate.queryForObject("select DT from MY.DT_TABLE where id=2", Timestamp.class);
        Assertions.assertEquals(instant, dt.toInstant());
    }

    @DisplayName("Дата, округленная до дня чтение из файла")
    @Test
    public void testFunctionsTruncatedSysdateToUnitDaysCsv() {
        Instant instant = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant();
        Timestamp dt = jdbcTemplate.queryForObject("select DT from MY.DT_TABLE where id=3", Timestamp.class);
        Assertions.assertEquals(instant, dt.toInstant());
    }

    @DisplayName("Корректно подставляет json файл из classpath")
    @Test
    public void testFunctionsFileJson() throws IOException {
        String expected = IOUtils.readInputStream(SingleFileCsvProducerTest.class.getResourceAsStream("test.json"));
        String actual = jdbcTemplate.queryForObject("select txt from my.test where id = 1", String.class);
        Assertions.assertEquals(actual.trim(), expected.trim());
    }

    @DisplayName("Корректно подставляет xml файл из classpath")
    @Test
    public void testFunctionsFileXml() throws IOException {
        String expected = IOUtils.readInputStream(SingleFileCsvProducerTest.class.getResourceAsStream("test.xml"));
        String actual = jdbcTemplate.queryForObject("select txt from my.test where id = 2", String.class);
        Assertions.assertEquals(actual.trim(), expected.trim());
    }

    @DisplayName("Месяц назад от текущей даты")
    @Test
    public void testFunctionsShiftedSysdateMonthAgoCsv() {
        LocalDate expected = ZonedDateTime.now().minusMonths(1).toLocalDate();
        Timestamp dt = jdbcTemplate.queryForObject("select DT from MY.DT_TABLE where id=4", Timestamp.class);
        Assertions.assertEquals(expected, dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
    }
}
