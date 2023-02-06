package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.steps.DefaultSteps.sign;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_USER_NAME;

@Aqua.Test
@Title("Изменение расположения подписи отправителя")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@RunWith(Parameterized.class)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SenderInfoStorySignatureDispositionTest extends BaseTest {

    private static final String REPLY_WITH_SIGNATURE_TOP = "signature text\n\n\n\n%s," +
        " \"%s\" <%s>:\nsdfasfasdfasfdasfd";
    private static final String REPLY_WITH_SIGNATURE_BOTTOM = "%s, \"%s\" " +
        "<%s>:\nsdfasfasdfasfdasfd\n\n\nsignature text";
    private static final String SIGN_TEXT = "signature text";

    private String subject = Utils.getRandomName();
    private Message msg;
    private String accountName;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Parameterized.Parameter(0)
    public int settingIndex;

    @Parameterized.Parameter(1)
    public String signText;

    @Parameterized.Parameter(2)
    public String messageForTestTitle;

    @Parameterized.Parameters(name = "Расположение подписи -  {2}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][] {
                {0, REPLY_WITH_SIGNATURE_TOP, "сразу после ответа"},
                {1, REPLY_WITH_SIGNATURE_BOTTOM, "внизу всего письма"}
        });
    }

    @Before
    public void logIn() throws InterruptedException, ParseException {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды, включаем просмотр письма в списке писем",
            of(
                SETTINGS_FOLDER_THREAD_VIEW, FALSE,
                SETTINGS_OPEN_MSG_LIST, TRUE
            )
        );
        msg = user.apiMessagesSteps().sendMail(lock.firstAcc(), subject, "sdfasfasdfasfdasfd");
        accountName = user.apiSettingsSteps().getUserSettings(SETTINGS_USER_NAME);
        user.apiSettingsSteps().changeSignsWithTextAndAmount(sign(SIGN_TEXT));
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
    }

    @Test
    @Title("Тестируем настройку “Расположение подписи“")
    @TestCaseId("1854")
    public void testSignature() throws ParseException {
        user.defaultSteps().selectsRadio(onSenderInfoSettingsPage().blockSetupSender().signPlace(), settingIndex);
        user.settingsSteps().saveSettingsIfCanAndClicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(0));
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().clicksOn(onMessageView().contentToolbarBlock().replyBtn());
        user.composeSteps().revealQuotes()
            .shouldSeeTextWithSignature(String.format(signText, getMsgDate(msg),
                accountName, lock.firstAcc().getSelfEmail()));
    }

    private String getMsgDate(Message msg) throws ParseException {
        return new SimpleDateFormat("dd.MM.yyyy, HH:mm").format(new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss").parse(msg.getDate().getIso()));
    }
}
