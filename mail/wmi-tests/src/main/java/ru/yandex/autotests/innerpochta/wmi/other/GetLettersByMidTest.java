/**
 *  Тестируется отправка письма, выхват мид через getLettersByMessageId и удаление письма
 *  @date 31.01.2012
 *  @time 19:32
 */
package ru.yandex.autotests.innerpochta.wmi.other;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.Creator;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxOperObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.GetLettersByMessageId;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsgs;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.GetLettersByMessageIdObj.messageId;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend.sendViaProd;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;


/**
 * @author jetliner
 * В pg не реализовано
 */
@Aqua.Test
@Title("Проверка ручки получения mid по messageId")
@Description("Тестирует ручку get_letters_by_message_id. В одном из способов получает messageId по имап. " +
                "Ахтунг. Содержит костыли для корпа")
@Features(MyFeatures.WMI)
@Stories(MyStories.OTHER)
@Credentials(loginGroup = GetLettersByMidTest.LOGIN_GROUP)
@Issue("MAILPG-117")
@IgnoreForPg
public class GetLettersByMidTest extends BaseTest {
    public static final String LOGIN_GROUP = "GetLettersByMid";

    private static String subject = "MailSendMethodTest" + RandomStringUtils.randomAlphabetic(10);
    private static String toName = "\"Pavel Durov, inContact.ru Admin\"";
    private static String mid;

    @BeforeClass
    public static void sendMail() throws Exception {
        HttpClientManagerRule aClient = auth().with("Jetlinersearchtest").login();

        // Костыль для корпа, т.к. на корпе не работает пересылка друг-другу у тестовых акков
        if (props().passportHost().getHost().contains(".yandex-team.")) {
            aClient.with(LOGIN_GROUP).login();
        }
        // *** Конец костыля ----------------------------------------


        DefaultHttpClient senderClient = aClient.authHC();
        MailSendMsgObj msg = new Creator(senderClient, aClient.acc())
                .getMailSendMsgObjFactory().getEmptyObj()
                .setSubj(subject)
                .setSend("This is the test message, please do not reply to it!")
                .setTo(toName + " <" + props().account(LOGIN_GROUP).getSelfEmail() + ">," +
                        ((!props().passportHost().getHost().contains(".yandex-team."))
                                // Костыль на
                                // костыле
                                ? " someStrangeTester <" + aClient.acc().getSelfEmail() + ">,"
                                : ""))
                .setFromMailbox(aClient.acc().getSelfEmail());

        sendViaProd(msg).post().via(senderClient);
    }

    /**
     * Дождаться, пока придет письмо, высланное в бефор-классе
     *
     * @throws Exception catch all
     */
    @Before
    public void prepare() throws Exception {
        waitWith.subj(subject).waitDeliver();
    }

    @Test
    @Title("Ручка get_letters_by_message_id должна возвращать тот же мид, что mailbox_list")
    public void testGetLettersByMessageIdCorp() throws Exception {
        mid = jsx(MailBoxList.class).post().via(hc).getMidOfMessage(subject);
        String messageId = api(Message.class)
                .params(MessageObj
                        .getMsg(mid))
                .post().via(hc)
                .getMessageIdTag();

        String midNew = jsx(GetLettersByMessageId.class)
                .params(messageId(messageId, folderList.defaultFID()))
                .post().via(hc).errorcodeShouldBeEmpty().getMid();
        assertThat("Миды в выдаче mailbox_list и get_letters_by_message_id разные", mid, equalTo(midNew));
    }


    /**
     * Удаляем сообщение с заданным mid,
     * Проверяем есть ли еще месседж с заданным сабжем в папке с полученным fid
     *
     * @throws Exception *
     */
    @AfterClass
    public static void deleteMessage() throws Exception {
        DefaultHttpClient hc = authClient.authHC();
        // Чистка во Входящих
        mid = jsx(MailBoxList.class)
                .post().via(hc)
                .getMidOfMessage(subject);
        if (null != mid && !mid.isEmpty()) {    // Если мид пуст, то ничего делать не надо
            jsx(MailboxOper.class)
                    .params(MailboxOperObj.deleteOneMsg(mid))
                    .post().via(hc);
        }
        assertThat("После чистки", hc, hasMsgs(subject, 0));
    }
}
