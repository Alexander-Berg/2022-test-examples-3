package ru.yandex.autotests.innerpochta.imap.login;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static java.util.Arrays.asList;
import static ru.yandex.autotests.innerpochta.imap.requests.DeleteRequest.delete;

@Aqua.Test
@Title("Команда LOGIN. Авторизация. Много раз")
@Features({ImapCmd.LOGIN})
@Stories(MyStories.COMMON)
@Description("LOGIN: позитивные и негативные кейсы")
@RunWith(Parameterized.class)
public class LoginManyTimesTest extends BaseTest {
    private static Class<?> currentClass = LoginManyTimesTest.class;

    @Parameterized.Parameter
    public int s;
    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    @Parameterized.Parameters(name = "{index}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {1}, {1}, {1}, {1}, {1},
                {1}, {1}, {1}, {1}, {1},
                {1}, {1}, {1}, {1}, {1},
                {1}, {1}, {1}, {1}, {1},
                {1}, {1}, {1}, {1}, {1},
                {1}, {1}, {1}, {1}, {1},
                {1}, {1}, {1}, {1}, {1},
                {1}, {1}, {1}, {1}, {1},
                {1}, {1}, {1}, {1}, {1}
        });
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("264")
    public void test() {
        imap.request(delete("sdf"));
    }
}
