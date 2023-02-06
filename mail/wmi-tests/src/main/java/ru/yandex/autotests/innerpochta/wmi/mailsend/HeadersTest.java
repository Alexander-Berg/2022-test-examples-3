package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.Creator;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageSourceObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ComposeCheck;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.xml.xpath.XPathExpressionException;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.innerpochta.wmi.core.base.DocumentConverter.from;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsg;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.DeleteUtils.deleteOneMessageBySubjFromInboxOutbox;

/**
 * Проверка хедеров в письме
 * <p/>
 * Захардкоженный тест - заставить работать на корпе пока не удалось
 *
 * @author jetliner
 */
@Aqua.Test
@Title("Проверка наличия хедеров у письма")
@Description("Отправляет письмо и тестирует различные хедеры. Часть получаем посредством имап")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = HeadersTest.LOGIN_GROUP)
public class HeadersTest extends BaseTest {
    public static final String LOGIN_GROUP = "HeadersTest";


    @ClassRule
    public static HttpClientManagerRule authFromClient = auth().with("Sendfrommetest");


    public static final String X_YANDEX_SENDER_UID = "X-Yandex-Sender-Uid";
    public static final String X_YANDEX_SPAM_HEADER = "X-Yandex-Spam";
    public static final String X_YANDEX_FRONT_HEADER = "X-Yandex-Front";
    public static final String X_YANDEX_TIME_MARK_HEADER = "X-Yandex-TimeMark";


    private static String subject = "HeaderTest" + Util.getRandomString();
    private static String mid;
    //Кавычки важны для поиска в заголовках!
    private static String toName = "\"Pavel Durov, inContact.ru Admin\"";

    /**
     * Отсылка письма с другого ящика в этот
     *
     * @throws Exception *
     */
    @BeforeClass
    public static void initAndPrepare() throws Exception {
        DefaultHttpClient shc = authFromClient.authHC();
        MailSendMsgObj msg = new Creator(shc, authFromClient.acc()).getMailSendMsgObjFactory().getEmptyObj()
                .setTo(toName + " <" + props().account(LOGIN_GROUP).getSelfEmail() + ">,"
                        + " sender <" + authFromClient.acc().getSelfEmail() + ">,")
                .setFromMailbox(authFromClient.acc().getSelfEmail())
                .setFromName("sdfasdfasfs" + "%0d%0aX-foo:bar")    //[DARIA-21823]
                .setSend("Headers::sendMail() <div>\"веб интерфейс, вроде ничего не перекидывал никуда.\"</div><div>" +
                        " </div><div>14.01.2013, 13:09, \"Nikolay Muravyov\" &lt;bonifaci@yandex-team.ru&gt;:</div>" +
                        "<blockquote><div>Роксана, можешь спросить, он письмо из спама переложил во входящие?" +
                        " Он пользуется вебом или настольной программой? </div><div> </div><div>14.01.2013, 12:48" +
                        ", \"Georgy Shmarovoz\" &lt;<a href=\"mailto:sgeorge@yandex-team.ru\">sgeorge@yandex-team." +
                        "ru</a>&gt;:</div><blockquote>Жалоб от него нет с 27.12.12, в логах проверки СО писем от " +
                        "Яндекса для этого получателя за указанные даты (09.01-11.01) я не вижу.<br /> <br /> Roxa" +
                        "na Kolosova пишет:<blockquote cite=\"mid:34951357930751@webcorp2g.yandex-team.ru\"><div" +
                        ">Логин Pavel419, про какой апдейт - все еще не ясно <a href=\"https://twitter.com/Pavel419/" +
                        "statuses/289675530226249730\">https://twitter.com/Pavel419/statuses/289675530226249730</a>" +
                        "</div><div> </div><div>11.01.2013, 14:04, \"Nikolay Muravyov\" <a href=\"mailto:bonifaci@y" +
                        "andex-team.ru\">&lt;bonifaci@yandex-team.ru&gt;</a>:</div><blockquote>Спросите, пожалуйста" +
                        ", что за апдейт, какой у него логин, и пусть жмакнет «не спам» на это письме<br /> + so" +
                        "<br /> <br /><div>11.01.2013 11:35, Инна Грингольц пишет:</div><blockquote cite=\"mid:40113" +
                        "7617.3879.1357889714506.JavaMail.blogmonitor@blogmonitor01e.tools.yandex.net\">Original" +
                        ": <a href=\"http://twitter.com/Pavel419/statuses/289625887131967488\">http://twitter.com/" +
                        "Pavel419/statuses/289625887131967488</a><br /> Author: <a href=\"http://twitter.com/Pavel" +
                        "419\">http://twitter.com/Pavel419</a><br /> Publication date: 2013.01.11 06:52<br /><bloc" +
                        "kquote style=\"border-left:3px solid #aaaaaa;padding:10px;\">Письмо от <strong>Яндекса</" +
                        "strong> про апдейт сам <strong>Яндекс</strong> поместил в папку спам. Забавно :)</blo" +
                        "ckquote>Пожалуйста, не убирайте monblog@ из списка адресатов</blockquote><br /><pre>-- \n" +
                        "Best Regards, Nikolay Muravyov\n" +
                        "Support of communitainment services Yandex</pre></blockquote></blockquote></blockquo" +
                        "te><div> </div><div> </div><div>-- <br />Best Regards, Nikolay Muravyov<br />Support " +
                        "of communitainment services Yandex</div></blockquote>")
                .setSubj(subject).setTtypeHtml();
        //пробуем отправлять через продакшн и mail_send
        api(MailSend.class).setHost(props().productionHost()).params(msg).post().via(shc).errorcodeShouldBeEmpty();

        // Ожидание
        waitWith.subj(subject).usingHC(shc).waitDeliver();
    }

    /**
     * Подготовка необходимых переменных
     * Поиск письма в папке
     * Получение его mid
     *
     * @throws Exception *
     */
    @Before
    public void prepare() throws Exception {
        mid = jsx(MailBoxList.class).post().via(hc).errorcodeShouldBeEmpty().getMidOfMessage(subject);
    }

    @Test
    @Description("Проверка хедера To\n" +
            "т.к. метод использует wmi не умеющий доставать пачку заголовков письма,\n" +
            "ищем просто текст в кавычках (как и в переменной toName)\n" +
            "- хедер должен соответствовать тому что в переменной toName")
    public void checkToHeader() throws Exception {
        Document message = api(Message.class)
                .params(MessageObj.getMsgWithContentFlag(mid))
                .post().via(hc)
                .toDocument();
        shouldHaveQuotedName(toName, message);
    }

    private void shouldHaveQuotedName(String name, Document message) throws XPathExpressionException {
        assertThat(from(message).byXpath("//quoted/text()").asList(), hasItem(equalTo(name)));
    }

    @Test
    @Issues({@Issue("DARIA-20483"), @Issue("MAILYATEAM-510"), @Issue("DARIA-18687")})
    @Title("Должны получить все нужные X-Yandex заголовки в письме")
    @Description("Получаем по imap заголовки письма\n" +
            "Проверяем наличие:\n" +
            "- X-Yandex-Sender-Uid\n" +
            "- X-Yandex-Spam:\n" +
            "- X-Yandex-Front:\n" +
            "- X-Yandex-TimeMark:\n" +
            "fixed bug: [DARIA-20483]\n" +
            "[MAILYATEAM-510]\n" +
            "[DARIA-18687]\n" +
            "[MAILPG-844]")
    public void checkXYandexHeaders() throws Exception {
        MessageSource source = api(MessageSource.class).params(MessageSourceObj.getSourceByMid(mid)).post().via(hc);
        String uidFrom = api(ComposeCheck.class).get().via(authFromClient.authHC()).getUid();

        //MAILPG-778
        assertThat(X_YANDEX_SENDER_UID + " неверный [MAILPG-844]", source.headerJsx(X_YANDEX_SENDER_UID), equalTo(uidFrom));

        assertThat(X_YANDEX_SPAM_HEADER + " неверный", source.headerJsx(X_YANDEX_SPAM_HEADER), notNullValue());
        assertThat(X_YANDEX_FRONT_HEADER + " неверный", source.headerJsx(X_YANDEX_FRONT_HEADER), notNullValue());
        assertThat(X_YANDEX_TIME_MARK_HEADER + " неверный", source.headerJsx(X_YANDEX_TIME_MARK_HEADER), notNullValue());


        System.out.println(source.messageSource());
        logger.warn("[DARIA-20483] Проверка количества заголовков Content-Type");
        assertThat("Неверное количество заголовков Content-Type [DARIA-20483]",
                countMatches(source.messageSource(), CONTENT_TYPE + ":"), equalTo(1));

        logger.warn("[MAILYATEAM-510] [DARIA-18687] Проверка содержимого Content-Type");
        assertThat(CONTENT_TYPE + " неверный", source.headerJsx(CONTENT_TYPE), equalTo("text/html; charset=utf-8"));
    }

    /**
     * Удаляем сообщение и проверяем, что его не осталось
     *
     * @throws Exception *
     */
    @AfterClass
    public static void deleteMessage() throws Exception {
        deleteOneMessageBySubjFromInboxOutbox(subject);
        assertThat("Message " + subject + " wasn't deleted", authClient.authHC(), not(hasMsg(subject)));
    }
}