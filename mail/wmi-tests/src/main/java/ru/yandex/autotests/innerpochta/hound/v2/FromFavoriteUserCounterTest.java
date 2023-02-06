package ru.yandex.autotests.innerpochta.hound.v2;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.seleniumhq.jetty7.http.HttpStatus;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.hound.V2FromFavoriteUserCounterResponse;
import ru.yandex.autotests.innerpochta.beans.labels.LabelSymbol;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.LabelsObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Labels;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.InvalidArguments.UNEXISTING_UID;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgument;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgumentWithCode;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.unknownUid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops.newLabelBySymbol;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка v2/from_favorite_user_counter")
@Description("Тесты на ручку v2/from_favorite_user_counter")
@Features(MyFeatures.HOUND)
@Stories(MyStories.WITH_ATTACHES_COUNTERS)
@Credentials(loginGroup = "HoundV2FromFavoriteUserCounterTest")
public class FromFavoriteUserCounterTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Проверка вызова без uid'а")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400WithoutUid() {
        apiHoundV2().fromFavoriteUserCounter()
                .get(shouldBe(invalidArgument(equalTo("uid parameter is required"))));
    }
    @Test
    @Title("Проверка вызова с неизвестным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForUnknownUid() {
        apiHoundV2().fromFavoriteUserCounter()
                .withUid(UNEXISTING_UID)
                .get(shouldBe(unknownUid()));
    }

    @Test
    @Title("Проверка вызова с некорректным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForIncorrectUid() {
        apiHoundV2().fromFavoriteUserCounter()
                .withUid("abacaba")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова с большим counter_limit'ом")
    public void shouldReceive400ForBigCounterLimit() {
        apiHoundV2().fromFavoriteUserCounter()
                .withUid(uid())
                .withCounterLimit("101")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова с большим mailbox_limit'ом")
    public void shouldReceive400ForBigMailboxLimit() {
        apiHoundV2().fromFavoriteUserCounter()
                .withUid(uid())
                .withMailboxLimit("5001")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова для слишком большого ящика писем")
    public void shouldReceive400ForBigMailbox() {
        sendWith(authClient).viaProd().count(3).send().waitDeliver().getMid();

        apiHoundV2().fromFavoriteUserCounter()
                .withUid(uid())
                .withMailboxLimit("2")
                .get(shouldBe(new ResponseSpecBuilder()
                        .expectStatusCode(HttpStatus.BAD_REQUEST_400).build()));
    }

    @Test
    @Title("Проверяем, что в ящике без писем счетчик равен 0." +
            "Отправляем письмо себе, помечаем меткой от избранных пользователей. " +
            "Количество нерпочитанных писем с меткой должно стать 1." +
            "Читаем это письмо, нерпочитанных от избранных пользователей должно стать 0")
    public void shouldReceiveCountNewMessagesWithLabel() throws Exception {
        V2FromFavoriteUserCounterResponse response = fromFavoriteUserCounter();
        assertThat("Ожидали, что нет новых писем от избранных пользователей в ящике без писем", response.getNewMessagesCount(), equalTo(0L));

        String mid = SendMessagesWithFromFavoriteUserLabel(1).get(0);

        response = fromFavoriteUserCounter();
        assertThat("Ожидалось увидеть одно новое письмо", response.getNewMessagesCount(), equalTo(1L));

        Mops.mark(authClient, new MidsSource(mid), ApiMark.StatusParam.READ)
                .post(shouldBe(okSync()));

        response = fromFavoriteUserCounter();
        assertThat("Ожидалось, что больше нет непрочитанных", response.getNewMessagesCount(), equalTo(0L));
    }

    @Test
    @Title("Проверка вызова для большого количества непрочитанных писем")
    public void shouldReceiveCounterLimit() throws Exception {
        List<String> mids = SendMessagesWithFromFavoriteUserLabel(3);

        V2FromFavoriteUserCounterResponse response = apiHoundV2().fromFavoriteUserCounter()
                .withUid(uid())
                .withCounterLimit("2")
                .get(shouldBe(ok200()))
                .body().as(V2FromFavoriteUserCounterResponse.class);

        assertThat("Должны увидеть counter_limit писем", response.getNewMessagesCount(), equalTo(2L));
    }

    private static List<String> SendMessagesWithFromFavoriteUserLabel(int count) throws Exception {
        List<String> mids = sendWith(authClient).viaProd().count(count).send().waitDeliver().getMids();
        String lid = lidBySymbol(LabelSymbol.FROM_FAVORITE_USER_LABEL, authClient);
        if (lid == null) {
            lid = newLabelBySymbol(authClient, LabelSymbol.FROM_FAVORITE_USER_LABEL.toString());
        }
        Mops.label(authClient, new MidsSource(mids), asList(lid))
                .post(shouldBe(okSync()));
        return mids;
    }

    private V2FromFavoriteUserCounterResponse fromFavoriteUserCounter() {
        return apiHoundV2().fromFavoriteUserCounter()
                .withUid(uid())
                .get(shouldBe(ok200()))
                .body().as(V2FromFavoriteUserCounterResponse.class);
    }

    static String lidBySymbol(LabelSymbol symbol, HttpClientManagerRule rule) {
        return api(Labels.class)
                .params(LabelsObj.empty().setUid(rule.account().uid()))
                .setHost(props().houndUri()).get().via(rule).lidBySymbol(symbol);
    }
}
