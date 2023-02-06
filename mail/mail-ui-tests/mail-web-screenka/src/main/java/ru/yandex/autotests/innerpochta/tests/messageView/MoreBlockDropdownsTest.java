package ru.yandex.autotests.innerpochta.tests.messageView;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableSortedMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule.addFolderIfNeed;
import static ru.yandex.autotests.innerpochta.rules.resources.AddMessageIfNeedRule.addMessageIfNeed;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_OFF;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_TRANSLATE;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Попапы у кнопок за выпадушкой «Ещё» в тулбаре")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.TOOLBAR)
public class MoreBlockDropdownsTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(addFolderIfNeed(() -> stepsProd.user()))
        .around(addMessageIfNeed(() -> stepsProd.user(), () -> lock.firstAcc()));

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем бажный переводчик, включаем открытие письма в списке писем",
            of(
                SETTINGS_PARAM_TRANSLATE, STATUS_OFF,
                SETTINGS_OPEN_MSG_LIST, STATUS_TRUE
            )
        );
    }

    @Test
    @Title("Должна быть выпадушка за кнопкой «Ещё»")
    @TestCaseId("2714")
    public void shouldSeeMoreBlock() {
        Consumer<InitStepsRule> actions = this::openMoreBlock;

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должна быть выпадушка папок")
    @TestCaseId("2715")
    public void shouldSeeFolderDropdown() {
        Consumer<InitStepsRule> actions = st -> {
            openMoreBlock(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().miscField().folder())
                .shouldSee(st.pages().mail().msgView().moveMessageDropdownMenu())
                .waitInSeconds(3);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }


    @Test
    @Title("Должен быть попап создания новой папки")
    @TestCaseId("2716")
    public void shouldSeeNewFolderPopup() {
        Consumer<InitStepsRule> actions = st -> {
            openMoreBlock(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().miscField().folder())
                .onMouseHoverAndClick(st.pages().mail().msgView().moveMessageDropdownMenu().createNewFolder())
                .shouldSee(st.pages().mail().home().createFolderPopup())
                .clicksOn(st.pages().mail().home().createFolderPopup().putInFolder())
                .shouldSee(st.pages().mail().home().moveMessageDropdownMenuMini());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должен быть попап создания новой папки c правилом")
    @TestCaseId("2717")
    public void shouldSeeNewFolderBigPopup() {
        Consumer<InitStepsRule> actions = st -> {
            openMoreBlock(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().miscField().folder())
                .onMouseHoverAndClick(st.pages().mail().msgView().moveMessageDropdownMenu().createNewFolder())
                .shouldSee(st.pages().mail().home().createFolderPopup())
                .clicksOn(st.pages().mail().home().createFolderPopup().filterLink())
                .shouldSee(st.pages().mail().home().createFolderPopup().simpleFilter());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должна быть выпадушка меток")
    @TestCaseId("2718")
    public void shouldSeeLabelsDropdown() {
        Consumer<InitStepsRule> actions = st -> {
            openMoreBlock(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().miscField().label())
                .shouldSee(st.pages().mail().msgView().labelsDropdownMenu())
                .waitInSeconds(3);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должнен появиться попап создания метки")
    @TestCaseId("2719")
    public void shouldSeeNewLabelPopup() {
        Consumer<InitStepsRule> actions = st -> {
            openMoreBlock(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().miscField().label())
                .shouldSee(st.pages().mail().msgView().labelsDropdownMenu())
                .clicksOn(st.pages().mail().msgView().labelsDropdownMenu().createNewLabel())
                .shouldSee(st.pages().mail().home().createLabelPopup());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должен быть попап создания метки c правилом")
    @TestCaseId("2720")
    public void shouldSeeLabelsFilterPopup() {
        Consumer<InitStepsRule> actions = st -> {
            openMoreBlock(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().miscField().label())
                .shouldSee(st.pages().mail().msgView().labelsDropdownMenu())
                .clicksOn(st.pages().mail().msgView().labelsDropdownMenu().createNewLabel())
                .shouldSee(st.pages().mail().home().createLabelPopup())
                .clicksOn(st.pages().mail().home().createLabelPopup().filterLink())
                .shouldSee(st.pages().mail().home().createLabelPopup().simpleFilter());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Открываем выпадушку «Ещё» в просмотре письма")
    private void openMoreBlock(InitStepsRule st) {
        st.user().messagesSteps().clicksOnMessageByNumber(0);
        st.user().defaultSteps().clicksOn(st.pages().mail().msgView().contentToolbarBlock().moreBtn())
            .shouldSee(st.pages().mail().msgView().miscField());
    }
}
