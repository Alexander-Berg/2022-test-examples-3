package ru.yandex.autotests.innerpochta.steps;

import org.hamcrest.core.IsNot;
import org.openqa.selenium.Keys;
import ru.yandex.autotests.innerpochta.annotations.SkipIfFailed;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import java.util.List;

import static ch.lambdaj.Lambda.filter;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.matchers.FilterActionsMatcher.filterActionsContains;
import static ru.yandex.autotests.innerpochta.matchers.PostRefreshMatcherDecorator.withPostRefresh;
import static ru.yandex.autotests.innerpochta.matchers.settings.FilterMatchers.customFiltersNames;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.SkipStep.SkipStepMethods.assumeStepCanContinue;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

public class FiltersSteps {

    AllureStepStorage user;
    private WebDriverRule webDriverRule;

    FiltersSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    private static final String CONDITION_PATTERN_WITHOUT_SUBJECT = "Если «От кого» совпадает c «%s»";
    private static final String ACTION_PATTERN = "— переместить письмо в папку «%s»";
    private static final String[] FIRST_CONDITIONS = {"От кого", "Кому или копия", "Кому", "Копия",
        "Тема", "Тело письма", "Название вложения", "Заголовок"};
    private static final String[] SECOND_CONDITIONS = {"совпадает c", "не совпадает c", "содержит", "не содержит"};

    private static final String MESSAGE_ADDRESS = "если «От кого» содержит «%s»";
    private static final String MESSAGE_SUBJECT = "«Тема» содержит «%s»";
    private static final int MAX_FILTER_CONDITION_COUNT = 8;

    @Step("Клик по «Создать новый фильтр»")
    public FiltersSteps clicksOnCreateNewFilter() {
        user.defaultSteps().shouldSee(user.pages().FiltersOverviewSettingsPage().createNewFilterButton())
            .clicksOn(user.pages().FiltersOverviewSettingsPage().createNewFilterButton())
            .shouldBeOnUrlWith(QuickFragments.SETTINGS_FILTERS_CREATE);
        return this;
    }

    @Step("Кликаем по кнопке Добавить условие")
    public FiltersSteps clicksOnAddConditionButton(int i) {
        user.defaultSteps().clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions()
            .addConditionButton());
        assertThat("Кнопка 'Добавить условие' не сработала", user.pages().FiltersCreationSettingsPage().setupFiltersCreate()
            .blockCreateConditions().conditionsList(), withWaitFor(hasSize(greaterThan(i))));
        return this;
    }

    @Step("Раскрываем выпадушку - “От кого“ для «{0}» условия")
    public FiltersSteps shouldOpenFromDropdown(int index) {
        user.defaultSteps().clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate()
            .blockCreateConditions().conditionsList().get(index).firstConditionDropDown())
            .shouldSee(user.pages().SettingsPage().selectConditionDropdown());
        return this;
    }

    @Step("Раскрываем выпадушку - “Совпадает с“ для «{0}» условия")
    public FiltersSteps shouldOpenMatchesDropdown(int index) {
        user.defaultSteps().clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate()
            .blockCreateConditions().conditionsList().get(index).secondConditionDropDown())
            .shouldSee(user.pages().SettingsPage().selectConditionDropdown());
        return this;
    }

    @Step("Раскрываем выпадушку меток")
    public FiltersSteps shouldOpenSelectLabelDropdown() {
        user.defaultSteps().turnTrue(user.pages().FiltersCreationSettingsPage().setupFiltersCreate()
            .blockSelectAction().markAsCheckBox())
            .clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockSelectAction().selectLabelDropdown())
            .shouldSee(user.pages().SettingsPage().selectConditionDropdown());
        return this;
    }

    public String createConditionStringOfFilter(String firstIf, String secondIf, String ifText) {
        return "Если\n" + firstIf + " " + secondIf + " " + "«" + ifText + "»";
    }

    @Step("Выбираем произвольное условие для нового фильтра")
    public String chooseRandomIfConditionForNewFilter() {
        String firstIf = chooseFirstRandomIfCondition();
        if (firstIf.contains("Заголовок")) {
            firstIf = inputsTextInHeaderWindow();
        }
        String secondIf = chooseSecondRandomIfCondition();
        String ifText = Utils.getRandomString();
        user.defaultSteps().inputsTextInElement(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions()
            .conditionsList().get(0).inputCondition(), ifText);
        return createConditionStringOfFilter(firstIf, secondIf, ifText);
    }

    @Step("Проверяем корректность данных: Вложения «{0}», Адрес «{1}», Тема «{2}»")
    public FiltersSteps shouldSeeCorrectDataInFilterConfigurationPage(Boolean containsAttachments, String address,
                                                                      String subject) {
        user.defaultSteps().shouldBeOnUrl(containsString("#setup/filters-create/id="));
        if (!(address.equals("") | subject.equals(""))) {
            checkConditionsInCreatedFilter("Тема", "содержит", subject, 0);
            checkConditionsInCreatedFilter("От кого", "содержит", address, 1);
        } else if (!address.equals("")) {
            checkConditionsInCreatedFilter("От кого", "содержит", address, 0);
        } else {
            checkConditionsInCreatedFilter("Тема", "содержит", subject, 0);
        }
        shouldSeeThatMessageMustBeWithAttachments(containsAttachments);
        return this;
    }

    private FiltersSteps checkConditionsInCreatedFilter(String firstCondition, String secondCondition,
                                                        String thirdCondition, int index) {
        user.defaultSteps().shouldContainText(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions()
            .conditionsList().get(index).firstConditionDropDown(), firstCondition)
            .shouldContainText(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(index).secondConditionDropDown(), secondCondition)
            .shouldContainValue(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(index).inputCondition(), thirdCondition);
        return this;
    }

    @Step("shouldSeeThatMessageMustBeWithAttachments(containsAttachments:{0})")
    public FiltersSteps shouldSeeThatMessageMustBeWithAttachments(boolean containsAttachments) {
        if (containsAttachments) {
            user.defaultSteps().shouldContainText(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockApplyConditionFor()
                .withAttachConditionDropdown(), "с вложениями");
        } else {
            user.defaultSteps().shouldContainText(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockApplyConditionFor()
                .withAttachConditionDropdown(), "с вложениями и без вложений");
        }
        return this;
    }

    @Step("Вводим дополнительное условие")
    public FiltersSteps createsAdvancedConditionsForFilter() {
        for (int i = 0; i < MAX_FILTER_CONDITION_COUNT; i++) {
            shouldOpenMatchesDropdown(i);
            user.defaultSteps().clicksOn(user.pages().SettingsPage().selectConditionDropdown().conditionsList().get(i / 2))
                .inputsTextInElement(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions().conditionsList()
                    .get(i).inputCondition(), "text" + i);
            shouldOpenFromDropdown(i);
            user.defaultSteps().clicksOn(user.pages().SettingsPage().selectConditionDropdown().conditionsList().get(i));
            if (i < MAX_FILTER_CONDITION_COUNT - 1) {
                clicksOnAddConditionButton(i);
            }
        }
        user.defaultSteps().clicksOn(user.pages().FiltersOverviewSettingsPage().headerForFilterPopUpBlock().saveHeaderButton());
        return this;
    }

    @Step("Выбираем первое условие")
    public String chooseFirstRandomIfCondition() {
        shouldOpenFromDropdown(0);
        int position = Utils.getRandomNumber(FIRST_CONDITIONS.length - 1, 0);
        String firstIf = FIRST_CONDITIONS[position];
        user.defaultSteps().clicksOn(user.pages().SettingsPage().selectConditionDropdown().conditionsList().get(position));
        return "«" + firstIf + "»";
    }

    @Step("Выбираем второе условие")
    public String chooseSecondRandomIfCondition() {
        user.defaultSteps().clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions());
        shouldOpenMatchesDropdown(0);
        int position = Utils.getRandomNumber(SECOND_CONDITIONS.length - 1, 0);
        String secondIf = SECOND_CONDITIONS[position];
        user.defaultSteps().clicksOn(user.pages().SettingsPage().selectConditionDropdown().conditionsList().get(position));
        return secondIf;
    }

    @Step("Воодим текст в поле - “Заголовок фильтра“")
    public String inputsTextInHeaderWindow() {
        String header = Utils.getRandomString();
        user.defaultSteps().clearTextInput(user.pages().FiltersOverviewSettingsPage().headerForFilterPopUpBlock().headerInbox())
            .inputsTextInElement(user.pages().FiltersOverviewSettingsPage().headerForFilterPopUpBlock().headerInbox(), header)
            .clicksOn(user.pages().FiltersOverviewSettingsPage().headerForFilterPopUpBlock().saveHeaderButton());
        return "заголовок «" + header + "»";
    }

    @Step("Выбираем действие: “Положить в папку“")
    public FiltersSteps chooseMoveToFolderAction() {
        user.defaultSteps().turnTrue(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockSelectAction().moveToFolderCheckBox());
        return this;
    }

    @Step("Создаём метку со станицы фильтра и настраиваем на неё фильтр")
    public String chooseToPutRandomlyCreatedMark() {
        user.defaultSteps().deselects(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockSelectAction().moveToFolderCheckBox());
        String markName = Utils.getRandomString();
        user.defaultSteps().turnTrue(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockSelectAction().markAsCheckBox())
            .clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockSelectAction().selectLabelDropdown())
            .clicksOnElementWithText(user.pages().SettingsPage().selectConditionDropdown().conditionsList(), "Новая метка...")
            .inputsTextInElement(user.pages().FiltersOverviewSettingsPage().newLabelPopUp().markNameInbox(), markName)
            .clicksOn(user.pages().FiltersOverviewSettingsPage().newLabelPopUp().createMarkButton());
        return markName;
    }

    @Step("Создаём папку со станицы фильтра и настраиваем на неё фильтр")
    public String chooseToMoveInRandomlyCreatedFolder() {
        String folderName = Utils.getRandomString();
        user.defaultSteps().clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate()
            .blockSelectAction().selectFolderDropdown())
            .clicksOnElementWithText(user.pages().SettingsPage()
                .selectConditionDropdown().conditionsList(), "Новая папка...")
            .shouldSee(user.pages().FiltersOverviewSettingsPage().newFolderPopUp().folderName());
        user.pages().FiltersOverviewSettingsPage().newFolderPopUp().folderName().sendKeys(folderName);
        user.defaultSteps().clicksOn(user.pages().FiltersOverviewSettingsPage().newFolderPopUp().create());
        return folderName;
    }

    @Step("Сохраняем фильтр")
    public FiltersSteps submitsFilter(Account acc) {
        user.defaultSteps().clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().submitFilterButton());
        confirmsPassword(acc.getPassword());
        user.defaultSteps().shouldNotSee(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().submitFilterButton());
        assertThat("Фильтр не создался", user.pages().FiltersOverviewSettingsPage().createdFilterBlocks(),
            withWaitFor(withPostRefresh(hasSize(greaterThan(0)), webDriverRule.getDriver())));
        return this;
    }

    @Step("Должны видеть условие фильтра в виде: {0}")
    public FiltersSteps shouldSeeSelectedConditionInFilter(String text) {
        assertThat("Фильтр не создался", user.pages().FiltersOverviewSettingsPage().createdFilterBlocks(),
            withWaitFor(withPostRefresh(hasSize(greaterThan(0)), webDriverRule.getDriver())));
        int lastIndex = user.pages().FiltersOverviewSettingsPage().createdFilterBlocks().size() - 1;
        user.defaultSteps().shouldSee(user.pages().FiltersOverviewSettingsPage().createNewFilterButton())
            .shouldSeeThatElementTextEquals(user.pages().FiltersOverviewSettingsPage()
                .createdFilterBlocks().get(lastIndex).сonditionContent(), text);
        return this;
    }

    @Step("Должны видеть условие фильтра с адресом:{0} и темой:{1}")
    public FiltersSteps shouldSeeSelectedConditionInFilter(String address, String subject) {
        String text = createConditionStringOfFilter(address, subject);
        int lastIndex = user.pages().FiltersOverviewSettingsPage().createdFilterBlocks().size() - 1;
        user.defaultSteps().shouldSee(user.pages().FiltersOverviewSettingsPage().createNewFilterButton())
            .shouldSeeThatElementTextEquals(user.pages().FiltersOverviewSettingsPage().createdFilterBlocks()
                .get(lastIndex).сonditionContent(), text);
        return this;
    }


    @Step("Должны видеть действие в правиле фильтра: {0}")
    public FiltersSteps shouldSeeSelectedActionInFilter(String... actions) {
        int lastIndex = user.pages().FiltersOverviewSettingsPage().createdFilterBlocks().size() - 1;
        assertThat("Действие в правиле не соответствует выбранному", user.pages().FiltersOverviewSettingsPage()
            .createdFilterBlocks().get(lastIndex), filterActionsContains(actions));
        return this;
    }

    @Step("Кликаем по кнопке “Удалить фильтр“")
    public FiltersSteps clicksOnDeleteFilter() {
        user.defaultSteps().clicksOn(user.pages().FiltersOverviewSettingsPage().createdFilterBlocks().get(0).deleteFilterButton())
            .shouldSee(user.pages().SettingsPage().popup());
        return this;
    }

    @Step("Кликаем по кнопке создания Новой метки")
    public FiltersSteps clicksOnMoreComplexFilterLink() {
        user.defaultSteps().clicksOn(user.pages().FiltersOverviewSettingsPage().newFilterPopUp().createComplexFilterLink());
        return this;
    }

    @SkipIfFailed
    @Step("Вводим пароль, если нужно")
    public FiltersSteps confirmsPassword(String password) {
        assumeStepCanContinue(
            user.pages().FiltersOverviewSettingsPage().passwordConfirmationBlock(),
            isPresent()
        );
        user.defaultSteps().inputsTextInElement(user.pages().FiltersOverviewSettingsPage().passwordConfirmationBlock()
            .passwordInbox(), password);
        user.defaultSteps().clicksOn(user.pages().FiltersOverviewSettingsPage().passwordConfirmationBlock()
            .submitPasswordButton());
        user.defaultSteps().shouldNotSee(user.pages().FiltersOverviewSettingsPage().passwordConfirmationBlock()
            .submitPasswordButton());
        return this;
    }


    @Step("Пересылать по адресу: «{0}»")
    public FiltersSteps chooseToForwardToAddress(String address) {
        user.defaultSteps().deselects(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockSelectAction()
            .moveToFolderCheckBox())
            .turnTrue(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockPasswordProtectedActions().forwardToCheckBox())
            .inputsTextInElement(user.pages().FiltersCreationSettingsPage().setupFiltersCreate()
                .blockPasswordProtectedActions().forwardToInbox(), address);
        user.hotkeySteps().pressSimpleHotKey(key(Keys.SPACE));
        user.defaultSteps().clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockPasswordProtectedActions()
            .forwardToInbox());
        return this;
    }

    @Step("Уведомлять по адресу: «{0}»")
    public FiltersSteps chooseToNotifyAddress(String address) {
        user.defaultSteps().deselects(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockSelectAction().moveToFolderCheckBox())
            .turnTrue(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockPasswordProtectedActions().notifyToCheckBox())
            .inputsTextInElement(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockPasswordProtectedActions()
                .notifyToInbox(), address);
        return this;
    }

    @Step("Отвечать с текстом: «{0}»")
    public FiltersSteps replyWithText(String text) {
        user.defaultSteps().deselects(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockSelectAction()
            .moveToFolderCheckBox())
            .turnTrue(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockPasswordProtectedActions()
                .replyWithTextCheckBox())
            .inputsTextInElement(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockPasswordProtectedActions()
                .replyWithTextInbox(), text);
        return this;
    }

    @Step("Вводим адрес для простого фильтра")
    public String inputsAddressForNewSimpleFilter() {
        String text = Utils.getRandomString();
        user.defaultSteps().shouldSee(user.pages().FiltersOverviewSettingsPage().createSimpleFilter())
            .inputsTextInElement(user.pages().FiltersOverviewSettingsPage().createSimpleFilter().fromInputBox(), text);
        return text;
    }

    @Step("Вводим тему для простого фильтра")
    public String inputsSubjectForNewSimpleFilter() {
        String text = Utils.getRandomString();
        user.defaultSteps().shouldSee(user.pages().FiltersOverviewSettingsPage().createSimpleFilter())
            .inputsTextInElement(user.pages().FiltersOverviewSettingsPage().createSimpleFilter().subjectInputBox(), text);
        return text;
    }

    @Step("Сохраняем фильтр")
    public FiltersSteps submitsSimpleFilter() {
        user.defaultSteps().clicksOn(user.pages().FiltersOverviewSettingsPage().createSimpleFilter().submitFilterButton());
        return this;
    }

    @Step("Выбираем метку для фильтра: “{0}“")
    public FiltersSteps selectsLabelForFilter(String label) {
        user.defaultSteps().shouldSee(user.pages().FiltersOverviewSettingsPage().createSimpleFilter().selectLabelDropdown())
            .clicksOn(user.pages().FiltersOverviewSettingsPage().createSimpleFilter().selectLabelDropdown());
        List<MailElement> list = filter(hasText(label), user.pages().SettingsPage().selectConditionDropdown().conditionsList());
        assertThat("Нет нужной метки", list, hasSize(greaterThan(0)));
        user.defaultSteps().clicksOn(list.get(0));
        return this;
    }

    @Step("Выбираем папку: “{0}“")
    public FiltersSteps selectsFolderForFilter(String folder) {
        user.defaultSteps().clicksOn(user.pages().FiltersOverviewSettingsPage().createSimpleFilter().folderSelect())
            .clicksOnElementWithText(user.pages().SettingsPage().selectConditionDropdown().conditionsList(), folder);
        return this;
    }

    @Step("Сохранить фильтр")
    public FiltersSteps submitsSimpleFilterFromPopUp() {
        user.defaultSteps().clicksOn(user.pages().FiltersOverviewSettingsPage().newFilterPopUp().submitFilterButton());
        return this;
    }

    @Step("Проверяем правильность созданного фильтра")
    public FiltersSteps shouldSeeMessageThatFilterForLabelIsReady(String address, String subject, String labelName) {
        user.defaultSteps().shouldNotSee(user.pages().HomePage().putMarkAutomaticallyButton())
            .shouldSee(user.pages().FiltersOverviewSettingsPage().messageAboutNewFilterForLabel());
        assertThat(
            "Неверный текст сообщения",
            user.pages().FiltersOverviewSettingsPage().messageAboutNewFilterForLabel().getText(),
            allOf(
                containsString(String.format("Писем с меткой «%s» пока нет", labelName)),
                containsString("Эта метка будет присваиваться письмам по вашим правилам:"),
                containsString(String.format("«Тема» содержит «%s»", subject)),
                containsString(String.format("«От кого» содержит «%s»", address))
            )
        );
        return this;
    }

    @Step("Меняем имя фильтра на “{0}“")
    public FiltersSteps changesFilterName(String name) {
        user.defaultSteps().clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().filterName())
            .clearTextInput(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().filterNameInput())
            .inputsTextInElement(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().filterNameInput(), name);
        return this;
    }

    @Step("Должны видеть имя фильтра: “{0}“")
    public void shouldSeeCorrectFilterName(String name) {
        user.defaultSteps().shouldSee(user.pages().FiltersOverviewSettingsPage().createdFilterBlocks().get(0))
            .shouldSeeThatElementTextEquals(
                user.pages().FiltersOverviewSettingsPage().createdFilterBlocks().get(0).filterName(),
                name
            );
    }

    @Step("«Не применять остальные правила» должен быть включен")
    public FiltersSteps shouldSeeDoNotApplyOtherFiltersCheckBoxEnabled() {
        user.defaultSteps().shouldBeSelected(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().dontApplyAnyOtherFilter());
        return this;
    }

    @Step("Выбор из первой группы условий")
    public String selectsGroupOfMessagesFromFirstDropBox() {
        user.defaultSteps().clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate()
            .blockApplyConditionFor().letterTypeConditionDropdown())
            .shouldSee(user.pages().SettingsPage().selectConditionDropdown())
            .clicksOn(user.pages().SettingsPage().selectConditionDropdown()
                .conditionsList().get(Utils.getRandomNumber(2, 0)));
        return user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockApplyConditionFor().letterTypeConditionDropdown()
            .getText();
    }

    @Step("Выбор из второй группы условий")
    public String selectsGroupOfMessagesFromSecondDropBox() {
        user.defaultSteps().clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate()
            .blockApplyConditionFor().withAttachConditionDropdown())
            .shouldSee(user.pages().SettingsPage().selectConditionDropdown())
            .clicksOn(user.pages().SettingsPage().selectConditionDropdown()
                .conditionsList().get(Utils.getRandomNumber(2, 0)));
        return user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockApplyConditionFor().withAttachConditionDropdown()
            .getText();
    }

    @Step("Должны видеть, что фильтр применяется к группам: «{0}», «{1}»")
    public FiltersSteps shouldSeeOptionsSelected(String firstGroup, String secondGroup) {
        assertThat("Условия фильтра некорректно сохранились", user.pages().FiltersCreationSettingsPage().setupFiltersCreate()
            .blockApplyConditionFor().letterTypeConditionDropdown(), WebElementMatchers.hasText(firstGroup));
        assertThat("Условия фильтра некорректно сохранились", user.pages().FiltersCreationSettingsPage().setupFiltersCreate()
            .blockApplyConditionFor().withAttachConditionDropdown(), WebElementMatchers.hasText(secondGroup));
        return this;
    }

    @Step("Выбираем логику фильтра: «хотя бы одно/все одновременно»")
    public FiltersSteps selectsLogicOfFilter(int index) {
        user.defaultSteps().clicksOn(user.pages().FiltersCreationSettingsPage().setupFiltersCreate().blockCreateConditions().selectLogicButton())
            .clicksOn(user.pages().SettingsPage().selectConditionDropdown().conditionsList().get(index));
        return this;
    }

    @Step("Должны видеть следующие условия фильра: “{0}“")
    public FiltersSteps shouldSeeLogicForFilterConditions(String logic) {
        assertThat("Неверная логика применения условий фильтра", user.pages().FiltersCreationSettingsPage().setupFiltersCreate()
            .blockCreateConditions().selectLogicButton(), WebElementMatchers.hasText(logic));
        return this;
    }

    @Step("Клик по фильру номер “{0}“")
    public FiltersSteps clicksOnFilter(int index) {
        user.defaultSteps().clicksOn(user.pages().FiltersOverviewSettingsPage().createdFilterBlocks().waitUntil(
            IsNot.not(empty())).get(index));
        return this;
    }

    @Step("Должны видеть, что фильтр {0} подтверждён")
    public FiltersSteps shouldSeeThatFilterIsConfirmed(int i) {
        assertThat("Фильтр не подтвержден", user.pages().FiltersOverviewSettingsPage().createdFilterBlocks().get(i)
            .filterIsDisabled(), withWaitFor(withPostRefresh(not(isPresent()), webDriverRule.getDriver())));
        return this;
    }

    @Step("Должны видеть, что фильтр {0} ждёт подтверждения")
    public FiltersSteps shouldSeeThatFilterIsWaitingForConfirmation(int i) {
        assertThat("Фильтр уже подтвержден", user.pages().FiltersOverviewSettingsPage().createdFilterBlocks().get(i)
            .filterIsDisabled(), withWaitFor(withPostRefresh(isPresent(), webDriverRule.getDriver())));
        return this;
    }

    private String createMessageString(String address, String subject, String labelName) {
        String message = String.format("Писем с меткой «%s» пока нет\n", labelName) +
            "Эта метка будет присваиваться письмам по вашим правилам:";
        return new StringBuilder(message).append("\n—\n").append(String.format(MESSAGE_ADDRESS, address))
            .append("\nи ").append(String.format(MESSAGE_SUBJECT, subject)).toString();
    }

    private String createConditionStringOfFilter(String address, String subject) {
        String text = "Если\n«Тема» содержит «" + subject + "»\n" +
            "и\n«От кого» содержит «" + address + "»";
        if (isEmpty(subject)) {
            text = "Если\n«От кого» содержит «" + address + "»";
        } else if (isEmpty(address)) {
            text = "Если\n«Тема» содержит «" + subject + "»";
        }
        return text;
    }

    @Step("Не должны видеть фильтр: «{0}»")
    public void shouldNotSeeCreatedFilter(String... filtersName) {
        assertThat("Не должно быть фильтра", webDriverRule, withWaitFor(IsNot.not(customFiltersNames(filtersName))));
    }
}
