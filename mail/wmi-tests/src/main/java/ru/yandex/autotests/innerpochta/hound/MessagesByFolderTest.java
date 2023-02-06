package ru.yandex.autotests.innerpochta.hound;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.To;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByThreadObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.Tab;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByThread;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 25.09.14
 * Time: 15:00
 * смотрим результат руками: curl 'localhost:9090/messages_by_folder?uid=4000019189&first=0&count=50&
 * fid=2160000190000543740&sort_type=date1'
 * [DARIA-39394]
 */
@Aqua.Test
@Title("[HOUND] Ручка messages_by_folder")
@Description("Общие тесты на ручку messages_by_folder")
@Credentials(loginGroup = "MessagesByFolderTest")
@Features(MyFeatures.HOUND)
@Issue("DARIA-39394")
@RunWith(DataProviderRunner.class)
public class MessagesByFolderTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @DataProvider({
            " \"beb be\"",
            " \"bebbebe beb\"",
            " \"bebb<be beb\""
    })
    @Issue("MAILPG-765")
    @Title("Сохраняем письмо с длинным to и различными display_name")
    @Description("1. Берём письмо из таска (можно в принципе любое письмо c хедером from/to/cc/bcc длинее 1014 символов)\n" +
            "2. Дёргаем messages_by_folder (messages_by_threads)\n" +
            "3. Убеждаемся, что там не один ресипиент с длиннющим display_name (пример плохой выдачи есть в таске)" +
            "Информация для тестирования: в оракле заголовки это varchar 1024")
    public void testDisplayName(String displayName) throws Exception {
        String longAddress = authClient.acc().getSelfEmail();
        for (int i = 0; i < 40; i++) {
            longAddress = longAddress + displayName + "<" + Util.getRandomAddress() + ">";
        }

        String tid = sendWith(authClient).viaProd().to(longAddress).saveDraft().waitDeliver().getTid();

        List<To> toListMesssages = api(MessagesByFolder.class).setHost(props().houndUri()).params(empty()
                .setUid(uid()).setFirst("0").setCount("1").setTill("2000000000")
                .setFid(folderList.draftFID()).setSortType("date1")).get().via(authClient).withDebugPrint()
                .resp().getEnvelopes().get(0).getTo();

        assertThat("Неправильно обрезаем <to> в ручке <messages_by_folder> [MAILPG-765]", toListMesssages.size(), greaterThan(1));


        List<To> toListThreads = api(MessagesByThread.class).setHost(props().houndUri()).params(MessagesByThreadObj.empty()
                .setUid(uid()).setFirst("0").setCount("1")
                .setTid(tid).setSortType("date1")).get().via(authClient).withDebugPrint()
                .resp().getEnvelopes().get(0).getTo();

        assertThat("Неправильно обрезаем <to> в ручке <messages_by_thread> [MAILPG-765]", toListThreads.size(), greaterThan(1));
    }

    @Test
    @Issue("MAILPG-2007")
    @Title("Проверяем tab")
    @Description("В выдаче энвелопов всех ручек должно быть поле tab")
    public void testTab() throws Exception {
        String mid = sendWith(authClient).viaProd().send().strict().waitDeliver().getMid();
        String tab = api(MessagesByFolder.class).setHost(props().houndUri()).params(empty()
                .setUid(uid()).setFirst("0").setCount("1")
                .setFid(folderList.defaultFID()).setSortType("date1")).get().via(authClient).withDebugPrint()
                .resp().getEnvelopes().get(0).getTab();
        assertThat("Поле tab должно быть default", tab, equalTo(Tab.defaultTab));
    }
}
