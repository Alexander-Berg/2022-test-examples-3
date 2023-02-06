package ru.yandex.autotests.innerpochta.steps;

import ch.lambdaj.Lambda;
import org.hamcrest.Matchers;
import org.openqa.selenium.interactions.Actions;
import ru.yandex.autotests.innerpochta.annotations.SkipIfFailed;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.ns.pages.folderblocks.CustomFolderBlock;
import ru.yandex.autotests.innerpochta.ns.pages.folderblocks.CustomLabelBlock;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import java.util.List;

import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.selectFirst;
import static com.google.common.collect.ImmutableMap.of;
import static edu.emory.mathcs.backport.java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.matchers.CurrentUrlMatcher.containsInCurrentUrl;
import static ru.yandex.autotests.innerpochta.matchers.MessagePageCountMatcher.messagePagesCount;
import static ru.yandex.autotests.innerpochta.matchers.message.CounterMatchers.customLabelCount;
import static ru.yandex.autotests.innerpochta.matchers.message.CounterMatchers.draftCount;
import static ru.yandex.autotests.innerpochta.matchers.message.CounterMatchers.inboxCount;
import static ru.yandex.autotests.innerpochta.matchers.message.InboxUnreadLabelCounterMatcher.inboxUnreadLabelCount;
import static ru.yandex.autotests.innerpochta.matchers.message.LabelMatcher.shouldSeeLabel;
import static ru.yandex.autotests.innerpochta.matchers.settings.FolderMatchers.customFolderNames;
import static ru.yandex.autotests.innerpochta.matchers.settings.LabelMatcher.customLabelCountOnMessagePage;
import static ru.yandex.autotests.innerpochta.matchers.settings.LabelMatcher.customLabelNameOnMessagePage;
import static ru.yandex.autotests.innerpochta.util.SkipStep.SkipStepMethods.assumeStepCanContinue;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.common.HasTextMatcher.hasText;

@SuppressWarnings({"UnusedReturnValue", "unchecked"})
public class LeftColumnSteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;

    LeftColumnSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    @Step("Должны видеть метку «{0}» в левой колонке")
    public LeftColumnSteps shouldSeeLabelOnHomePage(String labelName) {
        assertThat(
            "Не появились метки",
            webDriverRule,
            withWaitFor(customLabelCountOnMessagePage(greaterThan(0)))
        );
        assertThat(
            "Новая метка имеет неверное название",
            webDriverRule,
            withWaitFor(customLabelNameOnMessagePage(labelName))
        );
        return this;
    }

    @SkipIfFailed
    @Step("Должны видеть папки «{0}» в левой колонке")
    public void shouldSeeFoldersOnMessagePage(String... folderNames) {
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        assumeStepCanContinue(
            "Свернутых папок не обнаружено",
            user.pages().MessagePage().foldersNavigation().expandFoldersList(),
            withWaitFor(hasSize(greaterThan(0)), SECONDS.toMillis(5))
        );
        Lambda.forEach(user.pages().MessagePage().foldersNavigation().expandFoldersList()).click();
        shouldSeeFoldersCountOnHomePage(folderNames.length);
        shouldSeeFoldersWithName(folderNames);
    }

    @Step("Не должны видеть папку: «{0}»")
    public LeftColumnSteps shouldNotSeeFoldersWithName(String... folderNames) {
        assertThat("Не должно быть папки", webDriverRule, withWaitFor(not(customFolderNames(folderNames))));
        return this;
    }

    @Step("Должны видеть папки: «{0}»")
    public LeftColumnSteps shouldSeeFoldersWithName(String... folderNames) {
        user.defaultSteps().shouldSee(
            user.pages().MessagePage().foldersNavigation().customFolders().waitUntil(not(empty())).get(0)
        );
        assertThat("Нет папки с именем", webDriverRule, withWaitFor(customFolderNames(folderNames)));
        return this;
    }

    @Step("Количество пользовательских папок в левой колонке должно быть «{0}»")
    public LeftColumnSteps shouldSeeFoldersCountOnHomePage(int count) {
        assertThat(
            "Ожидалось другое количество папок",
            user.pages().MessagePage().foldersNavigation().customFolders(),
            withWaitFor(hasSize(count))
        );
        return this;
    }

    @Step("Количество меток в правой колонке должно быть «{0}»")
    public LeftColumnSteps shouldSeeLabelCountOnHomePage(int count) {
        assertThat(
            "Некорректное количество меток",
            user.pages().MessagePage().labelsNavigation().userLabels(),
            withWaitFor(hasSize(count))
        );
        return this;
    }

    @Step("Должны существовать пользовательские метки")
    public LeftColumnSteps shouldSeeCustomLabelOnMessagePage() {
        assertThat(
            "Нет пользовательских меток",
            user.pages().MessagePage().labelsNavigation().userLabels(),
            withWaitFor(hasSize(greaterThan(0)))
        );
        return this;
    }

    @Step("Очищаем папку удаленные кликом по «Метле»")
    public LeftColumnSteps clearsTrashFolder() {
        user.defaultSteps().executesJavaScript("document.querySelector('[href=\"#trash\"]')" +
            ".getElementsByClassName('qa-LeftColumn-ClearControl')[0].click()");
        return this;
    }

    @Step("Должны находиться в папке «{0}»")
    public LeftColumnSteps shouldSeeCurrentFolderIs(String folderName) {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation());
        int ls = user.pages().MessagePage().foldersNavigation().allFolders().size();
        for (int i = 0; i < ls; i++) {
            if (
                hasText(containsString(folderName))
                    .matches(user.pages().MessagePage().foldersNavigation().allFolders().get(i))
            ) {
                user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().allFolders().get(i));
                assertThat(
                    "Должны находиться в папке «" + folderName + "»",
                    user.pages().MessagePage().foldersNavigation().allFolders().get(i),
                    withWaitFor(hasClass(containsString("qa-LeftColumn-Folder_selected")))
                );
                return this;
            }
        }
        throw new IllegalStateException(String.format("Can't find folder with name «%s»", folderName));
    }

    @SkipIfFailed
    @Step("Раскрываем папки")
    public LeftColumnSteps expandFoldersIfCan() {
        assumeStepCanContinue(user.pages().MessagePage().foldersNavigation().expandInboxFolders(), isPresent());
        user.defaultSteps().clicksOn(user.pages().MessagePage().foldersNavigation().expandInboxFolders());
        return this;
    }

    @Step("Открываем папку «Отправленные»")
    public LeftColumnSteps opensSentFolder() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().foldersNavigation().sentFolder());
        assertThat(
            "Папка Отправленные не открылась",
            user.pages().MessagePage().foldersNavigation().currentFolder(),
            withWaitFor(hasText(containsString("Отправленные")))
        );
        return this;
    }

    @Step("Должны находиться в папке «{0}»")
    public LeftColumnSteps shouldBeInFolder(String folderName) {
        assertThat(
            "Перешли на неверную папку",
            user.pages().MessagePage().foldersNavigation().currentFolder(),
            withWaitFor(hasText(containsString(folderName)))
        );
        return this;
    }

    @Step("Счётчик папки «Удалённые» должен быть равен «{0}»")
    public LeftColumnSteps shouldSeeTrashFolderCounter(int expectedCounter) {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().trashFolder());
        assertThat(
            "Счетчик удаленных писем отличен от ожидаемого",
            user.pages().MessagePage().foldersNavigation().trashFolderCounter(),
            withWaitFor(allOf(isPresent(), hasText(Integer.toString(expectedCounter))))
        );
        return this;
    }

    @Step("Счетчик непрочитанных писем должен быть равен «{0}»")
    public LeftColumnSteps shouldSeeUnreadCounterIs(int expectedCounter) {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().inboxUnreadCounter());
        assertThat(
            "Счетчик непрочитанных писем отличен от ожидаемого",
            webDriverRule,
            withWaitFor(inboxUnreadLabelCount(expectedCounter))
        );
        return this;
    }

    @Step("Счётчик непрочитанных сообщений должен быть равен «{0}»")
    public LeftColumnSteps shouldSeeInboxUnreadCounter(int expectedCounter) {
        user.pages().MessagePage().foldersNavigation().inboxUnreadCounter().waitUntil(not(empty())).waitUntil(
            "Счетчик непрочитанных писем отличен от ожидаемого",
            hasText(Integer.toString(expectedCounter)),
            30
        );
        return this;
    }

    @Step("Счётчик непрочитанных сообщений в компактной ЛК должен быть равен «{0}»")
    public LeftColumnSteps shouldSeeInboxUnreadCounterCompactLC(int expectedCounter) {
        user.pages().MessagePage().foldersNavigation().inboxFolderLink().waitUntil(not(empty())).waitUntil(
            "Счетчик непрочитанных писем отличен от ожидаемого",
            hasText(Integer.toString(expectedCounter)),
            30
        );
        return this;
    }

    @Step("Счётчик пользовательской папки должен быть равен «{0}»")
    public LeftColumnSteps shouldSeeCustomFolderCounterIs(int expectedCounter) {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().customFolderCounter());
        String counter = user.pages().MessagePage().foldersNavigation().customFolders().get(2).folderCounter().getText()
            .trim().replace("⁄", "");
        assertThat(
            "Счетчик пользовательской папки отличен от ожидаемого",
            counter,
            equalTo(Integer.toString(expectedCounter))
        );
        return this;
    }

    @Step("Счётчик текущей пользовательской папки должен быть равен «{0}»")
    public LeftColumnSteps shouldSeeCurrentCustomFolderCounterIs(String expectedCounter) {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().customFolderCounter());
        String counter = user.pages().MessagePage().foldersNavigation().customFolders().get(2).folderCounter().getText();
        assertThat(
            "Счетчик пользовательской папки отличен от ожидаемого",
            counter,
            equalTo(expectedCounter)
        );
        return this;
    }

    @Step("В папке «{0}» должно быть «{1}» сообщени(й/я/е)")
    public LeftColumnSteps shouldSeeCustomFolderCounter(String folder, int expectedCounter) {
        assertThat(
            "Счетчик пользовательской папки отличен от ожидаемого",
            customFolderCounter(folder),
            equalTo(expectedCounter)
        );
        return this;
    }

    @Step("Счётчик папки «Спам» должен быть равен «{0}»")
    public LeftColumnSteps shouldSeeSpamCounter(int expectedCounter) {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().spamFolder());
        int actualCounter = (isPresent().matches(user.pages().MessagePage().foldersNavigation().spamFolderCounter())) ?
            Integer.parseInt(user.pages().MessagePage().foldersNavigation().spamFolderCounter().getText()
                .replace("⁄", "").trim()) : 0;
        assertThat("Счетчик спама отличен от ожидаемого", actualCounter, withWaitFor(equalTo(expectedCounter)));
        return this;
    }

    @Step("Счётчик папки «Входящие» должен быть равен «{0}»")
    public LeftColumnSteps shouldSeeTotalInboxCounter(int expectedCounter) {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().folderTotalCounter().get(0));
        assertThat("Счетчик писем отличен от ожидаемого", webDriverRule, withWaitFor(inboxCount(expectedCounter)));
        return this;
    }

    @Step("Счётчик новых писем должен быть равен «{0}»")
    public LeftColumnSteps shouldSeeUnreadInboxCounter(int expectedCounter) {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().inboxUnreadCounter());
        assertThat(
            "Счетчик новых писем отличен от ожидаемого",
            Integer.parseInt(user.pages().MessagePage().foldersNavigation().inboxUnreadCounter().getText()),
            withWaitFor(equalTo(expectedCounter))
        );
        return this;
    }

    @Step("Счетчик папки «Черновики» должен равняться «{0}»")
    public LeftColumnSteps shouldSeeDraftCounter(int expectedCounter) {
        assertThat("Счетчик черновиков отличен от ожидаемого", webDriverRule, draftCount(expectedCounter));
        return this;
    }

    @Step("Клик по непрочитанным")
    public LeftColumnSteps clicksOnUnreadMessages() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().msgFiltersBlock().showUnread());
        return this;
    }

    @Step("Счётчик папки «Входящие»")
    public int inboxTotalCounter() {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().folderTotalCounter().get(0));
        return Integer.parseInt(
            (user.pages().MessagePage().foldersNavigation().folderTotalCounter().get(0).getText())
        );
    }

    @Step("Счётчик непрочитанных")
    public int unreadCounter() {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().inboxUnreadCounter());
        return Integer.parseInt(user.pages().MessagePage().foldersNavigation().inboxUnreadCounter().getText());
    }

    @Step("Счётчик папки «Черновики»")
    public int getsDraftCounter() {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().selectedFolderCounter());
        return Integer.parseInt(user.pages().MessagePage().foldersNavigation().selectedFolderCounter().getText());
    }

    @Step("Счётчик папки «Спам»")
    public int getsSpamCounter() {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().spamFolder());
        return (isPresent().matches(user.pages().MessagePage().foldersNavigation().spamFolderCounter())) ?
            Integer.parseInt(user.pages().MessagePage().foldersNavigation().spamFolderCounter()
                .getText().replace("⁄", "").trim()) : 0;
    }

    @Step("Счётчик папки «Удалённые»")
    public int getsTrashCounter() {
        user.defaultSteps().shouldSee(user.pages().MessagePage().foldersNavigation().trashFolder());
        return (isPresent().matches(user.pages().MessagePage().foldersNavigation().trashFolderCounter())) ?
            Integer.parseInt(user.pages().MessagePage().foldersNavigation().trashFolderCounter().getText()) : 0;
    }

    @Step("Счетчик метки «{0}» не должен быть виден")
    public LeftColumnSteps shouldNotSeeCustomLabelCounter(String name) {
        List<CustomLabelBlock> list = filter(
            hasText(Matchers.containsString(name)),
            user.pages().MessagePage().labelsNavigation().userLabels()
        );
        assertThat("Нет нужной метки в листе", list, hasSize(greaterThan(0)));
        assertThat("Счетчик выбранной метки отличен от ожидаемого", list.get(0).labelCounter(), not(isPresent()));
        return this;
    }

    @Step("Клик по метке «{0}»")
    public LeftColumnSteps clickOnCustomLabel(String name) {
        List<CustomLabelBlock> list = filter(
            WebElementMatchers.hasText(Matchers.containsString(name)),
            user.pages().MessagePage().labelsNavigation().userLabels()
        );
        assertThat("Нет нужной метки в листе", list, hasSize(greaterThan(0)));
        new Actions(webDriverRule.getDriver()).click(list.get(0).labelName()).perform();
        return this;
    }

    @Step("Правый клик по метке «{0}»")
    public LeftColumnSteps rightClickOnCustomLabel(String name) {
        List<CustomLabelBlock> list = filter(
            hasText(Matchers.containsString(name)),
            user.pages().MessagePage().labelsNavigation().userLabels()
        );
        assertThat("Нет нужной метки в листе", list, hasSize(greaterThan(0)));
        new Actions(webDriverRule.getDriver()).contextClick(list.get(0).labelName()).perform();
        return this;
    }

    @Step("Количество сообщений в папке «{0}»")
    private int customFolderCounter(String name) {
        final List<CustomFolderBlock> folds = user.pages().MessagePage().foldersNavigation().customFolders();
        List<CustomFolderBlock> list = filter(hasText(Matchers.containsString(name)), folds);
        assertThat("Нет нужной папки", list, hasSize(greaterThan(0)));
        String text = list.get(0).folderCounter().getText().replace("/", "").trim();
        return (text.equals("")) ? 0 : Integer.parseInt(text);
    }

    @Step("Счётчик метки «{0}» должен быть «{1}»")
    public LeftColumnSteps shouldSeeCustomLabelCounter(String name, int expectedCounter) {
        assertThat(
            "Счетчик выбранной метки отличен от ожидаемого",
            webDriverRule,
            withWaitFor(customLabelCount(name, expectedCounter))
        );
        return this;
    }

    @Step("Не должно быть метки «{0}»")
    public LeftColumnSteps shouldNotSeeCustomLabel(String name) {
        assertThat("Не должно быть метки", webDriverRule, withWaitFor(not(shouldSeeLabel(name))));
        return this;
    }

    @Step("Должна быть метка «{0}»")
    public LeftColumnSteps shouldSeeCustomLabel(String... labelNames) {
        asList(labelNames).forEach(labelName ->
            assertThat("Должна быть метка", webDriverRule, withWaitFor(shouldSeeLabel((String) labelName)))
        );
        return this;
    }

    @Step("Открываем папку «Входящие» только новые")
    public LeftColumnSteps opensInboxFolder() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().foldersNavigation().inboxFolder());
        return this;
    }

    @Step("Открываем папку «Спам»")
    public LeftColumnSteps opensSpamFolder() {
        user.defaultSteps().opensFragment(QuickFragments.SPAM);
        assertThat(
            "Папка Спам не открылась",
            user.pages().MessagePage().foldersNavigation().currentFolder(),
            withWaitFor(hasText(containsString("Спам")))
        );
        return this;
    }

    @Step("Открываем папку «Удалённые»")
    public LeftColumnSteps opensTrashFolder() {
        user.defaultSteps().opensFragment(QuickFragments.TRASH);
        assertThat(
            "Папка Удалённые не открылась",
            user.pages().MessagePage().foldersNavigation().currentFolder(),
            withWaitFor(hasText(containsString("Удалённые")))
        );
        return this;
    }

    @Step("Открываем папку с номером «{0}»")
    public LeftColumnSteps opensCustomFolder(int folderIndex) {
        assertThat(
            "Страница с письмами не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)))
        );
        user.defaultSteps().onMouseHoverAndClick(
            user.pages().MessagePage().foldersNavigation().customFolders().get(folderIndex + 2)
        );
        assertThat(
            "Не перешли в пользовательскую папку",
            webDriverRule.getDriver(),
            withWaitFor(containsInCurrentUrl("folder"))
        );
        user.leftColumnSteps().shouldBeInFolder(
            user.pages().MessagePage().foldersNavigation().customFolders().get(folderIndex + 2).customFolderName().getText()
        );
        return this;
    }

    @Step("Клик по папке с именем «{0}»")
    public LeftColumnSteps opensCustomFolder(String name) {
        user.defaultSteps()
            .shouldSeeElementInList(user.pages().MessagePage().foldersNavigation().customFolders(), name);
        CustomFolderBlock folder = getFolderByName(name);
        user.defaultSteps().clicksOn(folder);
        assertThat(
            "Не перешли в пользовательскую папку",
            webDriverRule.getDriver(),
            withWaitFor(containsInCurrentUrl("folder"))
        );
        user.leftColumnSteps().shouldBeInFolder(name);
        return this;
    }

    @Step("Клик по папке с именем «{0}» с включенными табами")
    public LeftColumnSteps opensCustomFolderWithTabs(String name) {
        user.defaultSteps()
            .shouldSeeElementInList(user.pages().MessagePage().foldersNavigation().customUserFolders(), name);
        CustomFolderBlock folder = getFolderByNameWithTabs(name);
        user.defaultSteps().clicksOn(folder);
        assertThat(
            "Не перешли в пользовательскую папку",
            webDriverRule.getDriver(),
            withWaitFor(containsInCurrentUrl("folder"))
        );
        user.leftColumnSteps().shouldBeInFolder(name);
        return this;
    }

    @Step("Правый клик по папке с именем «{0}»")
    public LeftColumnSteps rightClickOnCustomFolder(String name) {
        CustomFolderBlock folder = getFolderByName(name);
        new Actions(webDriverRule.getDriver()).contextClick(folder).perform();
        return this;
    }

    @Step("Правый клик по сборщику в ЛК")
    public LeftColumnSteps rightClickOnCollector() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().foldersNavigation().inboxFolder());
        return this;
    }

    @Step("Проверяем, что папка с нужным именем существует и получаем её как элемент")
    private CustomFolderBlock getFolderByName(String name) {
        assertThat(
            "Страница с письмами не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)))
        );
        CustomFolderBlock folder = selectFirst(
            user.pages().MessagePage().foldersNavigation().customFolders(),
            hasText(Matchers.containsString(name))
        );
        assertThat("Нет элемента с нужным текстом", folder, isPresent());
        return folder;
    }

    @Step("Проверяем, что папка с нужным именем существует и получаем её как элемент (с включенными табами)")
    private CustomFolderBlock getFolderByNameWithTabs(String name) {
        assertThat(
            "Страница с письмами не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)))
        );
        CustomFolderBlock folder = selectFirst(
            user.pages().MessagePage().foldersNavigation().customUserFolders(),
            hasText(Matchers.containsString(name))
        );
        assertThat("Нет элемента с нужным текстом", folder, isPresent());
        return folder;
    }

    @Step("Меняем размер ЛК")
    public void setLeftColumnSize(String size) {
        user.apiSettingsSteps().callWithListAndParams(
            "Устанавливаем размер левой колонки",
            of(SIZE_LAYOUT_LEFT, size)
        );
    }

    @Step("Раскрывем все папки")
    public LeftColumnSteps openFolders() {
        String foldersFids = user.apiFoldersSteps().getAllFids();
        user.apiSettingsSteps().callWithListAndParams(
            "Раскрываем все папки",
            of(FOLDERS_OPEN, foldersFids)
        );
        user.defaultSteps().refreshPage();
        return this;
    }
}
