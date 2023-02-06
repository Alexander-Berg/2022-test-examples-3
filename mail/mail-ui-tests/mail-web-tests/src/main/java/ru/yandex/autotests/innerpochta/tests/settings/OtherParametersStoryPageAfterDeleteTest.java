package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.hamcrest.Matcher;
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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static org.cthul.matchers.CthulMatchers.either;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_UNION_AVATARS;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Страница после удаления письма")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
public class OtherParametersStoryPageAfterDeleteTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем чекбоксы, выключаем треды",
            of(
                SETTINGS_PARAM_MESSAGE_UNION_AVATARS, FALSE,
                SETTINGS_FOLDER_THREAD_VIEW, FALSE
            )
        );
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.composeSteps().sendDraftWithContentFor(
            lock.firstAcc().getSelfEmail(),
            Utils.getRandomName(),
            Utils.getRandomString()
        );
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_OTHER);
    }


    @Test
    @Title("После удаления письма остаёмся там же")
    @TestCaseId("1802")
    public void testPageCurrentAfterDelete() {
        pageAfterDelete(
            0,
            either(endsWith("#sent")).or(containsString("#folder/")),
            onMessagePage().foldersNavigation().sentFolder()
        );
    }

    @Test
    @Title("После удаления письма переходим на следующее письмо")
    @TestCaseId("1804")
    public void testPageNextMsgAfterDelete() {
        user.defaultSteps()
            .shouldSeeElementsCount(
                onOtherSettings().blockSetupOther().bottomPanel().pageAfterOptionsList(),
                3
            )
            .clicksOn(onOtherSettings().blockSetupOther().bottomPanel().pageAfterOptionsList().get(1))
            .clicksOn(onSettingsPage().selectConditionDropdown().conditionsList().get(1));
        user.settingsSteps().saveSettingsIfCanAndClicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(0));
        String subject = user.messagesSteps().getsMessageSubject(1);
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().clicksOn(onMessageView().toolbar().deleteButton())
            .shouldBeOnUrl(containsString("#message"));
        user.messageViewSteps().shouldSeeMessageSubject(subject);
    }

    private void pageAfterDelete(Integer selectNum, Matcher<String> shouldSeeInUrl, MailElement... elements) {
        user.defaultSteps()
            .shouldSeeElementsCount(
                onOtherSettings().blockSetupOther().bottomPanel().pageAfterOptionsList(),
                3
            )
            .clicksOn(onOtherSettings().blockSetupOther().bottomPanel().pageAfterOptionsList().get(1))
            .clicksOn(onSettingsPage().selectConditionDropdown().conditionsList().get(selectNum));
        user.settingsSteps().saveSettingsIfCanAndClicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(0));
        user.defaultSteps().clicksOn(elements);
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().clicksOn(onMessageView().toolbar().deleteButton())
            .shouldBeOnUrl(shouldSeeInUrl);
    }
}
