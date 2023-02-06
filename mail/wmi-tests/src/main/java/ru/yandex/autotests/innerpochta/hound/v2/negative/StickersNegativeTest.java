package ru.yandex.autotests.innerpochta.hound.v2.negative;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.hound.v2.stickers.ApiStickers;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.InvalidArguments.UNEXISTING_UID;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.*;

@Aqua.Test
@Title("[HOUND] Ручка v2/stickers")
@Description("Тесты на ручку v2/stickers")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2StickersTest")
public class StickersNegativeTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("Проверка вызова без uid'а")
    public void shouldReceive400WithoutUid() {
        apiHoundV2().stickers()
                .get(shouldBe(invalidArgument(equalTo("uid parameter is required"))));
    }

    @Test
    @Title("Проверка вызова с неизвестным uid'ом")
    public void shouldReceive400ForUnknownUid() {
        apiHoundV2().stickers()
                .withUid(UNEXISTING_UID)
                .withType(ApiStickers.TypeParam.REPLY_LATER)
                .get(shouldBe(unknownUid()));
    }

    @Test
    @Title("Проверка вызова с некорректным uid'ом")
    public void shouldReceive400ForIncorrectUid() {
        apiHoundV2().stickers()
                .withUid("abacaba")
                .withType(ApiStickers.TypeParam.REPLY_LATER)
                .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова без type'а")
    public void shouldReceive400WithoutType() {
        apiHoundV2().stickers()
                .withUid(authClient.account().uid())
                .get(shouldBe(invalidArgument(equalTo("invalid type argument"))));
    }

    @Test
    @Title("Проверка вызова c некорректным type")
    public void shouldReceive400ForIncorrectType() {
        apiHoundV2().stickers()
                .withUid(authClient.account().uid())
                .get(shouldBe(invalidArgument(equalTo("invalid type argument"))));
    }
}
