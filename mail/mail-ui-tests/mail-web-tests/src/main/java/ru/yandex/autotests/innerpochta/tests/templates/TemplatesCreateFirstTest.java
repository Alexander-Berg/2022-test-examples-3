package ru.yandex.autotests.innerpochta.tests.templates;

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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Создание шаблона у нового юзера")
@Features(FeaturesConst.TEMPLATES)
@Tag(FeaturesConst.TEMPLATES)
@Stories(FeaturesConst.TEMPLATES)
public class TemplatesCreateFirstTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().createAndUseTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Проверяем появление папки “Шаблоны“")
    @TestCaseId("1865")
    public void shouldSeeTemplatesFolder() {
        user.defaultSteps().shouldNotSee(onMessagePage().foldersNavigation().templatesFolder())
            .opensFragment(QuickFragments.COMPOSE);
        user.composeSteps().inputsSendText(Utils.getRandomName());
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup().templatesBtn())
            .clicksOn(onComposePopup().expandedPopup().templatesBtn())
            .clicksOn(onComposePopup().expandedPopup().templatePopup().saveBtn());
        user.leftColumnSteps().openFolders();
        user.defaultSteps().shouldSee(onMessagePage().foldersNavigation().templatesFolder());
    }
}
