package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author yaroslavna
 */

@Aqua.Test
@Title("Новый композ - Тест на кнопки в тулбаре")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.TOOLBAR)
public class ComposeToolbarTest extends BaseTest {

    private static final String CUSTOM_LABEL_SUBSTR = "CustomLabel";

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
        user.apiLabelsSteps().addNewLabel(CUSTOM_LABEL_SUBSTR, LABELS_PARAM_GREEN_COLOR);
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.COMPOSE);
    }

    @Test
    @Title("Тест на кнопку отменить")
    @TestCaseId("1225")
    public void composeToolbarCancelButton() {
        user.apiSettingsSteps()
            .callWithListAndParams("Включаем 3pane-vertical", of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL));
        user.defaultSteps().refreshPage()
            .opensFragment(QuickFragments.COMPOSE)
            .clicksOn(onComposePopup().expandedPopup().closeBtn())
            .shouldNotSee(onComposePopup().expandedPopup());
    }

    @Test
    @Title("Проверяем отображение проставленных меток при написании письма")
    @TestCaseId("1217")
    public void composeToolbarMarkLetterCustomLabel() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().composeMoreBtn())
            .clicksOn(onComposePopup().expandedPopup().composeMoreOptionsPopup().addLabelsOption())
            .shouldSee(onComposePopup().labelsPopup())
            .clicksOnElementWithText(onComposePopup().labelsPopup().labels(), CUSTOM_LABEL_SUBSTR)
            .shouldSee(onComposePopup().expandedPopup().labels())
            .shouldSeeElementInList(onComposePopup().expandedPopup().labels().labelsList(), CUSTOM_LABEL_SUBSTR);
    }
}
