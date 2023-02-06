package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
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

import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMPORTANT_LABEL_NAME_RU;
import static ru.yandex.autotests.innerpochta.util.MailConst.UNREAD_RU;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_AVATARS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_UNION_AVATARS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.WITH_BIGGER_TEXT;

/**
 * @author crafty
 */

@Aqua.Test
@Title("Поисковый саджест")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.SEARCH)
@RunWith(DataProviderRunner.class)
public class SearchSuggestTestTus {

    private static final String YQL_REQUEST = "тема";
    public static final String CREDS = "SearchSuggestTest";
    private static final String LABEL_QUERY = "метка:";
    private static final String FOLDER_QUERY = "папка:";
    private static final String DARK_THEME_QUERY_PARAM = "?theme=lamp";
    private static final int TIMEOUT = 15;

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".search-input__form-button")
    );

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withAdditionalIgnoredElements(IGNORE_THIS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Видим саджест папки")
    @TestCaseId("3863")
    public void shouldSeeFolderSuggest() {
        String subFolderName = stepsProd.user().apiFoldersSteps().createNewSubFolder(
            getRandomString(),
            stepsProd.user().apiFoldersSteps().createNewFolder(getRandomString())
        ).getName();

        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .inputsTextInElement(
                    st.pages().mail().home().mail360HeaderBlock().searchInput(),
                    FOLDER_QUERY + subFolderName
                )
                .shouldSee(
                    st.pages().mail().search().searchSuggest(),
                    st.pages().mail().search().searchSuggestFolderIcon()
                );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Видим саджест метки")
    @TestCaseId("3861")
    public void shouldSeeLabelSuggest() {
        String labelName = stepsProd.user().apiLabelsSteps()
            .addNewLabel(getRandomString(), LABELS_PARAM_GREEN_COLOR).getName();

        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().mail().home().mail360HeaderBlock().searchInput(), LABEL_QUERY + labelName)
                .shouldSee(
                    st.pages().mail().search().searchSuggest(),
                    st.pages().mail().search().searchSuggestLabelIcon()
                );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Видим саджест метки «Важные»")
    @TestCaseId("3864")
    public void shouldSeeLabelFlaggedSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .inputsTextInElement(
                    st.pages().mail().home().mail360HeaderBlock().searchInput(),
                    LABEL_QUERY + IMPORTANT_LABEL_NAME_RU
                )
                .shouldSee(
                    st.pages().mail().search().searchSuggest(),
                    st.pages().mail().search().searchSuggestFlaggedIcon()
                );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Видим саджест метки «Непрочитанные»")
    @TestCaseId("3865")
    public void shouldSeeLabelUnreadSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .inputsTextInElement(
                    st.pages().mail().home().mail360HeaderBlock().searchInput(),
                    LABEL_QUERY + UNREAD_RU
                )
                .shouldSee(
                    st.pages().mail().search().searchSuggest(),
                    st.pages().mail().search().searchSuggestUnreadIcon()
                );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Видим саджест YQL")
    @TestCaseId("3806")
    public void shouldSeeYqlSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().mail().home().mail360HeaderBlock().searchInput(), YQL_REQUEST)
                .shouldSee(
                    st.pages().mail().search().searchSuggest(),
                    st.pages().mail().search().searchSuggestYqlIcon()
                );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Видим саджест темы письма и конкретное письмо")
    @TestCaseId("3810")
    public void shouldSeeSubjectSuggest() {
        String emailSubject = prepareMessage();
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().mail().home().mail360HeaderBlock().searchInput(), emailSubject)
                .shouldSee(
                    st.pages().mail().search().searchSuggest(),
                    st.pages().mail().search().searchSuggestSubjectIcon(),
                    st.pages().mail().search().searchSuggestMailSubject()
                );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Кликаем на письмо в саджесте")
    @DataProvider({LAYOUT_2PANE, LAYOUT_3PANE_VERTICAL})
    @TestCaseId("3822")
    public void shouldOpenMessageFromSuggest(String layout) {
        String emailSubject = prepareMessage();
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Изменяем лейаут (2pane/3pane)",
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().mail().home().mail360HeaderBlock().searchInput(), emailSubject)
                .clicksOn(st.pages().mail().search().searchSuggestMailSubject())
                .onMouseHover(st.pages().mail().msgView().messageHead());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Крупный шрифт в саджесте")
    @TestCaseId("5361")
    public void shouldSeeLargeFontInSuggest() {
        String emailSubject = prepareMessage();
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем крупный шрифт",
            of(WITH_BIGGER_TEXT, STATUS_ON)
        );
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().mail().home().mail360HeaderBlock().searchInput(), emailSubject)
                .shouldSeeWithWaiting(st.pages().mail().search().searchSuggest(), TIMEOUT);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Саджест в темных темах")
    @TestCaseId("5358")
    public void shouldSeeSuggestInDifferentThemes() {
        String emailSubject = prepareMessage();
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().mail().home().mail360HeaderBlock().searchInput(), emailSubject)
                .shouldSee(st.pages().mail().search().searchSuggest());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(DARK_THEME_QUERY_PARAM).run();
    }

    @Test
    @Title("Саджест с выключенными аватарками")
    @TestCaseId("5367")
    public void shouldSeeSuggestWithDisabledAvatar() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем показ аватарок",
            of(SETTINGS_PARAM_MESSAGE_AVATARS, FALSE)
        );
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .shouldSee(st.pages().mail().search().searchSuggest());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Саджест с выключенными аватарками в чекбоксах")
    @TestCaseId("5367")
    public void shouldSeeSuggestWithDisabledCheckboxAvatar() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем показ аватарок в чекбоксах",
            of(SETTINGS_PARAM_MESSAGE_UNION_AVATARS, FALSE)
        );
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .shouldSee(st.pages().mail().search().searchSuggest());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Саджест с удаленной историей запросов")
    @TestCaseId("5368")
    public void shouldNotSeeSuggestHistory() {
        stepsProd.user().apiSearchSteps().cleanSuggestHistory();
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .shouldNotSee(st.pages().mail().search().searchSuggestHistoryIcon());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Поиск по клику в аватар")
    @TestCaseId("5398")
    public void shouldSeeContactSearch() {
        stepsProd.user().apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc().getSelfEmail(), getRandomString(), getRandomString());
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем показ аватарок в чекбоксах",
            of(SETTINGS_PARAM_MESSAGE_UNION_AVATARS, FALSE)
        );
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().displayedMessages().list().get(0).avatarImg())
                .shouldHasValue(
                    st.pages().mail().home().mail360HeaderBlock().searchInput(), lock.firstAcc().getSelfEmail());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    private String prepareMessage() {
        String emailSubject = getRandomString();
        stepsProd.user().apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc().getSelfEmail(), emailSubject, getRandomString());
        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(
            INBOX,
            stepsProd.user().apiFoldersSteps().createNewFolder(getRandomString()).getName()
        );
        return emailSubject;
    }
}
