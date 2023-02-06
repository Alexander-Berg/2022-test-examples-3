package ru.yandex.market.billing.pg.validation;


import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@ExtendWith(MockitoExtension.class)
public class OraPgValidationPgDAOTest extends FunctionalTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2020, 4, 15, 12, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_TIME.toInstant(ZoneOffset.UTC), ZoneOffset.UTC
    );
    private static final LocalDate FROM_DATE = LocalDate.parse("2020-06-05");
    private static final LocalDate TO_DATE = LocalDate.parse("2020-06-07");

    private static final ComparableTableMeta TABLE_META = new ComparableTableMeta("market_billing.transaction_log",
            List.of("supplier_id", "product"),
            "amount",
            "transaction_time");

    private static final ComparableRow COMPARABLE_ROW =
            new ComparableRow(200L, Map.of("supplier_id","198023", "product", "fee"));

    private OraPgValidationPgDAO oraPgValidationPgDAO;

    @Qualifier("namedParameterJdbcTemplate")
    @Autowired
    private NamedParameterJdbcTemplate oraJdbcTemplate;

    @BeforeEach
    void setup() {
        this.oraPgValidationPgDAO = new OraPgValidationPgDAO(oraJdbcTemplate, FIXED_CLOCK);
    }

    @DbUnitDataSet(
            after = "db/OraPgValidationPgDAOTest.logPgAbsentTest.after.csv"
    )
    @Test
    public void logPgAbsentTest() {
        String expectedMessage = "Absent in PG";
        logMessage(expectedMessage);
    }

    @DbUnitDataSet(
            after = "db/OraPgValidationPgDAOTest.logOraAbsentTest.after.csv"
    )
    @Test
    public void logOraAbsentTest() {
        String expectedMessage = "Absent in Oracle";
        logMessage(expectedMessage);
    }

    @DbUnitDataSet(
            after = "db/OraPgValidationPgDAOTest.logInvalidRowTest.after.csv"
    )
    @Test
    public void logInvalidRowTest() {
        logMessage("Different values");
    }

    @Test
    @DbUnitDataSet(
            before = "db/OraPgValidationPgDAOTest.deleteRowsTest.before.csv",
            after = "db/OraPgValidationPgDAOTest.deleteRowsTest.after.csv"
    )
    public void deleteRowsTest() {
        oraPgValidationPgDAO.deleteRows(TABLE_META, FROM_DATE, TO_DATE);
    }

    @Test
    @DbUnitDataSet(
            before = "db/OraPgValidationPgDAOTest.deleteRowsTest.before.csv",
            after = "db/OraPgValidationPgDAOTest.deleteRowsTest.after.csv"
    )
    public void deleteOneRowTest() {
        oraPgValidationPgDAO.deleteRows(TABLE_META, FROM_DATE, TO_DATE);
    }


    private void logMessage(String message) {
        List<Pair<ComparableRow, String>> problems = List.of(Pair.of(COMPARABLE_ROW, message));
        oraPgValidationPgDAO.insertRows(problems, TABLE_META, FROM_DATE, TO_DATE);
    }

}
