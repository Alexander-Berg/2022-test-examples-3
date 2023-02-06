package ru.yandex.market.commands;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

public class CheckOrderBusinessCommandFunctionalTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;
    @Autowired
    private CheckOrderBusinessCommand cmd;


    private static CommandInvocation commandInvocation(String... args) {
        return new CommandInvocation("check-order-business", args, Collections.emptyMap());
    }

    @Test
    @DbUnitDataSet(after = "CheckOrderBusinessCommandFunctionalTest/addBusiness.after.csv")
    void addBusiness() {
        cmd.executeCommand(commandInvocation("add", "1"), terminal);
    }

    @Test
    @DbUnitDataSet(before = "CheckOrderBusinessCommandFunctionalTest/deleteBusiness.before.csv",
            after = "CheckOrderBusinessCommandFunctionalTest/deleteBusiness.after.csv")
    void deleteBusiness() {
        cmd.executeCommand(commandInvocation("delete", "1"), terminal);
    }
}
