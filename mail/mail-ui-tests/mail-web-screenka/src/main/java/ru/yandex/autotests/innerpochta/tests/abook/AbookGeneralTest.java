package ru.yandex.autotests.innerpochta.tests.abook;

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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.CONTACTS;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_ABOOK;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * Created by irmashirma
 */
@Aqua.Test
@Title("Контакты - Общие тесты")
@Features({FeaturesConst.ABOOK, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.ABOOK)
@Stories(FeaturesConst.GENERAL)
public class AbookGeneralTest {

    private static final String SCROLL_SCRIPT = "window.scrollBy(0,250)";

    private String groupName = getRandomName();
    private int contactNumber = 20;

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiAbookSteps().removeAllAbookGroups()
            .addNewAbookGroupWithContacts(
                groupName,
                stepsProd.user().apiAbookSteps().getPersonalContacts().get(0)
            );
    }

    @Test
    @Title("Открываем группу из левой колонки")
    @TestCaseId("3025")
    public void shouldSeeSelectedGroup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().abookSteps().clicksOnGroup(groupName);
            st.user().defaultSteps().shouldBeOnUrlWith(CONTACTS);
        };
        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем настройки из левой колонки")
    @TestCaseId("3026")
    public void shouldSeeSettingsPage() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHoverAndClick(st.pages().mail().abook().groupsBlock().settingsButton())
                .shouldBeOnUrlWith(SETTINGS_ABOOK);

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть результаты поиска по контактам")
    @TestCaseId("2828")
    public void shouldSeeSearchContact() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSee(st.pages().mail().search().mail360HeaderBlock().searchContactInput())
            .clicksOn(st.pages().mail().search().mail360HeaderBlock().searchContactInput())
            .inputsTextInElement(st.pages().mail().search().mail360HeaderBlock().searchContactInput(), "a")
            .shouldSee(st.pages().mail().abook().searchResultsHeader());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Клик на крестик справа очищает строку поиска в контактах")
    @TestCaseId("2547")
    public void shouldSeeClearSearch() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .shouldSee(st.pages().mail().home().mail360HeaderBlock().searchContactInput())
            .inputsTextInElement(st.pages().mail().home().mail360HeaderBlock().searchContactInput(), getRandomName())
            .shouldSee(st.pages().mail().abook().searchResultsHeader())
            .clicksOn(st.pages().mail().home().mail360HeaderBlock().clearContactInput());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Клик на крестик слева сворачивает строку поиска в контактах")
    @TestCaseId("2547")
    public void shouldSeeSearchIsFolded() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().abook().toolbarBlock())
                .clicksOn(st.pages().mail().search().mail360HeaderBlock().searchContactInput())
                .shouldSee(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().closeContactInput())
                .shouldSee(st.pages().mail().home().mail360HeaderBlock().foldedSearch());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть ещё контакты")
    @TestCaseId("2832")
    public void shouldSeeMoreContacts() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().abook().moreContactsBtn());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть залипающий тулбар")
    @TestCaseId("2542")
    public void shouldSeeStickyToolbar() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().shouldSee(st.pages().mail().home().toolbar())
                .executesJavaScript(SCROLL_SCRIPT);
            st.user().defaultSteps().shouldSee(st.pages().mail().home().stickyToolBar());
        };
        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть аватары пользователей в списке контактов")
    @TestCaseId("3566")
    public void shouldSeeAvatarsInUserList() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().abook().contacts().get(0).contactAvatar());

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть аватары пользователей в поиске по контактам")
    @TestCaseId("3566")
    public void shouldSeeAvatarsInContactSearch() {
        Consumer<InitStepsRule> actions = st -> {
            String contactName = st.pages().mail().abook().contacts().get(0).name().getText();

            st.user().defaultSteps()
                .inputsTextInElement(st.pages().mail().home().mail360HeaderBlock().searchContactInput(), contactName)
                .shouldSee(st.pages().mail().abook().searchResultsHeader())
                .shouldSeeThatElementTextEquals(st.pages().mail().abook().contacts().get(0).name(), contactName)
                .shouldSee(st.pages().mail().abook().contacts().get(0).contactAvatar());
        };
        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть аватары пользователей в группе контактов")
    @TestCaseId("3566")
    public void shouldSeeAvatarsInContactGroup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().abookSteps().clicksOnGroup(groupName);
            st.user().defaultSteps()
                .shouldSee(st.pages().mail().abook().checkedGroup())
                .shouldSeeThatElementTextEquals(st.pages().mail().abook().contacts().get(0).groupLabel().get(0), groupName)
                .shouldSee(st.pages().mail().abook().contacts().get(0).contactAvatar());
        };
        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть аватары пользователей в карточке контакта")
    @TestCaseId("3566")
    public void shouldSeeAvatarsInContactCard() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().abookSteps()
                .clicksOnContact(st.user().apiAbookSteps().getPersonalContacts().get(0));
            st.user().defaultSteps()
                .shouldSee(st.pages().mail().abook().contactPopup().abookPersonAvatar());
        };
        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Страница контактов должна корректно загрузиться после рефреша")
    @TestCaseId("4225")
    public void shouldSeeContactsAfterRefresh() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .refreshPage()
            .shouldSee(
                st.pages().mail().abook().mail360HeaderBlock(),
                st.pages().mail().abook().toolbarBlock(),
                st.pages().mail().abook().groupsBlock()
            );

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Скроллим поисковую выдачу и открываем нужную карточку контакта")
    @TestCaseId("4373")
    public void shouldOpenContactCardInSearchAfterScroll() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().abookSteps().clicksOnContact(st.user().apiAbookSteps().getPersonalContacts().get(contactNumber));
            st.user().defaultSteps().shouldSee(st.pages().mail().abook().contactPopup());
        };

        parallelRun.withActions(actions).withUrlPath(CONTACTS).withAcc(lock.firstAcc()).run();
    }
}
