package ru.yandex.market.mboc.common.services.excel.date;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;

/**
 * @author s-ermakov
 */
@RunWith(Parameterized.class)
public class ExcelDateUtilsTest {

    private String excelFileResource;
    private LocalDate expectedDate;

    public ExcelDateUtilsTest(String excelFileResource, LocalDate expectedDate) {
        this.excelFileResource = excelFileResource;
        this.expectedDate = expectedDate;
    }

    @SuppressWarnings("checkstyle:magicNumber")
    @Parameterized.Parameters(name = "file {0} should contain date of {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"excel/dates/date_2019-09-30.xls", LocalDate.of(2019, Month.SEPTEMBER, 30)},
            {"excel/dates/date_2019-09-30.xlsx", LocalDate.of(2019, Month.SEPTEMBER, 30)},
            {"excel/dates/date_30slash09slash2019.xls", LocalDate.of(2019, Month.SEPTEMBER, 30)},
            {"excel/dates/date_30slash09slash2019.xlsx", LocalDate.of(2019, Month.SEPTEMBER, 30)},
            {"excel/dates/date_30.09.2019.xls", LocalDate.of(2019, Month.SEPTEMBER, 30)},
            {"excel/dates/date_30.09.2019.xlsx", LocalDate.of(2019, Month.SEPTEMBER, 30)},
            {"excel/dates/date_31.12.1999.xls", LocalDate.of(1999, Month.DECEMBER, 31)},
            {"excel/dates/date_31.12.1999.xlsx", LocalDate.of(1999, Month.DECEMBER, 31)},
            {"excel/dates/date_12slash31slash29.xlsx", LocalDate.of(2029, Month.DECEMBER, 31)},
            {"excel/dates/date_01slash01slash30.xlsx", LocalDate.of(1930, Month.JANUARY, 1)},
        });
    }

    @Test
    public void testParseValueFromExcel() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(excelFileResource)) {
            ExcelFile excelFile = ExcelFileConverter.convert(inputStream, ExcelHeaders.MBOC_EXCEL_IGNORES_CONFIG);
            MbocAssertions.assertThat(excelFile).containsValue(1, 0);

            String rawDateValue = excelFile.getValue(1, 0);
            LocalDate result = ExcelDateUtils.parseDate(rawDateValue);

            Assertions.assertThat(result).isEqualTo(expectedDate);
        }
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testDateIntervalsOverlappingIsFalse() {
        LocalDate fromDate1 = LocalDate.of(2000, 1, 1);
        LocalDate toDate1 = LocalDate.of(2000, 5, 1);
        LocalDate fromDate2 = LocalDate.of(2000, 6, 1);
        LocalDate toDate2 = LocalDate.of(2000, 8, 1);
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate1, toDate1, fromDate2, toDate2)).isFalse();
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate2, toDate2, fromDate1, toDate1)).isFalse();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testDateIntervalsOverlappingIsFalseDifferentYear() {
        LocalDate fromDate1 = LocalDate.of(2000, 1, 1);
        LocalDate toDate1 = LocalDate.of(2001, 1, 1);
        LocalDate fromDate2 = LocalDate.of(2001, 1, 2);
        LocalDate toDate2 = LocalDate.of(2003, 1, 1);
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate1, toDate1, fromDate2, toDate2)).isFalse();
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate2, toDate2, fromDate1, toDate1)).isFalse();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testDateIntervalsOverlappingIsTrue() {
        LocalDate fromDate1 = LocalDate.of(2000, 1, 1);
        LocalDate toDate1 = LocalDate.of(2000, 2, 1);
        LocalDate fromDate2 = LocalDate.of(2000, 1, 2);
        LocalDate toDate2 = LocalDate.of(2000, 3, 1);
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate1, toDate1, fromDate2, toDate2)).isTrue();
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate2, toDate2, fromDate1, toDate1)).isTrue();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testDateIntervalsOverlappingIsTrueOneInsideAnother() {
        LocalDate fromDate1 = LocalDate.of(2000, 1, 1);
        LocalDate toDate1 = LocalDate.of(2000, 5, 1);
        LocalDate fromDate2 = LocalDate.of(2000, 2, 1);
        LocalDate toDate2 = LocalDate.of(2000, 3, 1);
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate1, toDate1, fromDate2, toDate2)).isTrue();
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate2, toDate2, fromDate1, toDate1)).isTrue();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testDateIntervalsOverlappingIsTrueSameDay() {
        LocalDate fromDate1 = LocalDate.of(2000, 1, 1);
        LocalDate toDate1 = LocalDate.of(2000, 1, 2);
        LocalDate fromDate2 = LocalDate.of(2000, 1, 2);
        LocalDate toDate2 = LocalDate.of(2000, 1, 3);
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate1, toDate1, fromDate2, toDate2)).isTrue();
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate2, toDate2, fromDate1, toDate1)).isTrue();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testDateIntervalsOverlappingIsFalseOneDay() {
        LocalDate fromDate1 = LocalDate.of(2000, 1, 1);
        LocalDate toDate1 = LocalDate.of(2000, 1, 1);
        LocalDate fromDate2 = LocalDate.of(2000, 1, 2);
        LocalDate toDate2 = LocalDate.of(2000, 1, 2);
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate1, toDate1, fromDate2, toDate2)).isFalse();
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate2, toDate2, fromDate1, toDate1)).isFalse();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testDateIntervalsOverlappingIsTrueNullInterval() {
        LocalDate fromDate1 = LocalDate.of(2000, 1, 1);
        LocalDate toDate1 = LocalDate.of(2000, 1, 2);
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate1, toDate1, null, null)).isTrue();
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(null, null, fromDate1, toDate1)).isTrue();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testDateIntervalsOverlappingIsTrueOneDateIsNull() {
        LocalDate fromDate1 = LocalDate.of(2000, 1, 1);
        LocalDate toDate1 = LocalDate.of(2000, 1, 2);
        LocalDate fromDate2 = LocalDate.of(2000, 1, 1);
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate1, toDate1, fromDate2, null)).isTrue();
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate2, null, fromDate1, toDate1)).isTrue();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testDateIntervalsOverlappingIsFalseOneDateIsNull() {
        LocalDate fromDate1 = LocalDate.of(2000, 1, 1);
        LocalDate toDate1 = LocalDate.of(2000, 1, 2);
        LocalDate fromDate2 = LocalDate.of(2000, 3, 1);
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate1, toDate1, fromDate2, null)).isFalse();
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate2, null, fromDate1, toDate1)).isFalse();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testDateIntervalsOverlappingIsTrueSemiIntervals() {
        LocalDate fromDate1 = LocalDate.of(2000, 1, 1);
        LocalDate fromDate2 = LocalDate.of(2000, 3, 1);
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate1, null, fromDate2, null)).isTrue();
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate2, null, fromDate1, null)).isTrue();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testDateIntervalsOverlappingIsFalseSemiIntervals() {
        LocalDate fromDate1 = LocalDate.of(2000, 3, 1);
        LocalDate toDate2 = LocalDate.of(2000, 1, 1);
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate1, null, null, toDate2)).isFalse();
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(null, toDate2, fromDate1, null)).isFalse();
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testDateIntervalsOverlappingIsTrueSemiIntervalsFromTo() {
        LocalDate fromDate1 = LocalDate.of(2000, 1, 1);
        LocalDate toDate2 = LocalDate.of(2000, 3, 1);
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(fromDate1, null, null, toDate2)).isTrue();
        Assertions.assertThat(ExcelDateUtils
            .dateIntervalsOverlap(null, toDate2, fromDate1, null)).isTrue();
    }

    @Test
    public void printInstantAndPrintDateWillReturnEqualResult() {
        var localDateTime = LocalDateTime.parse("2020-08-09T10:00:00");
        var instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

        var localDateTimeString = ExcelDateUtils.printDate(localDateTime);
        var instantString = ExcelDateUtils.printDate(instant);

        Assertions.assertThat(localDateTimeString).isEqualTo(instantString);
    }
}
