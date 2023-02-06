package ru.yandex.mail.tests.sendbernar;

import org.hamcrest.Matchers;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.api.RequestTraits;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.properties.TvmProperties;
import ru.yandex.mail.common.utils.Random;
import ru.yandex.mail.tests.mops.Mops;
import ru.yandex.mail.tests.mops.source.MidsSource;
import ru.yandex.mail.tests.sendbernar.generated.SendMessageResponse;
import ru.yandex.mail.tests.sendbernar.models.Message;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.tests.sendbernar.SendbernarApi.apiSendbernar;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.maxEmailAddr400;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.mail.things.matchers.StringDiffer.notDiffersWith;

@Aqua.Test
@Title("Ручка send_message")
@Description("Проверяем работу ручки send_message")
@Stories("mail send")
@Issues(@Issue("MAILPG-771"))
public class SendMessageTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.sendMessage;
    }

    @Test
    @Description("Меняем from_mailbox на белорусский Яндекс и отправляем письмо")
    public void shouldChangeFromMailbox() throws Exception {
        String fromMailbox = authClient.account().email("yandex.by");


        assertThat("Новый адрес равен старому", fromMailbox,
                not(is(authClient.account().email())));


        sendMessage()
                .withTo(authClient.account().email())
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
        String name = Random.string();


        sendMessage()
                .withTo(authClient.account().email())
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
                .withTo(authClient.account().email())
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(NO_SUBJECT_TITLE).waitDeliver().getMid();
        String firstline = byMid(mid).firstline();


        assertThat("Неверное значение первой строки",
                firstline,
                isEmptyString());
    }

    @Test
    @Description("Отправляем письмо без темы и проверяем, что текст firstline не пустой")
    public void shouldSendWithEmptySubjectAndNotEmptyText() throws Exception {
        String text = Random.string();
        sendMessage()
                .withTo(authClient.account().email())
                .withText(text)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(NO_SUBJECT_TITLE).waitDeliver().getMid();
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
                .withTo(authClient.account().email())
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
        String firstLineExpected = Random.string();
        StringBuilder address = new StringBuilder(authClient.account().email());
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
    @Title("ЮТФ-8 строка в оформленном письме")
    @Issue("DARIA-18687")
    @Description("Отправка письма с ютф содержимым в теле\n" +
            "- Проверка темы, первой строки и тела")
    public void shouldSendMailWithRussianAndChineseLetters() throws Exception {
        String text = "Русские и китайские символы 时间是最好的稀释剂，新舆论热点的出现，" +
                "不断转移公众的视线，掩盖了旧闻的解决。但是，一" +
                "个成熟的社会不会因为新热点的出现而习惯性地遗忘“旧闻”";


        sendMessage()
                .withTo(authClient.account().email())
                .withSubj(subj)
                .withText(text)
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).waitDeliver().getMid();
        Message msg = byMid(mid);
        String sentMid = waitWith.subj(subj).sent().waitDeliver().getMid();
        Message sentMsg = byMid(sentMid);


        assertThat("Первая строка в письме не соотвествует содержимому!", msg.firstline(), is(text));
        assertThat("Содержимое в письме не соответствует ожиданиям!",
                msg.content(),
                notDiffersWith(text)
                        .exclude(" ")
                        .exclude("\r")
                        .exclude("\n"));

        assertThat("Первая строка в письме в отправленных не соотвествует содержимому!", sentMsg.firstline(), is(text));
        assertThat("Содержимое в письме в отправленных не соответствует ожиданиям!",
                sentMsg.content(),
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
        sendMessage()
                .withUid(getUid())
                .withCaller("mobile")
                .withTo(authClient.account().email())
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
                .withTo(authClient.account().email())
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
                .withTo(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()));

        String mid = waitWith.subj(subj).sent().waitDeliver().getMid();



        Mops.label(authClient, new MidsSource(mid), Collections.singletonList(lidByTitle("draft_label")))
                .post(shouldBe(ok200()));


        sendMessage()
                .withTo(authClient.account().email())
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
                .withTo(authClient.account().email())
                .withSubj(subj)
                .post(shouldBe(ok200()));

        String mid = waitWith.subj(subj).sent().waitDeliver().getMid();



        Mops.label(authClient, new MidsSource(mid), Collections.singletonList(lidByTitle("delayed_message")))
                .post(shouldBe(ok200()));


        sendMessage()
                .withTo(authClient.account().email())
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
                .withTo(authClient.account().email())
                .withMessageId(messageId)
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class);

        assertThat("message_id в ответе сендбернара не совпадает с переданным", resp.getMessageId(), equalTo(messageId));

        String mid = waitWith.subj(subj).waitDeliver().getMid();

        String messageIdHeader = byMid(mid).getHeader("message-id");

        assertThat("message_id в ответе mbody не совпадает с переданным в sendbernar", messageIdHeader, equalTo(messageId));
    }
}
