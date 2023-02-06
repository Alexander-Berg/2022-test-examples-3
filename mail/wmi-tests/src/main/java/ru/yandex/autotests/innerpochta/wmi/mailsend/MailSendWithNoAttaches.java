package ru.yandex.autotests.innerpochta.wmi.mailsend;

import com.google.common.base.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageBody;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsg;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsgsIn;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;


@Aqua.Test
@Title("Отправка писем. Простейшие письма")
@Description("Отправляет простые письма без особых изысков. Смотрит на содержимое")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "Group2")
public class MailSendWithNoAttaches extends BaseTest {
    MailSendMsgObj msg;
    MailSend sendOper;
    String subj;

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().outbox().all();


    @Before
    public void prepare() throws Exception {
        // Заготовки писем
        msg = msgFactory.getSimpleEmptySelfMsg();
        sendOper = api(MailSend.class).params(msg);
        subj = msg.getSubj();
    }

    @Test
    @Description("Отправка пустого письма самому себе\n" +
            "- Просто проверка что дошло")
    public void simpleEmptyMail() throws Exception {
        logger.warn("Самый простой тест на отправку письма");
        sendOper.post().via(hc);
        assertThat(hc, withWaitFor(hasMsg(subj)));
        assertThat(hc, withWaitFor(hasMsgsIn(subj, 1, folderList.sentFID()), SECONDS.toMillis(10)));
    }


    @Test
    @Issue("DARIA-18188")
    @Title("Отправка письма на адрес с подчеркиванием и ya на конце")
    public void mailToDomainWithYa() throws Exception {
        msg.setTo(Util.getRandomString() + "_sasdfaaerqw@greaaaaya.ru");
        String val = sendOper.post().via(hc).getResultValue();
        waitWith.subj(subj).inFid(folderList.sentFID()).waitDeliver();
        assertThat("При отправке адрес " + msg.getTo() + " был заявлен невалидным",
                val, not(equalTo("incorrect_to")));
    }


    @Test
    @Issue("DARIA-17772")
    @Title("Отправка письма на адрес с подчеркиванием")
    public void mailToYaSubDomainWithUnderlineInAddr() throws Exception {
        msg.setTo(Util.getRandomString() + "_wtfadress@greaaaa.yandex.ru");
        assertThat("При отправке адрес " + msg.getTo() + " был заявлен невалидным",
                sendOper.post().via(hc).getResultValue(), not(equalTo("incorrect_to")));
    }

    @Test
    @Title("Письмо с темой и телом")
    @Description("Отправка письма с test в теле\n" +
            "- Проверка темы, первой строки и тела")
    public void simpleMail() throws Exception {
        msg.setSend("test");
        sendOper.post().via(hc);

        waitWith.subj(subj).waitDeliver();

        List<String> msgContent = getFirstLineAndContentOfMsg();

        assertThat("Первая строка в письме не соотвествует содержимому!", msgContent.get(0), equalTo("test"));
        assertThat("Первая строка в письме в отправленных не соотвествует содержимому!",
                msgContent.get(2), equalTo("test"));
        assertThat("Содержимое в письме не соответствует ожиданиям!", msgContent.get(1), equalTo("test\n"));
        assertThat("Содержимое в письме в отправленных не соответствует ожиданиям!",
                msgContent.get(3), equalTo("test\n"));
    }

    @Test
    @Title("ЮТФ-8 строка в оформленном письме")
    @Issue("DARIA-18687")
    @Description("Отправка письма с ютф содержимым в теле\n" +
            "- Проверка темы, первой строки и тела")
    public void simpleUTFMail() throws Exception {
        String send = "Русские и китайские символы 时间是最好的稀释剂，新舆论热点的出现，" +
                "不断转移公众的视线，掩盖了旧闻的解决。但是，一" +
                "个成熟的社会不会因为新热点的出现而习惯性地遗忘“旧闻”";
        msg.setSend(send).setTtypeHtml();
        sendOper.post().via(hc);

        waitWith.subj(subj).waitDeliver();

        List<String> msgContent = getFirstLineAndContentOfMsg();

        assertThat("[DARIA-18687] Первая строка в письме не соотвествует содержимому!", msgContent.get(0), is(send));
        assertThat("Первая строка в письме в отправленных не соотвествует содержимому!",
                msgContent.get(2), equalTo(send));
        assertThat("Содержимое в письме не соответствует ожиданиям!", msgContent.get(1), equalTo(send + "\n"));
        assertThat("Содержимое в письме в отправленных не соответствует ожиданиям!",
                msgContent.get(3), equalTo(send + "\n"));
    }

    @Test
    @Issue("DARIA-13650")
    @Title("Письмо с точкой, как первый символ в строке")
    @Description("Отправляет письмо со строками, содержащими первый символ строки - точку\n" +
            "- Проверка, что точка никуда не делась\n" +
            "DARIA-13650")
    public void testMailWithDotAsFirstSymbol() throws Exception {
        logger.warn("Письмо с точкой, как первый символ в строке - [DARIA-13650]");

        String send = ".d\n" +
                "..\n" +
                "..\n" +
                "...";

        msg.setSend(send);
        sendOper.post().via(hc);

        waitWith.subj(subj).waitDeliver();

        List<String> msgContent = getFirstLineAndContentOfMsg();

        assertThat("Первая строка в письме не соотвествует содержимому! [DARIA-13650]", msgContent.get(0),
                equalTo(".d .. .. ..."));
        assertThat("Первая строка в письме в отправленных не соотвествует содержимому! [DARIA-13650]",
                msgContent.get(2), equalTo(".d .. .. ..."));
        assertThat("Содержимое в письме не соответствует ожиданиям! [DARIA-13650]", msgContent.get(1),
                equalTo(send + "\n"));
        assertThat("Содержимое в письме в отправленных не соответствует ожиданиям! [DARIA-13650]",
                msgContent.get(3), equalTo(send + "\n"));
    }

    @Test
    @Title("html письмо с проверкой внутренностей")
    @Description("Отправка html письма\n" +
            "- Проверка внутренностей")
    public void simpleHtmlMail() throws Exception {
        logger.warn("html письмо с проверкой внутренностей");
        msg.setTtypeHtml()
                .setSend("<html><body>test</body></html>");
        sendOper.post().via(hc);

        waitWith.subj(subj).waitDeliver();

        List<String> msgContent = getFirstLineAndContentOfMsg();

        assertThat("Первая строка в письме не соотвествует содержимому!", msgContent.get(0), containsString("test"));
        assertThat("Содержимое в письме не соответствует ожиданиям!", msgContent.get(1), containsString("test"));
    }

    @Test
    @Title("Проверка ссылки в хтмл письме. Внимание! Хардкод сравнение ссылки")
    @Description("Отправка html письма со ссылкой\n" +
            "- Хардкордная проверка на равенство")
    public void simpleMailWithLink() throws Exception {
        String link = "http://te.st";
        msg.setTtypeHtml()
                .setSend("<html><body>" + link + "</body></html>");
        sendOper.post().via(hc);

        waitWith.subj(subj).waitDeliver();

        List<String> msgContent = getFirstLineAndContentOfMsg();

        assertThat("Первая строка в письме не соотвествует содержимому!", msgContent.get(0), containsString(link));
        assertThat("Содержимое в письме не соответствует ожиданиям!", msgContent.get(1), containsString(link));

        // Получаем html в представлении с флагом showContentMeta
        Message mrsp = api(Message.class)
                .params(MessageObj.getMsgWithContentFlag(msgContent.get(4)))
                .post().via(hc);

        String hash = Base64.encodeBase64URLSafeString(link.getBytes(Charsets.UTF_8));

        assertTrue("Содержимое в письме в представлении html не соответствует ожиданиям. Получено: " +
                        mrsp.getContentTagAsSimpleHtmlResult(),
                mrsp.getContentTagAsSimpleHtmlResult().contains(hash));
    }

    @Test
    @Title("Проверка, что на этом письме, не возвращается никакой ошибки")
    @Description("Существует возможность, что в-директ сломается на такой ссылке\n" +
            "Проверяем что в месседж боди не вернется ошибки при отправке письма с таким содержимым")
    public void testSendNotValidUrl() throws Exception {
        String html = "http://www.avp.travel.ru%2FAVP%5F99%2Ehtm" +
                "%23%25D0%259F%25D0%259E%25D0%25A5%25D0%259E%25D0%2594%25D0%25AB";
        msg.setTtypeHtml()
                .setSend(html);
        sendOper.post().via(hc);

        String mid = waitWith.subj(subj).waitDeliver().getMid();
        Message resp = jsx(MessageBody.class)
                .params(MessageObj.getMsg(mid)).post().via(hc).as(Message.class);

        assertFalse("При выводе письма, были получены ошибки", resp.toString().contains("error"));
    }

    @Test
    @Title("Отправка большого количества буков")
    @Description("Отправка письма с большим количеством буков в теле\n" +
            "- Проверка отправленное=полученное")
    public void largeMail() throws Exception {
        String longString = Util.getLongString();
        msg.setTtypeHtml()
                .setSend(longString);
        sendOper.post().via(hc);

        waitWith.subj(subj).waitDeliver();

        List<String> msgContent = getFirstLineAndContentOfMsg();
        assertThat("Отправленная длинная строка не соответствует полученной!", StringUtils.chomp(msgContent.get(1)),
                equalTo(longString));
    }

    /**
     * Получает mid письма и ищет и помещает в список следующее:
     * <p/>
     * 0 - firstline inbox
     * 1 - content inbox
     * 2 - fistline outbox
     * 3 - content outbox
     * 4 - mid inbox
     * 5 - mid outbox
     *
     * @return List<String> - список значений, перечисленных выше
     * @throws java.io.IOException *
     */
    private List<String> getFirstLineAndContentOfMsg() throws Exception {
        List<String> msgContentAndFirstline = new ArrayList<String>(6); // 6 элементов
        String midInbox = jsx(MailBoxList.class).post().via(hc)
                .getMidOfMessage(subj);   // Мид во Входящих

        // ФИД Отправленных
        String outboxFID = folderList.sentFID();

        assertThat(hc, hasMsgsIn(subj, 1, outboxFID));

        String midOutbox = jsx(MailBoxList.class)   // Мид в Отправленных
                .params(MailBoxListObj.inFid(outboxFID))
                .post().via(hc)
                .getMidOfMessage(subj); // Мид в Отправленных


        // Во входящих
        Message mrsp = api(Message.class)
                .params(MessageObj.getMsg(midInbox))
                .post().via(hc);

        msgContentAndFirstline.add(mrsp.getFirstlineText());                  // 0
        msgContentAndFirstline.add(mrsp.getContentTagText());          // 1


        // В отправленных
        Message mrspOutbox = api(Message.class)
                .params(MessageObj.getMsg(midOutbox))
                .post().via(hc);

        msgContentAndFirstline.add(mrspOutbox.getFirstlineText());           // 2
        msgContentAndFirstline.add(mrspOutbox.getTextValueOfContentTag());   // 3

        // Для дальнейшего использования передаем миды
        msgContentAndFirstline.add(midInbox);   // 4
        msgContentAndFirstline.add(midOutbox);  // 5

        return msgContentAndFirstline;
    }
}
