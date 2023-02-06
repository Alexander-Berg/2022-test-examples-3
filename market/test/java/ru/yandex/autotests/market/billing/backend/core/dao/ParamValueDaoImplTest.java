package ru.yandex.autotests.market.billing.backend.core.dao;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.autotests.market.billing.backend.core.dao.billing.BillingDao;
import ru.yandex.autotests.market.billing.backend.core.dao.billing.BillingDaoFactory;
import ru.yandex.autotests.market.billing.beans.settings.Shop;

/**
 * Created with IntelliJ IDEA.
 * User: suok
 * Date: 26.09.13
 * Time: 15:33
 * To change this template use File | Settings | File Templates.
 */
public class ParamValueDaoImplTest {
    private static final Logger log = Logger.getLogger(ParamValueDaoImplTest.class);

    private BillingDao billingDao;

    @Before
    public void initDao() {
        billingDao = BillingDaoFactory.getBillingDao();
    }

    @Ignore
    @Test
    public void takeShopIds() {
        List<Shop> shops = billingDao.getShopFromParamValue();
        log.info("paramValues : " + shops);
    }
}
