package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Тест кнопки «Прочитано/Непрочитано» для списка писем в 3pane")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class ReadMessagesFromList3PaneTest extends BaseTest {

    private static final int THREAD_SIZE = 3;

    public AccLockRule lock = AccLockRule.use().useTusAccount();
    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiMessagesSteps().sendThread(lock.firstAcc(), getRandomString(), THREAD_SIZE);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessagesPresent();
    }

    @Test
    @Title("Пометить письмо треда непрочитанным, кликнув по прыщу в шапке письма")
    @TestCaseId("356")
    public void shouldSeeReadButtons() {
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().shouldSee(
            onMessagePage().displayedMessages().list().get(0).messageRead(),
            onMessageView().messageSubject().threadRead())
            .shouldNotSee(onMessageView().messageHead().messageUnread())
            .onMouseHoverAndClick(onMessageView().messageHead().messageRead())
            .shouldSee(
                onMessagePage().displayedMessages().list().get(0).messageUnread(),
                onMessageView().messageHead().messageUnread(),
                onMessageView().messageSubject().threadUnread()
            );
    }
}
