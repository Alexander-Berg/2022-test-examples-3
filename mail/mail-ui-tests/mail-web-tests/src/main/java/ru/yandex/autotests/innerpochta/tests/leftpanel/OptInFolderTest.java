package ru.yandex.autotests.innerpochta.tests.leftpanel;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

/**
 * * @author oleshko
 */
@Aqua.Test
@Title("Тесты на табы в ЛК")
@Features(FeaturesConst.OPTIN)
@Tag(FeaturesConst.LEFT_PANEL)
@Stories(FeaturesConst.LEFT_PANEL)
@RunWith(DataProviderRunner.class)
@Description("У юзера есть наразобранные новые рассылки")
public class OptInFolderTest extends BaseTest {

    private static final String FOLDER_NAME = "Pending";

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth);

    @Test
    @Title("В списке папок не должно быть папки Pending")
    @TestCaseId("6337")
    public void shouldNotSeePendingFolder() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().shouldSee(onMessagePage().optInLine())
            .shouldNotSeeElementInList(onMessagePage().foldersNavigation().allFolders(), FOLDER_NAME);
    }
}
