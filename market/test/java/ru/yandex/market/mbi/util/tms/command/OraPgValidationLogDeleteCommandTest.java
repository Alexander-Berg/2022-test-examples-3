package ru.yandex.market.mbi.util.tms.command;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tms.command.OraPgValidationLogDeleteCommand;

public class OraPgValidationLogDeleteCommandTest extends FunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private OraPgValidationLogDeleteCommand command;

    @BeforeEach
    void setup() {
        this.command = new OraPgValidationLogDeleteCommand(pgNamedParameterJdbcTemplate, transactionTemplate);
    }

    @DbUnitDataSet(
            before = "OraPgValidationLogDeleteCommandTest.delete2RowsTest.before.csv",
            after = "OraPgValidationLogDeleteCommandTest.delete2RowsTest.after.csv"
    )
    @Test
    public void delete2RowsTest() {
        String[] strings = {};
        command.executeCommand(
                new CommandInvocation(
                        command.getNames()[0],
                        strings,
                        ImmutableMap.<String, String>builder()
                                .put("from-date", "2020-01-01")
                                .put("to-date", "2021-08-20")
                                .put("group-column-names", "draft_id")
                                .put("table-name", "market_billing.fixed_tariffs_billed_amounts")
                                .build()
                ),
                null
        );
    }
}
