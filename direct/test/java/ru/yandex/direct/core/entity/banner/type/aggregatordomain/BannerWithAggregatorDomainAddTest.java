package ru.yandex.direct.core.entity.banner.type.aggregatordomain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithAggregatorDomain;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithAggregatorDomain.AGGREGATOR_DOMAIN;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.core.testing.steps.ClientSteps.DEFAULT_SHARD;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithAggregatorDomainAddTest extends BannerAdGroupInfoAddOperationTestBase {

    @Autowired
    private AggregatorDomainsRepository aggregatorDomainsRepository;

    @Test
    public void testAddAndRead_bannerWithAggregatorDomain() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        var banner = createBanner(adGroupInfo).withHref("http://vk.com/test");
        var id = prepareAndApplyValid(banner);

        var aggregatorDomains = aggregatorDomainsRepository.getAggregatorDomains(DEFAULT_SHARD, singleton(id));
        assertThat(aggregatorDomains.size(), equalTo(1));

        var actualBanner = getBanner(id);
        assertThat(actualBanner, hasProperty(
                AGGREGATOR_DOMAIN.name(), equalTo(aggregatorDomains.get(id))
        ));
    }

    @Test
    public void testAddAndRead_bannerWithCommonDomain() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        var banner = createBanner(adGroupInfo).withHref("http://ya.ru");
        var id = prepareAndApplyValid(banner);

        var aggregatorDomains = aggregatorDomainsRepository.getAggregatorDomains(DEFAULT_SHARD, singleton(id));
        assertThat(aggregatorDomains.size(), equalTo(0));

        var actualBanner = getBanner(id);
        assertThat(actualBanner, hasProperty(AGGREGATOR_DOMAIN.name(), nullValue()));
    }

    private BannerWithAggregatorDomain createBanner(AdGroupInfo adGroupInfo) {
        return clientTextBanner().withAdGroupId(adGroupInfo.getAdGroupId()).withGeoFlag(true);
    }
}
