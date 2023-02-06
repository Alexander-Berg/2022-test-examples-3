package ru.yandex.market.commands;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

/**
 * Тесты для {@link FillShopVatForRequestCommand}
 */
@DbUnitDataSet(before = "FillShopVatForRequestCommandTest.csv")
public class FillShopVatForRequestCommandTest extends FunctionalTest {
    @Autowired
    private Terminal terminal;
    @Autowired
    private FillShopVatForRequestCommand cmd;


    private static CommandInvocation commandInvocation(String[] args) {
        return new CommandInvocation("fill-vat-for-request-command", args, Collections.emptyMap());
    }

    @Test
    @DbUnitDataSet(after = "FillShopVatForRequestCommandTest.withoutPartner.after.csv")
    void fillShopVatWithoutPartnerId() {
        cmd.executeCommand(commandInvocation(new String[]{"1234567"}), terminal);
    }

    @Test
    @DbUnitDataSet(after = "FillShopVatForRequestCommandTest.withPartner.after.csv")
    void fillShopVatWithPartnerId() {
        cmd.executeCommand(commandInvocation(new String[]{"8912345", "400"}), terminal);
    }

}
