package ru.yandex.autotests.innerpochta.tests.autotests.Attachments;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MESSAGES;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_DISABLE_INBOXATTACHS;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Общие тесты на аттачи")
@Features(FeaturesConst.ATTACHES)
@Stories(FeaturesConst.GENERAL)
public class GeneralAttachmentsTest {

    private static final String WITH_ATTACHMENTS_LABEL_POSTFIX = "all/only_atta";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            accLock.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString(),
            IMAGE_ATTACHMENT,
            PDF_ATTACHMENT
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Показываем аттачи в метке «С вложениями» когда выключена настройка «Показывать вложения в списке писем»")
    @TestCaseId("287")
    public void shouldSeeAttachesInLabelWhenItsDisabled() {
        steps.user().apiSettingsSteps()
            .callWithListAndParams(
                "Выключаем показ аттачей в инбоксе",
                of(SETTINGS_DISABLE_INBOXATTACHS, TRUE)
            );
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(MESSAGES.makeTouchUrlPart(WITH_ATTACHMENTS_LABEL_POSTFIX))
            .shouldSee(steps.pages().touch().messageList().messageBlock().attachmentsInMessageList())
            .shouldNotSee(steps.pages().touch().messageList().messageBlock().clipOnMsg());
    }
}
