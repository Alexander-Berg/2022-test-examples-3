package ru.yandex.autotests.innerpochta.tests.autotests;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.MSGHEADERS;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getUserUid;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на кнопку «Свойства письма»")
@Features({FeaturesConst.OTHER})
@Stories(FeaturesConst.GENERAL)
public class MsgHeadersTest {

    private static final String MESSAGE_HEADERS_URL_PART = "/web-api/message-source/v1/%s/%s/yandex_email.eml";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule acc = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().apiMessagesSteps().sendMailWithNoSave(acc.firstAcc(), getRandomName(), "");
        steps.user().apiMessagesSteps().sendThread(acc.firstAcc(), getRandomName(), 2);
        steps.user().loginSteps().forAcc(acc.firstAcc()).logins();
    }

    @Test
    @Title("Должны открыть свойства письма из свайп-меню")
    @TestCaseId("1337")
    public void shouldOpenMagHeadersFromSwipeMenu() {
        steps.user().touchSteps().openActionsForMessages(1);
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MSGHEADERS.btn())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(
                String.format(
                    MESSAGE_HEADERS_URL_PART,
                    getUserUid(acc.firstAcc().getLogin()),
                    steps.user().apiMessagesSteps().getAllMessages().get(2).getMid()
                )
            ));
    }

    @Test
    @Title("Должны открыть свойства письма из просмотра письма")
    @TestCaseId("1338")
    public void shouldOpenMagHeadersFromMsgView() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageView().moreBtn())
            .shouldSee(steps.pages().touch().messageList().popup())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MSGHEADERS.btn())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(
                String.format(
                    MESSAGE_HEADERS_URL_PART,
                    getUserUid(acc.firstAcc().getLogin()),
                    steps.user().apiMessagesSteps().getAllMessages().get(0).getMid()
                )
            ));
    }
}
