package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.MailConst.YA_DISK_URL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Добавление аттачей в письмо")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ATTACHES)
public class AddAttachInComposeTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount(DISK_USER_TAG);
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    private String IMAGE_LINK =
        "https://upload.wikimedia.org/wikipedia/commons/thumb/5/54/Sun_white.jpg/1280px-Sun_white.jpg";

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().loginSteps().forAcc(lock.firstAcc()).logins();
        stepsProd.user().defaultSteps().opensUrl(YA_DISK_URL);
    }

    @Test
    @Title("Открываем попап добавления аттачей с диска")
    @TestCaseId("3238")
    public void shouldSeeAddDiskAttachPopup() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().composePopup().expandedPopup().diskAttachBtn())
            .shouldSee(st.pages().mail().composePopup().addDiskAttachPopup())
            .shouldSee(st.pages().mail().composePopup().addDiskAttachPopup().attachList().get(0));

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Добавляем файл через кнопку «Прикрепить»")
    @TestCaseId("3239")
    public void shouldSeeFileInCompose() {
        Consumer<InitStepsRule> actions = st -> st.user().composeSteps().addAttachFromDisk(0);

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть попап «Забыли аттачи»")
    @TestCaseId("3241")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66601")
    public void shouldSeeRememberPopupInCompose() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().inputsAddressInFieldTo(lock.firstAcc().getSelfEmail())
                .inputsSendText("attach")
                .clicksOnSendBtn();
            st.user().defaultSteps().shouldSee(st.pages().mail().compose().forgotAttachPopup());
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Добавляем файл двойным кликом")
    @TestCaseId("3242")
    public void shouldAddFileFromDoubleClick() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().composePopup().expandedPopup().diskAttachBtn())
                .shouldSee(st.pages().mail().composePopup().addDiskAttachPopup())
                .doubleClick(
                    st.pages().mail().composePopup().addDiskAttachPopup().attachList().waitUntil(not(empty())).get(0)
                )
                .shouldNotSee(
                    st.pages().mail().composePopup().addDiskAttachPopup(),
                    st.pages().mail().composePopup().expandedPopup().attachPanel().loadingAttach()
                );

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Удаляем аттач")
    @TestCaseId("3625")
    public void shouldSeeDeletedAttachInCompose() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().addAttachFromDisk(0);
            st.user().defaultSteps()
                .onMouseHover(st.pages().mail().composePopup().expandedPopup().attachPanel().linkedAttach().get(0))
                .clicksOn(
                    st.pages().mail().composePopup().expandedPopup().attachPanel().linkedAttach().get(0)
                        .attachDeteteBtn()
                );
        };
        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }

    @Test
    @TestCaseId("3583")
    @Title("Добавляем картинку по ссылке")
    public void shouldAddImageFrmLink() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().composePopup().expandedPopup().toolbarBlock().addImage())
            .clicksOn(st.pages().mail().composePopup().expandedPopup().addImageLink())
            .inputsTextInElement(
                st.pages().mail().composePopup().expandedPopup().addImagePopup().hrefInput(),
                IMAGE_LINK
            )
            .clicksOn(st.pages().mail().composePopup().expandedPopup().addImagePopup().addLinkBtn());

        parallelRun.withActions(actions).withUrlPath(COMPOSE).withAcc(lock.firstAcc()).run();
    }
}
