package ru.yandex.autotests.innerpochta.wmi.byTimestamp;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.ThreadsViewObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ThreadsView;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.data.TimestampData.timestampDataForThreads;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 18.03.14
 * Time: 19:33
 * <p/>
 * [DARIA-32622]
 * [DARIA-34367]
 * [DARIA-41540]
 * Любые операции над письмами(удаление/изменения)
 * ЗАПРЕЩЕНЫ!!!
 */
@Aqua.Test
@Title("Threads_by_timestamp_range, threads_view с параметрами since и till")
@Description("Проверяем только соответствие количества писем")
@Features(MyFeatures.WMI)
@Stories({MyStories.BY_TIMESTAMP, MyStories.THREAD_LIST})
@RunWith(Parameterized.class)
@Credentials(loginGroup = "ThreadsParamsSince")
@Issue("MAILPG-379")
public class ThreadsByTimestampRange extends BaseTest {
    private String since;
    private String till;
    private int expected;

    private DefaultHttpClient hcWithOAuth = authClient.oAuth();

    public ThreadsByTimestampRange(Integer since, Integer till, int expected) {
        if (since == null) {
            this.since = "";
        } else {
            this.since = since.toString();
        }

        if (till == null) {
            this.till = "";
        } else {
            this.till = till.toString();
        }
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "since-{0}-till-{1}-exp-{2}")
    public static Collection<Object[]> data() {
        return timestampDataForThreads();
    }

    @Test
    public void threadsViewWithSinceAndTill() throws IOException {
        ThreadsViewObj obj = new ThreadsViewObj()
                .setSince(since)
                .setTill(till)
                .setFirst(0)
                .setLast(50);
        ThreadsView oper = jsx(ThreadsView.class).params(obj).post().via(hc).withDebugPrint();
        assertThat(String.format("Количество писем в диапазоне " +
                        "since=<%s> till=<%s> не совпадает с ожидаемым [DARIA-33568]", since, till),
                oper.post().via(hc).countMessages(),
                equalTo(expected));
    }
}
