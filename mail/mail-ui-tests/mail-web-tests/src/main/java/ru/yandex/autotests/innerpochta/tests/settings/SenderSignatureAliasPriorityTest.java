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
import ru.yandex.autotests.innerpochta.util.MailConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.singletonList;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;


/**
 * Created by kurau
 */
@Aqua.Test
@Title("Проверка приоритета подписи")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SenderSignatureAliasPriorityTest extends BaseTest {

    private final static String FIX_USER_SIGNATURE = "Signature in ENGlish With Alias";

    private String tmpSignature = Utils.getRandomString();
    private String sbj = Utils.getRandomName();

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
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
        user.apiSettingsSteps().changeSignsWithTextAndAmount(
            sign(tmpSignature).withEmails(singletonList(lock.firstAcc().getLogin() + MailConst.DOMAIN_YARU)),
            sign(FIX_USER_SIGNATURE).withEmails(singletonList(lock.firstAcc().getSelfEmail()))
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
        user.settingsSteps().shouldSeeSignatureWith(tmpSignature, FIX_USER_SIGNATURE);
    }

    @Test
    @Title("Проверка приоритета подписи")
    @TestCaseId("1856")
    public void signatureAliasPriority() {
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE)
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), FIX_USER_SIGNATURE);
        user.composeSteps().inputsAddressInFieldTo(lock.firstAcc().getSelfEmail())
            .inputsSubject(sbj)
            .clicksOnSendButtonInHeader()
            .waitForMessageToBeSend();
        user.defaultSteps().clicksOn(onComposePopup().doneScreenInboxLink());
        user.messagesSteps().clicksOnMessageWithSubject(sbj);
        user.messageViewSteps().shouldSeeCorrectMessageText(FIX_USER_SIGNATURE);
    }
}

