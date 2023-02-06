package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

/**
 * Отправка писем с различными комбинациями русского в теме или тексте письма
 */

@Aqua.Test
@Title("Отправка писем. Письма, содержащие русский язык")
@Description("Отправляем письмо с русским языком в разных частях")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "MailWithRUSLang")
public class MailSendWithRussianLang extends BaseTest {

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().outbox();

    @Test
    @Title("Русский сабжект")
    @Description("Отправка письма с русским текстом в теме\n" +
            "- Поиск и загрузка первой строки письма с данной темой\n" +
            "- Проверка что первая строка соответствует отправленному")
    public void letterWithRussianSubject() throws Exception {
        logger.warn("Русский сабжект");
        String currentSubject = "Тема письма " + Util.getRandomString();
        String sendBody = "Тело письма MailSendWithRussianLang::letterWithRussianSubject()";
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSend(sendBody)
                .setSubj(currentSubject);
        // Отправка
        jsx(MailSend.class).params(msg).post().via(hc);
        // Ожидание доставки
        String mid = waitWith.subj(currentSubject).waitDeliver().getMid();
        // Получение mid
        String firstLine = api(Message.class)
                .params(MessageObj.getMsg(mid)).post().via(hc).getFirstlineText();
        assertThat("Первая строка не соотвествует ожидаемому значению", firstLine, equalTo(sendBody));

        // Чистка
        clean.subject(currentSubject);
    }

    @Test
    @Title("Отправка письма с русской ссылкой")
    @Description("Отправка письма с русской ссылкой в теле и русской темой\n" +
            "- Проверка содержимого полученного на равенство отправленному")
    public void letterWithLinkRf() throws Exception {
        logger.warn("Отправка письма с русской ссылкой. Внимание! Хардкод сравнение ссылки");
        String currentSubject = "Тема письма " + Util.getRandomString();
        String link = "http://русская_ссылка.рф";
        String sendBody = "<p>" + link + "</p>";
        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSend(sendBody)
                .setSubj(currentSubject)
                .setTtypeHtml();
        // Отправка
        jsx(MailSend.class).params(msg).post().via(hc);

        // Получение mid
        String mid = waitWith.subj(currentSubject).waitDeliver().getMid();

        String messageContent = api(Message.class)
                .params(MessageObj.getMsgWithContentFlag(mid)).post().via(hc).getContentTagAsSimpleHtmlResult();

        // Логирование
        logger.info("Найдено: " + messageContent);
        //String hash = Base64.encodeBase64URLSafeString(link.getBytes(Charsets.UTF_8));
        assertThat("Содержимое не соответствует ожидаемому. Ожидалось наличие внутри: русская_ссылка",
                messageContent,
                allOf(
                        containsString("l=aHR0cDovL3huLS1fLTdzYmIxYmNmNWFpYWRhcjVpNWIueG4tLXAxYWkv"),
//                        containsString(hash),
                        containsString("https://mail.yandex.ru/re"))
        );

        // Чистка
        clean.subject(currentSubject);
    }

}
