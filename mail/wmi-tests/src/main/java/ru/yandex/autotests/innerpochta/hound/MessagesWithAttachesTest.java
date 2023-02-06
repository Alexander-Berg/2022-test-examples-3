package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesWithAttaches;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesWithAttachesObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка /messages_with_attaches")
@Features(MyFeatures.HOUND)
@Stories(MyStories.OTHER)
@Credentials(loginGroup = "MessagesWithAttachesTest")
public class MessagesWithAttachesTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).inbox().outbox().allfolders();

    @Test
    @Title("Должны возвращать письмо с вложением")
    @Description("Посылаем два письма, одно с вложением. Дёргаем ручку. Ожидаем в ответе одно письмо")
    public void shouldSeeMessagesWithAttachesOnly() throws Exception {
        File attach = File.createTempFile(Util.getRandomString(), null);
        attach.deleteOnExit();
        sendWith(authClient).viaProd().addAttaches(attach).send().waitDeliver();
        sendWith(authClient).viaProd().send().waitDeliver();

        List<Envelope> envelopes = api(MessagesWithAttaches.class)
                .setHost(props().houndUri())
                .params(empty().setUid(uid())
                .setFirst("0")
                .setCount("30"))
                .get()
                .via(authClient)
                .withDebugPrint()
                .resp().getEnvelopes();

        assertThat("Ожидали письма с одним аттачем", envelopes.stream()
                .allMatch(e -> e.getAttachmentsCount() == 1));
    }
}