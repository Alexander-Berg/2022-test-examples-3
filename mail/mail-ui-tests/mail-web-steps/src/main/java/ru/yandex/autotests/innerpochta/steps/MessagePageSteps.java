package ru.yandex.autotests.innerpochta.steps;

import org.apache.commons.lang.StringUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.annotations.SkipIfFailed;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.DisplayedMessagesBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.MessageBlock;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.having;
import static java.text.MessageFormat.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static ru.lanwen.diff.uri.core.filters.AnyParamValueFilter.param;
import static ru.yandex.autotests.innerpochta.data.MailEnums.ElementAttributes.DATA_ACTION;
import static ru.yandex.autotests.innerpochta.matchers.ContainsTextMatcher.containsText;
import static ru.yandex.autotests.innerpochta.matchers.MessageExistsMatcher.messageWithSubjectAndPrefixExists;
import static ru.yandex.autotests.innerpochta.matchers.MessageExistsMatcher.messageWithSubjectExists;
import static ru.yandex.autotests.innerpochta.matchers.MessageExistsMatcher.msgNotExists;
import static ru.yandex.autotests.innerpochta.matchers.MessageInThreadCountMatcher.messagesInThreadCount;
import static ru.yandex.autotests.innerpochta.matchers.MessagePageCountMatcher.messagePagesCount;
import static ru.yandex.autotests.innerpochta.matchers.PostRefreshMatcherDecorator.withPostRefresh;
import static ru.yandex.autotests.innerpochta.matchers.RegExpMatcher.withPattern;
import static ru.yandex.autotests.innerpochta.matchers.message.MessageFolderMatcher.messageFolder;
import static ru.yandex.autotests.innerpochta.matchers.message.MessageMatchers.existMessageWith;
import static ru.yandex.autotests.innerpochta.matchers.message.MessageMatchers.hasDate;
import static ru.yandex.autotests.innerpochta.matchers.message.MessageMatchers.hasFolder;
import static ru.yandex.autotests.innerpochta.matchers.message.MessageMatchers.hasLabel;
import static ru.yandex.autotests.innerpochta.matchers.message.MessageMatchers.messageCountOnPage;
import static ru.yandex.autotests.innerpochta.matchers.message.MessageMatchers.messageShould;
import static ru.yandex.autotests.innerpochta.matchers.message.MessageMatchers.messagesInThreadShould;
import static ru.yandex.autotests.innerpochta.matchers.message.MessageMatchers.subject;
import static ru.yandex.autotests.innerpochta.matchers.message.MessageMatchers.subjectCount;
import static ru.yandex.autotests.innerpochta.matchers.message.MessageThreadMatcher.threadCount;
import static ru.yandex.autotests.innerpochta.util.MailConst.XIVA_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.SkipStep.SkipStepMethods.assumeStepCanContinue;
import static ru.yandex.autotests.innerpochta.util.Utils.getUserUid;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SIZE_VIEW_APP;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SIZE_VIEW_APP2;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.common.HasTextMatcher.hasText;

@SuppressWarnings({"UnusedReturnValue", "unchecked"})
public class MessagePageSteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;

    MessagePageSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    //--------------------------------------------------------
    // MESSAGE PAGE STEPS
    //-------------------------------------------------------

    @Step("Должны быть на странице входящих на домене {1} для аккаунта {0}")
    public MessagePageSteps shouldBeOnMessagePageFor(Account acc, String domain) {
        String login = StringUtils.substringBeforeLast(acc.getLogin(), "@");
        String hostname = webDriverRule.getBaseUrl().split("\\.(?=[^.]+$)")[0];
        String inboxUrlPattern = "%s.%s/?uid=%s#inbox";
        String url = String.format(inboxUrlPattern, hostname, domain, getUserUid(acc.getLogin()), login);
        user.defaultSteps().shouldBeOnUrlNotDiffWith(url, param("ncrnd").ignore());
        return this;
    }

    @Step("Клик по сообщению с темой «{0}»")
    public MessagePageSteps clicksOnMessageWithSubject(String subject) {
        shouldSeeMessageWithSubject(subject);
        user.defaultSteps().clicksOn(findMessageBySubject(subject).subject());
        assertThat(
            "Письмо не открылось",
            user.pages().MessageViewPage().messageHead().fromName(),
            withWaitFor(isPresent())
        );
        return this;
    }

    @Step("Правый клик по сообщению с темой «{0}»")
    public MessagePageSteps rightClickOnMessageWithSubject(String subject) {
        shouldSeeMessageWithSubject(subject);
        user.defaultSteps().rightClick(findMessageBySubject(subject).subject());
        return this;
    }

    @Step("Клик по шаблону с темой «{0}»")
    public MessagePageSteps clicksOnTemplateWithSubject(String subject) {
        shouldSeeMessageWithSubject(subject);
        user.defaultSteps().clicksOn(findMessageBySubject(subject).sender());
        return this;
    }

    @Step("Должны видеть сообщение с темой «{0}»")
    public void shouldSeeMessageWithSubjectAndPrefix(String subject) {
        assertThat(
            "Письмо не найдено",
            user.pages().MessagePage(),
            withWaitFor(
                withPostRefresh(messageWithSubjectAndPrefixExists(subject), webDriverRule.getDriver()),
                SECONDS.toMillis(60),
                SECONDS.toMillis(5)
            )
        );
    }

    @Step("Клик по [ {0} ], должны увидеть сообщение в строке уведомлений «{1}»")
    public void clicksOnElementAndChecksStatusLine(WebElement element, String message) {
        user.defaultSteps().clicksOn(element);
        waitForTextInStatusLine(message);
    }

    @Step("Проверяем, что не видим блок с отправителем, значит письмо закрыто")
    public MessagePageSteps shouldNotSeeOpenMessage() {
        assertThat(
            "Письмо открыто",
            user.pages().MessageViewPage().messageHead().fromName(),
            withWaitFor(not(isPresent()))
        );
        return this;
    }

    @Step("Проверяем, что не видим блок с темой сообщения, значит письмо закрыто")
    public MessagePageSteps shouldNotSeeOpenMessageSubjectField() {
        assertThat(
            "Письмо открыто",
            user.pages().MessageViewPage().messageSubjectInFullView(),
            withWaitFor(not(isPresent()))
        );
        return this;
    }

    @Step("Название папки коллектора в шапке должно совпадать с «{0}»")
    public MessagePageSteps shouldSeeTitleOfCollectorFolder(String title) {
        assertThat(
            "Папка не открылась/называется неверно",
            user.pages().MessagePage().collectorFldSubj(),
            withWaitFor(
                allOf(isPresent(), hasText(title)),
                SECONDS.toMillis(15),
                SECONDS.toMillis(5)
            )
        );
        return this;
    }

    @Step("Должны видеть адрес отправителя «{0}» на письме с темой «{1}»")
    public void shouldSeeAddressOnMessageWithSubject(String address, String subject) {
        assertThat("Письмо не найдено", webDriverRule, withWaitFor(existMessageWith(subject(subject))));
        assertThat(
            "Поле адреса не соответствует ожидаемому",
            findMessageBySubject(subject).sender(),
            hasAttribute("title", address)
        );
    }

    @Step("Должны видеть имя отправителя «{0}» на письме с темой «{1}»")
    public void shouldSeeNameOnMessageWithSubject(String name, String subject) {
        assertThat("Письмо не найдено", webDriverRule, withWaitFor(existMessageWith(subject(subject))));
        assertThat(
            "Поле отправитель не соответствует ожидаемому",
            findMessageBySubject(subject).sender(),
            hasText(name)
        );

    }

    @Step("Должны видеть письма с темой: «{0}»")
    public MessagePageSteps shouldSeeMessageWithSubject(String... expectedSubjects) {
        assertThat(
            "Письмо отсутствует на странице",
            user.pages().MessagePage(),
            withWaitFor(
                withPostRefresh(
                    messageWithSubjectExists(Arrays.asList(expectedSubjects).get(0)),
                    webDriverRule.getDriver()
                ),
                SECONDS.toMillis(30),
                SECONDS.toMillis(3)
            )
        );
        return this;
    }

    @Step("Должны видеть «{1}» письма с темой: «{0}»")
    public MessagePageSteps shouldSeeMessageWithSubjectCount(String expectedSubject, int count) {
        assertThat(
            "Страница с письмами не загрузилась",
            webDriverRule,
            withWaitFor(messageCountOnPage(greaterThan(0)))
        );
        assertThat(
            "Нет нужного письма",
            webDriverRule,
            withWaitFor(existMessageWith(subjectCount(expectedSubject, count)))
        );
        return this;
    }

    @Step("Должны увидеть сообщение с темой: {0}")
    public MessagePageSteps shouldSeeMessageWithSubjectWithoutRefresh(String... expectedSubjects) {
        assertThat(
            "Страница с письмами не загрузилась",
            webDriverRule,
            withWaitFor(messageCountOnPage(greaterThan(0)))
        );
        assertThat(
            "Письмо отсутствует на странице",
            user.pages().MessagePage(),
            withWaitFor(messageWithSubjectExists(expectedSubjects))
        );
        return this;
    }

    @Step("Не должны видеть письмо с темой: «{0}»")
    public MessagePageSteps shouldNotSeeMessageWithSubject(String... expectedSubjects) {
        assertThat(
            "Страница не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)))
        );
        assertThat(
            "Письмо присутствует на странице",
            user.pages().MessagePage(),
            withWaitFor(msgNotExists(expectedSubjects))
        );
        return this;
    }

    @Step("Получаем все письма с темой «{0}»")
    public MessageBlock findMessageBySubject(String subject) {
        return findAllMessageBySubject(subject).get(0);
    }

    @Step("Получаем все письма с темой «{0}»")
    private List<MessageBlock> findAllMessageBySubject(String subject) {
        List<MessageBlock> message = new ArrayList<>();
        for (DisplayedMessagesBlock page : user.pages().MessagePage().allDisplayedMessagesBlocks()) {
            message.addAll(
                filter(having(ch.lambdaj.Lambda.on(MessageBlock.class).subject(), hasText(subject)), page.list())
            );
        }
        assertThat("Can't find message with subject «" + subject + "»", message, hasSize(greaterThan(0)));
        return message;
    }

    @Step("Выделяем сообщения с номерами {0}")
    public MessagePageSteps clicksOnMultipleMessagesCheckBox(int... indexes) {
        assertThat(
            "Страница не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)))
        );
        for (int i : indexes) {
            user.defaultSteps()
                .clicksOn(user.pages().MessagePage().displayedMessages().list().get(i).avatarAndCheckBox());
        }
        return this;
    }

    @Step("Клик по чекбоксу сообщений с темами: {0}")
    public void clicksOnMultipleMessagesCheckBox(String... subjects) {
        Arrays.asList(subjects).forEach(this::clicksOnMessageCheckBoxWithSubject);
    }

    @Step("Клик по чекбоксу сообщения с темой «{0}»")
    public MessagePageSteps clicksOnMessageCheckBoxWithSubject(String subject) {
        assertThat(
            "Страница не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)))
        );
        shouldSeeMessageWithSubject(subject);
        user.defaultSteps().clicksOn(findMessageBySubject(subject).avatarAndCheckBox());
        return this;
    }

    @Step("Сообщение с темой «{0}» должно иметь дату «{1}»")
    public void shouldSeeMessageWithDate(String subject, String date) {
        assertThat(
            "Страница не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)))
        );
        assertThat(
            "Письмо отсутствует на странице",
            user.pages().MessagePage(),
            withWaitFor(
                withPostRefresh(messageWithSubjectExists(subject), webDriverRule.getDriver()),
                SECONDS.toMillis(30),
                SECONDS.toMillis(3)
            )
        );
        user.defaultSteps().shouldContainText(findMessageBySubject(subject).date(), date);
    }

    @Step("Ставим галочку на письмо с темой «{0}»")
    public MessagePageSteps selectMessageWithSubject(String... subject) {
        Arrays.asList(subject).forEach(this::clicksOnMessageCheckBoxWithSubject);
        return this;
    }

    @Step("Ставим галочку на письмо с индексом «{0}»")
    public MessagePageSteps selectMessageWithIndex(int index) {
        assertThat(
            "Страница не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)))
        );
        user.defaultSteps()
            .clicksOn(user.pages().MessagePage().displayedMessages().list().get(index).avatarAndCheckBox());
        return this;
    }

    @Step("Убираем галочку с письма с темой «{0}»")
    public MessagePageSteps deselectMessageCheckBoxWithSubject(String... subject) {
        assertThat(
            "Страница не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)))
        );
        assertThat(
            "Письмо отсутствует на странице",
            user.pages().MessagePage(),
            withWaitFor(messageWithSubjectExists(subject))
        );
        for (String subj : subject) {
            user.defaultSteps().clicksOn(findMessageBySubject(subj).avatarAndCheckBox());
        }
        return this;
    }

    @Step("Ставим галочку на письма в треде с номерами «{0}»")
    public MessagePageSteps selectMessagesInThreadCheckBoxWithNumber(int... numbers) {
        assertThat(
            "Страница с письмами не загрузилась",
            webDriverRule,
            withWaitFor(messageCountOnPage(greaterThan(0)))
        );
        assertThat(
            "Нет открытых тредов",
            user.pages().MessagePage().displayedMessages().messagesInThread(),
            hasSize(greaterThan(0))
        );
        for (int n : numbers) {
            user.defaultSteps().
                clicksOn(user.pages().MessagePage().displayedMessages().messagesInThread().get(n).avatarAndCheckBox());
        }
        return this;
    }

    @Step("Выбираем первое сообщение в списке")
    public String clicksOnMessageCheckBox() {
        String subject = getsMessageSubject(0);
        user.defaultSteps().shouldSee(user.pages().MessagePage().displayedMessages().list().get(0).avatarAndCheckBox())
            .clicksOn(user.pages().MessagePage().displayedMessages().list().get(0).avatarAndCheckBox());
        return subject;
    }

    @Step("Выделяем сообщение номер {0} в списке")
    public String clicksOnMessageCheckBoxByNumber(int index) {
        assertThat(
            "Страница с письмами не загрузилась",
            webDriverRule,
            withWaitFor(messageCountOnPage(greaterThan(0)))
        );
        user.defaultSteps().clicksOn(user.pages().MessagePage().displayedMessages().list().get(index).checkBox());
        return user.pages().MessagePage().displayedMessages().list().get(index).subject().getText();
    }

    @SkipIfFailed
    @Step("Помечаем сообщение с темой «{0}» важным")
    public void labelsMessageImportant(String expectedSubject) {
        assertThat(
            "Письмо не найдено",
            user.pages().MessagePage(),
            withWaitFor(
                withPostRefresh(messageWithSubjectExists(expectedSubject), webDriverRule.getDriver()),
                SECONDS.toMillis(30),
                SECONDS.toMillis(5)
            )
        );
        MessageBlock message = findMessageBySubject(expectedSubject);
        assumeStepCanContinue(message.importanceLabel(), not(hasAttribute(DATA_ACTION.getValue(), "unlabel")));
        user.defaultSteps().clicksOn(message.importanceLabel());
    }

    @Step("Помечаем сообщение прочитанным")
    public void labelsMessageAsReadFromDropDownMenu() {
        opensLabelDropdown();
        user.defaultSteps().clicksOn(user.pages().MessagePage().labelsDropdownMenu().markAsRead());
    }

    @Step("Помечаем письмо как непрочитанное через выпадающее меню")
    public void labelsMessageAsUnreadFromDropDownMenu() {
        opensLabelDropdown();
        user.defaultSteps().clicksOn(user.pages().MessagePage().labelsDropdownMenu().markAsUnread());
    }

    @Step("Открываем выпадушку проставления метки")
    private void opensLabelDropdown() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().markMessageDropDown())
            .shouldSee(user.pages().MessagePage().labelsDropdownMenu());
    }

    @Step("Помечаем сообщение меткой «{0}» через выпадающее меню")
    public MessagePageSteps markMessageWithCustomLabel(String labelName) {
        opensLabelDropdown();
        user.defaultSteps().clicksOnElementWithText(
                user.pages().MessagePage().labelsDropdownMenu().customMarks(),
                labelName.substring(1)
            )
            .waitInSeconds(1);
        return this;
    }

    @Step("Помечаем сообщение меткой «{0}» через тулбар в теле письма")
    public MessagePageSteps markMessageWithCustomLabelByContentToolbar(String labelName) {
        user.defaultSteps().clicksOn(user.pages().MessageViewPage().contentToolbarBlock().moreBtn())
            .shouldSee(user.pages().MessageViewPage().miscField())
            .clicksOn(user.pages().MessageViewPage().miscField().label())
            .shouldSee(user.pages().MessageViewPage().labelsDropdownMenu())
            .clicksOnElementWithText(
                user.pages().MessageViewPage().labelsDropdownMenu().customMarks(),
                labelName.substring(1)
            );
        return this;
    }

    @Step("Убираем метку «{0}» с сообщения через тулбар в теле письма")
    public void unmarkMessageWithCustomLabelByContentToolbar(String labelName) {
        user.defaultSteps().clicksOn(user.pages().MessageViewPage().contentToolbarBlock().moreBtn())
            .shouldSee(user.pages().MessageViewPage().miscField())
            .clicksOn(user.pages().MessageViewPage().miscField().label())
            .shouldSee(user.pages().MessageViewPage().labelsDropdownMenu())
            .clicksOnElementWithText(
                user.pages().MessageViewPage().labelsDropdownMenu().unlabelCustomMark(),
                labelName.substring(1)
            );
    }

    @Step("Ставим на письмо метку «Важное»")
    public MessagePageSteps labelsMessageImportantFromDropDownMenu() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().markMessageDropDown())
            .shouldSee(user.pages().MessagePage().labelsDropdownMenu().labelImportant())
            .clicksOn(user.pages().MessagePage().labelsDropdownMenu().labelImportant());
        return this;
    }

    @Step("Удаляем у письма метку «Важное»")
    public MessagePageSteps unlabelsMessageImportantFromDropDownMenu() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().markMessageDropDown())
            .shouldSee(user.pages().MessagePage().labelsDropdownMenu().unlabelImportant())
            .clicksOn(user.pages().MessagePage().labelsDropdownMenu().unlabelImportant());
        return this;
    }

    @Step("Удаляем кнопку [ {0} ]")
    public MessagePageSteps removesToolbarCustomButton(MailElement deleteCustomButton) {
        /* Ждем, пока заработает «Шестеренка редактирования пользовательских кнопок» */
        user.defaultSteps().waitInSeconds(2)
            .onMouseHoverAndClick(user.pages().MessagePage().toolbar().configureCustomButtons())
            .shouldSee(user.pages().CustomButtonsPage().overview());
        if (isPresent().matches(deleteCustomButton)) {
            user.defaultSteps().clicksOn(deleteCustomButton);
        }
        user.defaultSteps().shouldNotSee(user.pages().CustomButtonsPage().configureFoldersButton());
        user.defaultSteps().clicksOn(user.pages().CustomButtonsPage().overview().saveChangesButton());
        user.defaultSteps().shouldNotSee(user.pages().CustomButtonsPage().overview().saveChangesButton());
        return this;
    }

    @Step("На сообщении с темой «{1}» должна быть метка «{0}»")
    public MessagePageSteps shouldSeeThatMessageIsLabeledWith(String label, String... expectedSubjects) {
        shouldSeeMessageWithSubject(expectedSubjects);
        for (String expectedSubject : expectedSubjects) {
            assertThat(
                String.format("На письме «%s» нет нужной метки «%s»", expectedSubject, label),
                findMessageBySubject(expectedSubject), withWaitFor(hasLabel(label))
            );
        }
        return this;
    }

    @Step("На сообщении с темой «{1}» не должно быть метки «{0}»")
    public void shouldNotSeeThatMessageIsLabeledWith(String label, String... expectedSubjects) {
        for (String expectedSubject : expectedSubjects) {
            assertThat(
                String.format("На письме «%s» не должно быть метки «%s»", expectedSubject, label),
                findMessageBySubject(expectedSubject), withWaitFor(not(hasLabel(label)))
            );
        }
    }

    @Step("Должны видеть метку «{0}» на сообщении с темой «{1}»")
    public MessagePageSteps shouldSeeLabelsOnMessage(String label, String... expectedSubjects) {
        for (String expectedSubject : expectedSubjects) {
            List<MessageBlock> subjects = filter(
                having(
                    ch.lambdaj.Lambda.on(MessageBlock.class).subject(),
                    hasText(expectedSubject)
                ),
                user.pages().MessagePage().displayedMessages().list()
            );
            List<MessageBlock> labels = filter(
                having(ch.lambdaj.Lambda.on(MessageBlock.class).labels(), hasItem(hasText(label))),
                subjects
            );
            assertThat(
                String.format("На письме «%s» нет нужной метки «%s»", expectedSubject, label),
                labels,
                hasSize(greaterThan(0))
            );
        }
        return this;
    }

    @Step("Сообщение с темой «{0}» должно иметь метки «{1}»")
    public MessagePageSteps shouldSeeThatMessageIsLabeledWithMultipleMarks(String subject, String... labelNames) {
        MessageBlock message = findMessageBySubject(subject);
        for (String labelName : labelNames) {
            assertThat(
                String.format("На письме «%s» нет нужной метки «%s»", subject, labelName),
                message,
                hasLabel(labelName)
            );
        }
        return this;
    }

    @Step("Все сообщения в треде должны быть с меткой «{0}»")
    public MessagePageSteps shouldSeeThatAllMessagesInThreadIsLabeledWith(String labelName) {
        assertThat(
            "Нет открытого треда",
            user.pages().MessagePage().displayedMessages().messagesInThread(),
            hasSize(greaterThan(0))
        );
        for (int i = 0; i < user.pages().MessagePage().displayedMessages().messagesInThread().size(); i++) {
            assertThat(
                "На письме нет нужной метки",
                webDriverRule,
                messagesInThreadShould(hasLabel(labelName.toLowerCase()), i)
            );
        }
        return this;
    }

    @Step("Сообщение в треде с номером «{1}» должно быть помечено меткой «{0}»")
    public MessagePageSteps shouldSeeThatMessageInThreadIsLabeledWith(String labelName, int index) {
        assertThat(
            "Нет открытого треда",
            user.pages().MessagePage().displayedMessages().messagesInThread(),
            hasSize(greaterThan(0))
        );
        assertThat(
            "На письме нет нужной метки",
            webDriverRule,
            messagesInThreadShould(hasLabel(labelName.toLowerCase()), index)
        );
        return this;
    }

    @Step("На письмах в треде не должно быть меток")
    public MessagePageSteps shouldSeeThatAllMessagesInThreadIsNotLabeledWithMarks() {
        assertThat(
            "Нет открытого треда",
            user.pages().MessagePage().displayedMessages().messagesInThread(),
            hasSize(greaterThan(0))
        );
        for (int i = 0; i < user.pages().MessagePage().displayedMessages().messagesInThread().size(); i++) {
            assertThat(
                "На письме не должно быть меток",
                user.pages().MessagePage().displayedMessages().messagesInThread().get(i).labels(),
                hasSize(0)
            );
        }
        return this;
    }

    @Step("Должны видеть, что письмо прочитано")
    public MessagePageSteps shouldSeeThatMessageIsRead() {
        assertThat(
            "Письмо не отметилось прочитанным",
            user.pages().MessagePage().toolbar().markAsUnreadButton(),
            isPresent()
        );
        return this;
    }

    @Step("Должны видеть, что письмо прочитано")
    public MessagePageSteps shouldSeeThatMessageIsReadWithWaiting() {
        assertThat(
            "Письмо не отметилось прочитанным",
            user.pages().MessagePage().toolbar().markAsUnreadButton(),
            withWaitFor(
                isPresent(),
                XIVA_TIMEOUT
            )
        );
        return this;
    }

    @Step("Должны видеть, что письмо непрочитано")
    public MessagePageSteps shouldSeeThatMessageIsNotRead() {
        assertThat(
            "Письмо не отметилось прочитанным",
            user.pages().MessagePage().toolbar().markAsUnreadButton(),
            not(isPresent())
        );
        return this;
    }

    @Step("Не должны видеть метку на сообщении с темой «{0}»")
    public MessagePageSteps shouldSeeThatMessageIsNotLabeledWithCustomMark(String... expectedSubjects) {
        for (String expectedSubject : expectedSubjects) {
            assertThat("На письме есть метка", findMessageBySubject(expectedSubject).labels(), hasSize(0));
        }
        return this;
    }

    @Step("Сообщения с темой «{0}» должны быть помечены меткой «Важные»")
    public MessagePageSteps shouldSeeThatMessageIsImportant(String... expectedSubjects) {
        for (String expectedSubject : expectedSubjects) {
            assertThat(
                "Письмо не отметилось важным",
                findMessageBySubject(expectedSubject).isImportance(),
                withWaitFor(isPresent())
            );
        }
        return this;
    }

    @Step("Сообщения в треде с номерами «{0}» должны быть помечены как «Важные»")
    public MessagePageSteps shouldSeeThatMessagesInThreadAreImportant(int... indexes) {
        for (int i : indexes) {
            assertThat(
                "Письмо не отметилось важным",
                user.pages().MessagePage().displayedMessages().messagesInThread().get(i).isImportance(),
                withWaitFor(hasClass(containsString("is-active")))
            );
        }
        return this;
    }

    @Step("Сообщения с темой «{0}» не должны быть помечены меткой «Важные»")
    public MessagePageSteps shouldSeeThatMessageIsNotImportant(String... expectedSubjects) {
        for (String expectedSubject : expectedSubjects) {
            assertThat(
                "Письмо все еще важное",
                findMessageBySubject(expectedSubject).isImportance(),
                withWaitFor(not(isPresent()))
            );
        }
        return this;
    }

    @Step("Перемещаем сообщение с темой «{0}» в папку с номером «{1}»")
    public MessagePageSteps movesMessageToFolder(String subject, int index) {
        selectMessageWithSubject(subject);
        movesMessageToFolder(index);
        shouldNotSeeMessageWithSubject(subject);
        return this;
    }

    @Step("Перемещаем письмо с темой в папку")
    public MessagePageSteps movesMessageToFolder(String subject, String name) {
        selectMessageWithSubject(subject);
        movesMessageToFolder(name);
        shouldNotSeeMessageWithSubject(subject);
        return this;
    }

    @Step("Перемещаем выделенное сообщение в папку с номером «{0}»")
    public MessagePageSteps movesMessageToFolder(int index) {
        if (isPresent().matches(user.pages().MessagePage().toolbar().moveMessageDropDown())) {
            user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().moveMessageDropDown())
                .shouldSee(user.pages().MessagePage().moveMessageDropdownMenu())
                .onMouseHoverAndClick(user.pages().MessagePage().moveMessageDropdownMenu().customFolders().get(index));
        } else {
            user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar3Panel().moreActionsButton())
                .shouldSee(user.pages().MessagePage().moreActionsOnMessage3PaneDropdown())
                .clicksOn(user.pages().MessagePage().moreActionsOnMessage3PaneDropdown().moveToFolderButton())
                .shouldSee(user.pages().MessagePage().moveMessageDropdownMenuMini())
                .clicksOn(user.pages().MessagePage().moveMessageDropdownMenuMini().customFolders().get(index));
        }
        return this;
    }

    @Step("Перемещаем сообщение в папку «{0}»")
    public MessagePageSteps movesMessageToFolder(String folderName) {
        if (exists().matches(user.pages().MessagePage().toolbar().moveMessageDropDown())) {
            user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().moveMessageDropDown())
                .onMouseHover(user.pages().MessagePage().moveMessageDropdownMenu())
                .clicksOnElementWithText(
                    user.pages().MessagePage().moveMessageDropdownMenu().customFolders(),
                    folderName
                );
        } else {
            user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar3Panel().moreActionsButton())
                .shouldSee(user.pages().MessagePage().moreActionsOnMessage3PaneDropdown())
                .clicksOn(user.pages().MessagePage().moreActionsOnMessage3PaneDropdown().moveToFolderButton())
                .onMouseHover(user.pages().MessagePage().moveMessageDropdownMenuMini())
                .clicksOnElementWithText(
                    user.pages().MessagePage().moveMessageDropdownMenuMini().customFolders(),
                    folderName
                );
        }
        return this;
    }

    @Step("Перемещаем сообщение в папку с номером «{0}» через верхний тулбар ")
    public MessagePageSteps movesMessageToFolderInMessageView(int index) {
        user.defaultSteps().clicksOn(user.pages().MessageViewPage().toolbar().moveMessageBtn())
            .shouldSee(user.pages().MessageViewPage().moveMessageDropdownMenu())
            .onMouseHoverAndClick(user.pages().MessageViewPage().moveMessageDropdownMenu().customFolders().get(index));
        return this;
    }

    @Step("Выделяем все письма в папке")
    public MessagePageSteps selectsAllDisplayedMessagesInFolder() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().selectAllMessages());
        return this;
    }

    @Step("Создаём фильтр для папки «{0}» из строки статуса")
    public void createsFilterFromStatusLine(String folder, String sender) {
        waitForTextInStatusLine(format("Перекладывать все письма от «{0}» в папку «{1}»?", sender, folder));
        user.defaultSteps().clicksOn(user.pages().MessagePage().statusLineBlock().createFilterBtn());
        System.out.println(user.pages().MessagePage().statusLineBlock().getText());
        waitForTextInStatusLine(
            format("Правило настроено. Переложить все полученные ранее письма от «{0}» в эту папку?", sender)
        );
        user.defaultSteps().clicksOn(user.pages().MessagePage().statusLineBlock().editFilterBtn());
    }

    @Step("Создаём новую папку с именем «{0}» через выпадающее меню")
    public MessagePageSteps createsNewFolderFromDropDownMenu(String folder) {
        user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().moveMessageDropDown());
        user.defaultSteps().onMouseHoverAndClick(
            user.pages().MessagePage().moveMessageDropdownMenu().createNewFolder()
        );
        user.defaultSteps().shouldSee(user.pages().FoldersAndLabelsSettingPage().newFolderPopUp().folderName());
        user.defaultSteps().inputsTextInElement(
            user.pages().FoldersAndLabelsSettingPage().newFolderPopUp().folderName(),
            folder
        );
        user.defaultSteps().clicksOn(user.pages().FoldersAndLabelsSettingPage().newFolderPopUp().create());
        return this;
    }

    @Step("Клик по сообщению в списке писем с номером «{0}»")
    public MessagePageSteps clicksOnMessageByNumber(final int i) {
        assertThat(
            "Страница с письмами не загрузилась",
            webDriverRule,
            withWaitFor(messageCountOnPage(greaterThan(0)))
        );
        user.defaultSteps().clicksOn(
            user.pages().MessagePage().displayedMessages().list().waitUntil(not(empty())).get(i).sender()
        );
        return this;
    }

    @Step("Должны оказаться на странице с сообщением: «{0}»")
    public MessagePageSteps shouldSeeDonePageWithMessage(final String message) {
        assertThat(
            "Не дождались страницы Done с сообщением: " + message,
            user.pages().MessagePage().doneTitle(),
            withWaitFor(allOf(isPresent(), hasText(message)))
        );
        return this;
    }

    @Step("Количество видимых сообщений на странице должно быть «{0}»")
    public MessagePageSteps shouldSeeMsgCount(int count) {
        shouldSeeMessagesPresent();
        assertThat("Число писем отлично от ожидаемого", webDriverRule, withWaitFor(messageCountOnPage(equalTo(count))));
        return this;
    }

    @Step("Убираем пользовательскую метку с выбранных сообщений")
    public MessagePageSteps unlabelsMessageWithCustomMark() {
        user.defaultSteps().shouldSee(user.pages().MessagePage().toolbar().markMessageDropDown())
            .clicksOn(user.pages().MessagePage().toolbar().markMessageDropDown())
            .shouldSee(user.pages().MessagePage().labelsDropdownMenu().unlabelCustomMark().get(0))
            .clicksOn(user.pages().MessagePage().labelsDropdownMenu().unlabelCustomMark().get(0))
            .waitInSeconds(1);
        return this;
    }

    @Step("Должны видеть количество писем на странице - {0}")
    public MessagePageSteps shouldSeeCorrectNumberOfMessages(int number) {
        assertThat(
            "Неверное количество писем на странице",
            user.pages().MessagePage().displayedMessages().list().size(),
            withWaitFor(equalTo(number))
        );
        return this;
    }

    @Step("Должны видеть {0} писем на странице, и {1} писем в раскрытом треде")
    public MessagePageSteps shouldSeeCorrectNumberOfMessages(int messages, int messagesInThread) {
        assertThat(
            "Неверное количество писем на странице",
            user.pages().MessagePage().displayedMessages().list().size(),
            is(messages)
        );
        assertThat(
            "Неверное количество писем из расрытого треда на странице",
            user.pages().MessagePage().displayedMessages().messagesInThread().size(),
            is(messagesInThread)
        );
        return this;
    }

    @Step("Скролим страницу вниз")
    public MessagePageSteps scrollDownPage() {
        assertThat(
            "Страница с письмами не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)))
        );
        JavascriptExecutor jse = (JavascriptExecutor) webDriverRule.getDriver();
        jse.executeScript("window.scrollBy(0,250)", "");
        return this;
    }

    @Step("Скролим страницу наверх")
    public MessagePageSteps scrollUpPage() {
        assertThat(
            "Страница с письмами не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)))
        );
        JavascriptExecutor jse = (JavascriptExecutor) webDriverRule.getDriver();
        jse.executeScript("window.scrollBy(0,-450)", "");
        return this;
    }

    @Step("Должно быть «{0}» сообщений в треде")
    public MessagePageSteps shouldSeeMessageCountInThread(int size) {
        assertThat(
            "Количество писем в треде не соответствует ожидаемому",
            webDriverRule,
            withWaitFor(messagesInThreadCount(size))
        );
        return this;

    }

    @Step("Должны видеть строгий список в контекстном меню: {0}")
    public MessagePageSteps shouldSeeItemsInAdditionalContextMenu(String[] items) {
        assertThat(
            "Количество писем в треде не соответствует ожидаемому",
            extract(
                user.pages().MessagePage().allMenuListInMsgList().get(1).itemListInMsgList(),
                ch.lambdaj.Lambda.on(MailElement.class).getText()
            ),
            contains(items)
        );
        return this;
    }

    @Step("Сообщения в треде с номерами «{1}» должны быть в папке «{0}»")
    public MessagePageSteps shouldSeeMessageFoldersInThread(String folder, int... indexes) {
        assertThat(
            "Нет сообщений в открытом треде",
            user.pages().MessagePage().displayedMessages().messagesInThread(),
            withWaitFor(hasSize(greaterThan(0)))
        );
        for (int i : indexes) {
            assertThat(
                format("Ожидалось, что у письма в треде с номером «%s» будет проставлена папка «%s»", i, folder),
                webDriverRule,
                withWaitFor(messagesInThreadShould(hasFolder(folder), i))
            );
        }
        return this;
    }

    @Step("Должны видеть, что сообщения с темами «{1}» относятся к папке «{0}»")
    public MessagePageSteps shouldSeeThatMessagesAreInFolder(String folder, String... subjects) {
        for (String subject : subjects) {
            shouldSeeMessageWithSubjectWithoutRefresh(subject);
            assertThat(
                "Ожидалось другое местоположение письма",
                findMessageBySubject(subject),
                messageFolder(hasText(containsString(folder)))
            );
        }
        return this;
    }

    @Step("Раскрываем тред с темой «{0}»")
    public MessagePageSteps expandsMessagesThread(String subject) {
        if (withWaitFor(not(isPresent())).matches(
            user.pages().MessagePage().displayedMessages().messagesInThread()
        )) {
            assertThat("Нет письма с нужной темой", webDriverRule, withWaitFor(existMessageWith(subject(subject))));
            user.defaultSteps().clicksOn(findMessageBySubject(subject).expandThread());
        }
        user.pages().MessagePage().displayedMessages().messagesInThread().waitUntil(
            "Тред не открылся",
            not(empty()),
            15
        );
        return this;
    }

    @Step("В треде с темой «{0}» должно быть «{1}» сообщений/я/е")
    public MessagePageSteps shouldSeeThreadCounter(String subject, int counter) {
        assertThat(
            "Нет тредов на странице",
            user.pages().MessagePage().displayedMessages().list(),
            hasSize(greaterThan(0))
        );
        assertThat(
            "Ожидалось другое количество писем в треде",
            webDriverRule,
            withWaitFor(threadCount(subject, counter))
        );
        return this;
    }

    @Step("Помечаем все сообщения как «Cпам»")
    public MessagePageSteps labelsAllMessagesAsSpam() {
        loadsMoreMessages();
        selectsAllDisplayedMessagesInFolder();
        if (hasSize(greaterThan(0)).matches(user.pages().MessagePage().displayedMessages().list())) {
            user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().spamButton());
        }
        return this;
    }

    @Step("Должны увидеть текст «{0}» в строке уведомлений")
    private void waitForTextInStatusLine(final String text) {
        assertThat("Текст в уведомлении не содержит нужного значения", webDriverRule, withWaitFor(containsText(text)));
    }

    @Step("Ждём загрузки списка писем")
    public MessagePageSteps shouldSeeMessagesPresent() {
        assertThat(
            "Страница с письмами не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)), SECONDS.toMillis(25))
        );
        return this;
    }

    @Step("Не должно быть писем на странице")
    public MessagePageSteps shouldNotSeeMessagesPresent() {
        assertThat(
            "На странице присутствуют письма",
            user.pages().MessagePage().displayedMessages().list(),
            withWaitFor(hasSize(0))
        );
        return this;
    }

    @Step("Письма не должны быть выбраны")
    public MessagePageSteps shouldSeeThatMessagesAreNotSelected() {
        assertThat(
            "Письма все еще выбраны",
            user.pages().MessagePage().displayedMessages().selectedMessages(),
            withWaitFor(hasSize(0))
        );
        return this;
    }

    @Step("Письма должны быть выбраны")
    public MessagePageSteps shouldSeeThatMessagesAreSelected() {
        assertThat(
            "Письма все еще выбраны",
            user.pages().MessagePage().displayedMessages().selectedMessages(),
            withWaitFor(hasSize(greaterThan(0)))
        );
        return this;
    }

    @Step("{0} писем должны быть выбраны")
    public MessagePageSteps shouldSeeThatNMessagesAreSelected(int messagesCount) {
        shouldSeeThatMessagesAreSelected();
        assertThat(
            "Количество выделенных писем отличается от ожидаемого.",
            user.pages().MessagePage().displayedMessages().selectedMessages(),
            hasSize(CoreMatchers.equalTo(messagesCount))
        );
        return this;
    }

    @Step("Должны видеть, что папка пуста")
    public MessagePageSteps shouldSeeThatFolderIsEmpty() {
        assertThat(
            "В папке все еще есть письма",
            user.pages().MessagePage().emptyFolder().inboxLink(),
            withWaitFor(isPresent())
        );
        return this;
    }

    @Step("Включаем группировку по теме, если выключена")
    public MessagePageSteps enablesGroupBySubject() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().mail360HeaderBlock().settingsMenu())
            .shouldSee(user.pages().MessagePage().mainSettingsPopupNew())
            .turnTrue(user.pages().MessagePage().mainSettingsPopupNew().settingsCheckboxes().get(2));
        return this;
    }

    @Step("Должны видеть сообщение удовлетворяющее шаблону «{0}»")
    public MessagePageSteps shouldSeeMessagesWithDatePattern(String datePattern) {
        int m = user.pages().MessagePage().displayedMessages().list().size();
        for (int i = 0; i < m; i++) {
            assertThat(
                "Письма неверно отфильтровались по дате",
                webDriverRule,
                withWaitFor(messageShould(hasDate(withPattern(datePattern)), i))
            );
        }
        return this;
    }

    @Step("Выключаем группировку по теме, если включена")
    public void disablesGroupBySubject() {
        user.defaultSteps().clicksOn(user.pages().MessagePage().mail360HeaderBlock().settingsMenu())
            .shouldSee(user.pages().MessagePage().mainSettingsPopupNew())
            .deselects(user.pages().MessagePage().mainSettingsPopupNew().settingsCheckboxes().get(2));
    }

    @Step("Выбираем произвольный цвет для метки")
    public String selectsRandomColorForLabel() {
        int index = Utils.getRandomNumber(user.pages().CustomButtonsPage()
            .configureLabelButton().labelColors().size() - 1, 0);
        String style = user.pages().CustomButtonsPage()
            .configureLabelButton().labelColors().get(index).getAttribute("style");
        user.defaultSteps().clicksOn(user.pages().CustomButtonsPage()
            .configureLabelButton().labelColors().get(index));
        return style;
    }

    @Step("Цвет метки должен соответствовать {0}")
    public MessagePageSteps shouldSeeThatLabelOnFirstMessageHasColor(String color) {
        String style = user.pages().MessagePage().displayedMessages().list().get(0).labels().get(0)
            .getAttribute("style");
        assertEquals(
            "Цвета меток не совпадают!" + substringAfter(color, "rgb") + ", " + substringAfter(style, "rgb"),
            substringAfter(color, "rgb"),
            substringAfter(style, "rgb")
        );
        return this;
    }

    @Step("Должны видеть блок с аттачами в выпадающем меню")
    public void shouldSeeDropdownListOfAttachments() {
        assertThat(
            "Блок аттачей не загрузился",
            user.pages().MessagePage().messagePageAttachmentsBlock(),
            withWaitFor(isPresent())
        );
        assertThat(
            "Аттачей должно быть больше",
            user.pages().MessagePage().messagePageAttachmentsBlock().attachmentsList(),
            hasSize(5)
        );
        assertThat(
            "Неверный аттач",
            user.pages().MessagePage().messagePageAttachmentsBlock().attachmentsList().get(0).title().getText(),
            equalTo("doc.pdf")
        );
        assertThat(
            "Неверный аттач",
            user.pages().MessagePage().messagePageAttachmentsBlock().attachmentsList().get(2).title().getText(),
            equalTo("test excel.xlsx")
        );
        assertThat(
            "Неверный аттач",
            user.pages().MessagePage().messagePageAttachmentsBlock().attachmentsList().get(3).title().getText(),
            equalTo("test word.docx")
        );
        assertThat(
            "Неверный аттач",
            user.pages().MessagePage().messagePageAttachmentsBlock().attachmentsList().get(4).title().getText(),
            equalTo("test txt.txt")
        );
    }

    public String getsMessageSubject(int i) {
        assertThat(
            "Страница не загрузилась",
            user.pages().MessagePage(),
            withWaitFor(messagePagesCount(greaterThan(0)))
        );
        return user.pages().MessagePage().displayedMessages().list().get(i).subject().getText();
    }

    @SkipIfFailed
    @Step("Удаляем все сообщения")
    public MessagePageSteps deleteAllMessage() {
        assumeStepCanContinue(user.pages().MessagePage().displayedMessages().list(), hasSize(greaterThan(0)));
        user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().selectAllMessages());
        user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().deleteButton());
        assertThat("Не удалились сообщения", webDriverRule, withWaitFor(messageCountOnPage(is(0))));
        return this;
    }

    @Step("Не должны видеть контекстное меню")
    public MessagePageSteps shouldNotSeeContextMenu() {
        assertThat("Всё ещё есть контекстное меню", user.pages().MessagePage().allMenuList(), hasSize(0));
        return this;
    }

    @Step("Должны видеть контекстное меню")
    public MessagePageSteps shouldSeeContextMenu() {
        assertThat("Не появилось контекстное меню", user.pages().MessagePage().allMenuList(), hasSize(1));
        return this;
    }

    @Step("Должны видеть контекстное меню(старое)")
    public MessagePageSteps shouldSeeContextMenuInMsgList() {
        assertThat("Не появилось контекстное меню", user.pages().MessagePage().allMenuListInMsgList(), hasSize(1));
        return this;
    }

    @Step("Должны видеть дополнительное контекстное меню")
    public MessagePageSteps shouldSeeAdditionalContextMenu() {
        user.defaultSteps().waitInSeconds(1);
        assertThat("Не появилось контекстное меню", user.pages().MessagePage().allMenuListInMsgList(), hasSize(2));
        return this;
    }

    @Step("Открываем просмотр письма на отдельной странице кликом по дате")
    public MessagePageSteps opensMsgFullView() {
        scrollUpPage();
        user.defaultSteps().clicksOn(user.pages().MessageViewPage().messageHead().messageDate());
        user.defaultSteps().switchOnJustOpenedWindow();
        return this;
    }

    @Step("Сообщения с темой «{0}» должны быть закреплены")
    public MessagePageSteps shouldSeeThatMessageIsPinned(String expectedSubject) {
        for (MessageBlock message : findAllMessageBySubject(expectedSubject)) {
            assertThat("Письмо не закрепилось", message, hasClass(containsString("is-pinned")));
        }
        return this;
    }

    @Step("Сообщения с темой «{0}» должны быть не закреплены")
    public MessagePageSteps shouldSeeThatMessageIsUnPinned(String expectedSubject) {
        for (MessageBlock message : findAllMessageBySubject(expectedSubject)) {
            assertThat("Письмо закреплено", message, not(hasClass(containsString("is-pinned"))));
        }
        return this;
    }

    @Step("Только одно сообщение с заголовком {0} должно быть запинено")
    public MessagePageSteps shouldPinOnlyOneMsgInThread(String subject) {
        List<MessageBlock> allMessages = findAllMessageBySubject(subject);
        assertThat("Первое письмо не закрепилось", allMessages.remove(0), hasClass(containsString("is-pinned")));
        for (MessageBlock message : allMessages) {
            assertThat("Остальные письма закрепились", message, not(hasClass(containsString("is-pinned"))));
        }
        return this;
    }

    @Step("Ждем загрузки всех картинок аттачей в развёрнутом виджете аттачей")
    public MessagePageSteps shouldSeeAllAttachmentInMsgWidget() {
        assertThat(
            String.format(
                "Все из элементов «%s» должны быть видны",
                user.pages().MessagePage().messagePageAttachmentsBlock().attachmentsList()
            ),
            user.pages().MessagePage().messagePageAttachmentsBlock().attachmentsList().waitUntil(not(empty())),
            everyItem(withWaitFor(isDisplayed()))
        );
        user.defaultSteps().waitInSeconds(2);
        return this;
    }

    @Step("Ждем загрузки всех картинок аттачей в списке писем")
    public MessagePageSteps shouldSeeAllAttachmentInMsgList() {
        user.pages().MessagePage().displayedMessages().list().forEach(
            (msg) -> assertThat(
                String.format("Все из элементов «%s» должны быть видны", msg.attachments().list()),
                msg.attachments().list().waitUntil(not(empty())),
                everyItem(withWaitFor(isDisplayed()))
            )
        );
        user.defaultSteps().waitInSeconds(2);
        return this;
    }

    @Step("Разворачиваем аттачи и ждем загрузки всех картинок аттачей в письме")
    public MessagePageSteps shouldSeeAllAttachmentInMsgView() {
        user.defaultSteps().onMouseHoverAndClick(user.pages().MessageViewPage().attachments().infoBtn());
        assertThat(
            String.format(
                "Все из элементов «%s» должны быть видны",
                user.pages().MessageViewPage().attachments().list()
            ),
            user.pages().MessageViewPage().attachments().list().waitUntil(not(empty())),
            everyItem(withWaitFor(isDisplayed()))
        );
        user.defaultSteps().waitInSeconds(2);
        return this;
    }

    @Step("Сообщение с индексом «{1}» должно быть помечено меткой «{0}»")
    public MessagePageSteps shouldSeeLabelOnMessageByIndex(String labelName, int index) {
        assertThat(
            "Письмо с таким индексом не существует",
            user.pages().MessagePage().displayedMessages().list().get(index),
            withWaitFor(isDisplayed())
        );
        assertThat(
            "На письме нет нужной метки",
            webDriverRule,
            messageShould(hasLabel(labelName.toLowerCase()), index)
        );
        return this;
    }

    @Step("Должны видеть, что сообщение с индексом «{1}» находится в папке «{0}»")
    public MessagePageSteps shouldSeeThatMessageWithIndexIsInFolder(String folder, int index) {
        assertThat(
            "Письмо с таким индексом не существует",
            user.pages().MessagePage().displayedMessages().list().get(index),
            withWaitFor(isDisplayed())
        );
        assertThat(
            "Ожидалось другое местоположение письма",
            user.pages().MessagePage().displayedMessages().list().get(index),
            messageFolder(hasText(containsString(folder)))
        );
        return this;
    }

    @Step("Открываем на просмотр аттач «{1}» из письма «{0}» в списке писем")
    public MessagePageSteps shouldOpenAttachInMessageList(int msg, int attach) {
        user.defaultSteps()
            .onMouseHover(
                user.pages().MessagePage().displayedMessages().list().get(msg).attachments().list().get(attach)
            )
            .clicksOn(
                user.pages().MessagePage().displayedMessages().list().get(msg).attachments().list().get(attach).show()
            )
            .shouldSee(user.pages().MessageViewPage().imageViewer())
            .shouldNotSee(user.pages().MessageViewPage().imageViewerLoader());
        return this;
    }

    @Step("Должны видеть письма с темой: «{0}», ждём с задержкой")
    public MessagePageSteps shouldSeeMessageWithSubjectWithWaiting(String... expectedSubjects) {
        assertThat(
            "Письмо отсутствует на странице",
            user.pages().MessagePage(),
            withWaitFor(
                messageWithSubjectExists(expectedSubjects),
                XIVA_TIMEOUT
            )
        );
        return this;
    }

    @Step("Возвращаем mid сообщения с индексом «{0}» внутри развёрнутого треда")
    public String getMidFromThreadByIndex(int index) {
        return user.pages().MessagePage().displayedMessages().messagesInThread().get(index).getAttribute("data-id");
    }

    @Step("Ширина инбокса должна совпадать с «{0}»")
    public MessagePageSteps shouldSeeInboxWidth(Integer width) {
        assertThat(
            "Неправильная ширина инбокса",
            Integer.valueOf(user.apiSettingsSteps().getUserSettings(SETTINGS_SIZE_VIEW_APP)),
            Matchers.equalTo(width)
        );
        return this;
    }

    @Step("Ширина инбокса в компактном режиме должна совпадать с «{0}»")
    public MessagePageSteps shouldSeeInboxCompactWidth(Integer width) {
        assertThat(
            "Неправильная ширина инбокса",
            Integer.valueOf(user.apiSettingsSteps().getUserSettings(SETTINGS_SIZE_VIEW_APP2)),
            Matchers.equalTo(width)
        );
        return this;
    }

    @SkipIfFailed
    @Step("Нажимаем на кнопку «Ещё письма» если она есть")
    public MessagePageSteps loadsMoreMessages() {
        assumeStepCanContinue(user.pages().MessagePage().loadMoreMessagesButton(), isPresent());
        user.defaultSteps().clicksOn(user.pages().MessagePage().loadMoreMessagesButton());
        assertThat("Новые письма не загрузились", user.pages().MessagePage().displayedMessages(), isPresent());
        return this;
    }

    @Step("Должны видеть окно со списком горячих клавиш")
    public void shouldSeeHotKeysInfo() {
        assertThat(
            "Окно со списком горячих клавиш не появилось",
            user.pages().HomePage().hotKeysHelp(), withWaitFor(isPresent())
        );
    }

    @Step("Отвечаем на письмо {0}")
    public MessagePageSteps replyToMessage(String subject) {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(user.pages().MessageViewPage().toolbar().replyButton())
            .clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().opensFragment(QuickFragments.INBOX)
            .refreshPage();
        return this;
    }

    @Step("Пересылаем письмо {0}")
    public MessagePageSteps forwardMessage(String subject, String to) {
        user.messagesSteps().selectMessageWithSubject(subject);
        user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().forwardButton())
            .clicksOn(user.pages().ComposePopup().expandedPopup().popupTo());
        user.pages().ComposePopup().expandedPopup().popupTo().sendKeys(to);
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.apiMessagesSteps().markAllMsgRead();
        return this;
    }

    @Step("Разворачиваем выпадушку «Напомнить позже» в тулбаре для письма {0}")
    public MessagePageSteps openReplyLaterDropdown(int n) {
        clicksOnMessageCheckBoxByNumber(n);
        user.defaultSteps().clicksOn(user.pages().MessagePage().toolbar().replyLaterBtn())
            .shouldSee(user.pages().MessagePage().replyLaterDropDown());
        return this;
    }
}
