package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.terminal.Command;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для {@link CopySupplierCampaignInfoCommand}.
 */
@DbUnitDataSet(before = "CopySupplierCampaignInfoCommandTest.before.csv")
public class CopySupplierCampaignInfoCommandTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;
    @Autowired
    @Qualifier("copySupplierCampaignInfoCommand")
    private Command command;

    @BeforeEach
    void setUp() {
        when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
    }

    @DisplayName("Копирование кампании и клиента")
    @Test
    @DbUnitDataSet(after = "CopySupplierCampaignInfoCommandTest.testCopyCampaignAndClient.after.csv")
    void testCopyCampaignAndClient() {
        invoke(1L);
    }

    @DisplayName("Не копируем, если кампания есть")
    @Test
    @DbUnitDataSet(after = "CopySupplierCampaignInfoCommandTest.before.csv")
    void testNotCopyCampaignAndClient() {
        invoke(2L);
    }

    @DisplayName("Не копируем, если нет поставщика")
    @Test
    @DbUnitDataSet(after = "CopySupplierCampaignInfoCommandTest.before.csv")
    void testNotCopyCampaignAndClientNoSupplier() {
        invoke(3L);
    }

    @DisplayName("Не падаем и ничего не делаем, если аргументы не верны")
    @Test
    @DbUnitDataSet(after = "CopySupplierCampaignInfoCommandTest.before.csv")
    void testWrongArguments() {
        command.execute(
                new CommandInvocation("copy-supplier-campaign-info",
                        new String[]{"1", "2"},
                        Collections.emptyMap()),
                terminal
        );
    }

    private void invoke(long partnerId) {
        command.execute(
                new CommandInvocation("copy-supplier-campaign-info",
                        new String[]{String.valueOf(partnerId)},
                        Collections.emptyMap()),
                terminal
        );
    }
}
