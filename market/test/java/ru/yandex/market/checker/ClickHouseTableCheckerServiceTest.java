package ru.yandex.market.checker;

import java.time.LocalDate;
import java.time.Month;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tms.monitor.MbiTeam;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ClickHouseTableCheckerService}
 */
@ExtendWith(MockitoExtension.class)
class ClickHouseTableCheckerServiceTest extends FunctionalTest {
    private static final String DUMMY_TABLE = "dummy_table_name";
    private static final LocalDate DATE_3_SEP_2020 = LocalDate.of(2020, Month.SEPTEMBER, 3);
    private static final LocalDate CURRENT_DATE = LocalDate.of(2020, Month.SEPTEMBER, 5);

    private ClickHouseTableCheckerService clickHouseTableCheckerService;
    private ClickHouseTableCheckerDao clickHouseTableCheckerDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Mock
    private NamedParameterJdbcTemplate clickhouseJdbcTemplate;

    @BeforeEach
    void setUp() {
        clickHouseTableCheckerDao = new ClickHouseTableCheckerDao(
                namedParameterJdbcTemplate,
                clickhouseJdbcTemplate
        );
        clickHouseTableCheckerService = new ClickHouseTableCheckerService(
                clickHouseTableCheckerDao,
                transactionTemplate
        );

    }

    @Test
    @DbUnitDataSet(
            before = "ClickHouseTableCheckerServiceTest.withDate.before.csv",
            after = "ClickHouseTableCheckerServiceTest.withDate.after.csv"
    )
    @DisplayName("Тест на проверку полного lifecycle сервиса с передачей даты")
    void testSaveWithDate() {
        when(clickhouseJdbcTemplate.queryForObject(any(), any(MapSqlParameterSource.class), eq(long.class)))
                .thenReturn(10L);

        clickHouseTableCheckerService.checkTableData(
                DUMMY_TABLE,
                MbiTeam.BILLING,
                CURRENT_DATE,
                DATE_3_SEP_2020,
                "date"
        );
    }

    @Test
    @DbUnitDataSet(
            before = "ClickHouseTableCheckerServiceTest.withoutDate.before.csv",
            after = "ClickHouseTableCheckerServiceTest.withoutDate.after.csv"
    )
    @DisplayName("Тест на проверку полного lifecycle сервиса без передачи даты")
    void testSaveWithoutDate() {
        when(clickhouseJdbcTemplate.queryForObject(any(), any(MapSqlParameterSource.class), eq(long.class)))
                .thenReturn(10L);

        clickHouseTableCheckerService.checkTableData(
                DUMMY_TABLE,
                MbiTeam.BILLING,
                CURRENT_DATE,
                null,
                null
        );
    }

    @Test
    @DisplayName("Тест на проверку ошибки в передаче даты/колонки")
    void testWithException() {
        var exception = Assertions.assertThrows(
                NullPointerException.class,
                () -> clickHouseTableCheckerService.checkTableData(
                        DUMMY_TABLE,
                        MbiTeam.BILLING,
                        CURRENT_DATE,
                        DATE_3_SEP_2020, null
                )
        );
        Assert.assertThat("dateColumn must be not null, if targetDate is not null", equalTo(exception.getMessage()));
    }
}
