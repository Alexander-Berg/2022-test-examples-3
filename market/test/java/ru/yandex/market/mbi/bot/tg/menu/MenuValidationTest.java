package ru.yandex.market.mbi.bot.tg.menu;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.collection.IsIn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.bot.FunctionalTest;
import ru.yandex.market.mbi.bot.tg.action.command.CommandType;
import ru.yandex.misc.lang.StringUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class MenuValidationTest extends FunctionalTest {

    @Autowired
    private MenuService menuService;

    @BeforeEach
    public void before() throws IOException {
        menuService.initMenuFromFile();
    }

    @Test
    @DisplayName("Все команды начинаются с символа '/'")
    void testMenuCommandsStartWithSlash() {
        TelegramMenuSettings settings = getTelegramMenuSettings();
        List<TelegramMenuCommand> menu = settings.getMenu();
        assertEquals(
                menu.size(),
                menu.stream().map(TelegramMenuCommand::getCommand).filter(command -> command.startsWith("/")).count(),
                "Количество команд меню, которые начинаются с '/', не совпадает с их общим количеством"
        );

        List<TelegramMenuScenario> scenarios = settings.getScenarios();
        for (TelegramMenuScenario scenario : scenarios) {
            Set<String> corruptedCommands = scenario.getPages().stream().map(TelegramMenuScenarioPage::getKeyboard)
                    .flatMap(Collection::stream)
                    .flatMap(Collection::stream)
                    .map(TelegramMenuButton::getCommand)
                    .filter(Objects::nonNull)
                    .filter(command -> !command.startsWith("/"))
                    .collect(Collectors.toSet());

            assertThat(
                    "Не все команды в сценарии " + scenario.getScenarioId() + " начинаются с  '/'",
                    corruptedCommands,
                    IsEmptyCollection.empty()
            );
        }

    }

    @Test
    @DisplayName("Команды в меню не повторяются")
    void testMenuCommandsUnique() {
        TelegramMenuSettings settings = getTelegramMenuSettings();
        List<TelegramMenuCommand> menu = settings.getMenu();
        assertEquals(
                menu.size(),
                menu.stream().map(TelegramMenuCommand::getCommand).distinct().count(),
                "Количество уникальных команд меню не совпадает с их общим количеством"
        );
    }

    @Test
    @DisplayName("Команды в меню содержат только маленькие латинские буквы, цифры и подчёркивания")
    void testMenuCommandsContainOnlyLowercaseDigitsAndUnderscores() {
        TelegramMenuSettings settings = getTelegramMenuSettings();
        List<TelegramMenuCommand> menu = settings.getMenu();
        Set<String> malformedCommands = menu.stream().map(TelegramMenuCommand::getCommand)
                .filter(command -> !command.matches("/[a-z0-9_].+"))
                .collect(Collectors.toSet());

        assertThat(
                "Некоторые команды меню содержат недопустимые символы: " + malformedCommands,
                malformedCommands,
                IsEmptyCollection.empty()
        );
    }

    @Test
    @DisplayName("Команды в меню по длине не больше 32 символов")
    void testMenuCommandsContainMax32Symbols() {
        TelegramMenuSettings settings = getTelegramMenuSettings();
        List<TelegramMenuCommand> menu = settings.getMenu();
        Set<String> tooLongCommands = menu.stream().map(TelegramMenuCommand::getCommand)
                .filter(command -> command.length() > 32)
                .collect(Collectors.toSet());

        assertThat(
                "Некоторые команды меню слишком длинные (> 32 символов): " + tooLongCommands,
                tooLongCommands,
                IsEmptyCollection.empty()
        );
    }

    @Test
    @DisplayName("Описания команд в меню по длине не больше 256 символов")
    void testMenuCommandDescriptionsContainMax256Symbols() {
        TelegramMenuSettings settings = getTelegramMenuSettings();
        List<TelegramMenuCommand> menu = settings.getMenu();
        Set<String> tooLongDescriptions = menu.stream().map(TelegramMenuCommand::getDescription)
                .filter(description -> description.length() > 256)
                .collect(Collectors.toSet());

        assertThat(
                "Некоторые описания команд меню слишком длинные (> 256 символов): " + tooLongDescriptions,
                tooLongDescriptions,
                IsEmptyCollection.empty()
        );
    }

    @Test
    @DisplayName("У всех команд меню есть описания")
    void testEachMenuCommandHaveDescriptions() {
        TelegramMenuSettings settings = getTelegramMenuSettings();
        List<TelegramMenuCommand> menu = settings.getMenu();
        Set<String> commandsWithoutDescriptions = menu.stream()
                .filter(menuCommand -> StringUtils.isBlank(menuCommand.getDescription()))
                .map(TelegramMenuCommand::getCommand)
                .collect(Collectors.toSet());

        assertThat(
                "Некоторые команды меню не содержат описаний: " + commandsWithoutDescriptions,
                commandsWithoutDescriptions,
                IsEmptyCollection.empty()
        );
    }

    @Test
    @DisplayName("Сценарии имеют уникальные идентификаторы")
    void testScenarioIdsUnique() {
        TelegramMenuSettings settings = getTelegramMenuSettings();
        List<TelegramMenuScenario> scenarios = settings.getScenarios();
        assertEquals(
                scenarios.size(),
                scenarios.stream().map(TelegramMenuScenario::getScenarioId).distinct().count(),
                "Количество уникальных идентификаторов сценариев не совпадает с их общим количеством"
        );
    }

    @Test
    @DisplayName("Страницы имеют уникальные идентификаторы в рамках сценариев")
    void testScenarioPageIdsUnique() {
        TelegramMenuSettings settings = getTelegramMenuSettings();
        List<TelegramMenuScenario> scenarios = settings.getScenarios();

        for (TelegramMenuScenario scenario : scenarios) {
            List<TelegramMenuScenarioPage> pages = scenario.getPages();
            assertEquals(
                    pages.size(),
                    pages.stream().map(TelegramMenuScenarioPage::getPageId).distinct().count(),
                    "Количество уникальных идентификаторов страниц не совпадает с их общим количеством"
            );
        }
    }

    @Test
    @DisplayName("Все используемые сценарии описаны")
    void testAllUsedScenariosDefined() {
        TelegramMenuSettings settings = getTelegramMenuSettings();
        Set<String> allScenarios = settings.getScenarios().stream()
                .map(TelegramMenuScenario::getScenarioId)
                .collect(Collectors.toSet());

        Set<String> undefinedScenarios = settings.getMenu().stream()
                .map(TelegramMenuCommand::getScenario)
                .filter(menuScenario -> !allScenarios.contains(menuScenario))
                .collect(Collectors.toSet());

        assertThat(
                "Не описаны сценарии: " + undefinedScenarios,
                undefinedScenarios,
                IsEmptyCollection.empty()
        );
    }

    @Test
    @DisplayName("У всех сценариев определена начальная страница")
    void testAllUsedScenariosHaveInitialPage() {
        TelegramMenuSettings settings = getTelegramMenuSettings();
        Set<String> scenariosWithoutInitialPage = settings.getScenarios().stream()
                .filter(scenario -> scenario.getInitialPage() == null)
                .map(TelegramMenuScenario::getScenarioId)
                .collect(Collectors.toSet());

        assertThat(
                "Не заданы начальные страницы сценариев: " + scenariosWithoutInitialPage,
                scenariosWithoutInitialPage,
                IsEmptyCollection.empty()
        );
    }

    @Test
    @DisplayName("Все используемые страницы описаны")
    void testAllUsedScenarioPagesDefined() {
        TelegramMenuSettings settings = getTelegramMenuSettings();
        for (TelegramMenuScenario scenario : settings.getScenarios()) {
            Set<Integer> definedPageIds = scenario.getPages().stream()
                    .map(TelegramMenuScenarioPage::getPageId)
                    .collect(Collectors.toSet());
            Integer initialPage = scenario.getInitialPage();

            assertThat(
                    "Не описана начальная страница: " + initialPage + " в сценарии " + scenario.getScenarioId(),
                    initialPage,
                    IsIn.in(definedPageIds)
            );

            Set<Integer> undefinedPages =
                    scenario.getPages().stream().map(TelegramMenuScenarioPage::getKeyboard)
                            .flatMap(Collection::stream)
                            .flatMap(Collection::stream)
                            .map(TelegramMenuButton::getCommand)
                            .filter(Objects::nonNull)
                            .filter(command -> command.startsWith(CommandType.CHATBOT_MENU_GOTO_PAGE.getText()))
                            .map(command -> command.split(" ")[2])
                            .map(Integer::parseInt)
                            .filter(pageId -> !definedPageIds.contains(pageId))
                            .collect(Collectors.toSet());

            assertThat(
                    "Не описаны страницы: " + undefinedPages + " в сценарии " + scenario.getScenarioId(),
                    undefinedPages,
                    IsEmptyCollection.empty()
            );
        }
    }

    @Nonnull
    private TelegramMenuSettings getTelegramMenuSettings() {
        Optional<TelegramMenuSettings> settings = menuService.getSettings();
        if (settings.isEmpty()) {
            fail("Не удалось загрузить содержимое файла с настройками меню чат-бота");
        }
        return settings.get();
    }
}
