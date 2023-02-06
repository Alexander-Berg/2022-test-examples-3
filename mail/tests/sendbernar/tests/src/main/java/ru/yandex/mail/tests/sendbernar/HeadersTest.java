package ru.yandex.mail.tests.sendbernar;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.tests.sendbernar.generated.sendmessage.ApiSendMessage;
import ru.yandex.mail.tests.sendbernar.models.Message;
import ru.yandex.mail.things.utils.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;

@Aqua.Test
@Title("Ручка send_message")
@Description("Отправляет письмо и тестирует различные хедеры")
@Stories("mail send")
public class HeadersTest extends BaseSendbernarClass {
    @Override
    AccountWithScope mainUser() {
        return Accounts.headers;
    }

    private static final String X_YANDEX_SENDER_UID       = "X-Yandex-Sender-Uid";
    private static final String X_YANDEX_SPAM_HEADER      = "X-Yandex-Spam";
    private static final String X_YANDEX_FRONT_HEADER     = "X-Yandex-Front";
    private static final String X_YANDEX_TIME_MARK_HEADER = "X-Yandex-TimeMark";

    private UserCredentials authFromClient = new UserCredentials(Accounts.headersFrom);

    @Rule
    public CleanMessagesMopsRule cleanFrom = new CleanMessagesMopsRule(authFromClient).outbox().inbox();

    @Test
    @Issues({@Issue("DARIA-20483"), @Issue("DARIA-18687"), @Issue("MAILPG-844")})
    @Description("Отправляю письмо другому пользователю и смотрю какие хедеры ему пришли")
    public void shouldSendAndCheckXYandexHeaders() throws Exception {
        String name = "\"Pavel Durov, inContact.ru Admin\"";
        String strangeTo = String.format("%s <%s>, sender <%s>",
                name,
                authClient.account().email(),
                authFromClient.account().email());


        sendMessage()
                .withTo(strangeTo)
                .withSubj(subj)
                .withHtml(ApiSendMessage.HtmlParam.YES)
                .withText("<html><body><b>Какой-то текст</b></body></html>")
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).sent().waitDeliver().getMid();
        Message message = byMid(mid);


        assertThat(X_YANDEX_SENDER_UID + " неверный",
                message.getHeader(X_YANDEX_SENDER_UID),
                equalTo(getUid()));
        assertThat(X_YANDEX_SPAM_HEADER + " неверный",
                message.getHeader(X_YANDEX_SPAM_HEADER),
                notNullValue());
        assertThat(X_YANDEX_FRONT_HEADER + " неверный",
                message.getHeader(X_YANDEX_FRONT_HEADER),
                notNullValue());
        assertThat(X_YANDEX_TIME_MARK_HEADER + " неверный",
                message.getHeader(X_YANDEX_TIME_MARK_HEADER),
                notNullValue());
        assertThat("Неверное количество заголовков Content-Type",
                message.getHeaders(CONTENT_TYPE).size(),
                equalTo(1));
        assertThat(CONTENT_TYPE + " неверный",
                message.getHeader(CONTENT_TYPE),
                equalTo("text/html; charset=utf-8"));
    }
}
