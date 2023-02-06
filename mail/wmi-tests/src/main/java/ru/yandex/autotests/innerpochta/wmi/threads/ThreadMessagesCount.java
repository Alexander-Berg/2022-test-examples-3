package ru.yandex.autotests.innerpochta.wmi.threads;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsSetupUpdateSomeObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.ThreadListObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsSetupUpdateSome;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ThreadList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ThreadsView;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.BackupSettingWithWmiRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

/**
 * Проверяет работу тред вью совместно с отображением определенного количества писем на странице
 */
@Aqua.Test
@Title("Тестирование тредов. Треды и настройка количества писем на странице")
@Description("Следим за тем, чтобы треды смотрели на настройку количества писем на странице")
@RunWith(Parameterized.class)
@Features(MyFeatures.WMI)
@Stories({MyStories.THREADS, MyStories.SETTINGS})
@Credentials(loginGroup = "ThreadMessagesCount")
public class ThreadMessagesCount extends BaseTest {

    private String msgsCountSetting;
    private ThreadList threadListOper;

    private String subj;

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient).all().inbox().outbox();

    @Rule
    public BackupSettingWithWmiRule backup = BackupSettingWithWmiRule.with(authClient);


    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> data = new ArrayList<Object[]>();

        data.add(new Object[]{"messages_per_page", jsx(ThreadList.class)});
        data.add(new Object[]{"mobile_messages_per_page", api(ThreadList.class)});
        return data;

    }

    public ThreadMessagesCount(String msgsCountSetting, ThreadList threadListOper) {
        this.msgsCountSetting = msgsCountSetting;
        this.threadListOper = threadListOper;
    }

    @Before
    @After
    public void setDefaultMessagesPerPage() {
        jsx(SettingsSetupUpdateSome.class)
            .params(SettingsSetupUpdateSomeObj
                .settings().setUpdates(msgsCountSetting, "30"))
            .post().via(hc);
    }

    @Test
    @Description("Выставляет отображение сообщений на страницу равное 5\n" +
            "Отправляет 8 сообщений (больше 5)\n" +
            "Выводит тред-вью\n" +
            "- Проверяет что в тред-вью 6 писем  (на одно больше, свидетельствующее о том, что есть еще письма)")
    public void countMessagesInThread() throws Exception {
        int msgToSend = 8;
        Integer perPage = 5;

        logger.warn("Подсчет количества писем в треде");
        subj = sendWith.viaProd().count(msgToSend).waitDeliver().send().getSubj();

        // Меняем количество отображаемых на странице писем - ставим меньше чем отправили писем
        jsx(SettingsSetupUpdateSome.class)
                .params(SettingsSetupUpdateSomeObj
                        .settings().setUpdates(msgsCountSetting, perPage.toString()))
                .post().via(hc);

        String threadId = jsx(ThreadsView.class).post().via(hc).getThreadId(subj);
        Integer msgsInTread = threadListOper
                .params(ThreadListObj.getThread(threadId))
                .post().via(hc)
                .countMessagesInThread();
        assertThat("Количество сообщений на странице в треде не совпадает с ожидаемым", msgsInTread,
                equalTo(perPage + 1));
    }

}