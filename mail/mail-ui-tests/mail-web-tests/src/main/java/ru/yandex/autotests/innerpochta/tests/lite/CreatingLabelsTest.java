package ru.yandex.autotests.innerpochta.tests.lite;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
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

@Aqua.Test
@Title("Создание меток")
@Features(FeaturesConst.LITE)
@Tag(FeaturesConst.LITE)
@Stories(FeaturesConst.GENERAL)
public class CreatingLabelsTest extends BaseTest {

    private String labelName = Utils.getRandomString();
    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().className();

    @Before
    public void logIn() throws InterruptedException {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().openLightMail();
        user.liteMailboxSteps().clicksOnSettingsLink();
        user.liteSettingsSteps().clicksOnLabelsLink();
    }

    @Test
    @Title("Создаём метку")
    @TestCaseId("33")
    public void testCreateLabel() {
        user.liteInterfaceSteps().createsNewOneWithName(labelName);
        user.defaultSteps().shouldSeeElementInList(onSettingsLitePage().labelNamesList(), labelName);
        user.liteInterfaceSteps().deletesOneWithName(labelName);
    }

    @Test
    @Title("Переименовываем метку")
    @TestCaseId("34")
    public void testRenameLabel() {
        user.liteInterfaceSteps().createsNewOneWithName(labelName);
        String newName = Utils.getRandomString();
        user.liteInterfaceSteps().renamesOneWithNameTo(labelName, newName);
        user.defaultSteps().shouldNotSeeElementInList(onSettingsLitePage().labelNamesList(), labelName)
            .shouldSeeElementInList(onSettingsLitePage().labelNamesList(), newName);
        user.liteInterfaceSteps().deletesOneWithName(newName);
    }

    @Test
    @Title("Удаляем метку")
    @TestCaseId("35")
    public void testDeleteLabel() {
        user.liteInterfaceSteps().createsNewOneWithName(labelName);
        user.defaultSteps().shouldSeeElementInList(onSettingsLitePage().labelNamesList(), labelName);
        user.liteInterfaceSteps().deletesOneWithName(labelName);
        user.defaultSteps().shouldNotSeeElementInList(onSettingsLitePage().labelNamesList(), labelName);
    }
}
