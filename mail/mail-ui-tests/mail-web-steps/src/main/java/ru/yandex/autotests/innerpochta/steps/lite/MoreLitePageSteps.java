package ru.yandex.autotests.innerpochta.steps.lite;

import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.qatools.allure.annotations.Step;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;

public class MoreLitePageSteps {

    private AllureStepStorage user;

    public MoreLitePageSteps(AllureStepStorage user) {
        this.user = user;
    }
    
    @Step("Ставим метку «{0}»")
    public void selectsLabelToMarkNumber(Integer num) {
        user.pages().MoreLitePage().selectLid().getOptionByNumber(num).click();
    }

    @Step("Убираем метку «{0}»")
    public void selectsLabelToUnmarkNumber(Integer num) {
        user.pages().MoreLitePage().selectUnlid().getOptionByNumber(num).click();
    }

    @Step("Нажимаем «Выполнить»")
    public void clicksRunButton() {
        assertThat("Элемента нет на странице", user.pages().MoreLitePage().runButton(), isPresent());
        user.pages().MoreLitePage().runButton().click();
    }

    @Step("Нажимаем «Прочитано»")
    public void clicksReadButton() {
        assertThat("Элемента нет на странице", user.pages().MoreLitePage().readButton(), isPresent());
        user.pages().MoreLitePage().readButton().click();
    }

    @Step("Нажимаем «Непрочитано»")
    public void clicksUnreadButton() {
        assertThat("Элемента нет на странице", user.pages().MoreLitePage().unreadButton(), isPresent());
        user.pages().MoreLitePage().unreadButton().click();
    }

}