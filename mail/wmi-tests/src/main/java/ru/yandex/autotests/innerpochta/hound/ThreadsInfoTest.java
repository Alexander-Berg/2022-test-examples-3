package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.yplatform.Envelope;
import ru.yandex.autotests.innerpochta.beans.yplatform.ThreadLabel;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.ThreadsInfo;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.ThreadsInfoObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка /threads_info")
@Description("Проверяем список тредов")
@Credentials(loginGroup = "ThreadsInfoTest")
@Features(MyFeatures.HOUND)
@Stories(MyStories.THREAD_LIST)
public class ThreadsInfoTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Проверяем список тредов из посланных себе писем")
    @Description("Посылаем два письма. Дёргаем ручку. Ожидаем в ответе две треда.")
    public void shouldSeeNewThreads() throws Exception {
        String tid1 = sendWith(authClient).viaProd().send().waitDeliver().getTid();
        String tid2 = sendWith(authClient).viaProd().send().waitDeliver().getTid();

        ThreadsInfo threads = api(ThreadsInfo.class)
                .setHost(props().houndUri())
                .params(empty().setUid(uid())
                .setTid(tid1, tid2))
                .get()
                .via(authClient)
                .withDebugPrint();

        List<ThreadLabel> threadLabels = threads.resp().getThreadLabels();
        assertThat("Ожидали ровно два треда", threadLabels.size(), equalTo(2));

        List<Envelope> envelopes = threads.resp().getEnvelopes();
        assertThat("Ожидали другие треды", envelopes.stream()
                .map(Envelope::getThreadId)
                .collect(Collectors.toList())
                .containsAll(Arrays.asList(tid1, tid2)));
    }
}
