package ru.yandex.autotests.innerpochta.steps.lite;

import org.openqa.selenium.By;
import ru.yandex.autotests.innerpochta.annotations.SkipIfFailed;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Step;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.util.SkipStep.SkipStepMethods.assumeStepCanContinue;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;

public class MailboxListLitePageSteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;

    public MailboxListLitePageSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    @Step("Переводим чекбокс в положение «{1}»")
    public MailboxListLitePageSteps checkMessageWithNumber(Integer num) {
        (user.pages().MailboxListLitePage().messages().get(num).findElement(By.tagName("input"))).click();
        return this;
    }

    @SkipIfFailed
    @Step("Все чекбоксы в положение «{0}»")
    public MailboxListLitePageSteps turnsCheckboxCheckAllTo() {
        assumeStepCanContinue(user.pages().MailboxListLitePage().checkAllCheckbox(), isPresent());
        user.pages().MailboxListLitePage().checkAllCheckbox().click();
        return this;
    }


    @Step("Клик по кнопке «Ещё письма»")
    public MailboxListLitePageSteps clicksMoreButton() {
        assertThat("Элемента нет на странице", user.pages().MailboxListLitePage().moreButton(), isPresent());
        user.pages().MailboxListLitePage().moreButton().click();
        return this;
    }


    @Step("Письмо «{0}» должно быть в состоянии важности «{1}»")
    public MailboxListLitePageSteps shouldSeeMailNumberAsImportant(Integer num, Boolean value) {
        assertEquals(
            "На письме номер " + num.toString() + " ожидалось другое состояние важности",
            value,
            Utils.isSubElementAvailable(
                user.pages().MailboxListLitePage().messages().get(num),
                By.cssSelector("span>img")
            )
        );
        return this;
    }

    @Step("Все сообщения должны быть важными")
    public MailboxListLitePageSteps shouldSeeThatAllMessagesIsImportant() {
        shouldSeeImportantFlagsCountIs(user.pages().MailboxListLitePage().messages().size());
        return this;
    }


    @Step("Количество флажков важности «{0}»")
    public MailboxListLitePageSteps shouldSeeImportantFlagsCountIs(Integer count) {
        assertEquals("Ожидается другое количество флагов",
            count, (Integer) user.pages().MailboxListLitePage().importantFlags().size());
        return this;
    }

    @Step("Количество сообщений на странице")
    public Integer remembersMailCount() {
        return user.pages().MailboxListLitePage().messages().size();
    }

    @Step("Должны видеть «{0}» сообщений на странице")
    public MailboxListLitePageSteps shouldSeeMailCountIs(Integer count) {
        assertEquals("Ожидается другое количество писем",
            count, (Integer) user.pages().MailboxListLitePage().messages().size());
        return this;
    }


    @Step("Все сообщения должны быть непрочитанными")
    public MailboxListLitePageSteps shouldSeeThatAllMsgsInPageIsUnread() {
        shouldSeeUnreadMsgsCountIs(user.pages().MailboxListLitePage().messages().size());
        return this;
    }

    @Step("Непрочитанных писем должно быть «{0}»")
    public MailboxListLitePageSteps shouldSeeUnreadMsgsCountIs(Integer count) {
        assertEquals("Ожидается другое количество непрочитанных писем",
            count, (Integer) user.pages().MailboxListLitePage().unreadMessages().size());
        return this;
    }


    @Step("Сообщение с номером «{0}» должно быть в состоянии непрочитанности «{1}»")
    public MailboxListLitePageSteps shouldSeeThatMsgHaveIconUnread(Integer num, Boolean value) {
        assertEquals(
            "Ожидается другое состояние непрочитанности",
            value,
            Utils.isSubElementAvailable(
                user.pages().MailboxListLitePage().messages().get(num),
                By.cssSelector(".b-mail-icon_new")
            )
        );
        return this;
    }

    @Step("Клик по «Написать письмо»")
    public MailboxListLitePageSteps clicksComposeButton() {
        assertThat("Элемента нет на странице", user.pages().MailboxListLitePage().composeButton(), isPresent());
        user.pages().MailboxListLitePage().composeButton().click();
        return this;
    }

    @Step("Клик по сообщению с темой «{0}»")
    public MailboxListLitePageSteps clicksOnMailWithSubject(String subj) {
        final String xpath = "//span[@class='b-messages__subject' and contains(.,'" + subj + "')]";
        assertThat("Элемента нет на странице", user.pages().MailboxListLitePage().checkmailButton(), isPresent());
        user.pages().MailboxListLitePage().checkmailButton().click();
        webDriverRule.getDriver().findElement(By.xpath(xpath)).click();
        return this;

    }

    @Step("Клик по сообщению без ожидания")
    public MailboxListLitePageSteps clicksWithoutWaitOnMailWithSubject(String subj) {
        final String xpath = "//span[@class='b-messages__subject' and contains(.,'" + subj + "')]";
        webDriverRule.getDriver().findElement(By.xpath(xpath)).click();
        return this;
    }

    @Step("Клик по «Удалить»")
    public MailboxListLitePageSteps clicksOnDeleteButton() {
        assertThat("Элемента нет на странице", user.pages().MailboxListLitePage().deleteButton(), isPresent());
        user.pages().MailboxListLitePage().deleteButton().click();
        return this;
    }

    @Step("Клик по «Переслать»")
    public MailboxListLitePageSteps clicksOnForwardButton() {
        assertThat("Элемента нет на странице", user.pages().MailboxListLitePage().forwardButton(), isPresent());
        user.pages().MailboxListLitePage().forwardButton().click();
        return this;
    }

    @Step("Клик по «Спам»")
    public MailboxListLitePageSteps clicksOnToSpamButton() {
        assertThat("Элемента нет на странице", user.pages().MailboxListLitePage().tospamButton(), isPresent());
        user.pages().MailboxListLitePage().tospamButton().click();
        return this;
    }

    @Step("Клик по «Не спам»")
    public MailboxListLitePageSteps clicksOnNotSpamButton() {
        assertThat("Элемента нет на странице", user.pages().MailboxListLitePage().notspamButton(), isPresent());
        user.pages().MailboxListLitePage().notspamButton().click();
        return this;
    }

    @Step("Клик по черновикам")
    public MailboxListLitePageSteps clicksOnDraftsLink() {
        assertThat("Элемента нет на странице", user.pages().MailboxListLitePage().draftsLink(), isPresent());
        user.pages().MailboxListLitePage().draftsLink().click();
        return this;
    }

    @Step("Клик по спаму")
    public MailboxListLitePageSteps clicksOnSpamLink() {
        assertThat("Элемента нет на странице", user.pages().MailboxListLitePage().spamLink(), isPresent());
        user.pages().MailboxListLitePage().spamLink().click();
        return this;
    }

    @Step("Клик по сообщению с номером «{0}»")
    public MailboxListLitePageSteps clicksOnMailNumber(Integer num) {
        user.pages().MailboxListLitePage().messages().get(num)
            .findElement(By.cssSelector(".b-messages__message__link")).click();
        return this;
    }


    @Step("Клик по настройкам")
    public MailboxListLitePageSteps clicksOnSettingsLink() {
        assertThat("Элемента нет на странице", user.pages().MailboxListLitePage().settingsLink(), isPresent());
        user.pages().MailboxListLitePage().settingsLink().click();
        return this;
    }

}