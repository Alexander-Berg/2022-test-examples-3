package ru.yandex.autotests.innerpochta.imap.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.StoreRequest;
import ru.yandex.autotests.innerpochta.imap.utils.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.Lists.newArrayList;
import static ru.yandex.autotests.innerpochta.imap.data.TestData.allFlags;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.store;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanFlagsRule.withCleanFlagsBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 09.05.14
 * Time: 16:52
 * <p/>
 * На корпах падаем из-за:
 * [MAILPROTO-2096]
 * [MAILPROTO-2041]
 * [MAILPROTO-2181]
 * [MAILPROTO-2325]
 */
@Aqua.Test
@Title("Команда STORE. Ставим различные флаги на письма")
@Features({ImapCmd.STORE})
@Stories("#различные флаги")
@Description("Ставим различные флаги на письма из одной папки\n" +
        "Позитивное тестирование")
@RunWith(Parameterized.class)
public class StoreDifferentFlags extends BaseTest {
    private static Class<?> currentClass = StoreDifferentFlags.class;

    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withCleanFlagsBefore(newLoginedClient(currentClass));
    private String flag;
    private List<String> expectedFlags;

    public StoreDifferentFlags(String flag) {
        this.flag = flag;
        this.expectedFlags = newArrayList(Utils.removeRoundBrace(flag).split(" "));
    }

    @Parameterized.Parameters(name = "flag - {0}")
    public static Collection<Object[]> data() {
        return allFlags();
    }

    @Description("Проверяем, что Flags ставит указанные флаги, а остальные снимает")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("619")
    public void storeDifferentFlags() throws Exception {
        imap.select().inbox();

        imap.request(store("1", StoreRequest.FLAGS, flag)).shouldBeOk().flagsShouldBe(expectedFlags);
        imap.noop().pullChanges();
        imap.fetch().waitFlags("1", expectedFlags);
    }

    @Test
    @Description("Дважды ставим флаги с помощью FLAGS [MAILPROTO-2325]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("620")
    public void doubleStoreDifferentFlags() throws Exception {
        imap.select().inbox();
        imap.request(store("1", StoreRequest.FLAGS, flag)).shouldBeOk().flagsShouldBe(expectedFlags);
        imap.fetch().waitFlags("1", expectedFlags);
        //для делчейнов ставим второй раз
        imap.request(store("1", StoreRequest.FLAGS, flag)).shouldBeOk().flagsShouldBe(expectedFlags);
        imap.noop().pullChanges();
        imap.fetch().waitFlags("1", expectedFlags);
    }

    @Test
    @Description("Ставим флаг, затем его снимаем, не должны увидеть флаги")
    @ru.yandex.qatools.allure.annotations.TestCaseId("621")
    public void storeDifferentMinusFlagsShouldSeeNoFlags() {
        imap.select().inbox();
        imap.request(store("1", StoreRequest.FLAGS, flag)).shouldBeOk();
        imap.fetch().waitFlags("1", expectedFlags);
        //снимаем флаги, поэтому ожидаем пустоту в флагах
        imap.request(store("1", StoreRequest.MINUS_FLAGS, flag)).shouldBeOk().flagShouldBe("");
        imap.noop().pullChanges();
        imap.fetch().waitNoFlags("1");
    }

    @Test
    @Description("Снимаем флаги у письма БЕЗ флагов [MAILPROTO-2325]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("622")
    public void storeDifferentMinusWithMessageWithNoFlags() {
        imap.select().inbox();
        imap.request(store("1", StoreRequest.MINUS_FLAGS, flag)).shouldBeOk().flagsShouldBe("");
        imap.noop().pullChanges();
        imap.fetch().waitNoFlags("1");
    }

    @Description("Ставим флаг, затем его снимаем с SILENT.\n" +
            "Ожидаемый результат: не должны увидеть флаги и FETCH")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("623")
    public void storeDifferentMinusFlagsSilentShouldSeeNoFlags() {
        imap.select().inbox();
        imap.request(store("1", StoreRequest.FLAGS, flag)).shouldBeOk();
        imap.fetch().waitFlags("1", expectedFlags);

        imap.request(store("1", StoreRequest.MINUS_FLAGS_SILENT, flag)).shouldBeOk().shouldBeNoFlags();
        imap.noop().pullChanges();
        imap.fetch().waitNoFlags("1");
    }

    @Description("Проверяем, что нет фетча при установке флага SILENT")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("624")
    public void storeDifferentSilentFlags() {
        imap.select().inbox();
        imap.request(store("1", StoreRequest.FLAGS_SILENT, flag)).shouldBeOk().shouldBeNoFlags();
        imap.noop().pullChanges();
        imap.fetch().waitFlags("1", expectedFlags);
    }

    @Description("Дважды ставим флаг с SILENT")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("625")
    public void doubleStoreDifferentSilentFlags() {
        imap.select().inbox();
        imap.request(store("1", StoreRequest.FLAGS_SILENT, flag)).shouldBeOk().shouldBeNoFlags();
        imap.fetch().waitFlags("1", expectedFlags);
        imap.request(store("1", StoreRequest.FLAGS_SILENT, flag)).shouldBeOk().shouldBeNoFlags();
        imap.noop().pullChanges();
        imap.fetch().waitFlags("1", expectedFlags);
    }

    @Description("Ставим пользовательский флаг. Затем делаем STORE FLAG+ с различными флагами\n" +
            "Флаги должны добавиться")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("626")
    public void storeDifferentPlusFlags() {
        imap.select().inbox();
        String userFlag = MessageFlags.random();
        List<String> expFlags = new ArrayList<>();
        expFlags.addAll(expectedFlags);
        expFlags.add(userFlag);

        imap.request(store("1", StoreRequest.FLAGS, userFlag)).shouldBeOk().flagShouldBe(userFlag);

        imap.request(store("1", StoreRequest.PLUS_FLAGS, flag)).shouldBeOk().flagsShouldBe(expFlags);
        imap.noop().pullChanges();
        imap.fetch().waitFlags("1", expFlags);
    }

    @Description("Ставим пользовательский флаг. Затем делаем STORE FLAG+ с различными флагами\n" +
            "Флаги должны добавиться")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("627")
    public void storeDifferentPlusFlagsSilent() {
        imap.select().inbox();
        String userFlag = MessageFlags.random();
        List<String> expFlags = new ArrayList<>();
        expFlags.addAll(expectedFlags);
        expFlags.add(userFlag);

        imap.request(store("1", StoreRequest.FLAGS, userFlag)).shouldBeOk();

        imap.request(store("1", StoreRequest.PLUS_FLAGS_SILENT, flag)).shouldBeOk().shouldBeNoFlags();
        imap.noop().pullChanges();
        imap.fetch().waitFlags("1", expFlags);
    }
}
