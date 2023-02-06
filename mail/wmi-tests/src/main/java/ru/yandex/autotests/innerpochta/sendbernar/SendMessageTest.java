package ru.yandex.autotests.innerpochta.sendbernar;


import org.hamcrest.Matchers;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.sendbernar.GenerateOperationIdResponse;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SendMessageResponse;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.Message;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendmessage.ApiSendMessage;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiSendbernar;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.StringDiffer.notDiffersWith;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.*;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;


@Aqua.Test
@Title("Ручка send_message")
@Description("Проверяем работу ручки send_message")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Issues(@Issue("MAILPG-771"))
@Credentials(loginGroup = "Group1")
public class SendMessageTest extends BaseSendbernarClass {

    @Test
    @Description("Меняем from_mailbox на белорусский Яндекс и отправляем письмо")
    public void shouldChangeFromMailbox() throws Exception {
        String fromMailbox = authClient.acc().getLogin()+"@yandex.by";


        assertThat("Новый адрес равен старому", fromMailbox,
                not(is(authClient.acc().getSelfEmail())));


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .withFromMailbox(fromMailbox)
                .post(shouldBe(ok200()));


        String fromEmail = byMid(waitWith.subj(subj).waitDeliver().getMid()).fromEmail();


        assertThat("Новый from_mailbox не совпадает с from",
                fromMailbox,
                is(fromEmail));
    }

    @Test
    @Description("Меняем from_name и отправляем письмо")
    public void shouldChangeFromName() throws Exception {
        String name = getRandomString();


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .withFromName(name)
                .post(shouldBe(ok200()));


        String fromName = byMid(waitWith.subj(subj).waitDeliver().getMid()).fromName();


        assertThat("Новый from_name не совпадает с from",
                fromName,
                is(name));
    }

    @Test
    @Description("Отправляем пустое письмо без темы и проверяем пустой firstline")
    public void shouldSendWithEmptySubject() throws Exception {
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(WmiConsts.NO_SUBJECT_TITLE).waitDeliver().getMid();
        String firstline = byMid(mid).firstline();


        assertThat("Неверное значение первой строки",
                firstline,
                isEmptyString());
    }

    @Test
    @Description("Отправляем письмо и ждём письмо со статусом доставки")
    public void shouldGetDsnMessage() {
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .withConfirmDelivery(ApiSendMessage.ConfirmDeliveryParam.YES)
                .post(shouldBe(ok200()));


        waitWith.subj(subj).waitDeliver();
        waitWith.subj("Письмо успешно доставлено").waitDeliver();
    }

    @Test
    @Description("Отправляем письмо без темы и проверяем, что текст firstline не пустой")
    public void shouldSendWithEmptySubjectAndNotEmptyText() throws Exception {
        String text = getRandomString();
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withText(text)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(WmiConsts.NO_SUBJECT_TITLE).waitDeliver().getMid();
        String firstline = byMid(mid).firstline();


        assertThat("Неверное значение первой строки",
                firstline,
                is(text));
    }

    @Test
    @Title("Русский сабжект")
    @Description("Отправка письма с русским текстом в теме\n" +
            "- Поиск и загрузка первой строки письма с данной темой\n" +
            "- Проверка что первая строка соответствует отправленному")
    public void shouldSendWithRussianSubject() throws Exception {
        String russianSubject = "Русская тема";
        String russianBody = "Русское тело asdf";
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(russianSubject)
                .withText(russianBody)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(russianSubject).waitDeliver().getMid();
        String firstLine = byMid(mid).firstline();


        assertThat("Первая строка не соотвествует ожидаемому значению", firstLine, equalTo(russianBody));
    }

    @Test
    @Issues({@Issue("MAILADM-4558"), @Issue("DARIA-54337")})
    @Title("Отправка письма с большим количеством получателей")
    @Description("Письмо не должно отправиться со статусом: max_email_addr_reached")
    public void shouldNotSendMailWithManyAddresses() throws Exception {
        String firstLineExpected = Util.getRandomString();
        StringBuilder address = new StringBuilder(authClient.acc().getSelfEmail());
        for (int i = 0; i < 50; i++) {
            address.append(" ").append(randomAddress());
        }


        sendMessage()
                .withTo(address.toString())
                .withSubj(subj)
                .withTo(firstLineExpected)
                .post(shouldBe(maxEmailAddr400()));
    }

    @Test
    @Title("Отправка письма с force7bit")
    @Issue("MAILPG-2652")
    @Description("Отправка письма с force7bit и проверка тела на не 8bit")
    public void shouldSendMessageWithForce7bitFlagIn7bit() throws Exception {
        String text = "Русские и китайские символы 时间是最好的稀释剂，新舆论热点的出现，" +
                "不断转移公众的视线，掩盖了旧闻的解决。但是，一" +
                "个成熟的社会不会因为新热点的出现而习惯性地遗忘“旧闻”";


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj + "yes")
                .withText(text)
                .withForce7bit(ApiSendMessage.Force7bitParam.YES)
                .post(shouldBe(ok200()));
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj + "no")
                .withText(text)
                .withForce7bit(ApiSendMessage.Force7bitParam.NO)
                .post(shouldBe(ok200()));


        Message with7bit = byMid(
                waitWith.inbox().subj(subj+"yes").waitDeliver().getMid()
        );
        Message without7bit = byMid(
                waitWith.inbox().subj(subj+"no").waitDeliver().getMid()
        );


        assertThat("Заголовок content-type не text/plain",
                with7bit.getHeader("content-type"),
                equalTo("text/plain; charset=utf-8"));
        assertThat("Заголовок content-transfer-encoding не не base64",
                with7bit.getHeader("content-transfer-encoding"),
                equalTo("base64"));

        assertThat("Заголовок content-type не text/plain",
                without7bit.getHeader("content-type"),
                equalTo("text/plain; charset=utf-8"));
        assertThat("Заголовок content-transfer-encoding не base64",
                without7bit.getHeader("content-transfer-encoding"),
                equalTo("8bit"));
    }

    @Test
    @Title("ЮТФ-8 строка в оформленном письме")
    @Issue("DARIA-18687")
    @Description("Отправка письма с ютф содержимым в теле\n" +
            "- Проверка темы, первой строки и тела")
    public void shouldSendMailWithRussianAndChineseLetters() throws Exception {
        String text = "Русские и китайские символы 时间是最好的稀释剂，新舆论热点的出现，" +
                "不断转移公众的视线，掩盖了旧闻的解决。但是，一" +
                "个成熟的社会不会因为新热点的出现而习惯性地遗忘“旧闻”";


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .withText(text)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();
        Message msg = byMid(mid);
        String sentMid = waitWith.subj(subj).sent().waitDeliver().getMid();
        Message sentMsg = byMid(sentMid);


        assertThat("Тело письма не соотвествует ожидаемому", msg.text(), is(text));
        assertThat("Содержимое в письме не соответствует ожиданиям!",
                msg.sourceContent(),
                notDiffersWith(text)
                        .exclude(" ")
                        .exclude("\r")
                        .exclude("\n"));

        assertThat("Тело отправленного письма не соотвествует ожидаемому!", sentMsg.text(), is(text));
        assertThat("Содержимое в письме в отправленных не соответствует ожиданиям!",
                sentMsg.sourceContent(),
                notDiffersWith(text)
                        .exclude(" ")
                        .exclude("\r")
                        .exclude("\n"));
    }

    @Test
    @Title("Хедер X-Yandex-Mobile-Caller")
    @Issue("MAILPG-694")
    @Description("Добавляем заголовок X-Yandex-Mobile-Caller если caller=mobile")
    public void shouldAddHeaderWithMobileCallerWhenCallerIsMobile() throws Exception {
        apiSendbernar(getUserTicket()).sendMessage()
                .withUid(getUid())
                .withCaller("mobile")
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();
        Message message = byMid(mid);


        String header = "X-Yandex-Mobile-Caller";
        assertThat("X-Yandex-Mobile-Caller неверный",
                message.getHeader(header),
                Matchers.equalTo("mobile"));
    }

    @Test
    @Title("Хедер X-Yandex-Mobile-Caller")
    @Issue("MAILPG-694")
    @Description("Не добавляем заголовок X-Yandex-Mobile-Caller если caller не mobile")
    public void shouldNotAddHeaderWithMobileCallerWhenCallerIsNotMobile() throws Exception {
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();
        Message message = byMid(mid);


        String header = "X-Yandex-Mobile-Caller";
        assertThat("X-Yandex-Mobile-Caller неверный",
                message.getHeader(header),
                nullValue());
    }

    @Test
    @Title("Отправка письма с source_mid из отправленных")
    @Issue("MAILPG-1503")
    @Description("Не должны удалять письмо если оно лежит в отправленных но помеченно черновиком")
    public void shouldNotRemoveMessageWithDraftLabelFromSentFid() throws Exception {
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()));

        String mid = waitWith.subj(subj).sent().waitDeliver().getMid();



        Mops.label(authClient, new MidsSource(mid), Collections.singletonList(lidByTitle("draft_label")))
                .post(shouldBe(ok200()));


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .withSourceMid(mid)
                .post(shouldBe(ok200()));


        byMid(mid).exists();
    }

    @Test
    @Title("Отправка письма с source_mid из отправленных")
    @Issue("MAILPG-1503")
    @Description("Не должны удалять письмо если оно лежит в отправленных но помеченно отложенным")
    public void shouldNotRemoveMessageWithDelayedLabelFromSentFid() throws Exception {
        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()));

        String mid = waitWith.subj(subj).sent().waitDeliver().getMid();



        Mops.label(authClient, new MidsSource(mid), Collections.singletonList(lidByTitle("delayed_message")))
                .post(shouldBe(ok200()));


        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .withSourceMid(mid)
                .post(shouldBe(ok200()));


        byMid(mid).exists();
    }

    @Test
    @Title("Проверяем, сохраняем ли параметр message_id")
    public void shouldSaveMessageId() {
        String messageId = "<1121537786231@wmi5-qa.yandex.ru>";

        SendMessageResponse resp = sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withMessageId(messageId)
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class);

        assertThat("message_id в ответе сендбернара не совпадает с переданным", resp.getMessageId(), equalTo(messageId));

        String mid = waitWith.subj(subj).waitDeliver().getMid();

        String messageIdHeader = byMid(mid).getHeader("message-id");

        assertThat("message_id в ответе mbody не совпадает с переданным в sendbernar", messageIdHeader, equalTo(messageId));
    }

    @Test
    @Title("Отвечаем из кэша при отправке второго письма с одним и тем же operation_id")
    public void shouldResponseFromCache() {
        String id = generateOperationId()
                .post(shouldBe(ok200()))
                .as(GenerateOperationIdResponse.class)
                .getId();

        String messageId = sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withOperationId(id)
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class)
                .getMessageId();

        String messageId2 = sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withOperationId(id)
                .withSubj(subj)
                .post(shouldBe(ok203()))
                .as(SendMessageResponse.class)
                .getMessageId();

        assertThat("message_id в ответе из кэша не совпадает с изначальным", messageId, equalTo(messageId2));
    }
}
