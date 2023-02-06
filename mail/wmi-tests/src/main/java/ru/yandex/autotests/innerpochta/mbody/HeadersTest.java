package ru.yandex.autotests.innerpochta.mbody;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.yandex.autotests.innerpochta.beans.mbody.Headers;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.*;
import org.json.JSONObject;
import static org.hamcrest.Matchers.*;
import static java.util.function.Function.identity;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.mbody.MbodyResponses.*;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Features(MyFeatures.MBODY)
@Stories(MyStories.MBODY)
@Issue("MAILDEV-852")
@Credentials(loginGroup = "HeadersTest")
public class HeadersTest extends MbodyBaseTest {
    public final String FAILED_TO_PARSE_PARAMS_ERROR = "failed to parse params";
    public final String MSG_SUBJECT = "MbodyHeadersHandlerTest";

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    public List<String> SendMsgsAndGetMids(Integer mids_count) throws Exception {
        return sendWith(authClient)
                .viaProd()
                .count(mids_count)
                .subj(MSG_SUBJECT)
                .send()
                .waitDeliver()
                .getMids();
    }

    @Test
    @Title("Проверяем, что без обязательных аргументов ручка выдает 400")
    public void testApiHeadersWithoutRequiredArgs() {
        apiMbody().headers()
                .get(shouldBe(missingParam400(FAILED_TO_PARSE_PARAMS_ERROR)));

        apiMbody().headers()
                .withUid("121")
                .get(shouldBe(missingParam400(FAILED_TO_PARSE_PARAMS_ERROR)));

        apiMbody().headers()
                .withMid("2238238232")
                .withMid("2392922129")
                .withMid("232372727272")
                .get(shouldBe(missingParam400(FAILED_TO_PARSE_PARAMS_ERROR)));
    }

    @Test
    @Title("Проверяем что ручка возращает результат для всех мидов и содержимое хедеров")
    public void testApiHeaders() throws Exception {
        List<String> mids = SendMsgsAndGetMids(2);
        String resp = apiMbody().headers()
                .withMid(mids.get(0))
                .withMid(mids.get(1))
                .withUid(uid())
                .get(identity()).peek().asString();

        String login = authClient.acc().getLogin();
        Gson gson = new GsonBuilder().create();
        JSONObject respJson = new JSONObject(resp);

        assertThat("Проверяем наличие mid в полученном ответе", respJson.has(mids.get(0)));
        assertThat("Проверяем наличие mid в полученном ответе", respJson.has(mids.get(1)));

        assertThat("Проверяем что нет лишних ответов", respJson.length(), equalTo(mids.size()));

        Headers firstMessageHeaders = gson.fromJson(respJson.getJSONObject(mids.get(0)).toString(), Headers.class);
        Headers secondMessageHeaders = gson.fromJson(respJson.getJSONObject(mids.get(1)).toString(), Headers.class);

        assertThat("Проверяем хедер subject",
                firstMessageHeaders.getSubject().get(0),
                equalTo(MSG_SUBJECT));
        assertThat("Проверяем хедер subject",
                secondMessageHeaders.getSubject().get(0),
                equalTo(MSG_SUBJECT));

        assertThat("Проверяем хедер from",
                firstMessageHeaders.getFrom().get(0),
                containsString(login));
        assertThat("Проверяем хедер from",
                secondMessageHeaders.getFrom().get(0),
                containsString(login));

        assertThat("Проверяем что хедер message-id не пустой",
                not(firstMessageHeaders.getMessageId().isEmpty()));
        assertThat("Проверяем что хедер message-id не пустой",
                not(secondMessageHeaders.getMessageId().isEmpty()));

        assertThat("Проверяем что хедер received не пустой",
                not(firstMessageHeaders.getReceived().isEmpty()));
        assertThat("Проверяем что хедер received не пустой",
                not(secondMessageHeaders.getReceived().isEmpty()));

        assertThat("Проверяем хедер to",
                firstMessageHeaders.getTo().get(0),
                containsString(login));
        assertThat("Проверяем хедер to",
                secondMessageHeaders.getTo().get(0),
                containsString(login));
    }
}
