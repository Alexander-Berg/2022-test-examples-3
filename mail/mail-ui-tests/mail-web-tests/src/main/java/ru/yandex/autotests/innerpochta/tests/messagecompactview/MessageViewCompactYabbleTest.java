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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.GET_CLIPBOARD_FROM_BROWSER;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_HEAD_FULL_EDITION;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.passport.api.core.matchers.common.IsNot.not;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Яблы при просмотре письма в списке писем")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.YABBLE)
public class MessageViewCompactYabbleTest extends BaseTest {

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем открытие письма в списке писем, 2Pane и yabble",
            of(
                SETTINGS_OPEN_MSG_LIST, TRUE,
                SETTINGS_HEAD_FULL_EDITION, TRUE
            )
        );
        user.apiMessagesSteps().addCcEmails(DEV_NULL_EMAIL)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), getRandomName(), getRandomString());
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessagesPresent();
    }

    @Test
    @Title("В выпадушке контакта нажать «Скопировать адрес»")
    @TestCaseId("940")
    public void shouldCopyContactAddress() {
        String bufferContent;
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps().shouldSee(onMessageView().messageHead())
            .clicksOn(onMessageView().messageHead().recipientsCount())
            .clicksOn(onMessageView().messageHead().contactsInCC().waitUntil(not(empty())).get(0))
            .shouldSee(onMessageView().contactBlockPopup())
            .clicksOn(onMessageView().contactBlockPopup().copyAddress());
        assertEquals(
            String.format(
                "Неправильная строка в буффере, хотели %s, но в буффере %s",
                DEV_NULL_EMAIL,
                getBrowserClipboard()
            ),
            getBrowserClipboard(),
            DEV_NULL_EMAIL
        );
    }

    private String getBrowserClipboard() {
        user.defaultSteps().executesJavaScript(GET_CLIPBOARD_FROM_BROWSER);
        return user.defaultSteps().executesJavaScriptWithResult("return window.cb;");
    }
}
