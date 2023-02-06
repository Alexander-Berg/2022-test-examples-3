package ru.yandex.autotests.innerpochta.tests.hotkeys;


import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collection;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на хот кеи при написании письма для стандартного и 3pane horizintal")
@Features(FeaturesConst.HOT_KEYS)
@Tag(FeaturesConst.HOT_KEYS)
@Stories(FeaturesConst.GENERAL)
@RunWith(Parameterized.class)
public class ComposeMessageHotKeysTest extends BaseTest {

    private String subject;

    @Parameterized.Parameter
    public String layoutType;

    @Parameterized.Parameters(name = "layoutID: {0}")
    public static Collection<Object[]> testData() {
        Object[][] data = new Object[][]{
            {LAYOUT_2PANE},
            {SETTINGS_LAYOUT_3PANE_HORIZONTAL}
        };
        return Arrays.asList(data);
    }

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth).around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams("Переключаем интерфейс", of(SETTINGS_PARAM_LAYOUT, layoutType));
        subject = Utils.getRandomString();
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.COMPOSE);
    }

    @Test
    @Title("CONTROL + RETURN - Отправить")
    @TestCaseId("1409")
    public void testSendMessageHotKey() {
        user.composeSteps().inputsAddressInFieldTo(lock.firstAcc().getSelfEmail())
            .inputsSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(
            onComposePopup().expandedPopup().bodyInput(),
            key(Keys.CONTROL),
            key(Keys.ENTER)
        );
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().opensFragment(QuickFragments.SENT);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }

    @Test
    @Title("CONTROL + s - Сохранить как черновик")
    @TestCaseId("1410")
    public void testSaveAsDraftHotKey() {
        user.composeSteps().inputsAddressInFieldTo(lock.firstAcc().getSelfEmail())
            .inputsSubject(subject);
        user.hotkeySteps().pressCombinationOfHotKeys(
            onComposePopup().expandedPopup().bodyInput(),
            key(Keys.CONTROL),
            "s"
        );
        user.composeSteps().shouldSeeThatMessageIsSavedToDraft()
            .closeComposePopup();
        user.defaultSteps().opensFragment(QuickFragments.DRAFT);
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
    }
}
