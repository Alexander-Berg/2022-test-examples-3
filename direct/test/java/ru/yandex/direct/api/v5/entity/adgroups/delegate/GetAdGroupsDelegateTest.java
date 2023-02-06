package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.GetRequest;
import com.yandex.direct.api.v5.adgroups.SmartAdGroupFieldEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.GenericGetRequest;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceMainBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceMainBanners.fullPerformanceMainBanner;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class GetAdGroupsDelegateTest {
    @Autowired
    private Steps steps;

    @Autowired
    private ApiAuthenticationSource auth;

    @Autowired
    private GetAdGroupsDelegate delegate;

    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();

        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
    }

    @After
    public void resetMocks() {
        Mockito.reset(auth);
    }

    @Test
    public void processList_performanceAdGroup_withoutCustomFields() {
        var adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var adGroupsCriteria = new com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria().withIds(adGroupId);
        GenericGetRequest<AdGroupAnyFieldEnum, AdGroupsSelectionCriteria> genericGetRequest =
                delegate.convertRequest(new GetRequest().withSelectionCriteria(adGroupsCriteria));
        List<AdGroup> adGroups = delegate.get(genericGetRequest);
        assertThat(adGroups).hasSize(1);
    }

    @Test
    public void processList_performanceAdGroup_withCustomFields() {
        var adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var adGroupId = adGroupInfo.getAdGroupId();
        var adGroupsCriteria = new com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria().withIds(adGroupId);
        GetRequest externalRequest = new GetRequest()
                .withSelectionCriteria(adGroupsCriteria)
                .withSmartAdGroupFieldNames(SmartAdGroupFieldEnum.FEED_ID);
        GenericGetRequest<AdGroupAnyFieldEnum, AdGroupsSelectionCriteria> genericGetRequest =
                delegate.convertRequest(externalRequest);
        List<AdGroup> adGroups = delegate.get(genericGetRequest);
        assertThat(adGroups).hasSize(1);
        var actualAdGroup = (PerformanceAdGroup) adGroups.get(0);
        var expectedAdGroup = (PerformanceAdGroup) adGroupInfo.getAdGroup();
        assertThat(actualAdGroup.getFeedId()).isEqualTo(expectedAdGroup.getFeedId());
    }

    @Test
    public void processList_performanceAdGroup_withCustomFields_noCreatives() {
        var adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var bannerImageFormat = steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo());
        var bannerInfo = steps.performanceMainBannerSteps()
                .createPerformanceMainBanner(new NewPerformanceMainBannerInfo()
                        .withAdGroupInfo(adGroupInfo)
                        .withBanner(fullPerformanceMainBanner()
                                .withLogoImageHash(bannerImageFormat.getImageHash())
                                .withLogoStatusModerate(BannerLogoStatusModerate.SENT)));

        var adGroupId = adGroupInfo.getAdGroupId();
        var adGroupsCriteria = new com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria().withIds(adGroupId);
        GetRequest externalRequest = new GetRequest()
                .withSelectionCriteria(adGroupsCriteria)
                .withSmartAdGroupFieldNames(SmartAdGroupFieldEnum.FEED_ID, SmartAdGroupFieldEnum.LOGO_EXTENSION_HASH);
        GenericGetRequest<AdGroupAnyFieldEnum, AdGroupsSelectionCriteria> genericGetRequest =
                delegate.convertRequest(externalRequest);

        List<AdGroup> adGroups = delegate.get(genericGetRequest);

        assertThat(adGroups).hasSize(1);
        var actualAdGroup = (PerformanceAdGroup) adGroups.get(0);
        var actualBanners = actualAdGroup.getBanners();
        var expectedAdGroup = (PerformanceAdGroup) adGroupInfo.getAdGroup();
        var expectedBanner = (PerformanceBannerMain) bannerInfo.getBanner();
        assertSoftly(softly -> {
           softly.assertThat(actualAdGroup.getFeedId()).isEqualTo(expectedAdGroup.getFeedId());
           softly.assertThat(actualBanners).hasSize(1);
           softly.assertThat(actualBanners.get(0)).satisfies(actualBanner -> {
               assertThat(actualBanner).isNotNull();
               assertThat(actualBanner).isInstanceOf(PerformanceBannerMain.class);
               var actualPerformanceMainBanner = (PerformanceBannerMain) actualBanner;
               assertThat(actualPerformanceMainBanner.getLogoImageHash()).isEqualTo(expectedBanner.getLogoImageHash());
           });
        });
    }
}
