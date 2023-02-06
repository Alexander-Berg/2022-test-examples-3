package ru.yandex.market.billing.pg.validation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@ExtendWith(MockitoExtension.class)
public class OraPgValidationCheckCorrectedCommandTest extends FunctionalTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2020, 4, 15, 12, 0);
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
    @Mock
    private Terminal terminal;

    private OraPgValidationCheckCorrectedCommand command;

    @BeforeEach
    void setup() {
        OraPgValidationService validationService = new OraPgValidationService(
                validationOraGetterDAO,
                validationPgGetterDAO,
                new OraPgValidationPgDAO(oraJdbcTemplate, FIXED_CLOCK),
                transactionTemplate);
        this.command = new OraPgValidationCheckCorrectedCommand(validationService);
    }

    @DbUnitDataSet(
            before = "db/OraPgValidationCheckCorrectedCommandTest.oneRowCorrected.before.csv",
            after = "db/OraPgValidationCheckCorrectedCommandTest.oneRowCorrected.after.csv"
    )
    @Test
    void oneRowCorrected() {
        executeCommand(Map.of("all", ""));
    }

    @DbUnitDataSet(
            before = "db/OraPgValidationCheckCorrectedCommandTest.oneRowInvalid.before.csv",
            after = "db/OraPgValidationCheckCorrectedCommandTest.oneRowInvalid.after.csv"
    )
    @Test
    void oneRowInvalid() {
        List<ComparableRow> valuesOra = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022")),
                new ComparableRow(600L, Map.of("supplier_id", "198023"))
        );
        List<ComparableRow> valuesPg = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022")),
                new ComparableRow(700L, Map.of("supplier_id", "198023"))
        );
        mockDAO(valuesOra, valuesPg);
        executeCommand(Map.of("all", ""));
    }

    @DbUnitDataSet(
            before = "db/OraPgValidationCheckCorrectedCommandTest.matchTableName.before.csv",
            after = "db/OraPgValidationCheckCorrectedCommandTest.matchTableName.after.csv"
    )
    @Test
    void matchTableName() {
        List<ComparableRow> valuesOra = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022")),
                new ComparableRow(600L, Map.of("supplier_id", "198023"))
        );
        List<ComparableRow> valuesPg = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022")),
                new ComparableRow(600L, Map.of("supplier_id", "198023"))
        );
        mockDAO(valuesOra, valuesPg);
        executeCommand(Map.of("table-name", "market_billing.sorting_daily_billed_amounts"));
    }

    @DbUnitDataSet(
            before = "db/OraPgValidationCheckCorrectedCommandTest.unmatchedTableName.before.csv",
            after = "db/OraPgValidationCheckCorrectedCommandTest.unmatchedTableName.after.csv"
    )
    @Test
    void unmatchedTableName() {
        List<ComparableRow> valuesOra = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022")),
                new ComparableRow(600L, Map.of("supplier_id", "198023"))
        );
        List<ComparableRow> valuesPg = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022")),
                new ComparableRow(600L, Map.of("supplier_id", "198023"))
        );
        mockDAO(valuesOra, valuesPg);
        executeCommand(Map.of("table-name", "market_billing.transaction_time"));
    }

    private void executeCommand(Map<String, String> options) {
        String[] strings = {};
        command.executeCommand(
                new CommandInvocation(
                        command.getNames()[0],
                        strings,
                        options
                ),
                terminal
        );
    }

    private void mockDAO(List<ComparableRow> valueOra, List<ComparableRow> valuePg) {
        Mockito.when(validationOraGetterDAO.getComparableRows(Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any()))
                .thenReturn(valueOra);
        Mockito.when(validationPgGetterDAO.getComparableRows(Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any()))
                .thenReturn(valuePg);
    }
}
