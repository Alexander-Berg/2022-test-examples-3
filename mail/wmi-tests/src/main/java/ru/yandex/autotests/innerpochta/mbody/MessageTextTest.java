package ru.yandex.autotests.innerpochta.mbody;

import org.apache.commons.lang.StringUtils;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.mbody.Text;
import ru.yandex.autotests.innerpochta.beans.mbody.TextPart;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.Message;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.mbody.MbodyResponses.*;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Features(MyFeatures.MBODY)
@Stories(MyStories.MBODY)
@Credentials(loginGroup = "MbodyMessageTextTest")
public class MessageTextTest extends MbodyBaseTest {
    public static final String FAILED_TO_PARSE_PARAMS_ERROR = "failed to parse params";
    public static final String MESSAGE_NOT_FOUND_ERROR_PATTERN = "exception: error in forming message: getMessageAccessParams error: unknown mid=%s";
    public static final String NON_EXISTENT_MID =  "0";
    public static final String INVALID_MID = ":-)";
    public static final String SUBJ_PREFIX = "[mbody/MessageTextTest]";

    @ClassRule
    public static CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Без обязательных аргументов ручка выдает 400")
    public void testApiMessageTextWithoutRequiredArgs() {
        apiMbody().text()
                .get(shouldBe(missingParam400(FAILED_TO_PARSE_PARAMS_ERROR)));

        apiMbody().text()
                .withUid("77777")
                .get(shouldBe(missingParam400(FAILED_TO_PARSE_PARAMS_ERROR)));

        apiMbody().text()
                .withMid("1111111111")
                .get(shouldBe(missingParam400(FAILED_TO_PARSE_PARAMS_ERROR)));
    }

    @Test
    @Title("Для несуществующего письма ручка выдает 400")
    public void testApiMessageTextWithNonExistentMid() {
        final String midError = String.format(MESSAGE_NOT_FOUND_ERROR_PATTERN, NON_EXISTENT_MID);
        apiMbody().text()
                .withUid(authClient.account().uid())
                .withMid(NON_EXISTENT_MID)
                .get(shouldBe(badRequest400(midError)));
    }

    @Test
    @Title("Для невалидного mid ручка выдает 500")
    public void testApiMessageTextWithInvalidMid() {
        apiMbody().text()
                .withUid(authClient.account().uid())
                .withMid(INVALID_MID)
                .get(shouldBe(error500WithString(INVALID_MID)));
    }

    @Test
    @Title("Для небольшого text/plain письма на английском выдает текст полностью и распознает язык")
    public void testSmallEnglishTextPlainMessageText() throws Exception {
        final String content = "This is a mail body text.\nSecond line of message.";

        final String mid = sendWith(authClient)
                .viaProd()
                .subj(SUBJ_PREFIX + " English text/plain small message")
                .text(content)
                .send().waitDeliver().getMid();

        final Text resp = apiMbody().text()
                .withUid(authClient.account().uid())
                .withMid(mid)
                .get(shouldBe(ok200())).peek().as(Text.class);

        final int expectedPartCount = 1;
        final String expectedMode = "full";
        final String expectedLang = "en";
        final String expectedContent = content;

        checkResponse(resp, expectedPartCount, expectedMode, expectedLang, expectedContent);
    }

    @Test
    @Title("Для не очень большого text/plain письма на русском в base64 выдает начало текста и распознает язык")
    public void testNotVeryLargeRussianTextPlainBase64MessageText() throws Exception {
        final String contentPrefix = ""
                + "Это первая строка не очень длинного сообщения на русском.\n"
                + "Это вторая строка не очень длинного сообщения на русском.\n"
                + "Это третья строка не очень длинного сообщения на русском.\n"
                + "Это четвёртая строка не очень длинного сообщения на русском.\n"
                + "Это пятая строка не очень длинного сообщения на русском.\n"
                + "Это шестая строка не очень длинного сообщения на русском.\n"
                + "Это седьмая и последняя значимая строка не очень длинного сообщения на русском.\n"
                + " ";

        final String content = ""
                + contentPrefix
                + "\n\n"
                + StringUtils.repeat("повторяющиеся фрагменты для принудительного сохранения в base64 ", 16);

        final String mid = sendWith(authClient)
                .viaProd()
                .subj(SUBJ_PREFIX + " Русское text/plain & base64 не очень длинное сообщение")
                .text(content)
                .send().waitDeliver().getMid();

        Message message = new Message(mid, authClient);
        final String contentTransferEncoding = message.getHeader("Content-Transfer-Encoding");

        assertThat("Content-Transfer-Encoding должен быть base64",
                contentTransferEncoding, equalTo("base64"));

        final Text resp = apiMbody().text()
                .withUid(authClient.account().uid())
                .withMid(mid)
                .withMaxSize(Integer.toString(contentPrefix.getBytes(UTF_8).length))
                .get(shouldBe(ok200())).peek().as(Text.class);

        final int expectedPartCount = 1;
        final String expectedMode = "trimmed";
        final String expectedLang = "ru";
        final String expectedContent = contentPrefix;

        checkResponse(resp, expectedPartCount, expectedMode, expectedLang, expectedContent);
    }

    @Test
    @Title("Для не очень большого text/html письма на русском выдает отфильтрованный текст и распознает язык")
    public void testNotVeryLargeRussianHtmlMessageText() throws Exception {
        final String content = ""
                + "Это первая строка не очень <b>длинного</b> сообщения на русском.<br/>\n"
                + "Это вторая строка <i>не очень длинного</i> сообщения на русском.<br/>\n"
                + "Это <pre>третья строка не очень длинного сообщения</pre> на русском.<br/>\n"
                + "Это четвёртая <a href=\"http://ya.ru/\">строка</a> не очень длинного сообщения на русском.<br/>\n"
                + "<ul>\n"
                + "    <li>Это пятая строка не очень длинного сообщения на русском.</li>\n"
                + "    <li>Это шестая строка не очень длинного сообщения на русском.</li>\n"
                + "    <li>Это седьмая и последняя строка не очень длинного сообщения на русском.</li>\n"
                + "</ul>\n";

        final String mid = sendWith(authClient)
                .viaProd()
                .subj(SUBJ_PREFIX + " Русское text/html не очень длинное сообщение")
                .text(content)
                .html("yes")
                .send().waitDeliver().getMid();

        Message message = new Message(mid, authClient);
        final String contentType = message.getHeader("Content-Type");

        assertThat("Content-Type должен быть text/html в UTF-8",
                contentType, equalToIgnoringWhiteSpace("text/html; charset=utf-8"));

        final Text resp = apiMbody().text()
                .withUid(authClient.account().uid())
                .withMid(mid)
                .get(shouldBe(ok200())).peek().as(Text.class);

        final int expectedPartCount = 1;
        final String expectedMode = "full";
        final String expectedLang = "ru";
        final String expectedFilteredContent = ""
                + "Это первая строка не очень длинного сообщения на русском.<br>\n"
                + "Это вторая строка не очень длинного сообщения на русском.<br>\n"
                + "Это третья строка не очень длинного сообщения на русском.<br>\n"
                + "Это четвёртая строка не очень длинного сообщения на русском.<br>\n"
                + "    Это пятая строка не очень длинного сообщения на русском.<br>\n"
                + "    Это шестая строка не очень длинного сообщения на русском.<br>\n"
                + "    Это седьмая и последняя строка не очень длинного сообщения на русском.<br>\n";

        checkResponse(resp, expectedPartCount, expectedMode, expectedLang, expectedFilteredContent);
    }

    @Test
    @Title("Для очень большого письма выдает firstline")
    public void testVeryLargeMessageText() throws Exception {
        String content = ""
                + "Hello, my dear friend!\n"
                + "\n"
                + "This is a very large message what contains few readable text data\n"
                + "more than in firstline.\n"
                + "\n"
                + "There are several more lines in this message:\n"
                + "extra line one\n"
                + "extra line two\n"
                + "extra line three\n"
                + "extra line four\n"
                + "extra line five\n"
                + "extra line six\n"
                + "extra line seven\n"
                + "extra line eight\n"
                + "extra line nine\n"
                + "extra line ten\n"
                + "extra line eleven\n"
                + "extra line twelve\n"
                + "extra line thirteen\n"
                + "extra line fourteen\n"
                + "extra line fifteen\n"
                + "\n\n";

        for (int charLeft = 10000; 0 <= charLeft; ) {
            String line = Util.getRandomString() + "\n";
            charLeft -= line.length();
            content += line;
        }

        final Envelope envelope = sendWith(authClient)
                .viaProd()
                .subj(SUBJ_PREFIX + " English text/plain very large message")
                .text(content)
                .send().waitDeliver().getEnvelope().orElse(null);

        assertNotNull("Отправленное письмо должно быть найдено", envelope);

        final Text resp = apiMbody().text()
                .withUid(authClient.account().uid())
                .withMid(envelope.getMid())
                .get(shouldBe(ok200())).peek().as(Text.class);

        final int expectedPartCount = 1;
        final String expectedMode = "firstline";
        final String expectedLang = null;
        final String expectedContent = envelope.getFirstline();

        checkResponse(resp, expectedPartCount, expectedMode, expectedLang, expectedContent);
    }

    private void checkResponse(Text resp,
                               int expectedPartCount,
                               String expectedMode,
                               String expectedLang,
                               String expectedContent) throws Exception {

        assertThat("Ответ должен содержать ровно заданное число текстовых частей",
                resp.getTextParts().size(), equalTo(expectedPartCount));

        final TextPart part = resp.getTextParts().get(0);

        assertThat("Способ получения текста письма должен быть выбран правильно", part.getMode(), equalTo(expectedMode));
        assertThat("Язык письма должен быть распознан", part.getLang(), equalTo(expectedLang));
        assertThat(part.getContent(), equalToIgnoringWhiteSpace(expectedContent));
    }
}
