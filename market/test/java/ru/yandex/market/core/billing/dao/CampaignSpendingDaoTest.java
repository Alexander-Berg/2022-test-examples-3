package ru.yandex.market.core.billing.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;

class CampaignSpendingDaoTest extends FunctionalTest {
    @Autowired
    CampaignSpendingDao dao;

    @Test
    void updateCampaignsSpendings() {
        // simple smoke test
        dao.updateCampaignsSpendings();
    }

    @Test
    void updateCampaignSpending() {
        // simple smoke test
        dao.updateCampaignSpending(1L);
    }
}
