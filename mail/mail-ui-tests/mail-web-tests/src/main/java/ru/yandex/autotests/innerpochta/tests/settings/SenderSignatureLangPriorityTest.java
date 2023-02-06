package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;


/**
 * @author kurau
 */
@Aqua.Test
@Title("Проверка приоритета подписи")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SenderSignatureLangPriorityTest extends BaseTest {

    private final static String USER_SIGNATURE_RU = "Подпись твоя это";
    private final static String USER_SIGNATURE_ENG = "what does the fox say?";
    private final static String USER_SIGNATURE_TR = "Bu aradığınız droidler değil";
    private final static String MESSAGE_TEXT = "London is the capital of Great Britain";
    private final static String MESSAGE_SBJ = "ENG PLS";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiSettingsSteps().changeSignsWithTextAndAmount(
            sign(USER_SIGNATURE_TR).withLang("tr"),
            sign(USER_SIGNATURE_ENG).withLang("en"),
            sign(USER_SIGNATURE_RU).withLang("ru")
        );
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), MESSAGE_SBJ, MESSAGE_TEXT);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
        user.defaultSteps().shouldSeeElementsCount(
            onSenderInfoSettingsPage().blockSetupSender().signatures().signaturesList(),
            3
        );
        user.settingsSteps().shouldSeeSignatureWith(USER_SIGNATURE_TR, USER_SIGNATURE_RU, USER_SIGNATURE_ENG);
    }

    @Test
    @Title("Проверка приоритета подписи")
    @Description("В композе выставляется русская подпись, в ответе на Англ письмо - английская.")
    @TestCaseId("1859")
    public void signatureLangPriority() {
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE)
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), USER_SIGNATURE_RU)
            .opensDefaultUrl();
        user.defaultSteps()
            .clicksOn(onMessagePage().displayedMessages().list().get(0))
            .clicksOn(onMessageView().toolbar().replyButton())
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), USER_SIGNATURE_ENG);
    }
}
