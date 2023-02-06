package ru.yandex.autotests.innerpochta.tests.compose;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
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

import static org.openqa.selenium.Keys.ENTER;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Новый композ - Действия с ябблами")
@Features({FeaturesConst.COMPOSE})
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.YABBLE)
public class HotkeysYabbleTest extends BaseTest {

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
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.COMPOSE);
    }

    @Test
    @Title("Копирование адреса из дропдауна ябла")
    @TestCaseId("2813")
    public void shouldCopyAddressFromYabbleDropdown() {
        user.composeSteps().inputsAddressInFieldTo(DEV_NULL_EMAIL);
        user.hotkeySteps().pressHotKeys(onComposePopup().expandedPopup().popupTo(), ENTER);
        user.defaultSteps().clicksOn(onComposePopup().yabbleTo())
            .clicksOn(onComposePopup().yabbleDropdown().copyEmail(), onComposePopup().expandedPopup().bodyInput());
        user.hotkeySteps().pressHotKeysWithDestination(
            onComposePopup().expandedPopup().bodyInput(),
            Keys.chord(Keys.CONTROL, "v")
        );
        user.defaultSteps().shouldContainText(onComposePopup().expandedPopup().bodyInput(), DEV_NULL_EMAIL);
    }

    @Test
    @Title("Копирование адреса через ctrl+c")
    @TestCaseId("2839")
    public void shouldCopyAddressWithHotKey() {
        user.composeSteps().inputsAddressInFieldTo(DEV_NULL_EMAIL);
        user.hotkeySteps()
            .pressCombinationOfHotKeys(onComposePopup().expandedPopup().popupTo(), key(Keys.CONTROL), "a")
            .pressCombinationOfHotKeys(onComposePopup().expandedPopup().popupTo(), key(Keys.CONTROL), "c")
            .pressCombinationOfHotKeys(onComposePopup().expandedPopup().bodyInput(), key(Keys.CONTROL), "v");
        user.defaultSteps().shouldContainText(onComposePopup().expandedPopup().bodyInput(), DEV_NULL_EMAIL);
    }

    @Test
    @Title("Вставка адреса через ctrl+v")
    @TestCaseId("2840")
    public void shouldPasteAddressWithHotKey() {
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().bodyInput(), DEV_NULL_EMAIL);
        user.hotkeySteps()
            .pressHotKeys(onComposePopup().expandedPopup().bodyInput(), Keys.chord(Keys.CONTROL, "a"))
            .pressCombinationOfHotKeys(key(Keys.CONTROL), "c")
            .pressCombinationOfHotKeys(onComposePopup().expandedPopup().popupTo(), key(Keys.CONTROL), "v")
            .pressHotKeys(onComposePopup().expandedPopup().popupTo(), ENTER);
        user.defaultSteps().shouldSee(onComposePopup().yabbleTo());
    }

    @Test
    @Title("Отменяем ввод через ctrl+z")
    @TestCaseId("3100")
    public void shouldRollBackSymbolsByHotKeys() {
        user.defaultSteps()
                .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), getRandomString());
        user.hotkeySteps()
                .pressHotKeys(onComposePopup().expandedPopup().popupTo(), Keys.chord(Keys.CONTROL, "z"));
        user.defaultSteps().shouldContainText(onComposePopup().expandedPopup().popupTo(), "");
    }

}
