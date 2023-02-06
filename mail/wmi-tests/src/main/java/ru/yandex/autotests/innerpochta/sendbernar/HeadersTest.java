package ru.yandex.autotests.innerpochta.sendbernar;


import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.mbody.Attachment;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.Message;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.sendmessage.ApiSendMessage;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.MessagesWithInlines.getInlineHtml;


@Aqua.Test
@Title("Ручка send_message")
@Description("Отправляет письмо и тестирует различные хедеры")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "HeadersTest")
public class HeadersTest extends BaseSendbernarClass {
    private static final String X_YANDEX_SPAM_HEADER      = "X-Yandex-Spam";

    @ClassRule
    public static HttpClientManagerRule authFromClient = auth().with("Sendfrommetest");

    @Rule
    public CleanMessagesMopsRule cleanFrom = new CleanMessagesMopsRule(authFromClient).outbox().inbox();

    @Test
    @Issues({@Issue("DARIA-20483"), @Issue("DARIA-18687"), @Issue("MAILPG-844")})
    @Description("Отправляю письмо другому пользователю и смотрю какие хедеры ему пришли")
    public void shouldSendAndCheckXYandexHeaders() throws Exception {
        String name = "\"Pavel Durov, inContact.ru Admin\"";
        String strangeTo = String.format("%s <%s>, sender <%s>",
                name,
                authClient.acc().getSelfEmail(),
                authFromClient.acc().getSelfEmail());


        sendMessage()
                .withTo(strangeTo)
                .withSubj(subj)
                .withHtml(ApiSendMessage.HtmlParam.YES)
                .withText("<html><body><b>Какой-то текст</b></body></html>")
                .post(shouldBe(ok200()));


        String mid = waitWith.subj(subj).sent().waitDeliver().getMid();
        Message message = byMid(mid);


        assertThat(X_YANDEX_SPAM_HEADER + " неверный",
                message.getHeader(X_YANDEX_SPAM_HEADER),
                notNullValue());
        assertThat("Неверное количество заголовков Content-Type",
                message.getHeaders(CONTENT_TYPE).size(),
                equalTo(1));
        assertThat(CONTENT_TYPE + " неверный",
                message.getHeader(CONTENT_TYPE),
                equalTo("text/html; charset=utf-8"));
    }

    @Test
    @Issue("MAILPG-2366")
    @Description("Отправляем письмо с инлайн-картиной и проверяем, что Content-Id короче 255 символов")
    public void shouldSendEmlWithoutLongLines() throws Exception {
        int outlookLineLengthLimit = 255 - "Content-Id: <".length() - ">\r\n".length();

        String id = uploadedId();
        String viewLargeUrl = props().webattachHost() + "/message_part_real/?sid=" + id + "&no_disposition=y";

        sendMessage()
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .withHtml(ApiSendMessage.HtmlParam.YES)
                .withText(getInlineHtml(id, viewLargeUrl))
                .post(shouldBe(ok200()));

        String mid = waitWith.subj(subj).sent().waitDeliver().getMid();
        Message message = byMid(mid);

        for (Attachment attach : message.getAttachments()) {
            String cid = attach.getBinaryTransformerResult().getCid();
            assertThat("Content-Id может не влезть в outlook: \"" + cid + "\"",
                    cid.length(),
                    lessThanOrEqualTo(outlookLineLengthLimit));
        }
    }
}
