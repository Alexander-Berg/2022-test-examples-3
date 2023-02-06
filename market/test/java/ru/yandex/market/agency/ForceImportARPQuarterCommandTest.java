package ru.yandex.market.agency;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты для {@link ForceImportARPQuarterCommand}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class ForceImportARPQuarterCommandTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;

    @Autowired
    private ForceImportARPQuarterCommand forceImportARPQuarterCommand;

    @Test
    @DisplayName("Успешный вызов принудительного импорта")
    @DbUnitDataSet(
            before = "csv/ForceImportARPQuarterCommandTest.testSuccess.before.csv",
            after = "csv/ForceImportARPQuarterCommandTest.testSuccess.after.csv"
    )
    void testSuccess() {
        invoke("activity_quality", "datasource", "2019", "1");
    }

    @ParameterizedTest
    @MethodSource("testInvalidArgsData")
    @DisplayName("Невалиданые аргументы")
    @DbUnitDataSet(
            before = "csv/ForceImportARPQuarterCommandTest.testSuccess.before.csv",
            after = "csv/ForceImportARPQuarterCommandTest.testSuccess.before.csv"
    )
    void testInvalidArgs(String name, String arp, String level, String year, String quarter) {
        Assertions.assertThrows(Exception.class, () -> invoke(arp, level, year, quarter));
    }

    private static Stream<Arguments> testInvalidArgsData() {
        return Stream.of(
                Arguments.of(
                        "Неправильное имя",
                        "activity_quality_bad", "datasource", "2019", "1"
                ),
                Arguments.of(
                        "Неправильный уровень",
                        "activity_quality", "datasource_bad", "2019", "1"
                ),
                Arguments.of(
                        "Неправильный год",
                        "activity_quality", "datasource", "2019_bad", "1"
                ),
                Arguments.of(
                        "Неправильный квартал",
                        "activity_quality", "datasource", "2019", "1_bad"
                )
        );
    }

    private void invoke(String... args) {
        CommandInvocation commandInvocation = new CommandInvocation("force-import-arp",
                args,
                Collections.emptyMap());

        forceImportARPQuarterCommand.executeCommand(commandInvocation, terminal);
    }
}
