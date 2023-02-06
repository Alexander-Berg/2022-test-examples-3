package ru.yandex.autotests.innerpochta.tests.compose;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * Created by cosmopanda
 */
@Aqua.Test
@Title("Новый композ - Тест просмотр ссылок mailto")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ADDITIONAL)
public class ComposeMailToLinkTest extends BaseTest {

    private static final String SEND_TO = "test1.c@yandex.ru";
    private static final String CC = "test2.cosmopanda@yandex.ru";
    private static final String SUBJECT = "Otvet2244";
    private static final String BODY = "test";
    private static final String MSG_SBJ = "mail";
    private static final String URL_WITH_MALICIOUS_TO = "#compose?to=alice@ya.ru<mallory@evil.com>";
    private static final String MALICIOUS_TO = "mallory@evil.com";
    private static final String MAILTO = "mailto:test1.c@yandex.ru?SuBJect=Otvet2244&BODY=test&CC=test2" +
        ".cosmopanda@yandex.ru";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws IOException {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Тест на переход по mailto ссылке")
    @TestCaseId("3293")
    public void shouldOpenCorrectCompose() {
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), MSG_SBJ, MAILTO);
        user.messagesSteps().clicksOnMessageWithSubject(MSG_SBJ);
        user.defaultSteps().clicksOn(onMessageView().messageTextBlock().messageHref().get(0))
            .switchOnJustOpenedWindow()
            .shouldSee(onComposePopup().expandedPopup());
        user.composeSteps().shouldSeeSendToAreaHas(SEND_TO)
            .shouldSeeCCAreaContains(CC)
            .shouldSeeSubject(SUBJECT)
            .shouldSeeTextAreaContains(BODY);
    }

    @Test
    @Title("Тест на переход по ссылке с подделанным To")
    @TestCaseId("4465")
    public void shouldSeeRealEmail() {
        user.defaultSteps().opensDefaultUrlWithPostFix(URL_WITH_MALICIOUS_TO);
        user.composeSteps().shouldSeeSendToAreaContains(MALICIOUS_TO);
    }
}
