package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesUnread;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesUnreadObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка /messages_unread")
@Features(MyFeatures.HOUND)
@Stories(MyStories.OTHER)
@Credentials(loginGroup = "MessagesUnreadTest")
public class MessagesUnreadTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = new CleanMessagesMopsRule(authClient).allfolders();

    @Test
    @Title("Должны возвращать только непрочитанные письма")
    @Description("Посылаем три письма. Одно читаем, второе удаляем.Ожидаем в ответе только третье письмо")
    public void shouldSeeOnlyUnreadMessages() throws Exception {
        String midForMarkRead = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String expectedMid = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String midForTrash = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        Mops.mark(authClient, new MidsSource(midForMarkRead), ApiMark.StatusParam.READ)
                .post(shouldBe(okSync()));

        Mops.remove(authClient, new MidsSource(midForTrash)).post(shouldBe(okSync()));

        List<Envelope> envelopes = api(MessagesUnread.class)
                .setHost(props().houndUri())
                .params(empty().setUid(uid())
                .setCount("30")
                .setFirst("0"))
                .get()
                .via(authClient)
                .withDebugPrint()
                .resp().getEnvelopes();

        assertThat("Ожидали только одно письмо", envelopes.size(), equalTo(1));
        assertThat("Ожидали другое письмо", envelopes.get(0).getMid(), equalTo(expectedMid));
    }
}