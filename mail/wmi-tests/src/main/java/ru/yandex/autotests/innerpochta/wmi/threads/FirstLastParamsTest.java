package ru.yandex.autotests.innerpochta.wmi.threads;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.ThreadsViewObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ThreadsView;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 11.09.12
 * Time: 13:23
 * <p/>
 * Unmodify
 */
@Aqua.Test
@Title("Тестирование мейлбокс листа. Общие проверки")
@Description("Проверяем специфичные кейсы, вроде first и last [WMI-477]")
@Credentials(loginGroup = "Zoo")
@Features(MyFeatures.WMI)
@Stories(MyStories.THREADS)
@Issue("WMI-477")
@RunWith(Parameterized.class)
@Ignore("MAILDEV-1107")
public class FirstLastParamsTest extends BaseTest {
    private int first;
    private int last;
    private int awaiting;

    public FirstLastParamsTest(int first, int last, int awaiting) {
        this.first = first;
        this.last = last;
        this.awaiting = awaiting;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        ArrayList<Object[]> data = new ArrayList<Object[]>();

        data.add(new Object[]{1, 0, 0});
        data.add(new Object[]{0, 0, 0});
        data.add(new Object[]{-1, 0, 0});
        data.add(new Object[]{0, -1, 0});
        data.add(new Object[]{0, 1, 1});
        data.add(new Object[]{0, 10, 10});


        return data;
    }

    @Before
    public void prepare() throws Exception {
        logger.warn("Проверяем, что количество писем в выдаче мейлбокс листа в различных диапазонах first & last" +
                " соответствует ожидаемому [WMI-477]");
    }

    @Test
    @Description("Дергаем mailbox_list с различными параметрами first и last\n" +
            "Ожидаемый результат: количество должно совпадать с ожидаемым")
    public void mailboxListCounterChecks() throws IOException {
        MailBoxListObj obj = MailBoxListObj.empty();
        MailBoxList oper = jsx(MailBoxList.class).params(obj);

        assertResponseMBLHasRightCountMsgs(obj, oper, first, last, awaiting);
    }


    @Test
    @Description("Дергаем thread_view с различными параметрами first и last.\n" +
            "Ожидаемый результат: количество должно совпадать с ожидаемым")
    public void threadsViewCounterChecks() throws IOException {
        ThreadsViewObj obj = new ThreadsViewObj();
        ThreadsView oper = jsx(ThreadsView.class).params(obj);

        assertResponseTVHasRightCountMsgs(obj, oper, first, last, awaiting);
    }


    private void assertResponseMBLHasRightCountMsgs(MailBoxListObj obj,
                                                    MailBoxList oper,
                                                    int first, int last, int expected) throws IOException {
        String description = "При first=%0, last=%1, ожидалось: %2";
        obj.setFirst(first).setLast(last);
        assertThat(oper.params(obj).post().via(hc).countMessagesInFolder(),
                describedAs(description, equalTo(expected), first, last, expected));
    }

    private void assertResponseTVHasRightCountMsgs(ThreadsViewObj obj,
                                                   ThreadsView oper,
                                                   int first, int last, int expected) throws IOException {
        String description = "При first=%0, last=%1, ожидалось: %2";
        obj.setFirst(first).setLast(last);
        assertThat(oper.params(obj).post().via(hc).countMessages(),
                describedAs(description, equalTo(expected), first, last, expected));
    }

}
