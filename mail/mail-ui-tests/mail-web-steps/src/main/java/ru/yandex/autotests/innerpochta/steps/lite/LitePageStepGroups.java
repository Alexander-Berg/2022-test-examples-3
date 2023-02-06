package ru.yandex.autotests.innerpochta.steps.lite;

import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.qatools.allure.annotations.Step;

public class LitePageStepGroups {

    private AllureStepStorage user;

    public LitePageStepGroups(AllureStepStorage user) {
        this.user = user;
    }

    @Step("Помечаем сообщение меткой «{0}»")
    public void markMsgWithLabel(Integer numInSelect) {
        user.liteMailboxSteps().clicksMoreButton();
        user.liteMoreSteps().selectsLabelToMarkNumber(numInSelect);
        user.liteMoreSteps().clicksRunButton();
    }

    @Step("Убираем метку «{0}» с сообщения")
    public void unmarkMsgWithFromLabel(Integer numInSelect) {
        user.liteMailboxSteps().clicksMoreButton();
        user.liteMoreSteps().selectsLabelToUnmarkNumber(numInSelect);
        user.liteMoreSteps().clicksRunButton();
    }

    @Step("Помечаем прочитанным")
    public void marksRead() {
        user.liteMailboxSteps().clicksMoreButton();
        user.liteMoreSteps().clicksReadButton();
    }

    @Step("Помечаем непрочитанным")
    public void marksUnread() {
        user.liteMailboxSteps().clicksMoreButton();
        user.liteMoreSteps().clicksUnreadButton();
    }


    @Step("Пишем письмо")
    public void entersMail(String to, String subj, String text) {
        user.liteMailboxSteps().clicksComposeButton();
        user.composeLiteSteps().entersTo(to);
        user.composeLiteSteps().entersSubject(subj);
        user.composeLiteSteps().entersText(text);
    }

    @Step("Добавляем «{0}»")
    public void createsNewOneWithName(String name) {
        user.liteSettingsSteps().clicksOnAddButton();
        user.liteSettingsSteps().entersName(name);
        user.liteSettingsSteps().clicksOnSubmitButton();
    }

    @Step("Удаляем «{0}»")
    public void deletesOneWithName(String name) {
        user.liteSettingsSteps().selectsOneWithName(name);
        user.liteSettingsSteps().clicksOnRemoveButton();
        user.liteSettingsSteps().clicksOnSubmitButton();
    }

    @Step("Переименовываем «{0}» на «{1}»")
    public void renamesOneWithNameTo(String oldName, String newName) {
        user.liteSettingsSteps().selectsOneWithName(oldName);
        user.liteSettingsSteps().clicksOnRenameButton();
        user.liteSettingsSteps().entersName(newName);
        user.liteSettingsSteps().clicksOnSubmitButton();
    }


}