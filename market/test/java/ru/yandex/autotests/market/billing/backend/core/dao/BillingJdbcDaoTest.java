package ru.yandex.autotests.market.billing.backend.core.dao;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.autotests.market.billing.backend.core.dao.billing.BillingDao;
import ru.yandex.autotests.market.billing.backend.core.dao.billing.BillingDaoFactory;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.bids.OfferBid;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.schedule.ShopSchedule;
import ru.yandex.autotests.market.billing.beans.outlets.Shop;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * User: jkt
 * Date: 02.10.12
 * Time: 15:33
 */
public class BillingJdbcDaoTest {

    private static final Logger log = Logger.getLogger(BillingJdbcDaoTest.class);
    private BillingDao billingDao;

    @Before
    public void initDao() {
        billingDao = BillingDaoFactory.getBillingDao();
    }

    @Ignore
    @Test
    public void testGetOutletsForDatasource() throws Exception {
        int datasourceId = 35201;
        List<Shop> outletsForDatasources = billingDao.getShops(89, 35201, 21980);
        assertTrue("Outlets for datasource " + datasourceId + " is null",
                outletsForDatasources != null);
        assertTrue("Outlets for datasource " + datasourceId + " haven't been queried",
                outletsForDatasources.size() > 0);
        int expectedPointsNumber = 7;
        int actualPointsNumber = outletsForDatasources.get(0).getOutlets().size();
        assertTrue("Number of outlets for datasource " + datasourceId + " is not " + expectedPointsNumber,
                actualPointsNumber != expectedPointsNumber);
        log.debug(outletsForDatasources);
    }

    @Ignore
    @Test
    public void testGetCountOfoutletsForImportToIndexer() {
        int outletsCount = billingDao.getSelfOutletSqlFunctions().countShopOutlets();
        Assert.assertThat(outletsCount, is(not(0)));
    }

    @Ignore
    @Test
    public void testGetOfferBidsForDatasource() throws Exception {
        int campaignid = 21011987;
        List<OfferBid> bidsForCampaign = billingDao.getBidsForCampaign(campaignid);
        assertNotNull("Bids list is null.", bidsForCampaign);
        assertTrue("Bids list for campaign " + campaignid + " can not be empty", bidsForCampaign.size() != 0);
        for (OfferBid offerBid : bidsForCampaign) {
            assertTrue("Bid id cannot be null. " + offerBid, offerBid.getId() != null);
            assertTrue("Shop_id can not be null. " + offerBid, offerBid.getShopId() != null);
            assertTrue("Published bid info can not be null. " + offerBid,
                    offerBid.getPublishedBidinfo() != null);
        }
        log.debug(bidsForCampaign);
    }

    @Ignore
    @Test
    public void testGetShopSchedule() throws  Exception {
        int testCampaign = 1029045;
        List<ShopSchedule> shedules = billingDao.getSheduleForCampaign(testCampaign);
        assertThat("Schedule list is null.", shedules, is(notNullValue()));
        assertThat("Schedule list is empty.", shedules, hasSize(greaterThan(0)));
        for(ShopSchedule schedule: shedules) {
            assertThat("Schedule campaignId cannot be null. " + schedule,
                    schedule.getCampaignId(), is(notNullValue()));
            assertThat("Schedule datasourceId cannot be null. " + schedule,
                    schedule.getDatasourceId(), is(notNullValue()));
            assertThat("Schedule days cannot be null. " + schedule,
                    schedule.getDays(), is(notNullValue()));
            assertThat("Schedule minutes cannot be null. " + schedule,
                    schedule.getMinutes(), is(notNullValue()));
            assertThat("Schedule startDay cannot be null. " + schedule,
                    schedule.getStartDay(), is(notNullValue()));
            assertThat("Schedule startMinute cannot be null. " + schedule,
                    schedule.getStartMinute(), is(notNullValue()));
        }
        log.debug(shedules);
    }
}
