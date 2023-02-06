package ru.yandex.autotests.innerpochta.hound.v2;

import ru.yandex.autotests.innerpochta.beans.hound.FreezingInfo;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.hound.v2.unfreezeuser.ApiUnfreezeUser;

import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;

public class BaseFreezingTest extends BaseHoundTest {

    protected FreezingInfo freezingInfo() {
        return apiHoundV2().freezingInfo()
                .withUid(uid())
                .get(shouldBe(ok200()))
                .body().as(FreezingInfo.class);
    }

    protected ApiUnfreezeUser unfreezeUser() {
        return apiHoundV2().unfreezeUser().withUid(uid());
    }
}