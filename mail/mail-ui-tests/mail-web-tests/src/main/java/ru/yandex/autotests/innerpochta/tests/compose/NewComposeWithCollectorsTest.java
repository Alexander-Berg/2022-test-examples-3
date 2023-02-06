package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Сборщики")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
public class NewComposeWithCollectorsTest extends BaseTest {

    public String sbj = getRandomString();
    public String msjBody = getRandomString();

    private AccLockRule lock = AccLockRule.use().useTusAccount(COLLECTOR);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем тредный режим",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Подставляем адрес сборщика в поле «От кого»")
    @TestCaseId("4302")
    public void shouldSendFromCollectorsEmail() {
        user.defaultSteps().clicksOn(onHomePage().composeButton())
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .clicksOn(onComposePopup().expandedPopup().popupFrom())
            .clicksOn(onComposePopup().yabbleDropdown().changeEmail().get(4))
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), sbj)
            .inputsTextInElement(onComposePopup().expandedPopup().bodyInput(), msjBody)
            .clicksOn(onComposePopup().expandedPopup().sendBtn())
            .opensFragment(QuickFragments.SENT);
        user.messagesSteps().clicksOnMessageWithSubject(sbj);
        user.defaultSteps().shouldContainText(onMessageView().messageHead().fromAddress(), MAIL_COLLECTOR);
    }

}
