package ru.yandex.market.crm.campaign.domain.sending;

import ru.yandex.market.crm.campaign.domain.promo.entities.TestIdsGroup;

/**
 * @author apershukov
 */
public class TestEmailsGroup extends TestIdsGroup<TestEmail, TestEmailsGroup> {

    @Override
    protected TestEmailsGroup getThis() {
        return this;
    }
}
