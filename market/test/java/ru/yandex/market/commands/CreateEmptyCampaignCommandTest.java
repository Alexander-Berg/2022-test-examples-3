package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

/**
 * Тесты для {@link CreateEmptyCampaignCommand}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class CreateEmptyCampaignCommandTest extends FunctionalTest {

    @Autowired
    private CreateEmptyCampaignCommand command;
    @Autowired
    private Terminal terminal;
    @Autowired
    private PrintWriter printWriter;

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @Test
    @DisplayName("Без аргументов. Исключение")
    void testEmptyArgs() {
        Assertions.assertThatThrownBy(() -> invoke())
                .hasMessageContaining("Invalid number of arguments");
    }

    @Test
    @DisplayName("Больше 1 аргумента. Исключение")
    void testMultipleArgs() {
        Assertions.assertThatThrownBy(() -> invoke("create", "123", "456"))
                .hasMessageContaining("Invalid number of arguments");
    }

    @Test
    @DisplayName("Аргумент - не число")
    void testNotNumbers() {
        Assertions.assertThatThrownBy(() -> invoke("create", "abc"))
                .hasMessageContaining("Invalid partner_id");
    }

    @Test
    @DisplayName("Аргумент - некорректный режим работы")
    void testIncorrectMode() {
        Assertions.assertThatThrownBy(() -> invoke("bbb", "123"))
                .hasMessageContaining("Invalid mode");
    }

    @Test
    @DisplayName("Адв не трогаем")
    @DbUnitDataSet(before = "CreateEmptyCampaignCommandTest/testAdv.before.csv")
    void testAdv() {
        Assertions.assertThatThrownBy(() -> invoke("create", "100"))
                .hasMessageContaining("Partner is not supplier");
    }

    @Test
    @DisplayName("У поставщика есть открытая кампания. Ничего не делаем")
    @DbUnitDataSet(before = "CreateEmptyCampaignCommandTest/testActiveCampaign.before.csv")
    void testActiveCampaign() {
        Assertions.assertThatThrownBy(() -> invoke("create", "100"))
                .hasMessageContaining("has active campaign");
    }

    @Test
    @DisplayName("Создаем пустую кампанию для синего")
    @DbUnitDataSet(
            before = "CreateEmptyCampaignCommandTest/testEmptyCampaignForSupplier.before.csv",
            after = "CreateEmptyCampaignCommandTest/testEmptyCampaignForSupplier.after.csv"
    )
    void testEmptyCampaignForSupplier() {
        invoke("create", "100");
    }

    @Test
    @DisplayName("Создаем пустую кампанию для дбса")
    @DbUnitDataSet(
            before = "CreateEmptyCampaignCommandTest/testEmptyCampaignForDbs.before.csv",
            after = "CreateEmptyCampaignCommandTest/testEmptyCampaignForDbs.after.csv"
    )
    void testEmptyCampaignForDbs() {
        invoke("create", "100");
    }

    private void invoke(String... args) {
        CommandInvocation commandInvocation = new CommandInvocation("create-empty-campaign",
                args,
                Map.of());

        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DisplayName("Пытаемся закрыть кампанию поставщика, у которого нет открытой кампании.")
    @DbUnitDataSet(
            before = "CreateEmptyCampaignCommandTest/testCloseNotOpened.before.csv"
    )
    void testCloseNotOpened() {
        Assertions.assertThatThrownBy(() -> invoke("close", "100"))
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Пытаемся закрыть кампанию без нулевого клиента")
    @DbUnitDataSet(
            before = "CreateEmptyCampaignCommandTest/testCloseNonZeroClient.before.csv"
    )
    void testCloseNonZeroClient() {
        Assertions.assertThatThrownBy(() -> invoke("close", "100"))
                .hasMessageContaining("has active campaign");
    }

    @Test
    @DisplayName("Закрываем кампанию с нулевым клиентом")
    @DbUnitDataSet(
            before = "CreateEmptyCampaignCommandTest/testCloseNormal.before.csv",
            after = "CreateEmptyCampaignCommandTest/testCloseNormal.after.csv"
    )
    void testCloseNormal() {
        invoke("close", "100");
    }
}
