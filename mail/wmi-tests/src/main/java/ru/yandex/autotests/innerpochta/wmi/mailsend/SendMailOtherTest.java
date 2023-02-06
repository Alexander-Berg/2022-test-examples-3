package ru.yandex.autotests.innerpochta.wmi.mailsend;

import com.google.common.base.Joiner;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.*;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SendMessage;
import ru.yandex.autotests.innerpochta.wmi.core.oper.StoreMessage;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj.getMsg;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList.mailboxListJsx;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsCollectionMatcher.hasSameItemsAsCollection;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 01.12.14
 * Time: 14:51
 */
@Aqua.Test
@Title("Отправка писем. Проверка Message_id")
@Description("Проверк наличия в ответе message_id")
@Features(MyFeatures.WMI)
@Stories({MyStories.MAIL_SEND})
@Credentials(loginGroup = "SendMailOtherTest")
@RunWith(DataProviderRunner.class)
public class SendMailOtherTest extends BaseTest {

    public static final String CAPTCHA_PATH = "captcha.yandex.net/image?key=";

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient).all().allfolders();

    @ClassRule
    public static HttpClientManagerRule authClientTo = auth().with("sendMeTest", "testqa");

    @Rule
    public CleanMessagesRule cleanTo = CleanMessagesRule.with(authClientTo).all().allfolders();


    private MailSendMsgObj msg;

    private DefaultHttpClient httpClientTo = authClientTo.authHC();

    @Before
    public void prepare() throws Exception {
        msg = msgFactory.getSimpleEmptySelfMsg();
    }

    private String mid;

    public void shouldSeeMessageId(String mid, String messageId) throws IOException {
        String expected = api(Message.class).params(getMsg(mid)).post().via(hc).getMessageIdTag();
        assertThat("Проверяем, что message_id совпадает с message_id в message", messageId, equalTo(expected));
    }

    @Test
    @Issue("DARIA-41500")
    @Title("Проверка тэга message_id в mail_send")
    @Description("Отправляем письмо методом mail_send.\n" +
            "Сравниваем выдачу message_id в ручках mail_send и message.\n" +
            "Должны совпадать")
    public void testMessageIdInRespMailSend() throws Exception {
        String subj = Util.getRandomString();
        msg.setSubj(subj);
        String messageId = jsx(MailSend.class).params(msg).post().via(hc).getMessageId();
        mid = waitWith.subj(subj).waitDeliver().getMid();
        shouldSeeMessageId(mid, messageId);
    }


    @Test
    @Issue("DARIA-41500")
    @Title("Проверка тэга message_id в send_message")
    @Description("Отправляем письмо методом send_message.\n" +
            "Сравниваем выдачу message_id в ручках send_message и message.\n" +
            "Должны совпадать")
    public void testMessageIdInRespSendMessage() throws Exception {
        logger.warn("Отправка отложенного на 2 минуты письма");
        String subj = Util.getRandomString();
        msg.setSubj(subj);
        String messageId = jsx(SendMessage.class).params(msg).post().via(hc).as(MailSend.class).getMessageId();
        mid = waitWith.subj(subj).waitDeliver().getMid();
        shouldSeeMessageId(mid, messageId);
    }

    @Test
    @Issue("DARIA-41500")
    @Title("Проверка тэга message_id в store_message")
    @Description("Сохраняем письмо в черновик методом store_message.\n" +
            "Сравниваем выдачу message_id в ручках store_message и message.\n" +
            "Должны совпадать")
    public void testMessageIdInRespStoreMessage() throws Exception {
        String subj = Util.getRandomString();
        msg.setSubj(subj);
        String messageId = jsx(StoreMessage.class).params(msg).post().via(hc).as(MailSend.class).withDebugPrint().getMessageId();
        mid = waitWith.subj(subj).inFid(folderList.draftFID()).waitDeliver().getMid();

        shouldSeeMessageId(mid, messageId);
    }


    @Test
    @Issues({@Issue("DARIA-40940"), @Issue("MPROTO-3015")})
    @Title("Проверка типа письма, при отсутствии сохранения в отправленные")
    @Description("Отправляем письмо другому пользователю с параметром nosave=yes\n" +
            "(т.е. без сохранения в отправленные)\n" +
            "Проверяем наличие типа 4")
    public void sendMailShouldSeeType() throws Exception {
        String subj = "People type for me" + Util.getRandomString();
        sendWith.subj(subj).nosave(true).to(authClientTo.acc().getSelfEmail()).send();
        String mid = waitWith.subj(subj).usingHC(httpClientTo).waitDeliver().getMid();
        api(Message.class).params(getMsg(mid)).post().via(httpClientTo).shouldBe().types(containsString("4"));
    }

    @Test
    @Issue("MAILADM-4558")
    @Title("Отправка письма с большим display_name")
    @Description("Письмо должно успешно дойти")
    public void sendMailWithLongDisplayName() throws Exception {
        StringBuilder longAddress = new StringBuilder(Util.getLongString());
        for (int i = 0; i < 50; i++) {
            longAddress.append(Util.getLongString());
        }

        msg.setTo(longAddress + String.format(" <%s>", authClient.acc().getSelfEmail()));
        jsx(MailSend.class).params(msg).post().via(hc).statusOk();
        waitWith.subj(msg.getSubj()).waitDeliver();
    }

    @Test
    @Issue("MAILADM-4558")
    @Title("Отправка письма без to")
    @Description("Письмо не должно отправиться со статусом: no_recipients")
    public void sendMailWithoutTo() throws Exception {
        msg.setTo("");
        String status = jsx(MailSend.class).params(msg).post().via(hc).getStatusValue();
        assertThat(status, equalTo("no_recipients"));
    }

    @Test
    @Issue("MAILDEV-305")
    @Title("Отправляем письмо на email с кавычками. Должны нормализовать email")
    public void sendMailWithQuotes() throws Exception {
        String emailWithQuotes = "\"vicdev\"@yahoo.com";
        String expectedEmail = "vicdev@yahoo.com";
        msg.setTo(Joiner.on(", ").join(emailWithQuotes, authClientTo.acc().getSelfEmail()));
        jsx(MailSend.class).params(msg).post().via(hc).statusOk();
        waitWith.usingHC(authClientTo.authHC()).subj(msg.getSubj()).waitDeliver();
        shouldSeeToEmail(expectedEmail);
    }

    @Test
    @Issue("MAILDEV-305")
    @Title("Отправляем письмо на email с кавычками и точкой")
    public void sendMailWithQuotesAndDot() throws Exception {
        String emailWithQuotes = "\"vicdev.\"@yahoo.com";
        msg.setTo(Joiner.on(", ").join(emailWithQuotes, authClientTo.acc().getSelfEmail()));
        jsx(MailSend.class).params(msg).post().via(hc).statusOk();
        waitWith.usingHC(authClientTo.authHC()).subj(msg.getSubj()).waitDeliver();
        shouldSeeToEmail(emailWithQuotes);
    }

    @Step("Должны увидеть получателя <{0}> в двух письмах из папок \"Отправленные\" и \"Входящие\"")
    public void shouldSeeToEmail(String expected) {
        String mid = mailboxListJsx(MailBoxListObj.empty().setCurrentFolder(folderList.defaultFID())).post().via(authClientTo.authHC()).getMidOfMessage(msg.getSubj());
        List<String> emails = api(Message.class).params(MessageObj.getMsg(mid)).post().via(authClientTo.authHC()).toEmailList();
        assertThat("Не нашли в письме email-ов в папке \"Входящие\" [MAILDEV-305]", emails,
                hasSameItemsAsCollection(newArrayList(expected, authClientTo.acc().getSelfEmail().toLowerCase())));

        String midSent = mailboxListJsx(MailBoxListObj.empty().setCurrentFolder(folderList.sentFID())).post().via(hc)
                .getMidOfMessage(msg.getSubj());
        List<String> emailsOutbox = api(Message.class).params(MessageObj.getMsg(midSent)).post().via(hc).toEmailList();
        assertThat("Не нашли в письме email-ов в папке \"Отправленные\" [MAILDEV-305]", emailsOutbox,
                hasSameItemsAsCollection(newArrayList(expected, authClientTo.acc().getSelfEmail().toLowerCase())));
    }

    @Test
    @Issue("MAILDEV-100")
    @Title("Отправка письма c умлаутами или диакритиками")
    @DataProvider({
            "arşiv çıktı",
            "!@#$%^&*()_-+123",
            "Peter Råbåck",
            "Pëter Röback",
            "Jeremy Lainé",
    })
    @Description("Письмо должно успешно отправиться и to должен корректно отобразиться")
    public void sendMailWithDiacriticUmlautAndOthersTo(String name) throws Exception {
        msg.setFromName(name).setTo(quoted(name) + String.format(" <%s>", authClient.acc().getSelfEmail()));
        jsx(MailSend.class).params(msg).post().via(hc).statusOk();
        String mid = waitWith.subj(msg.getSubj()).waitDeliver().getMid();
        Message message = api(Message.class).params(MessageObj.getMsg(mid)).post().via(hc);
        String toName = message.getToName();
        String fromName = message.getFromName();
        assertThat("Неверный TO с умлаутами или диакритиками [MAILDEV-100]", toName, equalTo(name));
        assertThat("Неверный FROM с умлаутами или диакритиками [MAILDEV-100]", fromName, equalTo(name));
    }

    public static String quoted(String str) {
        return String.format("\"%s\"", str);
    }

    @Test
    @Issues({@Issue("MAILADM-4558"), @Issue("DARIA-54337")})
    @Title("Отправка письма пс большим количесвом получателей")
    @Description("Письмо не должно отправиться со статусом: max_email_addr_reached")
    public void sendMailWithManyAddresses() throws Exception {
        String firstLineExpected = Util.getRandomString();
        StringBuilder address = new StringBuilder(authClient.acc().getSelfEmail());
        for (int i = 0; i < 50; i++) {
            address.append(" ").append(Util.getRandomAddress());
        }

        msg.setSend(firstLineExpected).setTo(address.toString());
        String status = jsx(MailSend.class).params(msg).post().via(hc).getStatusValue();
        assertThat(status, equalTo("max_email_addr_reached"));
    }

    @Test
    @Issue("MAILPG-768")
    @Title("Проверяем <captcha_url> и <captcha_key>")
    @Description("Примерный вывод <captcha_key>d3M8qiAYxcBZjwxHfJfL21k6D3i4v2j8</captcha_key>\n" +
            "<captcha_url>http://s.captcha.yandex.net/image?key=d3M8qiAYxcBZjwxHfJfL21k6D3i4v2j8</captcha_url>")
    public void sendLightSpamShouldSeeCaptcha() throws Exception {
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSend(WmiConsts.LIGHT_SPAM).setTtypePlain();

        MailSend msOper = jsx(MailSend.class).params(msg).post().via(hc).shouldBeLightSpam();
        assertThat("Неверный URL капчи [MAILPG-768]", msOper.getCaptchaUrl(),
                containsString(CAPTCHA_PATH + msOper.getCaptchaKey()));
    }

}
