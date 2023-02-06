package ru.yandex.market.checkout.checkouter.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class FixPhantomRefundsCommantTest {

    private final DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    @Test
    public void checkDatesArgumentParser() throws Exception {
        FixPhantomRefundsCommand command = new FixPhantomRefundsCommand();

        FixPhantomRefundsCommand.Params params = command.getParamsFromArguments(
                new String[]{"dates", "fix", "2018-03-03", "2018-03-04"}
        );

        assertThat(params.getType(), equalTo(FixPhantomRefundsCommand.SearchType.BY_DATES));
        assertThat(params.getDateFrom(), equalTo(format.parse("2018-03-03")));
        assertThat(params.getDateTo(), equalTo(format.parse("2018-03-04")));
        assertThat(params.isFix(), equalTo(true));

    }

    @Test
    public void checkDatesArgumentParserWithoutFix() throws Exception {
        FixPhantomRefundsCommand command = new FixPhantomRefundsCommand();

        FixPhantomRefundsCommand.Params params = command.getParamsFromArguments(
                new String[]{"dates", "2018-03-03", "2018-03-04"}
        );

        assertThat(params.getType(), equalTo(FixPhantomRefundsCommand.SearchType.BY_DATES));
        assertThat(params.getDateFrom(), equalTo(format.parse("2018-03-03")));
        assertThat(params.getDateTo(), equalTo(format.parse("2018-03-04")));
        assertThat(params.isFix(), equalTo(false));

    }

    @Test
    public void checkPaymentsArgumentParserWithoutFix() {
        FixPhantomRefundsCommand command = new FixPhantomRefundsCommand();

        FixPhantomRefundsCommand.Params params = command.getParamsFromArguments(
                new String[]{"payments", "111", "222", "333"}
        );

        assertThat(params.getType(), equalTo(FixPhantomRefundsCommand.SearchType.BY_PAYMENT_IDS));
        assertThat(params.getDateFrom(), nullValue());
        assertThat(params.getDateTo(), nullValue());
        assertThat(params.getPaymentIds(), containsInAnyOrder(111L, 222L, 333L));
        assertThat(params.isFix(), equalTo(false));
    }

    @Test
    public void checkPaymentsArgumentParser() {
        FixPhantomRefundsCommand command = new FixPhantomRefundsCommand();

        FixPhantomRefundsCommand.Params params = command.getParamsFromArguments(
                new String[]{"payments", "fix", "111", "222", "333"}
        );

        assertThat(params.getType(), equalTo(FixPhantomRefundsCommand.SearchType.BY_PAYMENT_IDS));
        assertThat(params.getDateFrom(), nullValue());
        assertThat(params.getDateTo(), nullValue());
        assertThat(params.getPaymentIds(), containsInAnyOrder(111L, 222L, 333L));
        assertThat(params.getOrderIds(), nullValue());
        assertThat(params.isFix(), equalTo(true));
    }

    @Test
    public void checkOrdersArgumentParser() {
        FixPhantomRefundsCommand command = new FixPhantomRefundsCommand();

        FixPhantomRefundsCommand.Params params = command.getParamsFromArguments(
                new String[]{"orders", "fix", "111", "222", "333"}
        );

        assertThat(params.getType(), equalTo(FixPhantomRefundsCommand.SearchType.BY_ORDERS));
        assertThat(params.getDateFrom(), nullValue());
        assertThat(params.getDateTo(), nullValue());
        assertThat(params.getOrderIds(), containsInAnyOrder(111L, 222L, 333L));
        assertThat(params.getPaymentIds(), nullValue());
        assertThat(params.isFix(), equalTo(true));
    }
}
