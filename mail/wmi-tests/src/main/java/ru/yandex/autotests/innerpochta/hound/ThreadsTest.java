package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.yplatform.Thread;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Threads;
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
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.ThreadsObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка /threads")
@Description("Проверяем список тредов")
@Credentials(loginGroup = "HoundThreadsTest")
@Features(MyFeatures.HOUND)
@Stories(MyStories.THREAD_LIST)
public class ThreadsTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Проверяем список тредов из посланных себе писем")
    @Description("Посылаем два письма. Дёргаем ручку. Ожидаем в ответе две треда.")
    public void shouldSeeNewThreads() throws Exception {
        String tid1 = sendWith(authClient).viaProd().send().waitDeliver().getTid();
        String tid2 = sendWith(authClient).viaProd().send().waitDeliver().getTid();

        Threads response = api(Threads.class)
                .setHost(props().houndUri())
                .params(empty().setUid(uid()).setTids(tid1, tid2))
                .get()
                .via(authClient)
                .withDebugPrint();

        List<Thread> threads = response.threads().getThreads();
        assertThat("Ожидали ровно два треда", threads.size(), equalTo(2));

        assertThat("Ожидали другие треды", threads.stream()
                .map(Thread::getThreadId)
                .collect(Collectors.toList())
                .containsAll(Arrays.asList(tid1, tid2)));
    }
}
