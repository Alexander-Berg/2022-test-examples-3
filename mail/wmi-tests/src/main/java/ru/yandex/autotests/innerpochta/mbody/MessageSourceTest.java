package ru.yandex.autotests.innerpochta.mbody;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.*;

import java.util.Base64;

import static java.util.function.Function.identity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.mbody.MbodyResponses.missingParam400;
import static ru.yandex.autotests.innerpochta.wmi.core.mbody.MbodyResponses.badRequest400;
import static ru.yandex.autotests.innerpochta.wmi.core.mbody.MbodyResponses.error500WithString;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Features(MyFeatures.MBODY)
@Stories(MyStories.MBODY)
@Issue("MAILDEV-1059")
@Credentials(loginGroup = "MbodyMessageSourceTest")
public class MessageSourceTest extends MbodyBaseTest {
    public final String FAILED_TO_PARSE_PARAMS_ERROR = "failed to parse params";
    public final String MESSAGE_NOT_FOUND_ERROR_PATTERN = "exception: error in forming message: getMessageAccessParams error: unknown mid=%s";
    public static final String NOT_EXIST_MID = "6666666666666666666";
    public static final String INVALID_MID = "qwdUY7y02&Y*)";
    public static final String UTF_STRING = "Письмо с русскими символами";

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Проверяем, что без обязательных аргументов ручка выдает 400")
    public void testApiMessageSourceWithoutRequiredArgs() {
        apiMbody().messageSource()
                .get(shouldBe(missingParam400(FAILED_TO_PARSE_PARAMS_ERROR)));

        apiMbody().messageSource()
                .withUid("121")
                .get(shouldBe(missingParam400(FAILED_TO_PARSE_PARAMS_ERROR)));

        apiMbody().messageSource()
                .withMid("2238238232")
                .get(shouldBe(missingParam400(FAILED_TO_PARSE_PARAMS_ERROR)));
    }

    @Test
    @Title("Проверяем, что для несуществующего письма ручка выдает 400")
    public void testApiMessageSourceWithNotExistMid() {
        String midError = String.format(MESSAGE_NOT_FOUND_ERROR_PATTERN, NOT_EXIST_MID);
        apiMbody().messageSource()
                .withUid(authClient.account().uid())
                .withMid(NOT_EXIST_MID)
                .get(shouldBe(badRequest400(midError)));
    }

    @Test
    @Title("Проверяем, что для невалидного mid выдает 500")
    public void testApiMessageSourceWithInvalidMid() {
        apiMbody().messageSource()
                .withUid(authClient.account().uid())
                .withMid(INVALID_MID)
                .get(shouldBe(error500WithString(INVALID_MID)));
    }

    @Test
    @Title("Проверяем наличие Received, From, Subject в полученном исходнике письма")
    public void testApiMessageSource() throws Exception {
        String mid = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String resp = apiMbody().messageSource()
                .withUid(uid())
                .withMid(mid)
                .get(identity()).peek().asString();

        Gson gson = new GsonBuilder().create();
        JSONObject respJson = new JSONObject(resp);

        assertThat("Проверяем наличие поля text в полученном ответе", respJson.has("text"));

        String messageSource = decodeBase64(respJson.getString("text"));

        assertThat("С исходником что-то не так", messageSource, allOf(
                containsString("Received:"),
                containsString("From:"),
                containsString("Subject:")));
    }

    @Test
    @Title("Проверка для русского письма")
    public void testApiMessageSourceWithRus() throws Exception {
        String mid = sendWith(authClient).viaProd().text(UTF_STRING).send().waitDeliver().getMid();
        String resp = apiMbody().messageSource()
                .withUid(uid())
                .withMid(mid)
                .get(identity()).peek().asString();

        Gson gson = new GsonBuilder().create();
        JSONObject respJson = new JSONObject(resp);

        assertThat("Проверяем наличие поля text в полученном ответе", respJson.has("text"));

        String messageSource = decodeBase64(respJson.getString("text"));

        assertThat("Исходник должен содержать русские символы", messageSource, containsString(UTF_STRING));
    }

    @Test
    @Title("Проверяем наличие поля encoding в ответе")
    public void testApiMessageSourceReturnsEncoding() throws Exception {
        String mid = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String resp = apiMbody().messageSource()
                .withUid(uid())
                .withMid(mid)
                .get(identity()).peek().asString();

        Gson gson = new GsonBuilder().create();
        JSONObject respJson = new JSONObject(resp);

        assertThat("Проверяем наличие поля encoding в полученном ответе", respJson.has("encoding"));

        String encoding = respJson.getString("encoding");

        assertThat("Неверное значение encoding", encoding, equalTo("US-ASCII"));
    }

    @Test
    @Title("Проверяем наличие поля encoding в ответе для русского письма")
    public void testApiMessageSourceWithRusReturnsEncoding() throws Exception {
        String mid = sendWith(authClient).viaProd().text(UTF_STRING).send().waitDeliver().getMid();
        String resp = apiMbody().messageSource()
                .withUid(uid())
                .withMid(mid)
                .get(identity()).peek().asString();

        Gson gson = new GsonBuilder().create();
        JSONObject respJson = new JSONObject(resp);

        assertThat("Проверяем наличие поля encoding в полученном ответе", respJson.has("encoding"));

        String encoding = respJson.getString("encoding");

        assertThat("Неверное значение encoding", encoding, equalTo("utf-8"));
    }

    public static String decodeBase64(String source) {
        byte[] decodedSource = Base64.getDecoder().decode(source);
        return new String(decodedSource);
    }
}