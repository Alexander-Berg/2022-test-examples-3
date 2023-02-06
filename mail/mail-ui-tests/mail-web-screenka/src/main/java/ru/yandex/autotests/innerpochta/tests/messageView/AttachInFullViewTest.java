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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableBiMap.of;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Просмотр аттачей в письме на отдельной странице")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.ATTACHES)
public class AttachInFullViewTest {

    private static final String MSG_WITH_INLINE_ATTACH = "Инлайн аттач";
    private static final String MSG_WITH_IMAGE = "Аттач картинка";
    private static final String MSG_WITH_EML = "Eml с вложениями";
    private static final String CORRECT_TOOLTIP = "Открыть • 118 КБ";

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
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма на отдельной странице",
            of(SETTINGS_OPEN_MSG_LIST, EMPTY_STR)
        );
    }

    @Test
    @Title("Смотрим письмо с инлайн-аттачем")
    @TestCaseId("1072")
    public void shouldSeeMessageWithInlineAttach() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(MSG_WITH_INLINE_ATTACH);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().shownPictures());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Просматриваем картинку через просмотрщик")
    @TestCaseId("1100")
    public void shouldSeeImageInViewer() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(MSG_WITH_IMAGE);
            st.user().defaultSteps().offsetHover(
                st.pages().mail().msgView().attachments().list().get(0),
                10,
                10
            );
            assertEquals(
                "Тултип содержит неверный текст",
                CORRECT_TOOLTIP,
                st.pages().mail().msgView().attachmentToolTip().getText()
            );
            st.user().defaultSteps().onMouseHover(st.pages().mail().msgView().attachments().list().get(0).imgPreview())
                .clicksOn(st.pages().mail().msgView().attachments().list().get(0).show())
                .shouldSee(st.pages().mail().msgView().imageViewer())
                .shouldNotSee(st.pages().mail().msgView().imageViewerLoader());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Смотрим на аттачи в eml")
    @TestCaseId("3138")
    public void shouldSeeEmlWithAttaches() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(MSG_WITH_EML);
            st.user().defaultSteps().onMouseHover(st.pages().mail().msgView().attachments().list().get(0))
                .onMouseHoverAndClick(st.pages().mail().msgView().attachments().list().get(0).show())
                .shouldSee(st.pages().mail().msgView().attachedMessagePopup())
                .onMouseHover(st.pages().mail().msgView().attachedMessagePopup().attachments().list().get(0))
                .shouldSee(st.pages().mail().msgView().attachedMessagePopup().attachments().list().get(0).show());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть кнопки Сохранить, Скачать, Посмотреть по ховеру на аттач")
    @TestCaseId("6352")
    public void shouldSeeSaveDownloadOpenButtons() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageWithSubject(MSG_WITH_IMAGE);
            st.user().defaultSteps().onMouseHover(st.pages().mail().msgView().attachments().list().get(0))
                .shouldSee(
                    st.pages().mail().msgView().attachments().list().get(0).show(),
                    st.pages().mail().msgView().attachments().list().get(0).download(),
                    st.pages().mail().msgView().attachments().list().get(0).save()
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
