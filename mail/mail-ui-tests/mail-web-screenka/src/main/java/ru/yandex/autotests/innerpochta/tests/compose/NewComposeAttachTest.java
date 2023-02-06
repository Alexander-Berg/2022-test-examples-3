package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
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

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.TestConsts.IGNORED_ELEMENTS;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Аттачи")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class NewComposeAttachTest {
    private static final String ATTACH_LOCAL_NAME = "attach.png";
    private static final String ATTACH_VIEW_URL = "message_part_real";
    private static final String SUBJECT = "subject";
    private static final int FOLDER_POSITION = 1;

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredElements(IGNORED_ELEMENTS);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            lock.firstAcc().getSelfEmail(),
            getRandomString(),
            getRandomString(),
            PDF_ATTACHMENT
        );
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
    }

    @Test
    @Title("Показываем превью картинки у аттача")
    @TestCaseId("5656")
    public void shouldSeeAttachPreview() {
        Consumer<InitStepsRule> actions = st -> {
            addNewComposeExpAndUploadLocalAttach(st);
            st.user().defaultSteps().shouldSee(st.pages().mail().composePopup().expandedPopup().attachPanel()
                    .linkedAttach().waitUntil(not(empty())).get(0)
                )
                .onMouseHover(st.pages().mail().composePopup().expandedPopup().attachPanel().linkedAttach().get(0));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем превью прикреплённого аттача")
    @TestCaseId("3476")
    public void shouldSeeAttachFullPreview() {
        Consumer<InitStepsRule> actions = st -> {
            addNewComposeExpAndUploadLocalAttach(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().attachPanel()
                    .linkedAttach().waitUntil(not(empty())).get(0)
                )
                .switchOnWindow(1)
                .shouldContainTextInUrl(ATTACH_VIEW_URL)
                .shouldContainTextInUrl(ATTACH_LOCAL_NAME);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Добавляем аттачи всех видов в черновик")
    @TestCaseId("4707")
    public void shouldAddAllTypesAttachesToDraft() {
        Consumer<InitStepsRule> actions = st -> {
            saveDraftWithAttachesAllType(st);
            st.user().defaultSteps().opensFragment(QuickFragments.DRAFT);
            st.user().messagesSteps().clicksOnMessageByNumber(0);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Отправляем черновик с аттачами всех видов")
    @TestCaseId("4707")
    public void shouldSendDraftWithAllTypesAttachesTo() {
        Consumer<InitStepsRule> actions = st -> {
            saveDraftWithAttachesAllType(st);
            st.user().defaultSteps().opensFragment(QuickFragments.DRAFT);
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().inputsTextInElement(
                    st.pages().mail().composePopup().expandedPopup().popupToInput(),
                    lock.firstAcc().getSelfEmail()
                )
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().sbjInput(), SUBJECT)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().sendBtn())
                .opensFragment(QuickFragments.INBOX);
            st.user().messagesSteps().clicksOnMessageWithSubject(SUBJECT);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Применяем эксперимент нового композа и загружаем локальный файл")
    private void addNewComposeExpAndUploadLocalAttach(InitStepsRule st) {
        st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton());
        st.user().composeSteps()
            .uploadLocalFile(
                st.pages().mail().composePopup().expandedPopup().localAttachInput(),
                ATTACH_LOCAL_NAME
            );
    }

    @Step("Создаем черновик с аттачами всех видов")
    private void saveDraftWithAttachesAllType(InitStepsRule st) {
        addNewComposeExpAndUploadLocalAttach(st);
        st.user().defaultSteps()
            .clicksOn(st.pages().mail().composePopup().expandedPopup().diskAttachBtn())
            .clicksOn(
                st.pages().mail().composePopup().addDiskAttachPopup().attachList().get(0),
                st.pages().mail().composePopup().addDiskAttachPopup().addAttachBtn()
            )
            .clicksOn(st.pages().mail().composePopup().expandedPopup().mailAttachBtn())
            .doubleClick(st.pages().mail().composePopup().addDiskAttachPopup().attachList().get(FOLDER_POSITION))
            .clicksOn(
                st.pages().mail().composePopup().addDiskAttachPopup().attachList().get(0),
                st.pages().mail().composePopup().addDiskAttachPopup().addAttachBtn()
            )
            .shouldNotSee(st.pages().mail().composePopup().expandedPopup().attachPanel().loadingAttach())
            .clicksOn(st.pages().mail().composePopup().expandedPopup().closeBtn());
    }

}
