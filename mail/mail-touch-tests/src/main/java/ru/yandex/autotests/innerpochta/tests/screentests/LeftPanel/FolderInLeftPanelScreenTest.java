package ru.yandex.autotests.innerpochta.tests.screentests.LeftPanel;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.util.Set;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на папки")
@Features(FeaturesConst.LEFT_PANEL)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class FolderInLeftPanelScreenTest {

    private static final String LONG_NAME = "1234567890123456789012345678901234567890";

    private static final Set<Coords> IGNORED_AREA = Sets.newHashSet(
        new Coords(500, 1950, 460, 90)
    );

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED_AREA);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(acc.firstAcc(), Utils.getRandomString(), "");
    }

    @Test
    @Title("Текущая папка подсвечивается в списке папок, также проверяем порядок папок")
    @TestCaseId("13")
    public void shouldSeeHighlightedFolderName() {
        Consumer<InitStepsRule> act = st -> st.user().defaultSteps()
            .shouldSee(st.pages().touch().messageList().headerBlock())
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .shouldSee(st.pages().touch().sidebar().sidebarAvatar());

        stepsProd.user().apiMessagesSteps().sendMailWithSentTime(acc.firstAcc(), Utils.getRandomName(), "");
        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart("3"))
            .run();
    }

    @Test
    @Title("Текущая подпапка выделена в списке папок")
    @TestCaseId("12")
    public void shouldSeeActiveSubfolder() {
        String subfolder = Utils.getRandomString();
        Consumer<InitStepsRule> act = st -> st.user().defaultSteps()
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(st.pages().touch().sidebar().subfoldertoggler().get(0))
            .clicksOn(st.pages().touch().sidebar().folderBlocks().get(2))
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .shouldSee(st.pages().touch().sidebar().leftPanelBox());

        Folder folder = stepsProd.user().apiFoldersSteps().createNewFolder(Utils.getRandomString());
        stepsProd.user().apiFoldersSteps().createNewSubFolder(subfolder, folder);
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Название папки с длинным именем помещается в список папок")
    @TestCaseId("989")
    public void shouldSeeLongNameFolder() {
        Consumer<InitStepsRule> act = st -> st.user().defaultSteps()
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .shouldSee(st.pages().touch().sidebar().leftPanelBox());

        stepsProd.user().apiFoldersSteps().createNewFolder(LONG_NAME);
        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, LONG_NAME);
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Папки остаются развернутыми")
    @TestCaseId("1116")
    public void shouldSaveExpandedFolderTree() {
        String subfolder = Utils.getRandomString();
        Consumer<InitStepsRule> act = st -> st.user().defaultSteps()
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(
                st.pages().touch().sidebar().subfoldertoggler().waitUntil(not(empty())).get(0),
                st.pages().touch().sidebar().subfoldertoggler().get(1)
            )
            .refreshPage()
            .clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
            .shouldSee(st.pages().touch().sidebar().subfoldertoggler().waitUntil(not(empty())).get(1));

        stepsProd.user().apiFoldersSteps().createNewSubFolder(
            subfolder,
            stepsProd.user().apiFoldersSteps().createNewFolder(LONG_NAME)
        );
        stepsProd.user().apiFoldersSteps().createNewSubFolder(
            Utils.getRandomName(),
            stepsProd.user().apiFoldersSteps().getFolderByName(subfolder)
        );
        parallelRun.withActions(act).withAcc(acc.firstAcc()).run();
    }
}
