package ru.yandex.autotests.innerpochta.hound.v2.positive;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.hound.ArchiveStatus;
import ru.yandex.autotests.innerpochta.hound.v2.BaseFreezingTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Scope;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.shiva.Shiva;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.Scopes.TESTING;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;

@Aqua.Test
@Title("[HOUND] Ручка v2/unfreeze_user")
@Description("Тесты на ручку v2/unfreeze_user")
@Features(MyFeatures.HOUND)
@Stories(MyStories.FREEZING)
@Scope(TESTING)
@Credentials(loginGroup = "HoundV2UnfreezeUserTest")
public class UnfreezeUserPositiveTest extends BaseFreezingTest {


    @Test
    @Title("Заморозка с последюущей разморозкой")
    @Description("Проверяем, что корректно заморозим и разморозим пользователя")
    public void freezeUnfreezeOk() {
        if(freezingInfo().getState().equals(ArchiveStatus.UserState.FROZEN)) {
            unfreezeUser().post(shouldBe(ok200()));
        }

        assertThat(freezingInfo().getState(), equalTo(ArchiveStatus.UserState.ACTIVE));

        Shiva.freezeUser(authClient).get(shouldBe(Shiva.done200()));
        assertThat(freezingInfo().getState(), equalTo(ArchiveStatus.UserState.FROZEN));

        unfreezeUser().post(shouldBe(ok200()));
        assertThat(freezingInfo().getState(), equalTo(ArchiveStatus.UserState.ACTIVE));
    }

}