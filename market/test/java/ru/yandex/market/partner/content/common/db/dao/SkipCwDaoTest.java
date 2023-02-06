package ru.yandex.market.partner.content.common.db.dao;


import org.junit.*;
import org.springframework.beans.factory.annotation.*;
import ru.yandex.market.partner.content.common.*;

public class SkipCwDaoTest extends BaseDbCommonTest {
    @Autowired
    private SkipCwDao skipCwDao;

    @Test
    public void shouldSkip() {
        int shopId = 100;
        skipCwDao.addShopId(shopId);

        Assert.assertTrue(skipCwDao.shouldSkip(shopId));
    }

    @Test
    public void shouldNotSkipAfterDelete() {
        int shopId = 100;
        skipCwDao.addShopId(shopId);
        skipCwDao.deleteById(shopId);

        Assert.assertFalse(skipCwDao.shouldSkip(shopId));
    }
}
