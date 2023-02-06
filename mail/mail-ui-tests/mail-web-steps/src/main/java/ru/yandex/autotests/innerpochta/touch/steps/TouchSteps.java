package ru.yandex.autotests.innerpochta.touch.steps;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot;
import ru.yandex.qatools.allure.annotations.Step;

import static org.aspectj.runtime.internal.Conversions.intValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ScriptNumbers.MIN_LEFT_SWIPE_MOVE;
import static ru.yandex.autotests.innerpochta.touch.data.ScriptNumbers.MIN_LONGSWIPE_MOVE;
import static ru.yandex.autotests.innerpochta.touch.data.ScriptNumbers.MIN_RIGHT_SWIPE_MOVE;
import static ru.yandex.autotests.innerpochta.touch.pages.ComposeIframePage.IFRAME_COMPOSE;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;

/**
 * @author oleshko
 */
public class TouchSteps {

    private WebDriverRule webDriverRule;
    private AllureStepStorage user;

    public TouchSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    @Step("Свайпнуть письмо «{0}» влево")
    public TouchSteps rightSwipe(WebElement element) {
        user.defaultSteps().swipe(element, MIN_RIGHT_SWIPE_MOVE, 0);
        return this;
    }

    @Step("Свайпнуть письмо «{0}» влево на «{1}» px")
    public TouchSteps rightSwipe(WebElement element, int move) {
        user.defaultSteps().swipe(element, move, 0);
        return this;
    }

    @Step("Свайпнуть письмо «{0}» вправо")
    public TouchSteps leftSwipe(WebElement element) {
        user.defaultSteps().swipe(element, MIN_LEFT_SWIPE_MOVE, 0);
        return this;
    }

    @Step("Долгое нажатие на элемент «{0}»")
    public TouchSteps longTap(WebElement element) {
        user.defaultSteps().shouldSee(element);
        new Actions(webDriverRule.getDriver()).clickAndHold(element).perform();
        user.defaultSteps().waitInSeconds(1);
        new Actions(webDriverRule.getDriver()).release().perform();
        return this;
    }

    @Step("Свайпнуть первое письмо вправо и удерживать")
    public TouchSteps leftSwipeMsgAndKeepHolding() {
        user.defaultSteps().shouldSee(user.touchPages().messageList().messageBlock());
        new Actions(webDriverRule.getDriver())
            .clickAndHold(user.touchPages().messageList().messages().waitUntil(not(empty())).get(0))
            .moveByOffset(80, 0)
            .perform();
        return this;
    }

    @Step("Открыть попап действий с письмом из свайп меню «{0}»-ого письма")
    public void openActionsForMessages(int msgNum) {
        rightSwipe(user.touchPages().messageList().messages().waitUntil(not(empty())).get(msgNum));
        user.defaultSteps()
            .clicksOnElementWithWaiting(user.touchPages().messageList().messages().get(msgNum).swipeFirstBtn())
            .shouldSee(user.touchPages().messageList().popup());
    }

    @Step("Скролим список писем вниз до подгрузки ещё одной порции писем")
    public int scrollMsgListDown() {
        int msgNum = user.touchPages().messageList().messages().waitUntil(not(empty())).size();
        user.defaultSteps().scrollTo(user.touchPages().messageList().messages().get(msgNum - 1))
            .waitInSeconds(5);
        assertThat(
            String.format(
                "Список писем не подскролился! Кол-во писем: %s",
                user.touchPages().messageList().messages().size()
            ),
            msgNum < user.touchPages().messageList().messages().size()
        );
        return msgNum;
    }

    @Step("Делаем лонгсвайп письма")
    public TouchSteps longSwipe(WebElement element) {
        user.defaultSteps().shouldSee(user.touchPages().messageList().messages().waitUntil(not(empty())));
        new Actions(webDriverRule.getDriver())
            .moveToElement(element, element.getSize().getWidth() - 5, 0)
            .clickAndHold()
            .moveByOffset(intValue(element.getSize().getWidth() * MIN_LONGSWIPE_MOVE) - 1, 0)
            .release()
            .perform();
        return this;
    }

    @Step("Делаем ptr")
    public TouchSteps ptr() {
        new Actions(webDriverRule.getDriver())
            .clickAndHold(user.touchPages().messageList().messageBlock())
            .moveByOffset(0, 200)
            .release()
            .perform();
        return this;
    }

    @Step("Открываем айфрейм-композ по прямому урлу")
    public TouchSteps openComposeViaUrl() {
        user.defaultSteps().opensDefaultUrlWithPostFix(COMPOSE.makeTouchUrlPart());
        switchToComposeIframe();
        return this;
    }

    @Step("Открываем композ через интерфейс")
    public TouchSteps openCompose() {
        user.defaultSteps().clicksOn(user.touchPages().messageList().headerBlock().compose());
        switchToComposeIframe();
        return this;
    }

    @Step("Переключаемся в айфрейм композа")
    public TouchSteps switchToComposeIframe() {
        user.defaultSteps().shouldSee(user.touchPages().composeIframe().iframe())
            .switchTo(IFRAME_COMPOSE);
        return this;
    }

    @Step("Присылаем письмо на email {0} с темой {1} и дисковым аттачем {3}, считая с конца списка")
    public TouchSteps sendMsgWithDiskAttaches(String email, String subject, int num) {
        openComposeViaUrl();
        user.defaultSteps().clicksAndInputsText(
            user.touchPages().composeIframe().inputSubject(),
            subject
        )
            .clicksAndInputsText(
            user.touchPages().composeIframe().inputTo(),
            email
        )
            .clicksOn(user.touchPages().composeIframe().header().clip())
            .clicksOn(user.touchPages().composeIframe().attachFilesPopup().fromDisk());
        //TODO: добавлять на диск папку и прикреплять её к письму
        //.clicksOn(steps.pages().touch().composeIframe().diskAttachmentsPage().checkbox().get(0))
        int attachCount = user.touchPages().composeIframe().diskAttachmentsPage().attachments()
            .waitUntil(IsNot.not(empty())).size();
        //Считаем аттачи с конца списка, чтобы была возможность прикрепить последний аттач(который не картинка)
        user.defaultSteps()
            .clicksOn(user.touchPages().composeIframe().diskAttachmentsPage().attachments().get(attachCount - num))
            .clicksOn(user.touchPages().composeIframe().diskAttachmentsPage().attachBtn());
        assertThat(
            "Аттач не загрузился",
            user.touchPages().composeIframe().attachments().uploadedAttachment(),
            withWaitFor(hasSize(1), 15000)
        );
        user.defaultSteps().clicksOn(user.touchPages().composeIframe().header().sendBtn())
            .shouldBeOnUrl(containsString(INBOX_FOLDER.makeTouchUrlPart()));
        return this;
    }
}
