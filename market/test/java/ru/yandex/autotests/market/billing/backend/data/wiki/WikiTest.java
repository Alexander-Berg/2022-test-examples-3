package ru.yandex.autotests.market.billing.backend.data.wiki;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Created by ivmelnik on 07.09.16.
 */
public class WikiTest {

    @Ignore
    @Test
    public void testLoadFromWiki() {
        List<DbServiceName> dbServiceNames = DbServiceName.loadFromWiki();
        assertThat(dbServiceNames, is(not(empty())));

        List<ShopDataTestParams> shopDataTestParams = ShopDataTestParams.loadFromWiki();
        assertThat(shopDataTestParams, is(not(empty())));

        List<ShopsForParallelTesting> shopsForParallelTesting = ShopsForParallelTesting.loadFromWiki();
        assertThat(shopsForParallelTesting, is(not(empty())));

        List<UntouchableCampaigns> untouchableCampaigns = UntouchableCampaigns.loadFromWiki();
        assertThat(untouchableCampaigns, is(not(empty())));

        List<StorageFileTestParam> storageFileTestParams = StorageFileTestParam.loadFromWiki();
        assertThat(storageFileTestParams, is(not(empty())));
    }

}
