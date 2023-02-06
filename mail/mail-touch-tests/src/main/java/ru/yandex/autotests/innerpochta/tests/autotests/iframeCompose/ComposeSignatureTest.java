package ru.yandex.autotests.innerpochta.tests.autotests.iframeCompose;

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

import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.DRAFT_FOLDER;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на попап подписей в композе")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ADDITIONAL)
public class ComposeSignatureTest {

    private static final String SECOND_USER_SIGNATURE = "Second added signature";
    private static final String FIRST_USER_SIGNATURE = "First added signature.user";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        steps.user().apiSettingsSteps()
            .changeSignsWithTextAndAmount(sign(FIRST_USER_SIGNATURE), sign(SECOND_USER_SIGNATURE));
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().touchSteps().openComposeViaUrl();
    }

    @Test
    @Title("Должны закрыть попап подписей")
    @TestCaseId("1520")
    public void shouldCloseSignaturePopup() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().composeIframe().signBtn())
            .shouldSee(steps.pages().touch().composeIframe().signPopup())
            .clicksOn(steps.pages().touch().composeIframe().signPopup().closeBtn())
            .shouldNotSee(steps.pages().touch().composeIframe().signPopup());
    }

    @Test
    @Title("Должны видеть подпись в черновике")
    @TestCaseId("1421")
    public void shouldSeeSignatureInDraft() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().composeIframe().signBtn())
            .shouldContainText(steps.pages().touch().composeIframe().signature(), SECOND_USER_SIGNATURE)
            .clicksOnElementWithText(steps.pages().touch().composeIframe().signPopup().signList(), FIRST_USER_SIGNATURE)
            .clicksOn(steps.pages().touch().composeIframe().header().closeBtn())
            .shouldBeOnUrlWith(INBOX_FOLDER)
            .opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(DRAFT_FOLDER))
            .clicksOn(steps.pages().touch().messageList().messages().get(0));
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps()
            .shouldContainText(steps.pages().touch().composeIframe().signature(), FIRST_USER_SIGNATURE);
    }
}
