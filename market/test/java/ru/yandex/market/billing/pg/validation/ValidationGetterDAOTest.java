package ru.yandex.market.billing.pg.validation;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@ExtendWith(MockitoExtension.class)
public class ValidationGetterDAOTest extends FunctionalTest {

    private static final ComparableTableMeta TABLE_META = new ComparableTableMeta("market_billing.transaction_log",
            List.of("supplier_id", "product"),
            "amount",
            "transaction_time");

    @Autowired
    private ValidationGetterDAO validationOraGetterDAO;

    @Test
    @DbUnitDataSet(
            before = "db/ValidationGetterDAOTest.getOracleRowsTest.before.csv"
    )
    public void getOracleRowsTest() {
        List<ComparableRow> actualRows = validationOraGetterDAO.getComparableRows(
                TABLE_META.getOraTableName(),
                TABLE_META,
                LocalDate.parse("2020-04-02"),
                LocalDate.parse("2020-04-22")
        );
        MatcherAssert.assertThat(actualRows, Matchers.containsInAnyOrder(
                new ComparableRow(200L, Map.of("supplier_id", "774", "product", "boo")),
                new ComparableRow(1300L, Map.of("supplier_id", "774", "product", "fee")),
                new ComparableRow(200L, Map.of("supplier_id", "774", "product", "gee")),
                new ComparableRow(300L, Map.of("supplier_id", "774", "product", "mee"))
        ));
    }
}
