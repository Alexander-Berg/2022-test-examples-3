package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static java.util.function.Function.identity;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.MopsApi.apiMops;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 21.01.15
 * Time: 15:23
 */
@Aqua.Test
@Title("[MOPS] Ручка stat. Новый сервис mops на 8814 порту")
@Description("Проверяем выдачу stat для различных случаев: ")
@Features(MyFeatures.MOPS)
@Stories(MyStories.MOPS_STAT)
@Issue("DARIA-37655")
@Credentials(loginGroup = "StatMopsTest")
public class StatTest extends MopsBaseTest {
    private static final String NOT_EXIST_UID = "666";

    @Test
    @Title("Корректный uid")
    @Description("Запрос с корректным uid. Должны получить пустое множество задач (200 {})")
    public void statTest() throws IOException {
        val response = Mops.stat(authClient).get(identity());
        assertEmptyTasks(response);
    }

    @Test
    @Title("Без uid")
    @Issue("DARIA-43843")
    @Description("Запрос без uid. Ожидаемый результат: {}")
    public void statWithoutUidTest() throws IOException {
        val expectedError = "invalid arguments: empty uid parameter";

        apiMops(authClient.account().userTicket())
                .stat()
                .get(shouldBe(invalidRequest(equalTo(expectedError))));
    }

    @Test
    @Title("Некорректный uid")
    @Description("Запрос c несуществующим uid. Ожидаемый результат: {}")
    public void statWithIncorrectUidTest() throws IOException {
        val response = Mops.stat(authClient, NOT_EXIST_UID).get(identity());
        assertEmptyTasks(response);
    }
}