package ru.yandex.autotests.innerpochta.steps;

import ru.yandex.autotests.innerpochta.annotations.SkipIfFailed;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static ru.yandex.autotests.innerpochta.util.SkipStep.SkipStepMethods.assumeStepCanContinue;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * User: alex89
 * Date: 10.10.12
 */
public class MessageViewSteps {

    private AllureStepStorage user;

    MessageViewSteps(AllureStepStorage user) {
        this.user = user;
    }

    @Step("Текст сообщения должен содержать «{0}»")
    public MessageViewSteps shouldSeeCorrectMessageText(String text) {
        assertThat("Некорректный текст письма", user.pages().MessageViewPage().messageTextBlock(),
            withWaitFor(allOf(isPresent(), hasText(containsString(text)))));
        return this;
    }

    @Step("Должны видеть тему сообщения в письме на отдельной странице: «{0}»")
    public MessageViewSteps shouldSeeMessageSubject(String messageSubject) {
        assertThat("Письмо не открылось", user.pages().MessageViewPage().messageSubjectInFullView(),
            withWaitFor(allOf(exists(), isDisplayed())));
        assertThat("Перешли на неверное письмо", user.pages().MessageViewPage().messageSubjectInFullView(),
            withWaitFor(hasText(messageSubject)));
        return this;
    }

    @Step("Должны видеть тему сообщения в открытом письме в списке писем: «{0}»")
    public MessageViewSteps shouldSeeMessageSubjectInCompactView(String messageSubject) {
        assertThat("Письмо не открылось", user.pages().MessageViewPage().messageSubject().subject(),
            withWaitFor(allOf(exists(), isDisplayed())));
        assertThat("Перешли на неверное письмо", user.pages().MessageViewPage().messageSubject().subject(),
            withWaitFor(hasText(messageSubject)));
        return this;
    }

    @SkipIfFailed
    @Step("Разворачиваем информацию о получателях кликом по стрелочке.")
    public MessageViewSteps expandCcAndBccBlock() {
        assumeStepCanContinue(
            user.pages().MessageViewPage().messageHead().contactsInTo().get(0),
            not(isPresent())
        );
        user.defaultSteps().clicksOn(user.pages().MessageViewPage().messageHead().showFieldToggler());
        assertThat("Блок информации о получателях не открылся",
            (user.pages().MessageViewPage().messageHead().contactsInTo().get(0)), isPresent());
        return this;
    }

    @Step("Должны видеть «{0}» развернутых сообщений в треде ")
    public MessageViewSteps shouldSeeExpandedMsgInThread(Integer number) {
        assertThat("Неверное количество раскрытых сообщений",
            Integer.valueOf(user.pages().MessageViewPage().expandMsgInThread().size()), equalTo(number));
        return this;
    }

    @Step("Открываем QR и вводим текст")
    public MessageViewSteps openQRAndInputText(String text) {
        user.defaultSteps().clicksOn(user.pages().MessageViewPage().quickReplyPlaceholder())
            .shouldSee(user.pages().MessageViewPage().quickReply().replyText())
            .inputsTextInElement(user.pages().MessageViewPage().quickReply().replyText(), text);
        return this;
    }
}
