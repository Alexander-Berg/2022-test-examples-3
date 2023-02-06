package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
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
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.COLOR_SCHEME;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DONT_SAVE_HISTORY;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_AVATARS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_UNION_AVATARS;

/**
 * @author crafty
 */

@Aqua.Test
@Title("Поисковый саджест")
@Features({FeaturesConst.MESSAGE_LIST, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.SEARCH)
@RunWith(DataProviderRunner.class)
@Description("У пользователя подготовлены данные для саджеста")
public class SearchSuggestTest {

    private static final String LAST_SEARCH_SUGGEST = "LastSearchSuggestUser";
    public static final String CREDS = "SearchSuggestTest";
    private static final String COLORFUL_THEME = "colorful";
    private static final String SEARCH = "test";

    private static final Set<By> IGNORE_THIS = Sets.newHashSet(
        cssSelector(".search-input__form-button")
    );

    private ScreenRulesManager rules = screenRulesManager().withLock(AccLockRule.use().names(CREDS));
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withAdditionalIgnoredElements(IGNORE_THIS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain()
        .around(removeAllMessages(() -> stepsProd.user(), INBOX, TRASH, DRAFT));

    @Before
    public void setUp() {
        stepsProd.user().apiLabelsSteps().deleteAllCustomLabels();
        stepsProd.user().apiFoldersSteps().deleteAllCustomFolders();
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем 2 пейн, показ аватарок в чекбоксах, цветную тему, историю запросов",
            of(
                SETTINGS_PARAM_MESSAGE_AVATARS, TRUE,
                SETTINGS_PARAM_MESSAGE_UNION_AVATARS, TRUE,
                COLOR_SCHEME, COLORFUL_THEME,
                DONT_SAVE_HISTORY, FALSE,
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE
            )
        );
    }

    @Test
    @UseCreds(LAST_SEARCH_SUGGEST)
    @Title("Видим в саджесте прошлый запрос")
    @TestCaseId("3818")
    public void shouldSeeLastSearchSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .shouldSee(
                    st.pages().mail().search().searchSuggest(),
                    st.pages().mail().search().searchSuggestHistoryIcon()
                );

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Видим саджест контакта")
    @TestCaseId("3859")
    public void shouldSeeContactSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .inputsTextInElement(
                    st.pages().mail().home().mail360HeaderBlock().searchInput(),
                    lock.firstAcc().getSelfEmail()
                )
                .shouldSee(
                    st.pages().mail().search().searchSuggest(),
                    st.pages().mail().search().searchSuggestContactEmail()
                )
                .shouldSeeWithWaiting(st.pages().mail().search().searchSuggestMailSubject(), 10);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Запрос выделен болдом в саджесте")
    @DataProvider({LAYOUT_2PANE, LAYOUT_3PANE_VERTICAL})
    @TestCaseId("5355")
    public void shouldSeeBoldTextInSuggest(String layout) {
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(DEV_NULL_EMAIL, getRandomString(), getRandomString());
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Изменяем лейаут (2pane/3pane)",
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .inputsTextInElement(st.pages().mail().home().mail360HeaderBlock().searchInput(), SEARCH)
                .shouldSee(st.pages().mail().search().searchSuggest());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
