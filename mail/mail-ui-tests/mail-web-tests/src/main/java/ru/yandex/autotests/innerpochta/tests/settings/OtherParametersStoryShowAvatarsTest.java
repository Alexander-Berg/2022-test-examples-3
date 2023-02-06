package ru.yandex.autotests.innerpochta.tests.settings;

import com.yandex.xplat.testopithecus.MessageSpecBuilder;
import com.yandex.xplat.testopithecus.UserSpec;
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
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.USER_WITH_AVATAR_EMAIL;


@Aqua.Test
@Title("Настройки - Прочие - чекбокс показа портретов отправителей")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
public class OtherParametersStoryShowAvatarsTest extends BaseTest {

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
        user.imapSteps()
            .connectByImap()
            .addMessage(
                new MessageSpecBuilder().withDefaults()
                    .withSubject("123")
                    .withSender(new UserSpec(USER_WITH_AVATAR_EMAIL, "Other User"))
                    .build()
            )
            .closeConnection();
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Включаем аватарки в Настройках")
    @TestCaseId("1827")
    public void shouldTurnOnAvatars() {
        turnTrueAvatarSetting();
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages().list().get(0).avatarImg());
    }

    @Test
    @Title("Выключаем аватарки в Настройках")
    @TestCaseId("1830")
    public void shouldTurnOffAvatars() {
        deselectAvatarSetting();
        user.defaultSteps().shouldNotSee(onMessagePage().displayedMessages().list().get(0).avatarImg());
    }

    @Step("Включаем в Настройках чекбокс «Показывать портреты отправителей»")
    private void turnTrueAvatarSetting() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_OTHER)
            .turnTrue(onOtherSettings().blockSetupOther().topPanel().messagesAvatars());
        user.settingsSteps().saveSettingsIfCanAndClicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(0));
    }

    @Step("Выключаем в Настройках чекбокс «Показывать портреты отправителей»")
    private void deselectAvatarSetting() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_OTHER)
            .deselects(onOtherSettings().blockSetupOther().topPanel().messagesAvatars());
        user.settingsSteps().saveSettingsIfCanAndClicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(0));
    }
}