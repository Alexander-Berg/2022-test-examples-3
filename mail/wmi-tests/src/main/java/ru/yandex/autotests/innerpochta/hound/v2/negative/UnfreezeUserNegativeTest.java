package ru.yandex.autotests.innerpochta.hound.v2.negative;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.hound.ArchiveStatus;
import ru.yandex.autotests.innerpochta.hound.v2.BaseFreezingTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgument;

@Aqua.Test
@Title("[HOUND] Ручка v2/unfreeze_user")
@Description("Тесты на ручку v2/unfreeze_user")
@Features(MyFeatures.HOUND)
@Stories(MyStories.FREEZING)
@Credentials(loginGroup = "HoundV2UnfreezeUserNegativeTest")
public class UnfreezeUserNegativeTest extends BaseFreezingTest {

    @Test
    @Title("Разморозка активного пользователя")
    @Description("Проверяем, что получим ошибку")
    public void unfreezeFail() {

        assertThat(freezingInfo().getState(), equalTo(ArchiveStatus.UserState.ACTIVE));

        unfreezeUser().post(shouldBe(invalidArgument(equalTo("User is not frozen"))));

        assertThat(freezingInfo().getState(), equalTo(ArchiveStatus.UserState.ACTIVE));
    }
}
