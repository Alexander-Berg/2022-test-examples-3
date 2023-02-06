package ru.yandex.autotests.innerpochta.hound.v2.negative;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgumentWithCode;

@Aqua.Test
@Title("[HOUND] Ручка v2/attach_sid")
@Description("Тесты на ручку v2/attach_sid")
@Features(MyFeatures.HOUND)
@Stories(MyStories.ATTACH)
@Credentials(loginGroup = "HoundV2AttachSidTest")
@Issue("MAILPG-2764")
public class AttachSidNegativeTest extends BaseHoundTest {


    @Test
    @Title("Проверка вызова без uid'а")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400WithoutUid() {
        apiHoundV2().attachSid()
                .withUid("")
                .withMid("1234")
                .withHids("1.1")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова без mid'а")
    public void shouldReceive400WithoutMid() {
        apiHoundV2().attachSid()
                .withUid(uid())
                .withMid("")
                .withHids("1.1")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова без hids'а")
    public void shouldReceive400WithoutHids() {
        apiHoundV2().attachSid()
                .withUid(uid())
                .withMid("1")
                .withHids("")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова без параметров")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400WithoutAnything() {
        apiHoundV2().attachSid()
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }
}
