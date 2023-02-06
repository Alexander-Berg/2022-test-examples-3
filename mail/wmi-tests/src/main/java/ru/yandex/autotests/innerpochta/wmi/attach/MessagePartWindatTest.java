package ru.yandex.autotests.innerpochta.wmi.attach;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessagePartObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessagePart;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder;
import ru.yandex.autotests.innerpochta.wmi.core.oper.webattach.MessagePartReal;

import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.qatools.allure.annotations.*;

import java.util.List;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.weattach.MessagePartRealObj.emptyObj;

/**
 * Created by emilchess on 28/12/16.
 */

@Aqua.Test
@Title("Проверяем message_part на знание о файлах winmail.dat")
@Description("Проверяем, можно ли получить 2 аттача внутри виндата с хидом 1.2")
@Features(MyFeatures.WMI)
@Stories(MyStories.ATTACH)
@Issue("MAILDEV-503")
@Credentials(loginGroup = "OutlookUser")
@Ignore("MAILDEV-864")
public class MessagePartWindatTest extends BaseTest {
    @Test
    @Description("Проверяем, можно ли получить 2 аттача внутри виндата с хидом 1.2")
    public void testGetWindatAttaches() {
        List<Envelope> envelopes = api(MessagesByFolder.class).params(MessagesByFolderObj.empty()
                .setSuid(composeCheck.getSuid())
                .setMdb(composeCheck.getMDB())
                .setFirst("0")
                .setCount("50")
                .setFid("7"))
                .setHost(props().houndUri()).get().via(hc).resp().getEnvelopes();

        assert(!envelopes.isEmpty());
        String mid = envelopes.get(0).getMid();

        for (int i = 1; i < 3; i++) {
            String hid = "1.2." + Integer.toString(i);
            String sid = jsx(MessagePart.class)
                    .params(MessagePartObj.getEmptyObj().setIds(mid).setHid(hid))
                    .get().via(authClient.authHC()).arg();
            api(MessagePartReal.class).setHost(props().webattachHost())
                    .params(emptyObj().setSid(sid)).get().via(authClient.notAuthHC())
                    .statusCodeShouldBe(HttpStatus.SC_OK);
        }
    }
}
