package ru.yandex.direct.core.entity.banner.type.creative;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.BannerWithCreative;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestPricePackages;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.priceSalesDisallowedCreativeTemplate;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredCreativesWithCpmVideoTypeOnly;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmPriceCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultBannerstorageCreative;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmVideoForCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmVideoAdGroup;
import static ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class CpmBannerCreativePriceSalesValidatorTest {
    private static final long DEFAULT_CREATIVE_ID = 5L;
    private static final long DEFAULT_CAMPAIGN_ID = 6L;
    private static final long DEFAULT_AD_GROUP_ID = 7L;
    private static final long DEFAULT_CLIENT_ID = 8L;
    private static final long DEFAULT_PRICE_PACKAGE_ID = 9L;

    private BannerWithCreativeValidatorProvider serviceUnderTest = new BannerWithCreativeValidatorProvider();

    @Parameterized.Parameter(0)
    public String testName;

    @Parameterized.Parameter(1)
    public AdGroup adGroup;

    @Parameterized.Parameter(2)
    public BannerWithCreative banner;

    @Parameterized.Parameter(3)
    public Creative creative;

    @Parameterized.Parameter(4)
    public Defect defect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "NewCpmBannerCreativeValidator requiredCreativesWithCpmVideoTypeOnly",
                        activeCpmVideoAdGroup(null),
                        new CpmBanner(),
                        defaultHtml5(null,null),
                        requiredCreativesWithCpmVideoTypeOnly()
                },
                {
                        "NewCpmBannerCreativeValidator. Success",
                        activeCpmVideoAdGroup(null),
                        new CpmBanner(),
                        defaultCpmVideoForCpmBanner(null, null)
                                .withLayoutId(TestPricePackages.DEFAULT_ALLOWED_CREATIVE_TYPES.get(
                                        CreativeType.CPM_VIDEO_CREATIVE).get(0)),
                        null
                },
                {
                        "NewCpmBannerCreativeValidator priceSalesDisallowedCreativeTemplate",
                        activeCpmVideoAdGroup(null),
                        new CpmBanner(),
                        defaultCpmVideoForCpmBanner(null,null),
                        priceSalesDisallowedCreativeTemplate()
                },
                {
                        "NewCpmBannerCreativeValidator. Valid creative template id. Success",
                        activeCpmVideoAdGroup(null),
                        new CpmBanner(),
                        defaultBannerstorageCreative(null,null)
                                .withTemplateId(TestPricePackages.DEFAULT_ALLOWED_CREATIVE_TYPES.get(
                                CreativeType.BANNERSTORAGE).get(0)),
                        null
                },
        });
    }

    @Test
    public void validate() {

        creative.setId(DEFAULT_CREATIVE_ID);

        var container = initAdGroupAndContainer(adGroup, creative);
        fillBanner(banner, creative.getId(), adGroup );

        validateAndCheckDefect(banner, container, defect);
    }

    private void validateAndCheckDefect(BannerWithCreative banner,
                                        BannerWithCreativeValidationContainer container,
                                        Defect defect) {

        ValidationResult<BannerWithCreative, Defect> validationResult =
                serviceUnderTest.creativeValidator(container).apply(banner);

        if (defect != null) {
            assertThat(validationResult,
                    hasDefectDefinitionWith(validationError(
                            path(field(BannerWithCreative.CREATIVE_ID)), defect)));
        } else {
            assertThat(validationResult, hasNoDefectsDefinitions());
        }
    }

    private void fillBanner(BannerWithCreative banner, Long creativeId, AdGroup adGroup) {
        banner
                .withCreativeId(creativeId)
                .withAdGroupId(adGroup.getId())
                .withCampaignId(adGroup.getCampaignId());
    }

    private BannerWithCreativeValidationContainer initAdGroupAndContainer(AdGroup adGroup, Creative creative) {

        adGroup.setId(DEFAULT_AD_GROUP_ID);

        ClientInfo clientInfo = new ClientInfo()
                .withChiefUserInfo(new UserInfo().withUser(new User().withUid(1L)))
                .withClient(defaultClient().withClientId(DEFAULT_CLIENT_ID));
        PricePackage pricePackage = defaultPricePackage()
                .withId(DEFAULT_PRICE_PACKAGE_ID)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO));
        CpmPriceCampaign cpmPriceCampaign = defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                .withId(DEFAULT_CAMPAIGN_ID);
        adGroup.setCampaignId(cpmPriceCampaign.getId());

        Map<Long, Creative> creativesByIds = emptyMap();
        if (creative != null) {
            creativesByIds = Map.of(creative.getId(), creative);
        }

        BannerWithCreativeValidationContainer container = mock(BannerWithCreativeValidationContainer.class);
        when(container.getCreativesByIds())
                .thenReturn(creativesByIds);
        when(container.getCampaignType(any()))
                .thenReturn(cpmPriceCampaign.getType());
        when(container.getCampaignId(any()))
                .thenReturn(cpmPriceCampaign.getId());
        when(container.getCpmPriceCampaigns())
                .thenReturn(Map.of(cpmPriceCampaign.getId(), cpmPriceCampaign));
        when(container.getAdGroupType(any()))
                .thenReturn(adGroup.getType());
        when(container.getAdGroupId(any()))
                .thenReturn(adGroup.getId());
        when(container.getPricePackages())
                .thenReturn(Map.of(pricePackage.getId(), pricePackage));
        return container;
    }

}
