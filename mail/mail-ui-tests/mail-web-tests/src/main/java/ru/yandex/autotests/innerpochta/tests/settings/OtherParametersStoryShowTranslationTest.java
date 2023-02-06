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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_OFF;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_TRANSLATE;


/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Предлагать перевод писем")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
public class OtherParametersStoryShowTranslationTest extends BaseTest {

    private static final String LETTER_TEXT = "Are you going to be a teacher?";

    private String subject = Utils.getRandomName();
    private Message msg;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws InterruptedException, IOException {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем настройку предлагать перевод писем и включаем треды",
            of(
                SETTINGS_PARAM_TRANSLATE, STATUS_OFF,
                SETTINGS_FOLDER_THREAD_VIEW, TRUE
            )
        );
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, LETTER_TEXT);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Предлагать перевод писем")
    @TestCaseId("1833")
    public void shouldOnSettingShowTranslate() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_OTHER)
            .turnTrue(onOtherSettings().blockSetupOther().topPanel().translate())
            .clicksOn(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .shouldNotSee(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .shouldSee(onMessageView().translateNotification());
    }

    @Test
    @Title("Не предлагать перевод писем")
    @TestCaseId("1834")
    public void shouldOffSettingShowTranslate() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем настройку Предлагать перевод писем",
            of(SETTINGS_PARAM_TRANSLATE, STATUS_ON)
        );
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .shouldSee(onMessageView().messageSubjectInFullView())
            .shouldNotSee(onMessageView().translateNotification());
    }

    @Test
    @Title("Предлагать перевод писем 3pane + нетредный режим")
    @TestCaseId("2292")
    public void shouldOnSettingShowTranslate3pane() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane и отключаем треды",
            of(
                SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL,
                SETTINGS_FOLDER_THREAD_VIEW, FALSE
            )
        );
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_OTHER)
            .turnTrue(onOtherSettings().blockSetupOther().topPanel().translate())
            .clicksOn(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .shouldNotSee(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .shouldSee(onMessageView().translateNotification());
    }
}
