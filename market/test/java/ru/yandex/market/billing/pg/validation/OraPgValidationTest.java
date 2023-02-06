package ru.yandex.market.billing.pg.validation;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@ExtendWith(MockitoExtension.class)
public class OraPgValidationTest extends FunctionalTest {
    private static final ComparableTableMeta TABLE_META = new ComparableTableMeta("market_billing.transaction_log",
            List.of("supplier_id", "product"),
            "amount",
            "transaction_time");
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2020, 4, 15, 12, 0);
    private static final LocalDate FIXED_FROM = LocalDate.parse("2020-06-05");
    private static final LocalDate FIXED_TO = LocalDate.parse("2020-06-07");

    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_TIME.toInstant(ZoneOffset.UTC), ZoneOffset.UTC
    );


    @Mock
    private ValidationGetterDAO validationOraGetterDAO;

    @Mock
    private ValidationGetterDAO validationPgGetterDAO;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Qualifier("namedParameterJdbcTemplate")
    @Autowired
    private NamedParameterJdbcTemplate oraJdbcTemplate;
    @Qualifier("billingPgTransactionTemplate")

    private OraPgValidationService validationService;

    @BeforeEach
    void setup() {
        this.validationService = new OraPgValidationService(
                validationOraGetterDAO,
                validationPgGetterDAO,
                new OraPgValidationPgDAO(oraJdbcTemplate, FIXED_CLOCK),
                transactionTemplate);
    }

    @Test
    @DbUnitDataSet(
            before = "db/OraPgValidationTest.allCorrectTest.before.csv",
            after = "db/OraPgValidationTest.allCorrectTest.after.csv"
    )
    public void allCorrectTest() {
        List<ComparableRow> comparableEntities = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022", "product", "fee")),
                new ComparableRow(600L, Map.of("supplier_id", "198023", "product", "fee"))
        );
        mockDAO(comparableEntities, comparableEntities);
        MatcherAssert.assertThat(
                OraPgValidationUtil.checkDataComparability(comparableEntities, comparableEntities),
                Matchers.empty()
        );
        runValidation();
    }

    private void runValidation() {
        validationService.validate(TABLE_META, FIXED_FROM, FIXED_TO);
    }

    @Test
    @DbUnitDataSet(
            before = "db/OraPgValidationTest.oracleHasMoreRowsAfterCorresponds.before.csv",
            after = "db/OraPgValidationTest.oracleHasMoreRowsAfterCorresponds.after.csv"
    )
    public void oracleHasMoreRowsAfterCorresponds() {
        List<ComparableRow> valuesOra = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022", "product", "fee")),
                new ComparableRow(600L, Map.of("supplier_id", "198023", "product", "fee"))
        );
        List<ComparableRow> valuesPg = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022", "product", "fee"))
        );
        mockDAO(valuesOra, valuesPg);
        MatcherAssert.assertThat(
                OraPgValidationUtil.checkDataComparability(valuesOra, valuesPg),
                Matchers.contains(
                        Pair.of(new ComparableRow(600L,
                                Map.of("supplier_id", "198023", "product", "fee")), "Absent in PG")
                )
        );
        runValidation();
    }

    private void mockDAO(List<ComparableRow> valuesOra, List<ComparableRow> valuesPg) {
        Mockito.when(validationOraGetterDAO.getComparableRows(Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any()))
                .thenReturn(valuesOra);
        Mockito.when(validationPgGetterDAO.getComparableRows(Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any()))
                .thenReturn(valuesPg);
    }

    @Test
    @DbUnitDataSet(
            before = "db/OraPgValidationTest.pgHasMoreRowsAfterCorresponds.before.csv",
            after = "db/OraPgValidationTest.pgHasMoreRowsAfterCorresponds.after.csv"
    )
    public void pgHasMoreRowsAfterCorresponds() {
        List<ComparableRow> valuesOra = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022", "product", "fee"))
        );
        List<ComparableRow> valuesPg = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022", "product", "fee")),
                new ComparableRow(600L, Map.of("supplier_id", "198023", "product", "fee"))
        );
        mockDAO(valuesOra, valuesPg);
        MatcherAssert.assertThat(
                OraPgValidationUtil.checkDataComparability(valuesOra, valuesPg),
                Matchers.contains(
                        Pair.of(new ComparableRow(600L,
                                Map.of("supplier_id", "198023", "product", "fee")), "Absent in Oracle")
                )
        );
        runValidation();
    }

    @Test
    @DbUnitDataSet(
            before = "db/OraPgValidationTest.oneFieldNotCorrespond.before.csv",
            after = "db/OraPgValidationTest.oneFieldNotCorrespond.after.csv"
    )
    public void oneFieldNotCorrespond() {
        List<ComparableRow> valuesOra = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022", "product", "fee")),
                new ComparableRow(600L, Map.of("supplier_id", "198023", "product", "fee"))
        );
        List<ComparableRow> valuesPg = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022", "product", "fee")),
                new ComparableRow(700L, Map.of("supplier_id", "198023", "product", "fee"))
        );
        mockDAO(valuesOra, valuesPg);
        MatcherAssert.assertThat(
                OraPgValidationUtil.checkDataComparability(valuesOra, valuesPg),
                Matchers.contains(
                        Pair.of(new ComparableRow(600L,
                                Map.of("supplier_id", "198023", "product", "fee")), "Different values")
                )
        );
        validationService.validate(TABLE_META, FIXED_FROM, FIXED_TO);
    }

}
