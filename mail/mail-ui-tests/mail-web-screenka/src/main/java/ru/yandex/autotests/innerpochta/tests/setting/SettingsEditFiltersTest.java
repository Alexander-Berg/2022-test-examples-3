package ru.yandex.autotests.innerpochta.tests.setting;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule;
import ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_FILTERS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_FILTERS_EDIT;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule.addFolderIfNeed;
import static ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule.addMessageIfNeed;
import static ru.yandex.autotests.innerpochta.util.MailConst.DOMAIN_YANDEXRU;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_FOLDER;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Настройки - Редактирование фильтра")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FILTERS)
public class SettingsEditFiltersTest {

    private static final int TREAD_SIZE = 11;

    private String filterName;
    private String editFilterUrl;

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);
    private AddFolderIfNeedRule addFolder = addFolderIfNeed(() -> stepsProd.user());
    private AddMessageIfNeedRule addMsg = addMessageIfNeed(() -> stepsProd.user(), () -> lock.firstAcc());

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(addMsg)
        .around(addFolder);

    @Before
    public void setUp() {
        filterName = Util.getRandomString();
        Message msg = addMsg.getFirstMessage();
        String filterID = stepsProd.user().apiFiltersSteps().createFilterForFolderOrLabel(
            lock.firstAcc().getLogin(),
            msg.getSubject(),
            FILTERS_ADD_PARAM_MOVE_FOLDER,
            addFolder.getFirstFolder().getFid(),
            FILTERS_ADD_PARAM_CLICKER_MOVE,
            false
        ).getFilid();
        editFilterUrl = SETTINGS_FILTERS_EDIT.makeUrlPart(filterID);
        stepsProd.user().apiFiltersSteps().addAddressToBlacklist(getRandomString() + DOMAIN_YANDEXRU)
            .addAddressToWhitelist(getRandomString() + DOMAIN_YANDEXRU);
    }

    @Test
    @Title("Нажимаем на кнопку «Создать правило»")
    @TestCaseId("2624")
    public void shouldSeeCreateFilterPage() {
        Consumer<InitStepsRule> actions = st -> st.user().filtersSteps().clicksOnCreateNewFilter();

        parallelRun.withActions(actions).withUrlPath(SETTINGS_FILTERS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на кнопку «Удалить правило»")
    @TestCaseId("2625")
    public void shouldSeeDeleteFilterPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().shouldBeOnUrlWith(SETTINGS_FILTERS);
            st.user().filtersSteps().clicksOnDeleteFilter();
        };
        parallelRun.withActions(actions).withUrlPath(SETTINGS_FILTERS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на кнопку «Добавить условие»")
    @TestCaseId("2626")
    public void shouldSeeLogicDropdown() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().filtersSteps().clicksOnAddConditionButton(2);
            st.user().defaultSteps().shouldSee(st.pages().mail().createFilters().setupFiltersCreate()
                    .blockCreateConditions().conditionsList().get(2))
                .turnTrue(st.pages().mail().createFilters()
                    .setupFiltersCreate().blockSelectAction().deleteCheckBox());
        };
        parallelRun.withActions(actions).withUrlPath(editFilterUrl).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на кнопку «Удалить условие»")
    @TestCaseId("2627")
    public void shouldDeleteCondition() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().createFilters()
                .setupFiltersCreate().blockCreateConditions().conditionsList().get(0).deleteConditionButton());
            assertThat("Условие не удалилось", st.pages().mail().createFilters()
                .setupFiltersCreate().blockCreateConditions().conditionsList(), withWaitFor(hasSize(1)));
        };
        parallelRun.withActions(actions).withUrlPath(editFilterUrl).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем выпадушку И-ИЛИ")
    @TestCaseId("2628")
    public void shouldSeeTwoConditionBlocks() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().createFilters()
                    .setupFiltersCreate().blockCreateConditions().selectLogicButton())
                .shouldSee(st.pages().mail().settingsCommon().selectConditionDropdown());

        parallelRun.withActions(actions).withUrlPath(editFilterUrl).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем ссылку «Указать название»")
    @TestCaseId("2629")
    public void shouldSeeFilterNameInput() {
        Consumer<InitStepsRule> actions = st -> st.user().filtersSteps().changesFilterName(filterName);

        parallelRun.withActions(actions).withUrlPath(editFilterUrl).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем попап ввода пароля")
    @TestCaseId("2630")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68002")
    public void shouldSeeInputPassPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().createFilters()
                    .setupFiltersCreate().blockPasswordProtectedActions().replyWithTextCheckBox())
                .clicksOn(st.pages().mail().createFilters().setupFiltersCreate().submitFilterButton())
                .shouldSee(st.pages().mail().filtersCommon().passwordConfirmationBlock());

        parallelRun.withActions(actions).withUrlPath(editFilterUrl).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на кнопку «Проверить правило»")
    @TestCaseId("2631")
    public void shouldSeeMessageWithFilter() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().createFilters()
                    .setupFiltersCreate().blockCreateConditions().conditionsList().get(0).deleteConditionButton())
                .clicksOn(st.pages().mail().createFilters().setupFiltersCreate().previewButton())
                .shouldSee(st.pages().mail().createFilters().setupFiltersCreate().previewMessagesListHeader());

        parallelRun.withActions(actions).withUrlPath(editFilterUrl).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Разворачиваем список писем из «Проверить правило»")
    @TestCaseId("2658")
    public void shouldSeeMoreMessageWithFilter() {
        String filterMoreID = createFilterForMoreMsg();
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().createFilters()
                    .setupFiltersCreate().blockCreateConditions().conditionsList().get(0).deleteConditionButton())
                .clicksOn(st.pages().mail().createFilters().setupFiltersCreate().previewButton())
                .shouldSee(st.pages().mail().createFilters().setupFiltersCreate().previewMessagesListHeader())
                .clicksOn(st.pages().mail().createFilters().setupFiltersCreate().moreMessagesButton());

        parallelRun.withActions(actions).withUrlPath(SETTINGS_FILTERS_EDIT.makeUrlPart(filterMoreID))
            .withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Добавляем адрес в Черный список")
    @TestCaseId("2659")
    public void shouldSeeAddressInBlackList() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().createFilters().blackListBlock().blockedAddressBlock().get(0));

        parallelRun.withActions(actions).withUrlPath(SETTINGS_FILTERS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем верстку Белого списка")
    @TestCaseId("2671")
    public void shouldSeeAddressInWhiteList() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().createFilters().whiteListBlock().whitedAddressBlock().get(0));

        parallelRun.withActions(actions).withUrlPath(SETTINGS_FILTERS).withAcc(lock.firstAcc()).run();
    }

    @Step("Создаем фильтр для пачки более 10 писем")
    private String createFilterForMoreMsg() {
        String thread = Utils.getRandomString();
        stepsProd.user().apiMessagesSteps()
            .deleteAllMessagesInFolder(stepsProd.user().apiFoldersSteps().getFolderBySymbol(INBOX))
            .sendThread(lock.firstAcc(), thread, TREAD_SIZE);
        return stepsProd.user().apiFiltersSteps().createFilterForFolderOrLabel(
            lock.firstAcc().getLogin(),
            thread,
            FILTERS_ADD_PARAM_MOVE_FOLDER,
            addFolder.getFirstFolder().getFid(),
            FILTERS_ADD_PARAM_CLICKER_MOVE,
            false
        ).getFilid();
    }
}
