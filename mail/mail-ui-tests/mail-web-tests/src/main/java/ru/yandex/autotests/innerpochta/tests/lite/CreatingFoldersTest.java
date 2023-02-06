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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

@Aqua.Test
@Title("Создание папок")
@Description("Создание и удаление папок, подпапок")
@Features(FeaturesConst.LITE)
@Tag(FeaturesConst.LITE)
@Stories(FeaturesConst.GENERAL)
public class CreatingFoldersTest extends BaseTest {

    private String folderName = Utils.getRandomString();
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
        user.liteSettingsSteps().clicksOnFoldersLink();
    }

    @Test
    @Title("Создаём папку")
    @TestCaseId("30")
    public void testCreateFolder() {
        user.liteInterfaceSteps().createsNewOneWithName(folderName);
        user.defaultSteps().shouldSeeElementInList(onSettingsLitePage().folderNamesList(), folderName);
        user.liteInterfaceSteps().deletesOneWithName(folderName);
    }


    @Test
    @Title("Переименовываем папку")
    @TestCaseId("31")
    public void testRenameFolder() {
        user.liteInterfaceSteps().createsNewOneWithName(folderName);
        String newName = Utils.getRandomString();
        user.liteInterfaceSteps().renamesOneWithNameTo(folderName, newName);
        user.defaultSteps().shouldNotSeeElementInList(onSettingsLitePage().folderNamesList(), folderName)
            .shouldSeeElementInList(onSettingsLitePage().folderNamesList(), newName);
        user.liteInterfaceSteps().deletesOneWithName(newName);
    }


    @Test
    @Title("Удаляем папку")
    @TestCaseId("32")
    public void testDeleteFolder() {
        user.liteInterfaceSteps().createsNewOneWithName(folderName);
        user.defaultSteps().shouldSeeElementInList(onSettingsLitePage().folderNamesList(), folderName);
        user.liteInterfaceSteps().deletesOneWithName(folderName);
        user.defaultSteps().shouldNotSeeElementInList(onSettingsLitePage().folderNamesList(), folderName);
    }
}
