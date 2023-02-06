package ru.yandex.autotests.innerpochta.tests.screentests.Search;

import com.google.common.collect.Sets;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_RQST;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_TOUCH;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.AddLabelIfNeedRule.addLabelIfNeed;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Поиск по почте")
@Features(FeaturesConst.SEARCH)
@Stories(FeaturesConst.GENERAL)
public class SearchScreenTest {

    private static final int MESSAGES_COUNT = 1;

    private static final Set<Coords> IGNORED_AREA_PHONE =
        Sets.newHashSet(new Coords(20, 215, 375, 315)); //заглушка на 404, не угадать какая из 2 появится
    private static final Set<Coords> IGNORED_AREA_TABLET =
        Sets.newHashSet(new Coords(20, 195, 357, 500)); // /заглушка на 404, не угадать какая из 2 появится

    private Message msg;

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule accLock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);
    private AddLabelIfNeedRule addLabel = addLabelIfNeed(() -> stepsProd.user());

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(addLabel);

    @Test
    @Title("Поисковая фраза подсвечивается желтым в найденном письме")
    @TestCaseId("384")
    public void shouldSeeHighlightedQueryInMessage() {
        String subj = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            checkSearchResult(st);
            st.user().defaultSteps().clicksOn(st.pages().touch().search().messages().get(0))
                .shouldSee(st.pages().touch().messageView().threadHeader());
        };
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, subj);
        parallelRun.withActions(actions).withAcc(accLock.firstAcc())
            .withUrlPath(String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(subj))).run();
    }

    @Test
    @Title("Должны увидеть заглушку в поиске, когда ничего не найдено")
    @TestCaseId("300")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeEmptySearch() {
        String rqst = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            inputRqstAndClickFind(st, rqst);
            st.user().defaultSteps().shouldSee(st.pages().touch().search().emptySearchResultImg());
        };
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withIgnoredAreas(IGNORED_AREA_PHONE)
            .withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны увидеть заглушку в поиске, когда ничего не найдено")
    @TestCaseId("300")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeEmptySearchTablet() {
        String rqst = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            inputRqstAndClickFind(st, rqst);
            st.user().defaultSteps().shouldSee(st.pages().touch().search().emptySearchResultImg());
        };
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withIgnoredAreas(IGNORED_AREA_TABLET)
            .withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны увидеть черновик в поисковой выдаче")
    @TestCaseId("840")
    public void shouldSeeDraftInSearch() {
        String subj = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            inputRqstAndClickFind(st, subj);
            st.user().defaultSteps().shouldSee(st.pages().touch().search().messageBlock());
        };
        stepsProd.user().apiMessagesSteps().createDraftWithSubject(subj);
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны увидеть пустую выдачу при пустом запросе")
    @TestCaseId("835")
    public void shouldSeeEmptySearchAfterEmptyRequest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().search().header().find())
                .shouldNotSee(st.pages().touch().search().emptySearchResultImg())
                .shouldSee(st.pages().touch().search().searchedBefore());

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны увидеть удалённое письмо в поиске")
    @TestCaseId("837")
    public void shouldSeeDeleteMsgInSearch() {
        String subj = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            inputRqstAndClickFind(st, subj);
            st.user().defaultSteps().shouldSee(st.pages().touch().search().messageBlock());
        };
        stepsProd.user().apiMessagesSteps().deleteMessages(
            stepsProd.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "")
        );
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны увидеть важное письмо с меткой в поиске")
    @TestCaseId("866")
    public void shouldSeeLabeledMsgInSearch() {
        String subj = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            inputRqstAndClickFind(st, subj);
            st.user().defaultSteps().shouldSee(st.pages().touch().search().messageBlock());
        };
        msg = stepsProd.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        stepsProd.user().apiLabelsSteps().markWithLabel(
            msg,
            stepsProd.user().apiLabelsSteps().getAllUserLabels().get(0)
        )
            .markImportant(msg);
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("Должны отметить письмо прочитанным в поиске")
    @TestCaseId("867")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeReadMsgInSearch() {
        String subj = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiMessagesSteps().markAllMsgUnRead();
            inputRqstAndClickFind(st, subj);
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().messageBlock().unreadToggler())
                .shouldNotSee(st.pages().touch().search().messageBlock().unreadToggler());
        };
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("[Планшеты] Должны отметить прочитанным письмо в поиске")
    @TestCaseId("867")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeReadMsgInSearchTablet() {
        String subj = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiMessagesSteps().markLetterUnRead(msg);
            inputRqstAndClickFind(st, subj);
            st.user().defaultSteps().shouldNotSee(st.pages().touch().search().messageBlock().unreadToggler());
        };
        msg = stepsProd.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("[Телефоны] Должны увидеть письмо удалённым после удаления")
    @TestCaseId("839")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldDeleteMsgInSearch() {
        String subj = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            inputRqstAndClickFind(st, subj);
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().search().messageBlock().avatar())
                .clicksOn(st.pages().touch().search().groupOperationsToolbarPhone().delete());
        };
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart()).run();
    }

    @Test
    @Title("[Планшеты] Должны увидеть письмо удалённым после удаления")
    @TestCaseId("839")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldDeleteMsgInSearchTablet() {
        String subj = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(TRASH, INBOX);
            st.user().defaultSteps().refreshPage()
                .clicksOn(st.pages().touch().search().messageBlock().avatar())
                .clicksOn(st.pages().touch().messageView().groupOperationsToolbarTablet().delete());
        };
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        parallelRun.withActions(actions).withAcc(accLock.firstAcc())
            .withUrlPath(String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(subj))).run();
    }

    @Test
    @Title("При скролле поисковой выдачи поисковая строка не уезжает")
    @TestCaseId("838")
    public void shouldScrollSearch() {
        String subj = Utils.getRandomName();
        Consumer<InitStepsRule> actions = st -> {
            inputRqstAndClickFind(st, subj);
            st.user().touchSteps().scrollMsgListDown();
            st.user().defaultSteps().shouldSee(st.pages().touch().search().header().input());
        };
        stepsProd.user().apiMessagesSteps().sendThread(accLock.firstAcc(), subj, 16);
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Отключаем тредный режим",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(SEARCH_TOUCH.makeTouchUrlPart())
            .runSequentially();
    }

    @Step("Вводим в поиск запрос и нажмаем найти")
    private void inputRqstAndClickFind(InitStepsRule st, String rqst) {
        st.user().defaultSteps().clicksAndInputsText(st.pages().touch().search().header().input(), rqst)
            .clicksOn(st.pages().touch().search().header().find());
    }

    @Step("Проверяем, что поисковая выдачу загрузилась")
    private void checkSearchResult(InitStepsRule st) {
        st.user().defaultSteps().shouldSee(st.pages().touch().search().messageBlock())
            .shouldSeeElementsCount(st.pages().touch().searchResult().messages(), MESSAGES_COUNT);
    }
}
