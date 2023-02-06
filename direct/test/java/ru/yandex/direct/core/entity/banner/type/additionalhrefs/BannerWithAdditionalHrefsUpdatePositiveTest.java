package ru.yandex.direct.core.entity.banner.type.additionalhrefs;

import java.util.Collection;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerAdditionalHref;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithAdditionalHrefsUpdatePositiveTest extends BannerClientInfoUpdateOperationTestBase {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final BannerAdditionalHref ADDITIONAL_HREF_1 = new BannerAdditionalHref()
            .withHref("http://ya.ru");
    private static final BannerAdditionalHref ADDITIONAL_HREF_2 = new BannerAdditionalHref()
            .withHref("http://google.com");
    private static final BannerAdditionalHref ADDITIONAL_HREF_3 = new BannerAdditionalHref()
            .withHref("http://yahoo.com");

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public List<BannerAdditionalHref> oldAdditionalHrefs;

    @Parameterized.Parameter(2)
    public List<BannerAdditionalHref> newAdditionalHrefs;

    @Parameterized.Parameter(3)
    public List<BannerAdditionalHref> expectedAdditionalHrefs;

    @Parameterized.Parameter(4)
    public StatusBsSynced expectedStatusBsSynced;

    @Parameterized.Parameter(5)
    public BannerStatusModerate expectedStatusModerate;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "нет доп ссылок -> null",
                        emptyList(),
                        null,
                        emptyList(),
                        StatusBsSynced.YES,
                        BannerStatusModerate.YES
                },
                {
                        "нет доп ссылок -> empty list",
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        StatusBsSynced.YES,
                        BannerStatusModerate.YES
                },
                {
                        "нет доп ссылок -> новые ссылки",
                        emptyList(),
                        List.of(ADDITIONAL_HREF_2, ADDITIONAL_HREF_3),
                        List.of(ADDITIONAL_HREF_2, ADDITIONAL_HREF_3),
                        StatusBsSynced.NO,
                        BannerStatusModerate.READY
                },
                {
                        "есть доп ссылки -> null",
                        List.of(ADDITIONAL_HREF_1, ADDITIONAL_HREF_2),
                        null,
                        emptyList(),
                        StatusBsSynced.NO,
                        BannerStatusModerate.READY
                },
                {
                        "есть доп ссылки -> empty list",
                        List.of(ADDITIONAL_HREF_1, ADDITIONAL_HREF_2),
                        emptyList(),
                        emptyList(),
                        StatusBsSynced.NO,
                        BannerStatusModerate.READY
                },
                {
                        "есть доп ссылки -> те же ссылки",
                        List.of(ADDITIONAL_HREF_1, ADDITIONAL_HREF_2),
                        List.of(ADDITIONAL_HREF_1, ADDITIONAL_HREF_2),
                        List.of(ADDITIONAL_HREF_1, ADDITIONAL_HREF_2),
                        StatusBsSynced.YES,
                        BannerStatusModerate.YES
                },
                {
                        "есть доп ссылки -> новые ссылки",
                        List.of(ADDITIONAL_HREF_1, ADDITIONAL_HREF_2),
                        List.of(ADDITIONAL_HREF_2, ADDITIONAL_HREF_3),
                        List.of(ADDITIONAL_HREF_2, ADDITIONAL_HREF_3),
                        StatusBsSynced.NO,
                        BannerStatusModerate.READY
                },
        });
    }

    @Test
    public void test() {
        clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps()
                .addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, true);
        CpmPriceCampaign campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo);
        CpmYndxFrontpageAdGroup adGroup = steps.adGroupSteps().createDefaultAdGroupForPriceSales(campaign, clientInfo);
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, campaign);
        OldCpmBanner banner = activeCpmBanner(campaign.getId(), adGroup.getId(), creativeInfo.getCreativeId())
                .withAdditionalHrefs(toOldAdditionalHrefs(oldAdditionalHrefs));
        steps.bannerSteps().createActiveCpmBannerRaw(clientInfo.getShard(), banner, adGroup);

        ModelChanges<CpmBanner> modelChanges = ModelChanges.build(banner.getId(), CpmBanner.class,
                CpmBanner.ADDITIONAL_HREFS, newAdditionalHrefs);
        prepareAndApplyValid(modelChanges);

        CpmBanner actualBanner = getBanner(banner.getId());
        assertThat(actualBanner.getAdditionalHrefs()).isEqualTo(expectedAdditionalHrefs);
        assertThat(actualBanner.getStatusBsSynced()).isEqualTo(expectedStatusBsSynced);
        assertThat(actualBanner.getStatusModerate()).isEqualTo(expectedStatusModerate);
    }

    private List<OldBannerAdditionalHref> toOldAdditionalHrefs(List<BannerAdditionalHref> additionalHrefs) {
        return mapList(additionalHrefs,
                additionalHref -> new OldBannerAdditionalHref().withHref(additionalHref.getHref()));
    }
}
