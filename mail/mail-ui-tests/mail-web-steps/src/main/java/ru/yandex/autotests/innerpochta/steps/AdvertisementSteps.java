package ru.yandex.autotests.innerpochta.steps;

import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.FREEZE_DONE_SCRIPT;
import static ru.yandex.qatools.matchers.webdriver.ExistsMatcher.exists;

@SuppressWarnings("UnusedReturnValue")
public class AdvertisementSteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;

    AdvertisementSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    @Step("Видим рекламу над списком писем и в левой колонке")
    public AdvertisementSteps shouldSeeAdLineAndLeft() {
        user.defaultSteps().shouldSee(
            user.pages().MessagePage().directLine(),
            user.pages().MessagePage().directLeft()
        );
        return this;
    }

    @Step("Id рекламного блока совпадает с проверяемым значением")
    public AdvertisementSteps checkLineAndLeftBlockId(String line, String left) {
        user.defaultSteps()
            .shouldSeeThatElementTextEquals(user.pages().MessagePage().directLineInfo().blockId(), line)
            .shouldSeeThatElementTextEquals(user.pages().MessagePage().directLeftInfo().blockId(), left);
        return this;
    }

    @Step("Переходим между страницами и проверяем смену ID блоков")
    public AdvertisementSteps checkChangeBlocksId(String list, String compose, String done, String email) {
        user.defaultSteps()
            .shouldSeeThatElementTextEquals(user.pages().MessagePage().directLeftInfo().blockId(), list)
            .opensFragment(COMPOSE)
            .shouldSeeThatElementTextEquals(user.pages().MessagePage().directLeftInfo().blockId(), compose);
        user.composeSteps().inputsAddressInFieldTo(email)
            .inputsSubject(Utils.getRandomString());
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn())
            .shouldSee(user.pages().ComposePopup().doneScreen())
            .executesJavaScript(FREEZE_DONE_SCRIPT)
            .shouldSeeThatElementTextEquals(user.pages().MessagePage().directLeftInfo().blockId(), done);
        return this;
    }

    @Step("Отправляем письмо и переходим на Done без замораживания страницы")
    public AdvertisementSteps goToDone() {
        user.defaultSteps().opensFragment(COMPOSE)
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        user.composeSteps().inputsAddressInFieldTo(DEV_NULL_EMAIL)
            .inputsSubject(Utils.getRandomString());
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn())
            .shouldSee(user.pages().ComposePopup().doneScreen());
        return this;
    }

    @Step("Проверяем показ всех видов рекламы")
    public AdvertisementSteps shouldSeeAllAd() {
        user.pages().MessagePage().directLine().waitUntil(exists());
        user.pages().MessagePage().directLeft().waitUntil(exists());
        user.defaultSteps().shouldSee(
            user.pages().MessagePage().directLine(),
            user.pages().MessagePage().directLeft()
        );
        user.defaultSteps().opensFragment(QuickFragments.CONTACTS)
            .shouldSee(
                user.pages().MessagePage().directLine(),
                user.pages().MessagePage().directLeft()
            );
        return this;
    }

    @Step("Проверяем, что не показываем все виды рекламы")
    public AdvertisementSteps shouldNotSeeAllAd() {
        user.defaultSteps().shouldNotSee(
                user.pages().MessagePage().directLine(),
                user.pages().MessagePage().directLeft()
            )
            .opensFragment(QuickFragments.CONTACTS)
            .shouldNotSee(
                user.pages().MessagePage().directLine(),
                user.pages().MessagePage().directLeft()
            );
        return this;
    }
}