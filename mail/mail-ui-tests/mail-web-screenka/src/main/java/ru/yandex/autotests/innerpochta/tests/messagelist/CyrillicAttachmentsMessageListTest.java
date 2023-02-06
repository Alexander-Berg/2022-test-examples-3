package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.webcommon.rules.AccountsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_DISABLE_INBOXATTACHS;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Аттачи в списке писем")
@Description("На юзере заранее создано письмо с EML и письмо с множеством аттачей")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.ATTACHES)
@RunWith(DataProviderRunner.class)
public class CyrillicAttachmentsMessageListTest {

    private static final String CREDS_2 = "CyrillicAttachmentsTest";
    private static final int MSG_WITH_CYRILLIC_ATTACH = 0;
    private static final String ATTACH_TOOLTIP = "кириллический_аттач.txt (13 байт)";
    private static final String ATTACH_NAME = "кириллический_аттач.txt";

    private ScreenRulesManager rules = screenRulesManager().withLock(AccLockRule.use().names(CREDS_2));
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static AccountsRule account = new AccountsRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем превью аттачей в списке писем",
            of(SETTINGS_DISABLE_INBOXATTACHS, false)
        );
    }

    @Test
    @Title("Отображение аттачей с кириллическими символами в списке писем")
    @TestCaseId("4668")
    public void shouldSeeCyrillicAttachInMessagelist() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldHasTitle(
                st.pages().mail().home().displayedMessages().list().get(MSG_WITH_CYRILLIC_ATTACH)
                    .attachments().list().get(0).toolTip(),
                ATTACH_TOOLTIP
            );

        parallelRun.withActions(actions).withAcc(lock.acc(CREDS_2)).run();
    }
}
