package ru.yandex.autotests.innerpochta.tests.leftpanel;

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
import ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule.addFolderIfNeed;
import static ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule.addLabelIfNeed;
import static ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule.addMessageIfNeed;
import static ru.yandex.autotests.innerpochta.util.MailConst.LEFT_PANEL_FULL_SIZE;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASS_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.SERVER_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тест на узкую левую колонку в 3-pane")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.COMPACT_LEFT_PANEL)
public class CompactLeftPanel3paneTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private AddFolderIfNeedRule addFolderIfNeed = addFolderIfNeed(() -> user);
    private AddLabelIfNeedRule addLabelIfNeed = addLabelIfNeed(() -> user);
    private AddMessageIfNeedRule addMsgIfNeed = addMessageIfNeed(() -> user, () -> lock.firstAcc());

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user))
        .around(addFolderIfNeed)
        .around(addLabelIfNeed)
        .around(addMsgIfNeed);

    @Before
    public void setUp() {
        user.apiCollectorSteps().createNewCollector(MAIL_COLLECTOR, PASS_COLLECTOR, SERVER_COLLECTOR);
        user.apiSettingsSteps()
            .callWithListAndParams(
                "Выключаем компактную левую колонку и 3pane",
                of(
                    SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL,
                    SIZE_LAYOUT_LEFT, LEFT_PANEL_FULL_SIZE
                )
            );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Сворачиваем левую колонку в 3-pane")
    @TestCaseId("2298")
    public void shouldSeeCompactLeftPanel3pane() {
        Label label = addLabelIfNeed.getFirstLabel();
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().settingsMenu())
            .onMouseHoverAndClick(onMessagePage().mainSettingsPopupNew().settingsCheckboxes().get(1)) //TODO: fix after DARIA-71053
            .onMouseHoverAndClick(onMessagePage().mainSettingsPopupNew().settingsCheckboxes().get(1))
            .shouldSee(
                onMessagePage().compactLeftPanel(),
                onMessagePage().foldersNavigation(),
                onMessagePage().labelsNavigation(),
                onMessagePage().collectorsNavigation()
            );
        user.leftColumnSteps().shouldSeeLabelOnHomePage(label.getName().substring(0, 1).toUpperCase());
    }
}
