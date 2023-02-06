package ru.yandex.market.crm.campaign.domain.sending;

import ru.yandex.market.crm.campaign.domain.promo.entities.TestIdsGroup;

/**
 * @author apershukov
 */
public class TestDevicesGroup extends TestIdsGroup<TestPushDevice, TestDevicesGroup> {

    @Override
    protected TestDevicesGroup getThis() {
        return this;
    }
}
