package ru.yandex.autotests.innerpochta.wmi.threads;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.ThreadListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.ThreadsViewObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ThreadList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ThreadsView;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 11.09.12
 * Time: 13:23
 * <p/>
 * unmodify
 */
@Aqua.Test
@Title("Сравнение выдачи заголовков с границами и без")
@Description("Проверяем специфичные кейсы, вроде first и last [WMI-477]")
@Features(MyFeatures.WMI)
@Issue("WMI-477")
@Stories(MyStories.THREADS)
@Credentials(loginGroup = "Zoo")
public class FirstLastParamsResponseInDefault extends BaseTest {
    @Before
    public void prepare() throws Exception {
        logger.warn("Проверяем, что количество писем в выдаче тредов в различных диапазонах first & last" +
                " соответствует ожидаемому [WMI-477]");
    }

    @Test
    @Description("Проверяем, что все письма из выдачи с first и last содержатся в обычной выдаче для mailbox_list")
    public void mailboxListCounterChecks() throws IOException {
        MailBoxListObj obj = MailBoxListObj.empty().setFirst(0).setLast(29);
        MailBoxList oper = jsx(MailBoxList.class).params(obj);

        logger.warn("Проверка, что все письма из выдачи с first и last содержатся в обычной выдаче");
        assertThat("Не все письма из новой выдачи содержатся в обычной выдаче",
                oper.post().via(hc).getMidsOfMessagesInFolder(),
                everyItem(isIn(jsx(MailBoxList.class).params(MailBoxListObj.empty())
                        .post().via(hc).getMidsOfMessagesInFolder())));
    }


    @Test
    @Description("Проверяем, что все письма из выдачи с first и last содержатся в обычной выдаче для threads_view")
    public void threadsViewCounterChecks() throws IOException {
        ThreadsViewObj obj = new ThreadsViewObj().setFirst(0).setLast(30);
        ThreadsView oper = jsx(ThreadsView.class).params(obj);

        logger.warn("Проверка, что все письма из выдачи с first и last содержатся в обычной выдаче");
        assertThat("Не все письма из новой выдачи содержатся в обычной выдаче",
                oper.post().via(hc).getAllMids(),
                everyItem(isIn(jsx(ThreadsView.class).post().via(hc).getAllMids())));

    }

    @Test
    @Description("Проверяем, что все письма из выдачи с first и last содержатся в обычной выдаче для threads_list")
    public void threadListCounterChecks() throws IOException {
        String threadId = jsx(ThreadsView.class).post().via(hc).getAnyThreadId();
        ThreadListObj obj = new ThreadListObj().setThreadId(threadId);
        ThreadList oper = jsx(ThreadList.class).params(obj);

        obj.setFirst(0).setLast(0);

        assertThat("При нулевых значениях границ, ожидается пустая выдача",
                oper.post().via(hc).countMessagesInThread(),
                equalTo(0));

        logger.warn("Проверка, что все письма из выдачи с first и last содержатся в обычной выдаче");
        obj.setFirst(0).setLast(1);

        assertThat("Не все письма из новой выдачи содержатся в обычной выдаче",
                oper.post().via(hc).getAllMids(),
                everyItem(isIn(jsx(ThreadList.class)
                        .params(new ThreadListObj()
                                .setThreadId(threadId))
                        .post().via(hc).getAllMids())));

    }

}
