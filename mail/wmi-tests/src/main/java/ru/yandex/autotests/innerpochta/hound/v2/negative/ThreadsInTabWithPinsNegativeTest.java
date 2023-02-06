package ru.yandex.autotests.innerpochta.hound.v2.negative;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.InvalidArguments.UNEXISTING_UID;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgument;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgumentWithCode;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.noSuchTab;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.unknownUid;

@Aqua.Test
@Title("[HOUND] Ручка v2/threads_in_tab_with_pins")
@Description("Тесты на ошибки ручки v2/threads_in_tab_with_pins")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2ThreadsInTabWithPinsTest")
public class ThreadsInTabWithPinsNegativeTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Проверка вызова без uid'а")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400WithoutUid() {
        apiHoundV2().threadsInTabWithPins()
                .get(shouldBe(invalidArgument(equalTo("uid parameter is required"))));
    }

    @Test
    @Title("Проверка вызова с некорректным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForIncorrectUid() {
        apiHoundV2().threadsInTabWithPins()
                .withUid("abacaba")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова без count")
    public void shouldReceive400WithoutCount() {
        apiHoundV2().threadsInTabWithPins()
                .withUid(uid())
                .get(shouldBe(invalidArgument(equalTo("count parameter is required"))));
    }

    @Test
    @Title("Проверка вызова с некорректным count")
    public void shouldReceive400WithIncorrectCount() {
        apiHoundV2().threadsInTabWithPins()
                .withUid(uid())
                .withCount("qwerty")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова без first или page")
    public void shouldReceive400WithoutFirstOrPage() {
        apiHoundV2().threadsInTabWithPins()
                .withUid(uid())
                .withCount("100")
                .get(shouldBe(invalidArgument(equalTo("first or page parameter is required"))));
    }

    @Test
    @Title("Проверка вызова с некорректным first")
    public void shouldReceive400WithIncorrectFirst() {
        apiHoundV2().threadsInTabWithPins()
                .withUid(uid())
                .withCount("100")
                .withFirst("qwerty")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова с некорректным page")
    public void shouldReceive400WithIncorrectPage() {
        apiHoundV2().threadsInTabWithPins()
                .withUid(uid())
                .withCount("100")
                .withPage("qwerty")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова без tab")
    public void shouldReceive400WithoutTab() {
        apiHoundV2().threadsInTabWithPins()
                .withUid(uid())
                .withCount("100")
                .withFirst("0")
                .get(shouldBe(invalidArgument(equalTo("tab parameter is required"))));
    }

    @Test
    @Title("Проверка вызова с неизвестным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForUnknownUid() {
        apiHoundV2().threadsInTabWithPins()
                .withUid(UNEXISTING_UID)
                .withCount("100")
                .withFirst("0")
                .withTab("news")
                .get(shouldBe(unknownUid()));
    }

    @Test
    @Title("Проверка вызова с неизвестным tab")
    public void shouldReceive400WithUnknownTab() {
        apiHoundV2().threadsInTabWithPins()
                .withUid(uid())
                .withCount("100")
                .withFirst("0")
                .withTab("asdf")
                .get(shouldBe(noSuchTab()));
    }
}
