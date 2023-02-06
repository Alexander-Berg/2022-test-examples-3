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
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.unknownUid;

@Aqua.Test
@Title("[HOUND] Ручка v2/yamail_status")
@Description("Тесты на ошибки ручки v2/yamail_status")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2YamailStatusTest")
@IgnoreForPg("MAILPG-2767")
public class YamailStatusNegativeTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Проверка вызова без uid'а")
    public void shouldReceive400WithoutUid() {
        apiHoundV2().yamailStatus()
                .get(shouldBe(invalidArgument(equalTo("uid parameter is required"))));
    }

    @Test
    @Title("Проверка вызова с неизвестным uid'ом")
    public void shouldReceive400ForUnknownUid() {
        apiHoundV2().yamailStatus()
                .withUid(UNEXISTING_UID)
                .get(shouldBe(unknownUid()));
    }

    @Test
    @Title("Проверка вызова с некорректным uid'ом")
    public void shouldReceive400ForIncorrectUid() {
        apiHoundV2().yamailStatus()
                .withUid("abacaba")
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }
}
