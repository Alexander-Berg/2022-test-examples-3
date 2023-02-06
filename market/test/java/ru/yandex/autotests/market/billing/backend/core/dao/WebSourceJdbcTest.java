package ru.yandex.autotests.market.billing.backend.core.dao;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.autotests.market.billing.backend.core.dao.billing.BillingDao;
import ru.yandex.autotests.market.billing.backend.core.dao.billing.BillingDaoFactory;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.bids.WebBidsSource;

import static ru.yandex.autotests.market.billing.backend.core.dao.entities.bids.WebBidsSource.SHOP_OFFER_ID;
import static ru.yandex.autotests.market.billing.backend.core.dao.entities.bids.WebBidsSource.TITLE;

/**
 * UNIT_TEST
 * User: strangelet
 * Date: 24.05.13 : 14:37
 */
public class WebSourceJdbcTest {

    private static final Logger log = Logger.getLogger(WebSourceJdbcTest.class);

    private BillingDao billingDao;
    public static final int SHOP_ID = 193;

    @Before
    public void initDao() {
        billingDao = BillingDaoFactory.getBillingDao();
    }

    @Ignore
    @Test
    public void takeWebBidSourceTest() {
        final WebBidsSource bidsSource = billingDao.takeBidSourceForShop(SHOP_ID);
        log.info("For shop_id = " + SHOP_ID + " bid source is " + bidsSource);

    }

    @Ignore
    @Test
    public void switchWebBidSourceTest() {
        final WebBidsSource bidsSource = billingDao.takeBidSourceForShop(SHOP_ID);
        log.info("For shop_id = " + SHOP_ID + " bid source is " + bidsSource);

        if (bidsSource.equals(SHOP_OFFER_ID))
            billingDao.setBidSourceForShop(TITLE, SHOP_ID);
        else
            billingDao.setBidSourceForShop(SHOP_OFFER_ID, SHOP_ID);

        final WebBidsSource nextBidsSource = billingDao.takeBidSourceForShop(SHOP_ID);
        log.info("For shop_id = " + SHOP_ID + " new bid source is " + nextBidsSource);


    }
}
