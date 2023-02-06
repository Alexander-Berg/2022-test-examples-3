package ru.yandex.market.deliverycalculator.indexer.command;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;

/**
 * Тесты для {@link DumpPartnerInfoCommand}.
 *
 * @author Vladislav Bauer
 */
class DumpPartnerInfoCommandTest extends AbstractTmsCommandTest {

    private static final int PARTNER_ID_NO_DATA = 1917;
    private static final int PARTNER_ID_DATA = 1961;

    @Autowired
    private DumpPartnerInfoCommand dumpPartnerInfoCommand;


    @Test
    @DisplayName("Данные по магазину отсутствуют")
    void testNoData() {
        final String result = executeCommand(PARTNER_ID_NO_DATA);

        Assertions.assertEquals("Could not find partner 1917", result);
    }

    @Test
    @DisplayName("Корректное получение данных по магазину")
    @DbUnitDataSet(before = "dumpPartnerData.before.csv")
    void testDumpPartnerData() {
        final String expected = StringTestUtil.getString(getClass(), "testDumpPartnerData.json");
        final String result = executeCommand(PARTNER_ID_DATA);

        JSONAssert.assertEquals(expected, result, false);
    }


    private String executeCommand(final long partnerId) {
        final CommandInvocation commandInvocation = commandInvocation("dump-partner-info", partnerId);
        dumpPartnerInfoCommand.executeCommand(commandInvocation, terminal());
        return terminalData();
    }

}
