package ru.yandex.autotests.innerpochta.hound.v2.positive;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.webattach.MessagePartReal;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.weattach.MessagePartRealObj.emptyObj;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка v2/attach_sid")
@Description("Тесты на ручку v2/attach_sid")
@Features(MyFeatures.HOUND)
@Stories(MyStories.ATTACH)
@Credentials(loginGroup = "HoundV2AttachSidTest")
@Issue("MAILPG-2764")
public class AttachSidPositiveTest extends BaseHoundTest {
    static String MULTIPLE_PART_KEY = "all";

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Проверяем, что генерим валидный сид")
    @Description("Проверяем, что генерим валидный сид, по которому могут быть загружены аттачи")
    public void singleAttachDownloadOk() throws Exception {
        File attach = AttachUtils.genFile(1);
        attach.deleteOnExit();

        String mid = sendWith(authClient).viaProd().addAttaches(attach).send().waitDeliver().getMid();

        Map<String, String> response = apiHoundV2().attachSid()
                .withUid(authClient.account().uid())
                .withMid(mid)
                .withHids("1.1")
                .get(shouldBe(ok200()))
                .jsonPath().getMap("", String.class, String.class);

        String sid = response.get("1.1");

        assertThat("Поле <all> пустое, потому что всего 1 аттач",
                response.containsKey(MULTIPLE_PART_KEY), is(false));

        api(MessagePartReal.class)
                .setHost(props().webattachHost())
                .params(emptyObj().setSid(sid))
                .get()
                .via(authClient.notAuthHC())
                .statusCodeShouldBe(HttpStatus.SC_OK);
    }

    @Test
    @Title("Проверяем, что генерим валидные сиды")
    @Description("Проверяем, что генерим валидные сиды, по которым могут быть загружены аттачи")
    public void attachesDownloadOk() throws Exception {
        List<File> files = Arrays.asList(
                AttachUtils.genFile(1), AttachUtils.genFile(1),
                AttachUtils.genFile(1), AttachUtils.genFile(1)
        );
        for (File f : files) {
            f.deleteOnExit();
        }

        String mid = sendWith(authClient).viaProd().addAttaches(files).send().waitDeliver().getMid();
        Map<String, String> response = apiHoundV2().attachSid()
                .withUid(authClient.account().uid())
                .withMid(mid)
                .withHids("1.1")
                .withHids("1.2")
                .withHids("1.3")
                .withHids("1.4")
                .get(shouldBe(ok200()))
                .jsonPath().getMap("", String.class, String.class);

        assertThat("Поле <all> не пустое, потому что больше 1 аттача",
                response.get(MULTIPLE_PART_KEY), not(isEmptyOrNullString()));

        for (String sid : response.values()) {
            api(MessagePartReal.class)
                    .setHost(props().webattachHost())
                    .params(emptyObj().setSid(sid))
                    .get()
                    .via(authClient.notAuthHC())
                    .statusCodeShouldBe(HttpStatus.SC_OK);
        }
    }

    @Test
    @Title("Многократное добавление одинаковых хидов")
    @Description("Проверяем, что при многократном добавлении одинаковых хидов в ответе они появляются только один раз")
    public void multipleIdenticalHids() throws Exception {

        String firstHid = "1.1";
        String secondHid = "1.2";
        Map<String, String> response = apiHoundV2().attachSid()
                .withUid(authClient.account().uid())
                .withMid("any_mid")
                .withHids(firstHid)
                .withHids(firstHid)
                .withHids(firstHid)
                .withHids(secondHid)
                .withHids(secondHid)
                .withHids(firstHid)
                .withHids(secondHid)
                .withHids(firstHid)
                .withHids(firstHid)
                .get(shouldBe(ok200()))
                .jsonPath().getMap("", String.class, String.class);

        assertThat("Поле <all> не пустое, потому что больше 1 аттача",
                response.get(MULTIPLE_PART_KEY), not(isEmptyOrNullString()));
        assertThat("Количество сидов верное", response.size(), is(2 + 1/*all*/));

        assertThat("Сид для 1 аттача корректный", response.get(firstHid), not(isEmptyOrNullString()));
        assertThat("Сид для 2 аттача корректный", response.get(secondHid), not(isEmptyOrNullString()));
    }
}
