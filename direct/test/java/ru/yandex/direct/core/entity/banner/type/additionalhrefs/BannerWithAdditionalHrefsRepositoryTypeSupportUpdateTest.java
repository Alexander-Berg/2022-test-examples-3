package ru.yandex.direct.core.entity.banner.type.additionalhrefs;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.model.BannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_1;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_2;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_3;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.HREF_4;
import static ru.yandex.direct.core.testing.data.TestBannerAdditionalHrefs.toNewBannerAdditionalHrefs;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithAdditionalHrefsRepositoryTypeSupportUpdateTest extends
        BannerClientInfoUpdateOperationTestBase {

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public List<String> initialAdditionalHrefs;

    @Parameterized.Parameter(2)
    public List<String> newAdditionalHrefs;

    private OldCpmBanner banner;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "update additionalHrefs empty -> empty",
                        emptyList(),
                        emptyList(),
                },
                {
                        "update additionalHrefs empty -> one",
                        emptyList(),
                        List.of(HREF_1),
                },
                {
                        "update additionalHrefs empty -> two",
                        emptyList(),
                        List.of(HREF_1, HREF_2),
                },
                {
                        "update additionalHrefs one -> empty",
                        List.of(HREF_1),
                        emptyList(),
                },
                {
                        "update additionalHrefs one -> the same",
                        List.of(HREF_1),
                        List.of(HREF_1),
                },
                {
                        "update additionalHrefs one -> another",
                        List.of(HREF_1),
                        List.of(HREF_2),
                },
                {
                        "update additionalHrefs one -> one + another",
                        List.of(HREF_1),
                        List.of(HREF_2, HREF_1),
                },
                {
                        "update additionalHrefs two -> empty",
                        List.of(HREF_1, HREF_2),
                        emptyList(),
                },
                {
                        "update additionalHrefs two -> the same",
                        List.of(HREF_1, HREF_2),
                        List.of(HREF_1, HREF_2),
                },
                {
                        "update additionalHrefs two -> one of two",
                        List.of(HREF_1, HREF_2),
                        List.of(HREF_2),
                },
                {
                        "update additionalHrefs two -> one another",
                        List.of(HREF_1, HREF_2),
                        List.of(HREF_3),
                },
                {
                        "update additionalHrefs two -> one of two and one another",
                        List.of(HREF_1, HREF_2),
                        List.of(HREF_2, HREF_3),
                },
                {
                        "update additionalHrefs two -> two another",
                        List.of(HREF_1, HREF_2),
                        List.of(HREF_3, HREF_4),
                },
                {
                        "update additionalHrefs delete in the middle",
                        List.of(HREF_1, HREF_2, HREF_3),
                        List.of(HREF_1, HREF_3),
                },
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);

        clientInfo = steps.clientSteps().createDefaultClient();
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        var adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        var creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, campaign);
        enableFeatureAdditionalHrefs(clientInfo);

        banner = activeCpmBanner(campaign.getId(), adGroup.getId(), creativeInfo.getCreativeId())
                .withAdditionalHrefs(mapList(initialAdditionalHrefs,
                        additionalHref -> new OldBannerAdditionalHref().withHref(additionalHref)));
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), banner, adGroup);
    }

    @Test
    public void additionalHrefsAreUpdated() {
        Long bannerId = banner.getId();
        List<BannerAdditionalHref> additionalHrefs = toNewBannerAdditionalHrefs(newAdditionalHrefs);
        ModelChanges<CpmBanner> modelChanges = ModelChanges.build(bannerId, CpmBanner.class,
                CpmBanner.ADDITIONAL_HREFS, additionalHrefs);

        prepareAndApplyValid(modelChanges);

        CpmBanner actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getAdditionalHrefs()).isEqualTo(additionalHrefs);
    }

    private void enableFeatureAdditionalHrefs(ClientInfo clientInfo) {
        steps.featureSteps()
                .addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, true);
    }
}
