package ru.yandex.autotests.innerpochta.hound.v2;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.seleniumhq.jetty7.http.HttpStatus;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.hound.V2WithAttachesCountersResponse;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;

import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;

import static java.util.function.Function.identity;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.InvalidArguments.UNEXISTING_UID;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgument;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgumentWithCode;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.unknownUid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка v2/with_attaches_counters")
@Description("Тесты на ручку v2/with_attaches_counters")
@Features(MyFeatures.HOUND)
@Stories(MyStories.WITH_ATTACHES_COUNTERS)
@Credentials(loginGroup = "HoundV2WithAttachesCountersTest")
public class WithAttachesCountersTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Проверка вызова без uid'а")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400WithoutUid() {
        apiHoundV2().withAttachesCounters()
                .get(shouldBe(invalidArgument(equalTo("uid parameter is required"))));
    }

    @Test
    @Title("Проверка вызова с неизвестным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForUnknownUid() {
        apiHoundV2().withAttachesCounters()
                .withUid(UNEXISTING_UID)
                .get(shouldBe(unknownUid()));
    }

    @Test
    @Title("Проверка вызова с некорректным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForIncorrectUid() {
        apiHoundV2().withAttachesCounters()
                .withUid("abacaba")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверяем, что в ящике без писем счетчики равны 0." +
            "Отправляем письмо с аттачем себе. Должны появится 2 письма c аттачами," +
            "одно из них непрочитанное с аттачем." +
            "Читаем это письмо, нерпочитанных с аттачем должно стать 0")
    public void shouldReceiveCountMessagesWithAttaches() throws Exception {
        V2WithAttachesCountersResponse response = getAttachesCounters();
        assertThat("Ожидали, что нет новых писем с аттачами в ящике без писем", response.getNewMessagesCount(), equalTo(0L));
        assertThat("Ожидали, что нет писем с аттачами в ящике без писем", response.getMessagesCount(), equalTo(0L));

        File attach = AttachUtils.genFile(1);
        attach.deleteOnExit();
        String mid = sendWith(authClient).viaProd().addAttaches(attach).send().waitDeliver().getMid();

        response = getAttachesCounters();
        assertThat("Ожидали, что появится 1 непрочитанное письмо с аттачем", response.getNewMessagesCount(), equalTo(1L));
        assertThat("Ожидали, что появятся 2 письма с аттачами", response.getMessagesCount(), equalTo(2L));

        Mops.mark(authClient, new MidsSource(mid), ApiMark.StatusParam.READ)
                .post(shouldBe(okSync()));

        response = getAttachesCounters();
        assertThat("Ожидали, что станет 0 непрочитанных писем с аттачами", response.getNewMessagesCount(), equalTo(0L));
        assertThat("Ожидали, что число всех писем с аттачами не изменится", response.getMessagesCount(), equalTo(2L));
    }

    private V2WithAttachesCountersResponse getAttachesCounters() {
        return apiHoundV2().withAttachesCounters()
                .withUid(uid())
                .get(shouldBe(ok200()))
                .body().as(V2WithAttachesCountersResponse.class);
    }
}
