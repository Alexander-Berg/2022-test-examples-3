package ru.yandex.autotests.innerpochta.tests.setting;

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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.ATTACHMENTS;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.EXCEL_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.TXT_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WORD_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_DISABLE_INBOXATTACHS;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Виджет Аттачей")
@Description("На юзере подготовлены письма с аттачами и без аттачей")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.ATTACHES)
public class SettingsAttachmentTest {

    private String subj_attaches = Utils.getRandomName();

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
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем настройку «Отображать вложения в списке писем»",
            of(SETTINGS_DISABLE_INBOXATTACHS, STATUS_ON)
        );
        stepsProd.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(lock.firstAcc().getSelfEmail(),
            subj_attaches, "", IMAGE_ATTACHMENT, PDF_ATTACHMENT, EXCEL_ATTACHMENT, WORD_ATTACHMENT, TXT_ATTACHMENT
        );
    }

    @Test
    @Title("Должны быть аттачи в метке «С вложениями»")
    @TestCaseId("2660")
    public void shouldSeeAttachWidgetAfterLabel() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().msgFiltersBlock().showWithAttach())
                .shouldBeOnUrlWith(ATTACHMENTS);
            st.user().messagesSteps().shouldSeeAllAttachmentInMsgList();
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны быть аттачи в поиске с вложениями")
    @TestCaseId("2661")
    public void shouldSeeAttachWidgetAfterCheckBox() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .clicksOn(st.pages().mail().search().mail360HeaderBlock().searchOptionsBtn())
                .clicksOn(st.pages().mail().search().advancedSearchBlock().advancedSearchRows().get(0))
                .shouldSee(st.pages().mail().search().otherResultsHeader());
            st.user().messagesSteps().shouldSeeAllAttachmentInMsgList();
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
