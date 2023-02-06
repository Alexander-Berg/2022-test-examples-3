package ru.yandex.autotests.innerpochta.spaniel;

import org.apache.commons.httpclient.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.spaniel.SearchCreateResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.webattach.MessagePartReal;
import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.Map;

import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.spaniel.SearchMather.searchIsComplete;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.weattach.MessagePartRealObj.emptyObj;

@Aqua.Test
@Title("[Spaniel] Проксирование ручки /v2/attach_sid из хаунда")
@Credentials(loginGroup = "SpanielAttachSidTest")
@Features(MyFeatures.SPANIEL)
@Stories(MyStories.HIDDING_PLACE)
public class AttachSidProxyTest extends SpanielBaseTest {
    String makeSearch() {
        String searchId = prebuildSearchCreateRequest(getUid())
                .withText(pattern)
                .post(shouldBe(ok200()))
                .as(SearchCreateResponse.class)
                .getSearchId();

        MatcherAssert.assertThat(authClient, withWaitFor(searchIsComplete(orgId, getUid(), searchId)));

        return searchId;
    }

    @Test
    @Title("Проверяем, что генерим через проксируемый запрос валидный сид, по которому могут быть загружены аттачи")
    public void shouldDownloadSingleAttach() throws Exception {
        File attach = AttachUtils.genFile(1);
        attach.deleteOnExit();

        String mid = sendAndIndexWithAttach(authClient, pattern, attach);
        String searchId = makeSearch();

        Map<String, String> response = apiSpaniel().attachSid()
                .withAdminUid(getUid())
                .withOrgId(orgId)
                .withUid(getUid())
                .withSearchId(searchId)
                .withMid(mid)
                .withHids("1.1")
                .get(shouldBe(ok200()))
                .jsonPath().getMap("", String.class, String.class);

        String sid = response.get("1.1");

        api(MessagePartReal.class)
                .setHost(props().webattachHost())
                .params(emptyObj().setSid(sid))
                .get()
                .via(authClient.notAuthHC())
                .statusCodeShouldBe(HttpStatus.SC_OK);
    }

    @Test
    @Title("Запрос от юзера без прав администратора")
    @Description("Запрос без прав администратора должен завершаться ошибкой")
    public void shouldReceive400WithoutAdminPermissions() throws Exception {
        File attach = AttachUtils.genFile(1);
        attach.deleteOnExit();

        String mid = sendAndIndexWithAttach(authClient, pattern, attach);
        String searchId = makeSearch();

        apiSpaniel().attachSid()
                .withAdminUid(notAdminUid)
                .withOrgId(orgId)
                .withUid(getUid())
                .withSearchId(searchId)
                .withMid(mid)
                .withHids("1.1")
                .get(shouldBe(accessDenied400()));
    }
}
