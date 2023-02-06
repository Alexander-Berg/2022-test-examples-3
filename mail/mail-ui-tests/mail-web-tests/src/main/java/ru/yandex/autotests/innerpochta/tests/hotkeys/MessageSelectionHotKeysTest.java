package ru.yandex.autotests.innerpochta.tests.hotkeys;


import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;


/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на хот кеи для выделения писем для стандартного интерфейса")
@Features(FeaturesConst.HOT_KEYS)
@Tag(FeaturesConst.HOT_KEYS)
@Stories(FeaturesConst.GENERAL)
public class MessageSelectionHotKeysTest extends BaseTest {

    private static final int MSG_COUNT = 5;

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
    public void setUp() {
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), MSG_COUNT)
                .sendThread(lock.firstAcc(), Utils.getRandomName(), MSG_COUNT);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages());
    }

    @Test
    @Title("а - Выбираем все письма в папке")
    @TestCaseId("1449")
    public void testSelectAllMessagesHotKey() {
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_UP))
            .pressCombinationOfHotKeys(key(Keys.CONTROL), "a");
        user.messagesSteps().shouldSeeThatNMessagesAreSelected(MSG_COUNT + 1);
    }

    @Test
    @Title("SHIFT + UP/DOWN - Выделяем несколько писем")
    @TestCaseId("1450")
    public void testSelectMessagesWithShiftHotKey() {
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_RIGHT))
            .pressCombinationOfHotKeys(key(Keys.SHIFT), key(Keys.ARROW_DOWN))
            .pressSimpleHotKey(key(Keys.ARROW_DOWN))
            .pressCombinationOfHotKeys(key(Keys.SHIFT), key(Keys.ARROW_DOWN));
        user.messagesSteps().shouldSeeThatNMessagesAreSelected(4);
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), key(Keys.ARROW_UP));
        user.messagesSteps().shouldSeeThatNMessagesAreSelected(2);
    }
}
