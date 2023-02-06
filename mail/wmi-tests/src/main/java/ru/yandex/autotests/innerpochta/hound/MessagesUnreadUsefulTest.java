package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesUnreadUseful;
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
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesUnreadUsefulObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка /messages_unread_useful")
@Features(MyFeatures.HOUND)
@Stories(MyStories.OTHER)
@Credentials(loginGroup = "MessagesUnreadUsefulTest")
public class MessagesUnreadUsefulTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).inbox().outbox().allfolders();

    @Test
    @Title("Должны не возвращать писем сверх лимита")
    @Description("Посылаем два письма. Дёргаем ручку с аргументом count=1. Ожидаем в ответе одно письмо")
    public void shouldFollowCount() throws Exception {
        String mid1 = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String mid2 = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        List<Envelope> envelopes = api(MessagesUnreadUseful.class)
                .setHost(props().houndUri())
                .params(empty().setUid(uid())
                .setCount("1"))
                .get()
                .via(authClient).withDebugPrint()
                .resp().getEnvelopes();

        assertThat("Ожидали только одно письмо", envelopes.size(), equalTo(1));
        assertThat("Ожидали другое письмо", envelopes.stream()
                .allMatch(e -> e.getMid().equals(mid1) || e.getMid().equals(mid2)));
    }

    @Test
    @Title("Должны возвращать письма только из инбокса и пользовательских папок")
    @Description("Посылаем два письма. Складываем одно письмо в корзину. Дёргаем ручку. Ожидаем в ответе одно письмо")
    public void shouldSeeMessagesFromUsefulFoldersOnly() throws Exception {
        String mid1 = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String mid2 = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        Mops.remove(authClient, new MidsSource(mid2)).post(shouldBe(okSync()));

        List<Envelope> envelopes = api(MessagesUnreadUseful.class)
                .setHost(props().houndUri())
                .params(empty().setUid(uid())
                .setCount("30"))
                .get()
                .via(authClient).withDebugPrint()
                .resp().getEnvelopes();

        assertThat("Ожидали только одно письмо", envelopes.size(), equalTo(1));
        assertThat("Ожидали другое письмо", envelopes.get(0).getMid(), equalTo(mid1));
    }
}