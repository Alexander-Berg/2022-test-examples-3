package ru.yandex.autotests.innerpochta.steps;

import gumi.builders.UrlBuilder;
import ru.yandex.autotests.innerpochta.annotations.SkipIfFailed;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.data.Select;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.elements.CustomFolderBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.right.UserAddressesBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.signature.SignatureToolbarBlock;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.forEach;
import static ch.lambdaj.Lambda.having;
import static edu.emory.mathcs.backport.java.util.Arrays.asList;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static ru.yandex.autotests.innerpochta.matchers.CollectorCountMatcher.collectorCount;
import static ru.yandex.autotests.innerpochta.matchers.UriMatcher.urlShould;
import static ru.yandex.autotests.innerpochta.matchers.settings.AbookGroupMatcher.abookGroup;
import static ru.yandex.autotests.innerpochta.matchers.settings.AbookGroupMatcher.withName;
import static ru.yandex.autotests.innerpochta.matchers.settings.FolderMatchers.customFolder;
import static ru.yandex.autotests.innerpochta.matchers.settings.FolderMatchers.customFolderCount;
import static ru.yandex.autotests.innerpochta.matchers.settings.FolderMatchers.inboxFolderCount;
import static ru.yandex.autotests.innerpochta.matchers.settings.FolderMatchers.withFolderName;
import static ru.yandex.autotests.innerpochta.matchers.settings.FolderMatchers.withFolderNameAndCount;
import static ru.yandex.autotests.innerpochta.matchers.settings.FoldersCountMatcher.foldersCount;
import static ru.yandex.autotests.innerpochta.matchers.settings.LabelMatcher.customLabelWithName;
import static ru.yandex.autotests.innerpochta.matchers.settings.SignaturesMatcher.hasSignaturesCount;
import static ru.yandex.autotests.innerpochta.matchers.settings.SignaturesMatcher.signaturesCountOnPage;
import static ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe.IFRAME_SUBS_LIZA;
import static ru.yandex.autotests.innerpochta.util.SkipStep.SkipStepMethods.assumeStepCanContinue;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.matchers.webdriver.AttributeMatcher.title;

@SuppressWarnings({"unused", "UnusedReturnValue", "unchecked"})
public class SettingsPageSteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;

    SettingsPageSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    //---------------------------------------------------------
    // SETTINGS PAGE STEPS
    //---------------------------------------------------------

    @Step("Выбираем (element:{0}, index:{1})")
    public SettingsPageSteps select(Select element, int index) {
        element.getOptionByNumber(index).click();
        return this;
    }

    @Step("Должны видеть кнопку “Создать правило“.")
    public SettingsPageSteps clicksOnRulesOfFilteringLink() {
        user.defaultSteps().clicksOn(user.pages().SettingsPage().blockSettingsNav().filtersSetupLink())
            .shouldSee(user.pages().FiltersOverviewSettingsPage().createNewFilterButton());
        return this;
    }

    @Step("Должны видеть выбранный часовой пояс(timeZone:{0}).")
    public SettingsPageSteps shouldSeeSelectedTimeZone(String timeZone) {
        user.defaultSteps().refreshPage()
            .shouldSee(user.pages().SettingsPage().blockSettingsNav().selectTime());
        assertThat(
            "Часовой пояс выставился неверно!",
            user.pages().SettingsPage().blockSettingsNav().selectTime(),
            title(timeZone)
        );
        return this;
    }

    @Step("Кликаем на ссылку «Узнать о преимуществах IMAP»")
    public SettingsPageSteps clicksOnShowImapAdvantages() {
        assertThat(
            "Ссылка «Узнать о преимуществах IMAP» не найдена!",
            user.pages().MailClientsSettingsPage().blockSetupClients().imap().imapAdvantages().showAdvantagesLink(),
            withWaitFor(isDisplayed())
        );
        user.defaultSteps().clicksOn(
                user.pages().MailClientsSettingsPage().blockSetupClients().imap().imapAdvantages().showAdvantagesLink()
            )
            .shouldSee(
                user.pages().MailClientsSettingsPage().blockSetupClients().imap().imapAdvantages().selectIMAPLink()
            );
        return this;
    }

    @Step("Должны видеть доп. настройки для POP3")
    public SettingsPageSteps shouldSeePop3Settings() {
        assertThat(
            "Дополнительные настройки POP3 не появились!",
            user.pages().MailClientsSettingsPage().blockSetupClients().pop3().enableAllCheckboxes(),
            withWaitFor(isDisplayed())
        );
        return this;
    }

    @Step("Должен появиться попап")
    public SettingsPageSteps shouldSeeSettingsPopup() {
        assertThat("Попап не появился!", user.pages().SettingsPage().popup(), withWaitFor(isDisplayed()));
        return this;
    }

    //---------------------------------------------------------
    //-sender info
    //---------------------------------------------------------

    @Step("Нажимаем на кнопку редактирования подписи")
    public SettingsPageSteps clicksOnEditSignature() {
        user.defaultSteps().onMouseHoverAndClick(
                user.pages().SenderInfoSettingsPage().blockSetupSender().signatures().editSignature()
            )
            .shouldSee(user.pages().SenderInfoSettingsPage().blockSetupSender().signatures().saveSignature());
        return this;
    }

    @Step("Очищаем текст подписи")
    public SettingsPageSteps clearsSignatureText(MailElement signatureForm) {
        user.defaultSteps().clicksOn(signatureForm)
            .clearTextInput(signatureForm);
        return this;
    }

    @Step("Изменяем текст подписи на «{0}»")
    public SettingsPageSteps editSignatureValue(String value, MailElement signatureForm) {
        user.defaultSteps().clicksOn(signatureForm)
            .inputsTextInElement(signatureForm, value);
        return this;
    }

    @Step("Вводим имя отправителя: «{0}»")
    public SettingsPageSteps entersSenderName(String name) {
        user.defaultSteps().clearTextInput(user.pages().SenderInfoSettingsPage().blockSetupSender().fromName())
            .inputsTextInElement(user.pages().SenderInfoSettingsPage().blockSetupSender().fromName(), name);
        return this;
    }

    @Step("Выбираем дефолтный адрес для пользователя")
    public String selectsDefaultEmailAddress(int aliasIndex) {
        user.defaultSteps().clicksOn(
            user.pages().SenderInfoSettingsPage().blockSetupSender().blockAliases().domainsList().get(0)
        );
        String text = user.pages().SettingsPage().selectConditionDropdown().conditionsList().get(aliasIndex).getText();
        user.defaultSteps().clicksOn(
            user.pages().SettingsPage().selectConditionDropdown().conditionsList().get(aliasIndex)
        );
        return text;
    }

    @Step("Выбираем адрес номер «{0}» из списка адресов для юзера")
    public String selectsEmailAddressFromAlternatives(int collectorIndex) {
        user.defaultSteps().onMouseHoverAndClick(
            user.pages().SenderInfoSettingsPage().blockSetupSender().blockAliases().logins().get(collectorIndex)
        );
        return user.pages().SenderInfoSettingsPage().blockSetupSender().blockAliases().logins().get(collectorIndex)
            .getAttribute("value");
    }

    @Step("Меняем алиас для адреса:{0}, домен:{1})")
    public SettingsPageSteps changeAliasForAddress(String address, String domain) {
        List<UserAddressesBlock> list = filter(
            having(ch.lambdaj.Lambda.on(UserAddressesBlock.class).address(), hasText(address)),
            user.pages().SenderInfoSettingsPage().blockSetupSender().blockAliases().userAdresses()
        );
        UserAddressesBlock firstAddress = list.get(0);
        user.defaultSteps().clicksOn(firstAddress.address())
            .clicksOn(firstAddress.domain())
            .clicksOnElementWithText(user.pages().SettingsPage().selectConditionDropdown().conditionsList(), domain)
            .clicksIfCanOn(user.pages().SenderInfoSettingsPage().blockSetupSender().saveButton());
        return this;
    }

    //---------------------------------------------------------
    //-other page
    //---------------------------------------------------------
    @Step("Выставляем количество отображаемых сообщений на странице: «{0}»")
    public SettingsPageSteps entersMessagesPerPageCount(String count) {
        user.defaultSteps().clearTextInput(
                user.pages().OtherSettingsPage().blockSetupOther().topPanel().messagesPerPage())
            .inputsTextInElement(
                user.pages().OtherSettingsPage().blockSetupOther().topPanel().messagesPerPage(),
                count
            );
        return this;
    }

    private SettingsPageSteps entersMessagesPerPageCount(Integer count) {
        entersMessagesPerPageCount(count.toString());
        return this;
    }

    @Step("Кликаем на Вопросик справа от «Использовать горячие клавиши»")
    public SettingsPageSteps clicksOnHotKeysInfo() {
        assertThat(
            "«Вопросик» для горячих клавиш не появился!",
            user.pages().OtherSettingsPage().blockSetupOther().topPanel().hotKeysInfo(),
            withWaitFor(isDisplayed())
        );
        user.defaultSteps().clicksOn(user.pages().OtherSettingsPage().blockSetupOther().topPanel().hotKeysInfo());
        shouldSeeSettingsPopup();
        return this;
    }

    @SkipIfFailed
    @Step("Сохраняем настройки")
    public SettingsPageSteps saveTopPanelChangesOnOtherSetup() {
        assumeStepCanContinue(
            user.pages().OtherSettingsPage().blockSetupOther().topPanel().saveButton(),
            isPresent()
        );
        user.defaultSteps().clicksOn(user.pages().OtherSettingsPage().blockSetupOther().topPanel().saveButton())
            .shouldNotSee(user.pages().OtherSettingsPage().blockSetupOther().topPanel().saveButton());
        return this;
    }

    @SkipIfFailed
    @Step("Сохраняем настройки абука")
    public SettingsPageSteps saveAbookSetup() {
        assumeStepCanContinue(
            user.pages().AbookSettingsPage().blockSetupAbook().importExportView().saveChangeButton(),
            isPresent()
        );
        user.defaultSteps().clicksOn(
            user.pages().AbookSettingsPage().blockSetupAbook().importExportView().saveChangeButton()
        );
        return this;
    }

    @SkipIfFailed
    @Step("Сохраняем изменения")
    private SettingsPageSteps saveBottomPanelChangesOnOtherSetup() {
        assumeStepCanContinue(
            user.pages().OtherSettingsPage().blockSetupOther().bottomPanel().save(),
            isPresent()
        );
        user.defaultSteps().clicksOn(user.pages().OtherSettingsPage().blockSetupOther().bottomPanel().save())
            .shouldNotSee(user.pages().OtherSettingsPage().blockSetupOther().bottomPanel().save());
        return this;
    }

    @Step("Сохраняем настройки")
    public SettingsPageSteps saveOtherSettingsSetup() {
        saveTopPanelChangesOnOtherSetup();
        saveBottomPanelChangesOnOtherSetup();
        return this;
    }

    @Step("Выставляем количество отображаемых сообщений на странице: «{0}»")
    public int changesMsgPerPageTo(int count) {
        int oldCount = Integer.parseInt(
            user.pages().OtherSettingsPage().blockSetupOther().topPanel().messagesPerPage().getAttribute("value")
        );
        entersMessagesPerPageCount(count);
        return oldCount;
    }

    @Step("Переходим в инбокс и сохраняем настройки, если надо")
    public SettingsPageSteps saveSettingsIfCanAndClicksOn(MailElement element) {
        user.defaultSteps().clicksOn(element);
        if (isPresent().matches(user.pages().SettingsPage().saveSettingsPopUp().saveAndContinueBtn())) {
            user.defaultSteps().clicksOn(user.pages().SettingsPage().saveSettingsPopUp().saveAndContinueBtn());
        }
        return this;
    }

    @SkipIfFailed
    @Step("Сохраняем настройки, если надо")
    public SettingsPageSteps saveSettingsIfCan() {
        assumeStepCanContinue(
            user.pages().AbookSettingsPage().blockSetupAbook().importExportView().saveChangeButton(),
            isPresent()
        );
        user.defaultSteps()
            .clicksOn(user.pages().AbookSettingsPage().blockSetupAbook().importExportView().saveChangeButton())
            .shouldNotSee(
                user.pages().AbookSettingsPage().blockSetupAbook().importExportView().saveChangeButton()
            );
        return this;
    }

    //Folders and Marks page

    @Step("Удаляем все пользовательские папки")
    public SettingsPageSteps deletesAllCustomFolders() {
        while (isPresent().matches(
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().blockCreatedFolders())) {
            user.defaultSteps().clicksIfCanOn(
                    user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().blockCreatedFolders()
                        .customFolders().get(0).info()
                )
                .clicksIfCanOn(
                    user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().deleteCustomFolder()
                );
            if (isPresent().matches(user.pages().FoldersAndLabelsSettingPage().deleteFolderPopUp().confirmDelete())) {
                user.defaultSteps().clicksOn(
                    user.pages().FoldersAndLabelsSettingPage().deleteFolderPopUp().confirmDelete()
                );
            }
            user.defaultSteps().refreshPage();
        }
        return this;
    }

    @Step("Удаляем все пользовательские метки")
    public SettingsPageSteps deleteAllLabels() {
        assertThat(
            "Страница настроек меток не загрузилась",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels(),
            withWaitFor(Utils.isPresent())
        );
        int size = user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().size();
        int i = 0;
        while (!user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().isEmpty()) {
            user.defaultSteps()
                .clicksOn(
                    user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().get(0)
                )
                .clicksOn(user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().deleteLabel());
            if (isPresent().matches(user.pages().FoldersAndLabelsSettingPage().deleteLabelPopUp().deleteBtn())) {
                user.defaultSteps().clicksOn(user.pages().FoldersAndLabelsSettingPage().deleteLabelPopUp().deleteBtn());
            }
            shouldSeeLabelsCount(size - 1 - i++);
        }
        return this;
    }

    @Step("Раскрываем плюсики, если надо")
    public SettingsPageSteps openThreadIfCan() {
        if (withWaitFor(hasSize(greaterThan(0))).matches(
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().blockCreatedFolders().closedTogglers()
        )) {
            forEach(
                user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().blockCreatedFolders().closedTogglers()
            ).click();
        }
        return this;
    }

    @Step("Должны видеть правильную сортировку меток по имени «labelA, labelB, labelC, labelD»")
    public SettingsPageSteps shouldSeeLabelsSortedByName() {
        assertThat(
            "Метки неверно отсортировались",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().get(0).getText(),
            containsString("LabelA")
        );
        assertThat(
            "Метки неверно отсортировались",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().get(1).getText(),
            containsString("LabelB")
        );
        assertThat(
            "Метки неверно отсортировались",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().get(2).getText(),
            containsString("LabelC")
        );
        assertThat(
            "Метки неверно отсортировались",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().get(3).getText(),
            containsString("LabelD")
        );
        return this;
    }

    @Step("Должны видеть правильную сортировку меток по счётчику «labelD, labelA, labelB, labelC»")
    public SettingsPageSteps shouldSeeLabelsSortedByCount() {
        assertThat(
            "Метки неверно отсортировались",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().get(3).getText(),
            containsString("LabelC")
        );
        assertThat(
            "Метки неверно отсортировались",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().get(2).getText(),
            containsString("LabelB")
        );
        assertThat(
            "Метки неверно отсортировались",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().get(1).getText(),
            containsString("LabelA")
        );
        assertThat(
            "Метки неверно отсортировались",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().get(0).getText(),
            containsString("LabelD")
        );
        return this;
    }

    @Step("Должны видеть блок подтверждения для метки «{0}»")
    public SettingsPageSteps shouldSeeConfirmationPopUp(String label) {
        assertThat(
            "Блока подтверждения удаления метки не появилось",
            user.pages().FoldersAndLabelsSettingPage().deleteLabelPopUpOld().deleteBtnOld(),
            withWaitFor(isPresent())
        );
        assertThat(
            "Неверный текст в блоке подтверждения удаления метки",
            user.pages().FoldersAndLabelsSettingPage().deleteLabelPopUpOld(),
            hasText(containsString("Вы действительно хотите удалить метку «" + label + "» (содержит одно письмо)?"))
        );
        return this;
    }

    @Step("Счётчик папки «{0}» должен быть равен «{1}»")
    public SettingsPageSteps shouldSeeMessageCountInFolder(String folderName, String count) {
        assertThat(
            format("Не нашли папку с именем «%s» и счётчиком «%s»", folderName, count),
            webDriverRule,
            withWaitFor(customFolder(withFolderNameAndCount(folderName, count)))
        );
        return this;
    }

    @Step("В папке входящие должно быть {0} писем")
    public SettingsPageSteps shouldSeeMessageCountInInbox(String count) {
        assertThat("Неверное количество писем в папке", webDriverRule, withWaitFor(inboxFolderCount(is(count))));
        return this;
    }

    @Step("Кликаем по кнопке “Новая папка“")
    public SettingsPageSteps clicksOnCreateNewFolder() {
        user.defaultSteps().clicksOn(
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().newFolderButton()
        );
        shouldSeeSettingsPopup();
        return this;
    }

    @Step("Кликаем по кнопке “Новая метка“")
    public SettingsPageSteps clicksOnCreateNewLabel() {
        user.defaultSteps().clicksOn(user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().newLabel());
        shouldSeeSettingsPopup();
        return this;
    }

    @Step("Кликаем по кнопке “Удалить папку“")
    public SettingsPageSteps clicksOnDeleteFolder() {
        user.defaultSteps().clicksOn(
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().deleteCustomFolder()
        );
        return this;
    }

    @Step("Переименовываем первую метку в списке меток")
    public String renameLabel() {
        String name = Utils.getRandomString();
        clicksOnLabel();
        user.defaultSteps().clicksOn(user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().changeLabel());
        inputsLabelName(name);
        user.defaultSteps().clicksOn(user.pages().FoldersAndLabelsSettingPage().newLabelPopUp().createMarkButton());
        return name;
    }

    @Step("Переименовываем метку с именем {0} в списке меток")
    public String renameLabelWithName(String labelName) {
        String name = Utils.getRandomString();
        clicksOnLabelWithName(labelName);
        user.defaultSteps().clicksOn(user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().changeLabel());
        inputsLabelName(name);
        user.defaultSteps().clicksOn(user.pages().FoldersAndLabelsSettingPage().newLabelPopUp().createMarkButton());
        return name;
    }

    @Step("Переименовываем первую папку в списке папок")
    public String renamesFolder() {
        String name = "{}!@#$%^&*()-=";
        clicksOnFolder();
        user.defaultSteps().clicksOn(
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().renameFolderButton()
        );
        inputsFoldersName(name);
        user.defaultSteps().clicksOn(user.pages().FoldersAndLabelsSettingPage().newFolderPopUp().create());
        return name;
    }

    public String renamesFolder(String folder) {
        return renamesFolder(folder, Utils.getRandomString());
    }

    @Step("Переименовываем папку «{0}» на «{1}»")
    public String renamesFolder(String folder, String name) {
        clicksOnFolder(folder);
        user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().renameFolderButton().click();
        inputsFoldersName(name);
        user.defaultSteps().clicksOn(user.pages().FoldersAndLabelsSettingPage().newFolderPopUp().create());
        return name;
    }

    @Step("Должны видеть группу с именем «{0}»")
    public SettingsPageSteps shouldSeeGroupWithName(String name) {
        assertThat("Не нашли группу с именем", webDriverRule, withWaitFor(abookGroup(withName(name))));
        return this;
    }

    @SkipIfFailed
    @Step("Удаляем группы")
    public SettingsPageSteps deletesContactsGroup() {
        assumeStepCanContinue(
            user.pages().AbookSettingsPage().blockSetupAbook().groupsManage().createdGroups(),
            hasSize(greaterThan(0))
        );
        int size = user.pages().AbookSettingsPage().blockSetupAbook().groupsManage().createdGroups().size();
        for (int i = 0; i < size; i++) {
            user.defaultSteps().clicksOn(
                    user.pages().AbookSettingsPage().blockSetupAbook().groupsManage().createdGroups().get(0)
                )
                .clicksOn(user.pages().AbookSettingsPage().blockSetupAbook().groupsManage().deleteGroupButton());
        }
        return this;
    }

    @Step("Вводим текст с форматированием в инпут редактирования подписи")
    public SettingsPageSteps inputsSignatureWithFormatting(String text) {
        user.defaultSteps().clearTextInput(
                user.pages().SenderInfoSettingsPage().blockSetupSender().signatures().input().get(1)
            )
            .clicksOn(user.pages().SenderInfoSettingsPage().blockSetupSender().signatures().fontSelect().get(1))
            .clicksOn(user.pages().SenderInfoSettingsPage().signatureTextFormatItems().get(1))
            .appendTextInElement(
                user.pages().SenderInfoSettingsPage().blockSetupSender().signatures().input().get(1),
                text
            );
        return this;
    }

    @Step("Создаём новую папку с именем «{0}»")
    public SettingsPageSteps createNewFolder(String folderName) {
        assertThat(
            "Не загрузилась страница настройки папок",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().foldersNames(),
            withWaitFor(hasSize(greaterThan(0)))
        );
        user.defaultSteps().shouldSee(
                user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().newFolderButton()
            )
            .clicksOn(user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().newFolderButton())
            .shouldSee(user.pages().FoldersAndLabelsSettingPage().newFolderPopUp())
            .inputsTextInElement(user.pages().FoldersAndLabelsSettingPage().newFolderPopUp().folderName(), folderName)
            .clicksOn(user.pages().FoldersAndLabelsSettingPage().newFolderPopUp().create());
        assertThat(
            "Не создалась папка",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().blockCreatedFolders().customFolders(),
            withWaitFor(hasSize(greaterThan(0)))
        );
        return this;
    }

    @SkipIfFailed
    @Step("Создаём метку «{0}» если её ещё нет")
    public SettingsPageSteps createsNewLabelIfNeed(String label) {
        List<MailElement> list = filter(
            hasText(label),
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList()
        );
        assumeStepCanContinue(list, hasSize(0));
        clicksOnCreateNewLabel();
        inputsLabelName(label);
        user.pages().FoldersAndLabelsSettingPage().newLabelPopUp().createMarkButton().click();
        return this;
    }

    @Step("Кликаем на кнопку «Создать папку» в попапе")
    public SettingsPageSteps clicksOnSubmitFolder() {
        user.defaultSteps().clicksOn(user.pages().FoldersAndLabelsSettingPage().newFolderPopUp().create());
        return this;
    }

    @Step("Сообщение о доступности соединения")
    public boolean isVisibleSuccessfulConnectionNotify() {
        return withWaitFor(isPresent()).matches(user.pages().CollectorSettingsPage().blockSetup().blockServerSetup()
            .successfulConnectionNotification());
    }

    @Step("Вводим имя папки «{0}»")
    public SettingsPageSteps inputsFoldersName(String folderName) {
        user.defaultSteps().shouldSee(user.pages().FoldersAndLabelsSettingPage().newFolderPopUp())
            .clearTextInput(user.pages().FoldersAndLabelsSettingPage().newFolderPopUp().folderName())
            .inputsTextInElement(user.pages().FoldersAndLabelsSettingPage().newFolderPopUp().folderName(), folderName);
        return this;
    }

    @Step("Должны видеть, что создалась метка «{0}»")
    public SettingsPageSteps shouldSeeLabelCreated(String labelName) {
        user.defaultSteps().shouldSee(
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().waitUntil(not(empty()))
                .get(0)
        );
        assertThat(
            "Новая метка не появилась на странице настроек",
            webDriverRule,
            withWaitFor(customLabelWithName(labelName))
        );
        return this;
    }

    @Step("Папка «{0}» должна иметь подпапку «{1}»")
    public SettingsPageSteps shouldSeeParentHasSubfolder(String folderName, String subFolderName) {
        assertThat(
            "Родительский каталог называется неверно",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().blockCreatedFolders()
                .customFoldersThreads().get(0).name(),
            withWaitFor(allOf(exists(), hasText(folderName)))
        );
        assertThat(
            "Вложенный каталог не создался",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().blockCreatedFolders()
                .customFoldersThreads().get(0).customFolders().get(1),
            withWaitFor(exists())
        );
        assertThat(
            "Вложенный каталог называется неверно",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().blockCreatedFolders()
                .customFoldersThreads().get(0).customFolders().get(1).name(),
            withWaitFor(hasText(containsString(subFolderName)))
        );
        return this;
    }

    @Step("Вводим имя метки: «{0}»")
    public SettingsPageSteps inputsLabelName(String name) {
        user.defaultSteps().shouldSee(user.pages().FoldersAndLabelsSettingPage().newLabelPopUp().markNameInbox())
            .clicksOn(user.pages().FoldersAndLabelsSettingPage().newLabelPopUp().markNameInbox())
            .inputsTextInElement(user.pages().FoldersAndLabelsSettingPage().newLabelPopUp().markNameInbox(), name);
        return this;
    }

    @Step("Клик по папке: «{0}»")
    public SettingsPageSteps clicksOnFolder(String folderName) {
        openThreadIfCan();
        shouldSeeFolder(folderName);
        List<CustomFolderBlock> folders = filter(
            having(ch.lambdaj.Lambda.on(CustomFolderBlock.class).name(), hasText(folderName)),
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().blockCreatedFolders().customFolders()
        );
        user.defaultSteps().onMouseHoverAndClick(folders.get(0).info());
        return this;
    }

    @Step("Клик на первую папку в списке")
    public SettingsPageSteps clicksOnFolder() {
        assertThat(
            "Папка не создалась",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().blockCreatedFolders(),
            withWaitFor(foldersCount(greaterThanOrEqualTo(1)))
        );
        user.defaultSteps().onMouseHoverAndClick(
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().blockCreatedFolders()
                .customFolders().get(0).info()
        );
        return this;
    }

    @Step("Должны видеть папку «{0}»")
    public SettingsPageSteps shouldSeeFolder(String... folderNames) {
        asList(folderNames).forEach(folderName ->
            assertThat(
                "Нет папки с именем",
                webDriverRule,
                withWaitFor(customFolder(withFolderName((String) folderName)))
            )
        );
        return this;
    }

    //Collector page

    @Step("Удаляем сборщики")
    public SettingsPageSteps deleteMailBoxesFromCollectorSettingsPage() {
        int size = user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors().size();
        if (size != 0) {
            for (int i = 0; i < size; i++) {
                if (isPresent().matches(
                    user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors().get(0)
                        .deleteMailboxBtn()
                )) {
                    user.defaultSteps().clicksOn(
                        user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors().get(0)
                            .deleteMailboxBtn()
                    );
                } else {
                    user.defaultSteps()
                        .clicksOn(
                            user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors().get(0)
                                .collectorLink()
                        )
                        .clicksOn(
                            user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors().get(0)
                                .deleteMailboxBtn()
                        );
                }
                user.defaultSteps().clicksOn(user.pages().CollectorSettingsPage().deleteCollectorPopUp().deleteBtn());
                assertThat(
                    "Не дождались удаления сборщика",
                    webDriverRule,
                    withWaitFor(collectorCount(equalTo(size - i - 1)))
                );
            }
        }
        return this;
    }

    @Step("Чекаем SSL")
    public SettingsPageSteps selectsSslCheckBox() {
        user.defaultSteps().turnTrue(user.pages().CollectorSettingsPage().blockMain().serverSetup().useSsl())
            .clicksOn(user.pages().CollectorSettingsPage().blockMain().serverSetup().port());
        return this;
    }

    @Step("Сохраняем изменения")
    public SettingsPageSteps clicksOnSaveChangesButton() {
        user.defaultSteps().clicksOn(user.pages().CollectorSettingsPage().blockSetup().save());
        return this;
    }

    @Step("Должен содаться сборщик")
    public boolean isCollectorCreated() {
        return withWaitFor(isPresent()).matches(user.pages().CollectorSettingsPage().blockSetup().save());
    }

    @Step("Изменения сохранились")
    public boolean isCollectorChangesSaved() {
        return withWaitFor(
            urlShould(not(containsString("popid="))),
            SECONDS.toMillis(35)
        ).matches(webDriverRule.getDriver());
    }

    @Step("Должны увидеть сообщение о дубликате")
    public boolean shouldSeeNotificationAboutDuplicate() {
        return withWaitFor(isPresent()).matches(
            user.pages().CollectorSettingsPage().blockMain().blockNew().notifications().alreadyExistsNotification()
        );
    }

    @Step("Должны увидеть корректный текст уведомления о дубликате: «{1}»")
    public SettingsPageSteps shouldSeeCorrectTextOnNotificationAboutDuplicate(String server, String notificationLogin) {
        assertThat(
            "Неверный текст уведомления",
            user.pages().CollectorSettingsPage().blockMain().blockNew().notifications().alreadyExistsNotification(),
            hasText(format("Сборщик почты с сервера %s для логина %s уже существует.", server, notificationLogin))
        );
        return this;
    }

    @Step("Должны увидеть настройки сервера")
    public boolean shouldSeeServerSettingsConfiguration() {
        return withWaitFor(isPresent()).matches(user.pages().CollectorSettingsPage().blockMain().serverSetup());
    }

    @Step("Не должны видеть нотификацию от сервера")
    public boolean shouldSeeNoResponseFromServerNotification() {
        return withWaitFor(isPresent()).matches(
            user.pages().CollectorSettingsPage().blockMain().notifications().noResponseNotification()
        );
    }

    @Step("Должно появиться уведомление о неверном логине/пароле")
    public SettingsPageSteps shouldSeeCorrectTextOnNoResponseFromServerNotification() {
        assertThat(
            "Неверный текст уведомления",
            user.pages().CollectorSettingsPage().blockMain().blockNew().notifications().noResponseNotification(),
            hasText("Сервер не отвечает, либо введен неверный логин или пароль.")
        );
        return this;
    }

    @Step("Должно появиться уведомление о пустом поле “Email“")
    public boolean shouldSeeNotificationAboutEmptyEmail() {
        return isPresent().matches(
            user.pages().CollectorSettingsPage().blockMain().blockNew().notifications().emptyEmailNotification()
        );
    }

    @Step("Должны увидеть верный текст уведомления о незаполненном поле «email»")
    public SettingsPageSteps shouldSeeCorrectTextOnEmptyEmailNotification() {
        assertThat(
            "Неверный текст уведомления",
            user.pages().CollectorSettingsPage().blockMain().blockNew().notifications().emptyEmailNotification(),
            hasText("Поле не заполнено")
        );
        return this;
    }

    @Step("Должны видеть уведомление о незаполненном поле «пароль»")
    private boolean shouldSeeNotificationAboutEmptyPassword() {
        return isPresent().matches(user.pages().CollectorSettingsPage().blockMain()
            .blockNew().notifications().emptyPasswordNotification());
    }

    @Step("Должны увидеть верный текст уведомления о незаполненном поле «пароль»")
    public SettingsPageSteps shouldSeeCorrectTextOnEmptyPasswordNotification() {
        shouldSeeNotificationAboutEmptyPassword();
        assertThat(
            "Неверный текст уведомления",
            user.pages().CollectorSettingsPage().blockMain().blockNew().notifications().emptyPasswordNotification(),
            hasText("Поле не заполнено")
        );
        return this;
    }

    @Step("Должен быть виден сборщик для «{0}»")
    public SettingsPageSteps shouldSeeNewCollector(String login) {
        assertThat("Сборщик не создался", webDriverRule, withWaitFor(collectorCount(equalTo(1))));
        assertThat(
            "Сборщик называется неверно",
            user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors().get(0).collectorLink(),
            hasText(login)
        );
        return this;
    }

    @Step("Вводим Сервер: «{0}»")
    public SettingsPageSteps inputsTextInServerInputBox(String server) {
        user.defaultSteps().inputsTextInElement(
            user.pages().CollectorSettingsPage().blockMain().serverSetup().server(),
            server
        );
        return this;
    }

    @Step("Вводим Логин «{0}»")
    public SettingsPageSteps inputsTextInLoginInputBox(String login) {
        assertThat(
            "Поле не появилось",
            user.pages().CollectorSettingsPage().blockMain().serverSetup().login(),
            withWaitFor(isPresent())
        );
        user.defaultSteps().inputsTextInElement(
            user.pages().CollectorSettingsPage().blockMain().serverSetup().login(),
            login
        );
        return this;
    }

    @Step("Вводим Email: «{0}»")
    public SettingsPageSteps inputsTextInEmailInputBox(String login) {
        user.defaultSteps().shouldSee(user.pages().CollectorSettingsPage().blockMain().blockNew().email())
            .clearTextInput(user.pages().CollectorSettingsPage().blockMain().blockNew().email())
            .inputsTextInElement(user.pages().CollectorSettingsPage().blockMain().blockNew().email(), login);
        return this;
    }

    @Step("Вводим Пароль: «{0}»")
    public SettingsPageSteps inputsTextInPassInputBox(String password) {
        user.defaultSteps().clearTextInput(user.pages().CollectorSettingsPage().blockMain().blockNew().password())
            .inputsTextInElement(user.pages().CollectorSettingsPage().blockMain().blockNew().password(), password);
        return this;
    }

    @Step("Кликаем по {0}-му в списке сборщику")
    public SettingsPageSteps clicksOnCollector(int index) {
        assertThat("Нет ни одного сборщика", webDriverRule, withWaitFor(collectorCount(greaterThan(0))));
        user.defaultSteps().clicksOn(
            user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors().get(index).collectorLink()
        );
        return this;
    }

    @Step("Заполняем email и пароль ({0}:{1})")
    public SettingsPageSteps inputsEmailAndPassword(String email, String password) {
        inputsTextInPassInputBox(password);
        inputsTextInEmailInputBox(email);
        return this;
    }

    @Step("Меняем интерфейс на 3pane")
    public SettingsPageSteps changesInterfaceTo3PaneH() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_INTERFACE_3PANE_HORIZONTAL);
        return this;
    }

    @SkipIfFailed
    @Step("Расчекиваем “Показывать Дела на страницах почты“")
    public SettingsPageSteps deselectsCheckBoxOnToDoPage() {
        user.defaultSteps().deselects(user.pages().SettingsPage().setupTodo().showTodoCheckbox());
        assumeStepCanContinue(user.pages().SettingsPage().setupTodo().saveBtn(), isPresent());
        user.defaultSteps().clicksOn(user.pages().SettingsPage().setupTodo().saveBtn());
        return this;
    }

    @Step("Меняем тип интерфейса на 3pane (Вертикальный)")
    public SettingsPageSteps changesInterfaceTo3PaneV() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_INTERFACE_3PANE_VERTICAL);
        return this;
    }

    @Step("Меняем тип интерфейса «{0}»")
    public SettingsPageSteps changesInterfaceTo(int interfaceIndex) {
        user.defaultSteps().clicksOn(
            user.pages().SettingsPage().setupInterface().interfaceOptions().get(interfaceIndex)
        );
        return this;
    }

    @Step("Кликаем по 1-й метке в списке")
    public SettingsPageSteps clicksOnLabel() {
        user.defaultSteps().clicksOn(
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList().get(0)
        );
        return this;
    }

    @Step("Кликаем по метке в списке с именем {0}")
    public SettingsPageSteps clicksOnLabelWithName(String name) {
        user.defaultSteps().clicksOnElementWithText(
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList(), name
        );
        return this;
    }

    @Step("Количество меток должно быть «{0}»")
    public SettingsPageSteps shouldSeeLabelsCount(int count) {
        assertThat(
            "Ожидалось другое количество меток",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().labels().userLabelsList(),
            withWaitFor(hasSize(count))
        );
        return this;
    }

    @Step("Количество пользовательских папок на странице настроек должен быть «{0}»")
    public SettingsPageSteps shouldSeeCustomFoldersCountOnSettingsPage(int count) {
        assertThat("Ожидалось другое количество папок", webDriverRule, withWaitFor(customFolderCount(equalTo(count + 6))));
        return this;
    }

    @Step("Количество раскрывающихся папок должно быть «{0}»")
    public SettingsPageSteps shouldSeeThreadsCount(int count) {
        assertThat(
            "Ожидалось другое количество тредов",
            user.pages().FoldersAndLabelsSettingPage().setupBlock().folders().blockCreatedFolders()
                .customFoldersThreads(),
            hasSize(count)
        );
        return this;
    }

    @Step("Заходим в настройки {0}-го коллектора")
    public SettingsPageSteps clicksOnConfigureCollector(int index) {
        user.defaultSteps().clicksOn(
            user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors().get(index)
                .configureMailboxBtn()
        );
        return this;
    }

    @Step("Свитч должен быть включен")
    public SettingsPageSteps shouldSeeSwitchOn(MailElement switchElement) {
        switchElement.should("Свитч не включен!", hasAttribute("class", containsString("b-switch_on")));
        return this;
    }

    @Step("Свитч должен быть выключен")
    public SettingsPageSteps shouldSeeSwitchOff(MailElement switchElement) {
        switchElement.should("Свитч не выключен!", hasAttribute("class", containsString("b-switch_off")));
        return this;
    }

    @Step("Переводим свитч во включенное состояние")
    public SettingsPageSteps turnSwitchOn(MailElement switchElement) {
        if (switchElement.getAttribute("class").contains("b-switch_off")) {
            user.defaultSteps().clicksOn(switchElement);
        }
        return this;
    }

    @Step("Переводим свитч в выключенное состояние")
    public SettingsPageSteps turnSwitchOff(MailElement switchElement) {
        if (switchElement.getAttribute("class").contains("b-switch_on")) {
            user.defaultSteps().clicksOn(switchElement);
        }
        return this;
    }

    //---------------------------------------------------------
    //-Abook settings
    //---------------------------------------------------------

    @SkipIfFailed
    @Step("Сохраняем изменения если нужно")
    public void savesMailClientsSettings() {
        assumeStepCanContinue(
            user.pages().MailClientsSettingsPage().blockSetupClients().saveBtn(),
            isDisplayed()
        );
        user.defaultSteps().clicksOn(user.pages().MailClientsSettingsPage().blockSetupClients().saveBtn());
    }

    //---------------------------------------------------------
    //-Security settings
    //---------------------------------------------------------

    @Step("Количество сборщиков должно быть «{0}»")
    public SettingsPageSteps shouldSeeCollectorCount(int count) {
        assertThat("Ожидалось другое количество сборщиков", webDriverRule, collectorCount(equalTo(count)));
        return this;
    }

    @Step("Должны видеть нотификацию при попытке создать сборщик на текущий ящик")
    public boolean isPresentNotificationAboutCurrentEmail() {
        return isPresent().matches(
            user.pages().CollectorSettingsPage().blockMain().blockNew().notifications().currentEmailNotification()
        );
    }

    @Step("Изменяем значение чекбокса списка дел на противоположное")
    public boolean invertTodoCheckbox() {
        boolean selected = user.pages().SettingsPage().setupTodo().showTodoCheckbox().isSelected();
        user.pages().SettingsPage().setupTodo().showTodoCheckbox().setChecked(!selected);
        return selected;
    }


    @Step("Открываем настройки сборщика номер «{0}»")
    public SettingsPageSteps goesToCollectorConfiguration(int index) {
        assertThat("Нет ни одного сборщика", webDriverRule, withWaitFor(collectorCount(greaterThan(0))));
        if (isPresent().matches(
            user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors().get(index)
                .configureMailboxBtn()
        )) {
            user.defaultSteps().clicksOn(
                user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors().get(index)
                    .configureMailboxBtn()
            );
        } else {
            user.defaultSteps()
                .clicksOn(
                    user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors().get(index)
                        .collectorLink()
                )
                .clicksOn(
                    user.pages().CollectorSettingsPage().blockMain().blockConnected().collectors().get(index)
                        .configureMailboxBtn()
                );
        }
        return this;
    }

    // =====================
    // Множественные подписи
    // =====================

    @Step("Должны видеть подпись с текстом «{0}»")
    public SettingsPageSteps shouldSeeSignatureWith(String... texts) {
        List<String> signatures = extract(
            user.pages().SenderInfoSettingsPage().blockSetupSender().signatures().signaturesList(),
            ch.lambdaj.Lambda.on(SignatureToolbarBlock.class).textSignature().getText()
        );
        assertThat("Нет подписи с нужным текстом", signatures, hasItems(texts));
        return this;
    }

    @Step("Не должны видеть дублирующиеся подписи")
    public SettingsPageSteps shouldNotSeeDuplicateSignature() {
        List<String> signatures = extract(
            user.pages().SenderInfoSettingsPage().blockSetupSender().signatures().signaturesList(),
            ch.lambdaj.Lambda.on(SignatureToolbarBlock.class).textSignature().getText()
        );
        Set<String> set = new HashSet<>(signatures);
        assertThat("Есть дубликаты", signatures, hasSize(set.size()));
        return this;
    }

    @Step("Не должны видеть подпись с текстом «{0}»")
    public SettingsPageSteps shouldNotSeeSignatureWith(String text) {
        List<String> signatures = extract(
            user.pages().SenderInfoSettingsPage().blockSetupSender().signatures().signaturesList(),
            ch.lambdaj.Lambda.on(SignatureToolbarBlock.class).textSignature().getText()
        );
        assertThat("Подписи с таким текстом не должно быть", signatures, not(hasItem(text)));
        return this;
    }

    @Step("Удаляем все подписи у юзера")
    public SettingsPageSteps deleteAllSignatures() {
        user.defaultSteps().executesJavaScript("Jane.$H('settings').setSettings({signs: []})");
        return this;
    }

    @Step("Включаем на юзере показ паранжи для квикреплая")
    public SettingsPageSteps showQuickReplyParanja() {
        user.defaultSteps().executesJavaScript("ns.Model.get('settings').setSettings({'qr-paranja-shows':0})");
        return this;
    }

    @Step("Обнуляем настройку показа попапа «Пометить все письма в папке как прочитанные?»")
    public SettingsPageSteps showMarkMsgAsReadPopup() {
        user.defaultSteps().executesJavaScript("ns.Model.get('settings').setSettingOff('no_popup_mark_read')");
        return this;
    }

    @Step("Обнуляем настройку показа соц аватарок")
    public SettingsPageSteps turnOffSocNetAvatars() {
        user.defaultSteps().executesJavaScript("ns.Model.get('settings').setSettingOff('show_socnet_avatars')");
        return this;
    }

    @Step("Количество подписей у пользователя должно быть «{0}»")
    public SettingsPageSteps shouldSeeSignaturesCountInJsResponse(int count) {
        assertThat(
            "Нет ожидаемого количества подписей на сервере",
            webDriverRule.getDriver(),
            withWaitFor(hasSignaturesCount(count))
        );
        return this;
    }

    @Step("Количество подписей у пользователя должно быть «{0}»")
    public SettingsPageSteps shouldSeeSignaturesCountOnPage(int count) {
        assertThat(
            "Нет ожидаемого количества подписей на сервере",
            webDriverRule,
            withWaitFor(signaturesCountOnPage(count))
        );
        return this;
    }

    @Step("Формируем ожидаемый url страницы редактирования для правила с Id: {0}.")
    public String createSetupUrl(AccLockRule lock, String filter_id) {
        return UrlBuilder.fromString(webDriverRule.getBaseUrl())
            .withPath("/neo2/")
            .addParameter("uid", Util.getUserUid(lock.firstAcc().getLogin()))
            .addParameter("login", lock.firstAcc().getLogin())
            .withFragment("setup/filters-create/id=" + filter_id)
            .toString();
    }

    @Step("Создаем встречу")
    public SettingsPageSteps createMeeting() {
        user.defaultSteps().executesJavaScript(
            "ns.Model.get('notifications').addNotification({type: 'calendar',\n" +
                "name: 'Поговорить',\n" +
                "location: 'Камчатка',\n" +
                "start: Date.now(),\n" +
                "end: Date.now(),\n" +
                "vip: true,\n" +
                "label: 'reminder'})");
        return this;
    }

    // =====================
    // Рассылки
    // =====================

    @Step("Открываем поп-ап настроек Рассылок и переключаемся в него")
    public SettingsPageSteps openSubscriptionsSettings() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().mail360HeaderBlock().settingsMenu())
            .clicksOn(user.pages().MessagePage().mainSettingsPopupNew().subsPromoSettings())
            .switchTo(IFRAME_SUBS_LIZA)
            .shouldNotSee(user.pages().SubscriptionsSettingsPage().loader())
            .shouldSee(user.pages().SubscriptionsSettingsPage().closeSubs());
        return this;
    }
}
