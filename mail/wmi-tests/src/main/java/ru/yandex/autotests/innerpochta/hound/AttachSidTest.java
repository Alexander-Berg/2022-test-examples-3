package ru.yandex.autotests.innerpochta.hound;


import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.hound.AttachSidRequest;
import ru.yandex.autotests.innerpochta.beans.hound.Download;
import ru.yandex.autotests.innerpochta.beans.hound.HoundResponse;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.weattach.MessagePartRealObj.emptyObj;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Проверка выдачи ручки attach_sid")
@Credentials(loginGroup = "Attachlink")
@Features(MyFeatures.HOUND)
@Stories(MyStories.ATTACH)
@Issue("MAILDEV-830")
public class AttachSidTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Issue("MAILDEV-830")
    @Title("Проверяем, что генерим валидный сид")
    @Description("Проверяем, что генерим валидный сид, по которому могут быть загружены аттачи")
    public void attachesDownloadOk() throws Exception {
        File attach = AttachUtils.genFile(1);
        File attach2 = AttachUtils.genFile(1);
        attach.deleteOnExit();
        attach2.deleteOnExit();

        String mid = sendWith(authClient).viaProd().addAttaches(attach, attach2).send().waitDeliver().getMid();

        ArrayList<Download> downloads = new ArrayList<Download>() {{
            add(new Download().withMid(mid).withHids(
                    new ArrayList<String>() {{
                        add("1.1");
                    }}
            ));
            add(new Download().withMid(mid).withHids(
                    new ArrayList<String>() {{
                        add("1.2");
                    }}
            ));
            // zip архив
            add(new Download().withMid(mid).withHids(
                    new ArrayList<String>() {{
                        add("1.1");
                        add("1.2");
                    }}
            ));
        }};

        AttachSidRequest sr = new AttachSidRequest().withUid(uid()).withDownloads(downloads);

        String body = new Gson().toJson(Arrays.asList(sr));

        List<String> sids = apiHound()
                .attachSid()
                .withUid(authClient.account().uid())
                .withReq((req) -> req.setContentType("application/json")
                .setBody(body)).post(Function.identity()).as(HoundResponse.class)
                .getResult().get(0).getSids();
        for (String sid : sids) {
            api(MessagePartReal.class).setHost(props().webattachHost())
                    .params(emptyObj().setSid(sid)).get().via(authClient.notAuthHC())
                    .statusCodeShouldBe(HttpStatus.SC_OK);
        }
    }
}
