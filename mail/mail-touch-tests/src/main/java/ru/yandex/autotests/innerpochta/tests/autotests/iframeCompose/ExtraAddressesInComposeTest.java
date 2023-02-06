package ru.yandex.autotests.innerpochta.tests.autotests.iframeCompose;

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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.REPLY_ALL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на сборщики в композе")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
public class ExtraAddressesInComposeTest {

    private static final String SELF_NAME = "Def-Имя-autotests Def-Фамилия-autotests";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    String subj;

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().className());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(removeAllMessages(() -> steps.user(), SENT, DRAFT, TRASH));

    @Before
    public void prep() {
        subj = Utils.getRandomString();
        String yesterday = LocalDateTime.now().minusDays(1).format(DATE_FORMAT);
        steps.user().apiMessagesSteps().getAllMessagesByDate(yesterday, INBOX)
            .forEach(msg -> steps.user().apiMessagesSteps().deleteMessages(msg));
        steps.user().apiMessagesSteps().sendMailToSeveralReceivers(
            subj,
            Utils.getRandomString(),
            accLock.firstAcc().getSelfEmail(),
            DEV_NULL_EMAIL
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Сборщик должен быть виден в списке алиасов")
    @Description("У юзера подключён сборщик, потому что подключённый не сразу появляется в списке алиасов")
    @TestCaseId("1063")
    public void shouldSeeCollectorInFieldFrom() {
        steps.user().touchSteps().openComposeViaUrl();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().expandComposeFields())
            .clicksOn(steps.pages().touch().composeIframe().fieldFrom())
            .scrollTo(steps.pages().touch().composeIframe().inputBody())
            .shouldSeeThatElementHasText(steps.pages().touch().composeIframe().suggestAliases(), MAIL_COLLECTOR);
    }

    @Test
    @Title("Адрес для восстановления автоматически убран из адресатов при ответе на письмо")
    @TestCaseId("804")
    public void shouldNotSeeRestoreAddress() {
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageList().subjectList().waitUntil(not(empty())), subj)
            .clicksOn(steps.pages().touch().messageView().moreBtn())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), REPLY_ALL.btn());
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().shouldSeeElementsCount(steps.pages().touch().composeIframe().yabbles(), 1)
            .shouldContainText(steps.pages().touch().composeIframe().yabble(), SELF_NAME);
    }
}
