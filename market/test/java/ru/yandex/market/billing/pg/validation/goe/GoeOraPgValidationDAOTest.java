package ru.yandex.market.billing.pg.validation.goe;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.pg.validation.ComparableRow;
import ru.yandex.market.billing.pg.validation.ComparableTableMeta;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.billing.pg.validation.goe.GoeOraPgValidationDAO.SELECT_JOIN_CO_FOR_COUNT_VALIDATION;

class GoeOraPgValidationDAOTest extends FunctionalTest {

    private static final LocalDate FROM_DATE = LocalDate.of(2021, Month.SEPTEMBER, 20);
    private static final LocalDate TO_DATE = LocalDate.of(2021, Month.OCTOBER, 1);

    private static final ComparableTableMeta TABLE_META = new ComparableTableMeta(
            "market_billing.orders_transactions",
            "market_billing.orders_transactions",
            List.of("transaction_id", "status", "creation_date"),
            "status",
            "creation_date",
            SELECT_JOIN_CO_FOR_COUNT_VALIDATION,
            SELECT_JOIN_CO_FOR_COUNT_VALIDATION);

    @Autowired
    private GoeOraPgValidationDAO goeOraValidationDAO;

    @Test
    @DbUnitDataSet(
            before = "GoeOraPgValidationDAOTest.getOracleComparableDetailedRowsTest.before.csv"
    )
    public void getOracleComparableCountRowsTest() {
        List<ComparableRow> actualRows = goeOraValidationDAO.getComparableCountRows(
                TABLE_META.getOraCountSqlQuery(),
                TABLE_META.getOraTableName(),
                TABLE_META,
                FROM_DATE,
                TO_DATE
        );

        MatcherAssert.assertThat(actualRows, Matchers.containsInAnyOrder(
                new ComparableRow(1, Map.of(
                        "status", "8"))
        ));
    }
}
