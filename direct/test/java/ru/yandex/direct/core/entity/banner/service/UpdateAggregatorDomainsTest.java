package ru.yandex.direct.core.entity.banner.service;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateAggregatorDomainsTest {
    private static final String HREF_WITH_AGGREGATOR_DOMAIN = "http://vk.com/test";
    private static final String HREF_WITHOUT_AGGREGATOR_DOMAIN = "https://www.yandex.ru/company";
    private static final String AGGREGATOR_DOMAIN = "test.vk.com";

    @Autowired
    private Steps steps;

    @Autowired
    private AggregatorDomainsRepository aggregatorDomainsRepository;

    @Autowired
    private BannersAddOperationFactory bannersAddOperationFactory;

    @Autowired
    private BannersUpdateOperationFactory bannersUpdateOperationFactory;

    private AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        adGroupInfo = steps.keywordSteps().createDefaultKeyword().getAdGroupInfo();
    }

    @Test
    public void updateAggregatorDomains_AddOperation() {
        Long bannerId = addBannerWithHref(HREF_WITH_AGGREGATOR_DOMAIN);
        checkResult(bannerId, AGGREGATOR_DOMAIN);
    }

    @Test
    public void updateAggregatorDomains_AddOperation_WhenNoAggregatorDomain() {
        Long bannerId = addBannerWithHref(HREF_WITHOUT_AGGREGATOR_DOMAIN);
        checkResult(bannerId, null);
    }

    @Test
    public void updateAggregatorDomains_UpdateOperation_removeAggregatorDomain() {
        Long bannerId = addBannerWithHref(HREF_WITH_AGGREGATOR_DOMAIN);

        String domainBeforeUpdate = getAggregatorDomain(bannerId);
        assumeThat(domainBeforeUpdate, equalTo(AGGREGATOR_DOMAIN));

        updateBannerHref(bannerId, HREF_WITHOUT_AGGREGATOR_DOMAIN);
        checkResult(bannerId, null);
    }

    @Test
    public void updateAggregatorDomains_UpdateOperation_addAggregatorDomain() {
        Long bannerId = addBannerWithHref(HREF_WITHOUT_AGGREGATOR_DOMAIN);

        String domainBeforeUpdate = getAggregatorDomain(bannerId);
        assumeThat(domainBeforeUpdate, equalTo(null));

        updateBannerHref(bannerId, HREF_WITH_AGGREGATOR_DOMAIN);
        checkResult(bannerId, AGGREGATOR_DOMAIN);
    }

    @Test
    public void updateAggregatorDomains_AddOperation_forCustomDomain() {
        Long bannerId = addBannerWithHref("https://yandex.ru/maps/org/197060445683");

        checkResult(bannerId, "197060445683.maps.yandex.ru");
    }

    private Long addBannerWithHref(String href) {
        TextBanner banner = fullTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()).withHref(href);

        MassResult<Long> massResult = bannersAddOperationFactory
                .createPartialAddOperation(
                        singletonList(banner), adGroupInfo.getClientId(), adGroupInfo.getUid(), false)
                .prepareAndApply();
        assumeThat(massResult, isFullySuccessful());

        return massResult.getResult().get(0).getResult();
    }

    private void updateBannerHref(Long bannerId, String href) {
        ModelChanges<BannerWithSystemFields> modelChanges =
                ModelChanges.build(bannerId, TextBanner.class, TextBanner.HREF, href)
                        .castModelUp(BannerWithSystemFields.class);

        MassResult<Long> massResult = bannersUpdateOperationFactory.createPartialUpdateOperation(
                singletonList(modelChanges), adGroupInfo.getUid(), adGroupInfo.getClientId())
                .prepareAndApply();
        assumeThat(massResult, isFullySuccessful());
    }

    private void checkResult(Long bannerId, String expectedDomain) {
        String domain = getAggregatorDomain(bannerId);
        assertThat(domain, equalTo(expectedDomain));
    }

    private String getAggregatorDomain(Long bannerId) {
        Map<Long, String> aggregatorDomains = aggregatorDomainsRepository.getAggregatorDomains(
                adGroupInfo.getShard(), singletonList(bannerId));
        return aggregatorDomains.get(bannerId);
    }
}
