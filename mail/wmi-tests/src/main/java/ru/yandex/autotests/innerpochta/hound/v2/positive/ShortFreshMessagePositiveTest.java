package ru.yandex.autotests.innerpochta.hound.v2.positive;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.seleniumhq.jetty7.http.HttpStatus;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.beans.hound.Message;
import ru.yandex.autotests.innerpochta.beans.hound.V2ShortMessageResponse;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark.StatusParam;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static java.util.function.Function.identity;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiHoundV2;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeMessageForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка v2/short_fresh_message")
@Description("Тесты на ручку v2/short_fresh_message")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2FreshMsgTest")
@RunWith(DataProviderRunner.class)
public class ShortFreshMessagePositiveTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Ручка v2/short_fresh_message с пустым relevant")
    @Description("Проверяем, что ручка возвращает пустой список при пустом relevant")
    public void shouldReceiveEmptyListForEmptyRelevant() throws Exception {
        makeMessageForTab(authClient, Tabs.Tab.NEWS);
        makeMessageForTab(authClient, Tabs.Tab.SOCIAL);

        assertThat("Должны были вернуть пустой список", getMessages(), empty());
    }

    @Test
    @Title("Ручка v2/short_fresh_message без непрочитанных")
    @Description("Проверяем, что ручка возвращает пустой список если нет непрочитанных писем")
    public void shouldReceiveEmptyListForNoUnread() throws Exception {
        List<String> mids = sendWith(authClient).viaProd().count(1).send().waitDeliver().getMids();
        Mops.mark(authClient, new MidsSource(mids), StatusParam.READ)
                .post(shouldBe(okSync()));

        assertThat("Должны были вернуть пустой список", getMessages(), empty());
    }

    @Test
    @Title("Ручка v2/short_fresh_message без свежих")
    @Description("Проверяем, что ручка возвращает пустой список если нет свежих писем")
    public void shouldReceiveEmptyListForNoFresh() {
        sendWith(authClient).viaProd().count(1).send().waitDeliver();
        apiHoundV2().resetTabUnvisited()
                .withUid(uid()).withTab(Tabs.Tab.RELEVANT.getName())
                .get(shouldBe(ok200()));

        assertThat("Должны были вернуть пустой список", getMessages(), empty());
    }

    @Test
    @Title("Ручка v2/short_fresh_message с новыми письмами")
    @Description("Проверяем, что ручка возвращает последнее непрочитанное письмо\n" +
            "Отправляем 3 письма, помечаем последнее прочитанным\n" +
            "Проверяем, что ручка вернет предпоследнее")
    public void shouldReceiveLastUnreadMessage() throws Exception {
        int totalCount = 3;
        List<Envelope> sent = sendWith(authClient).viaProd().count(totalCount).send().waitDeliver().getEnvelopes();
        assertThat("Ожидали получить " + String.valueOf(totalCount) + " писем",
                sent, hasSize(totalCount));

        Envelope newest = sent.get(0);
        Envelope expected = sent.get(1);
        Mops.mark(authClient, new MidsSource(newest.getMid()), StatusParam.READ)
                .post(shouldBe(okSync()));

        List<Message> msgs = getMessages();
        assertThat("Должны были вернуть одно письмо", msgs, hasSize(1));
        assertThat("Получили не то письмо, которое ожидали",
                msgs.get(0), allOf(
                        hasProperty("mid", equalTo(expected.getMid())),
                        hasProperty("subject", equalTo(expected.getSubject())),
                        hasProperty("receiveDate", equalTo(expected.getReceiveDate()))
                ));
    }

    private List<Message> getMessages() {
        return apiHoundV2().shortFreshMessage()
                .withUid(uid())
                .get(shouldBe(ok200()))
                .body().as(V2ShortMessageResponse.class)
                .getMessages();
    }
}
