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
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.BIG_SIZE;
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_BIG;
import static ru.yandex.autotests.innerpochta.util.MailConst.YA_DISK_URL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Большие аттачи")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ATTACHES)
@RunWith(DataProviderRunner.class)
public class NewComposeBigAttachesTest extends BaseTest {

    private String sbj = getRandomName();

    private static final String DISK_URL = "https://yadi.sk";

    private AccLockRule lock = AccLockRule.use().useTusAccount(DISK_BIG);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensUrl(YA_DISK_URL)
            .opensDefaultUrlWithPostFix("/compose");
    }

    @Test
    @Title("Отправляем письмо с аттачем весом более 25 МБ и скачиваем с диска")
    @TestCaseId("1976")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-69328")
    public void shouldDownloadBigAttachThroughDisk() {
        sendLetterWithBigAttach();
        user.defaultSteps().opensFragment(QuickFragments.SENT);
        user.messagesSteps().clicksOnMessageWithSubject(sbj);
        user.defaultSteps().onMouseHover(onMessageView().attachments().list().get(0))
            .clicksOn(onMessageView().attachments().list().get(0).download())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(DISK_URL);
    }

    @Test
    @Title("Отправляем письмо с аттачем весом более 25 МБ и просматриваем на диске")
    @TestCaseId("1976")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-69328")
    public void shouldViewBigAttachThroughDisk() {
        sendLetterWithBigAttach();
        user.defaultSteps().opensFragment(QuickFragments.SENT);
        user.messagesSteps().clicksOnMessageWithSubject(sbj);
        user.defaultSteps().onMouseHover(onMessageView().attachments().list().get(0))
            .clicksOn(onMessageView().attachments().list().get(0).show())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(DISK_URL);
    }

    @Step("Отправляем письмо с большим аттачем")
    private void sendLetterWithBigAttach() {
        user.defaultSteps()
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), sbj);
        user.composeSteps().uploadLocalFile(onComposePopup().expandedPopup().localAttachInput(), BIG_SIZE);
        user.defaultSteps().waitInSeconds(5)
            .shouldNotSee(onComposePopup().expandedPopup().attachPanel().loadingAttach())
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
    }
}
