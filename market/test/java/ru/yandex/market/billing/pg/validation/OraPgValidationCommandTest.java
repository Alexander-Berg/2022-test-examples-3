package ru.yandex.market.billing.pg.validation;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
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
public class OraPgValidationCommandTest extends FunctionalTest {
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

    private OraPgValidationCommand command;

    @BeforeEach
    void setup() {
        OraPgValidationService validationService = new OraPgValidationService(
                validationOraGetterDAO,
                validationPgGetterDAO,
                new OraPgValidationPgDAO(oraJdbcTemplate, FIXED_CLOCK),
                transactionTemplate);
        this.command = new OraPgValidationCommand(validationService);
    }

    @DbUnitDataSet(
            before = "db/OraPgValidationCommandTest.oracleHasMoreRowsAfterCorresponds.before.csv",
            after = "db/OraPgValidationCommandTest.oracleHasMoreRowsAfterCorresponds.after.csv"
    )
    @Test
    void oracleHasMoreRowsAfterCorresponds() {
        List<ComparableRow> valuesOra = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022", "product", "fee")),
                new ComparableRow(600L, Map.of("supplier_id", "198023", "product", "fee"))
        );
        List<ComparableRow> valuesPg = List.of(
                new ComparableRow(500L, Map.of("supplier_id", "198022", "product", "fee"))
        );
        mockDAO(valuesOra, valuesPg);
        executeCommand();
    }

    private void executeCommand() {
        String[] strings = {};
        command.executeCommand(
                new CommandInvocation(
                        command.getNames()[0],
                        strings,
                        ImmutableMap.<String, String>builder()
                                .put("from-date", "2020-06-05")
                                .put("to-date", "2020-06-07")
                                .put("comparable-column-name", "amount")
                                .put("timestamp-name", "event_time")
                                .put("group-column-names", "supplier_id,product")
                                .put("table-name", "market_billing.transaction_log")
                                .build()
                ),
                terminal
        );
    }

    private void mockDAO(List<ComparableRow> valuesOra, List<ComparableRow> valuesPg) {
        LocalDate fromDate = LocalDate.parse("2020-06-05");
        LocalDate toDate = LocalDate.parse("2020-06-07");
        Mockito.when(validationOraGetterDAO.getComparableRows(Mockito.any(), Mockito.any(),
                        Mockito.eq(fromDate),
                        Mockito.eq(toDate)))
                .thenReturn(valuesOra);
        Mockito.when(validationPgGetterDAO.getComparableRows(Mockito.any(), Mockito.any(),
                        Mockito.eq(fromDate),
                        Mockito.eq(toDate)))
                .thenReturn(valuesPg);
    }

}
