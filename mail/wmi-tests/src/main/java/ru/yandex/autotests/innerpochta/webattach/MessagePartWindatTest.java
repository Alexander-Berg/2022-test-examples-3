package ru.yandex.autotests.innerpochta.webattach;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.*;

import static org.junit.Assert.*;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.weattach.MessagePartRealObj.emptyObj;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FoldersObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder;
import ru.yandex.autotests.innerpochta.wmi.core.oper.webattach.MessagePartReal;
import ru.yandex.qatools.allure.annotations.*;

import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Folders.folders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Aqua.Test
@Title("Проверяем message_part на знание о файлах winmail.dat")
@Description("Проверяем, можно ли получить 2 аттача внутри виндата с хидом 1.2\n" +
        "https://sandbox.yandex-team.ru/resource/1067331195/view")
@Features(MyFeatures.WEBATTACH)
@Stories(MyStories.ATTACH)
@Credentials(loginGroup = "OutlookUser")
public class MessagePartWindatTest extends BaseWebattachTest {
    @Test
    @Title("Проверяем, можно ли получить 2 аттача внутри виндата с хидом 1.2")
    public void testGetWindatAttaches() {

        String uid = authClient.account().uid();
        String fid = folders(FoldersObj.empty().setUid(uid)).get().via(authClient).fid("windat");

        List<Envelope> envelopes = api(MessagesByFolder.class).params(MessagesByFolderObj.empty()
            .setUid(uid)
            .setFirst("0")
            .setCount("50")
            .setFid(fid))
            .setHost(props().houndUri()).get().via(authClient).resp().getEnvelopes();

        assertTrue("Папка windat должна быть непустой", !envelopes.isEmpty());
        assertEquals("Тема письма должна соответствовать 'windat bro'",
                "windat bro", envelopes.get(0).getSubject());

        String mid = envelopes.get(0).getMid();

        List<String> hids = Arrays.asList("1.1.1", "1.1.2");
        List<String> sids = new ArrayList<>();
        for (String hid : hids) {
            sids.add(getAttachSid(mid, Arrays.asList(hid)).get(0));
        }

        for (String sid : sids) {
            api(MessagePartReal.class).setHost(props().webattachHost())
                    .params(emptyObj().setSid(sid)).get().via(authClient)
                    .statusCodeShouldBe(HttpStatus.SC_OK);
        }
    }
}
