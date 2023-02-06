package ru.yandex.market.billing.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.core.util.TerminalUtils;
import ru.yandex.market.mbi.tms.monitor.MbiTeam;

public class TestDbConnectionCommand extends AbstractCommandMonitorFriendly {
    private static final Logger log = LoggerFactory.getLogger(TestDbConnectionCommand.class);
    private static final String COMMAND_NAME = "test-billing-pg";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public TestDbConnectionCommand(
            NamedParameterJdbcTemplate billingPgJdbcTemplate,
            TransactionTemplate billingPgTransactionTemplate
    ) {
        this.jdbcTemplate = billingPgJdbcTemplate;
        this.transactionTemplate = billingPgTransactionTemplate;
    }

    @Override
    protected void executeCommand(CommandInvocation commandInvocation, Terminal terminal) {
        transactionTemplate.execute(status -> {
            Integer one = jdbcTemplate.queryForObject("select 1", new MapSqlParameterSource(), Integer.class);
            if (!Integer.valueOf(1).equals(one)) {
                throw new RuntimeException("Bad");
            }
            return null;
        });
        TerminalUtils.printlnInTerminal(terminal, "Success");
    }

    @Override
    public String[] getNames() {
        return new String[]{COMMAND_NAME};
    }

    @Override
    public String getDescription() {
        return COMMAND_NAME + " : Тест коннект к бд биллинга pgaas";
    }

    @Override
    public MbiTeam getAssignedMbiTeam() {
        return MbiTeam.BILLING;
    }
}
