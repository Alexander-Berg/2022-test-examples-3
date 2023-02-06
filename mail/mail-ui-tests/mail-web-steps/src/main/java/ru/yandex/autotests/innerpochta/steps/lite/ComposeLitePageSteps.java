package ru.yandex.autotests.innerpochta.steps.lite;

import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.qatools.allure.annotations.Step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;


public class ComposeLitePageSteps {

    private AllureStepStorage user;

    public ComposeLitePageSteps(AllureStepStorage user) {
        this.user = user;
    }

    @Step("Вводим адрес «{0}»")
    public ComposeLitePageSteps entersTo(String to) {
        user.pages().ComposeLitePage().toInput().sendKeys(to);
        return this;
    }

    @Step("Вводим тему «{0}»")
    public ComposeLitePageSteps entersSubject(String subject) {
        user.pages().ComposeLitePage().subjInput().sendKeys(subject);
        return this;
    }

    @Step("Очищаем тему")
    public ComposeLitePageSteps clearsSubject() {
        user.pages().ComposeLitePage().subjInput().clear();
        return this;
    }

    @Step("Вводим текст «{0}»")
    public ComposeLitePageSteps entersText(String text) {
        user.pages().ComposeLitePage().sendInput().clear();
        user.pages().ComposeLitePage().sendInput().sendKeys(text);
        return this;
    }

    @Step("Отсылаем")
    public ComposeLitePageSteps clicksSendButton() {
        assertThat("Элемента нет на странице", user.pages().ComposeLitePage().submitButton(), isPresent());
        user.pages().ComposeLitePage().submitButton().click();
        return this;
    }

    @Step("Сохраняем")
    public ComposeLitePageSteps clicksSaveButton() {
        assertThat("Элемента нет на странице", user.pages().ComposeLitePage().saveButton(), isPresent());
        user.pages().ComposeLitePage().saveButton().click();
        return this;
    }

    @Step("Должны видеть количество вложений «{0}»")
    public ComposeLitePageSteps shouldSeeAttachesCount(Integer count) {
        assertEquals("Ожидалось другое количество аттачей", count, (Integer) user.pages().ComposeLitePage().attachesList().size());
        return this;
    }

    @Step("Должны видеть галочки аттачей «{0}»")
    public ComposeLitePageSteps shouldSeeAttachesCheckboxCount(Integer count) {
        assertEquals("Ожидалось другое количество галочек аттачей",
                count, (Integer) user.pages().ComposeLitePage().attachesCheckboxList().size());
        return this;
    }

    @Step("Поле кому должно содержать «{0}»")
    public ComposeLitePageSteps shouldSeeToFieldEquals(String value) {
        assertEquals("Поле для ввода адресата должно содержать: " + value,
                value, user.pages().ComposeLitePage().toInput().getAttribute("value"));
        return this;
    }

    @Step("Тема должна быть «{0}»")
    public ComposeLitePageSteps shouldSeeSubjectFieldEquals(String value) {
        assertEquals("Поле для ввода темы должно содержать: " + value,
                value, user.pages().ComposeLitePage().subjInput().getAttribute("value"));
        return this;
    }

    @Step("Вспоминаем тему")
    public String remembersSubjectFromSubjectInputField() {
        return user.pages().ComposeLitePage().subjInput().getAttribute("value");
    }

    @Step("Текст должен совпадать с «{0}»")
    public ComposeLitePageSteps shouldSeeSendInputEquals(String value) {
        assertEquals("Поле для ввода текста должно содержать: " + value,
                value, user.pages().ComposeLitePage().sendInput().getAttribute("value"));
        return this;
    }

    @Step("Не должны видеть тему «{0}»")
    public ComposeLitePageSteps shouldNotSeeEqualSubjectInInputField(String subject) {
        assertFalse("Ожидалось, темы будут различны. Получено: " + subject,
                subject.equals(user.pages().ComposeLitePage().subjInput().getAttribute("value")));
        return this;
    }

}