package ru.yandex.direct.core.entity.banner.type.additionalhrefs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_1;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_2;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.toNewBannerAdditionalHrefs;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithAdditionalHrefsRepositoryTypeSupportAddTest extends BannerClientInfoAddOperationTestBase {

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public List<String> additionalHrefsToSave;

    private CpmPriceCampaign campaign;
    private CpmYndxFrontpageAdGroup adGroup;
    private CreativeInfo creativeInfo;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "additionalHrefs == emptyList()",
                        Collections.emptyList(),
                },
                {
                        "one additionalHref",
                        List.of(HREF_1),
                },
                {
                        "two additionalHrefs",
                        List.of(HREF_1, HREF_2),
                },
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, campaign);
        enableFeatureAdditionalHrefs(clientInfo);
    }

    @Test
    public void additionalHrefsAreSaved() {
        List<BannerAdditionalHref> additionalHrefs = toNewBannerAdditionalHrefs(additionalHrefsToSave);
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withAdditionalHrefs(additionalHrefs);
        Long bannerId = prepareAndApplyValid(banner);

        CpmBanner actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getAdditionalHrefs()).isEqualTo(additionalHrefs);
    }

    private void enableFeatureAdditionalHrefs(ClientInfo clientInfo) {
        steps.featureSteps()
                .addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, true);
    }
}
