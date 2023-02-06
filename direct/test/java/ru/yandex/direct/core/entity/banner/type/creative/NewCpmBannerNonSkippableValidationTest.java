package ru.yandex.direct.core.entity.banner.type.creative;

import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerWithCreative;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeConstants;
import ru.yandex.direct.core.entity.user.model.User;
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
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentCreativeAndCpmVideoAdGroupSkippability;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultNonSkippableCpmVideoCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.activeNonSkippableCpmVideoAdGroup;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class NewCpmBannerNonSkippableValidationTest {
    private static final long DEFAULT_CREATIVE_ID = 5L;
    private static final long DEFAULT_CAMPAIGN_ID = 6L;
    private static final long DEFAULT_AD_GROUP_ID = 7L;
    private static final long DEFAULT_CLIENT_ID = 8L;

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
                        "NewCpmBannerCreativeValidator. Success",
                        activeNonSkippableCpmVideoAdGroup(null),
                        new CpmBanner(),
                        defaultNonSkippableCpmVideoCreative(null, null)
                                .withLayoutId(CreativeConstants.NON_SKIPPABLE_CPM_VIDEO_LAYOUT_ID.span().lowerEndpoint()),
                        null
                },
                {
                        "Interactive preset. Success",
                        activeNonSkippableCpmVideoAdGroup(null),
                        new CpmBanner(),
                        defaultNonSkippableCpmVideoCreative(null, null)
                                .withLayoutId(8L),
                        null
                },
                {
                        "NewCpmBannerCreativeValidator inconsistentCreativeAndCpmVideoAdGroupSkippability",
                        activeNonSkippableCpmVideoAdGroup(null),
                        new CpmBanner(),
                        defaultNonSkippableCpmVideoCreative(null,null)
                                .withLayoutId(CreativeConstants.NON_SKIPPABLE_CPM_VIDEO_LAYOUT_ID.span().lowerEndpoint() - 1),
                        inconsistentCreativeAndCpmVideoAdGroupSkippability()
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
        CpmBannerCampaign cpmBannerCampaign = defaultCpmBannerCampaign()
                .withClientId(clientInfo.getClientId().asLong())
                .withUid(clientInfo.getUid())
                .withId(DEFAULT_CAMPAIGN_ID);
        adGroup.setCampaignId(cpmBannerCampaign.getId());

        Map<Long, Creative> creativesByIds = emptyMap();
        if (creative != null) {
            creativesByIds = Map.of(creative.getId(), creative);
        }

        BannerWithCreativeValidationContainer container = mock(BannerWithCreativeValidationContainer.class);
        when(container.getCreativesByIds())
                .thenReturn(creativesByIds);
        when(container.getCampaignType(any()))
                .thenReturn(cpmBannerCampaign.getType());
        when(container.getCampaignId(any()))
                .thenReturn(cpmBannerCampaign.getId());
        when(container.getAdGroupType(any()))
                .thenReturn(adGroup.getType());
        when(container.getAdGroupId(any()))
                .thenReturn(adGroup.getId());
        when(container.isNonSkippableCpmVideoAdGroup(any()))
                .thenReturn(true);
        return container;
    }

}
