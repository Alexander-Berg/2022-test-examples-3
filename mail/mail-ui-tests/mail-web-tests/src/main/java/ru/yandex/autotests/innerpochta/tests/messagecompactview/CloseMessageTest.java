package ru.yandex.autotests.innerpochta.tests.messagecompactview;

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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;

/**
 * * @author yaroslavna
 */
@Aqua.Test
@Title("Тест на закрытие письма")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.COMPACT_VIEW)
public class CloseMessageTest extends BaseTest {

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private String subject;
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));


    @Before
    public void logIn() throws IOException {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        subject = user.apiMessagesSteps().sendMail(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем",
            of(SETTINGS_OPEN_MSG_LIST, STATUS_ON)
        );
    }

    @Test
    @Title("Закрытие письма кликом на крестик")
    @TestCaseId("1077")
    public void shouldCloseMsgWithCross() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessageView().closeMsgBtn());
        user.messagesSteps().shouldNotSeeOpenMessage();
    }

    @Test
    @Title("Закрытие письма кликом на тему письма")
    @TestCaseId("3623")
    public void shouldCloseMsgByClickOnSubj() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().clicksOn(onMessageView().messageSubject().subject());
        user.messagesSteps().shouldNotSeeOpenMessage();
    }

    @Test
    @Title("Закрытие письма кликом вне письма")
    @TestCaseId("3624")
    public void shouldCloseMsgByClickOutOfMsgView() {
        user.messagesSteps().clicksOnMessageWithSubject(subject);
        user.defaultSteps().offsetClick(
            onMessagePage().mail360HeaderBlock().yandexLogoMainPage().getLocation().getX() - 10,
            onMessagePage().mail360HeaderBlock().yandexLogoMainPage().getLocation().getY()
        );
        user.messagesSteps().shouldNotSeeOpenMessage();
    }

}
