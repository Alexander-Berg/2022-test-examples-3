package ru.yandex.direct.core.entity.banner.type.organization;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;
import static ru.yandex.direct.feature.FeatureName.IS_ENABLE_PREFER_V_CARD_OVER_PERMALINK;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithOrganizationAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Autowired
    private RbacService rbacService;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization organization;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        var clientId = clientInfo.getClientId();
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        organization = defaultActiveOrganization(clientId);
        var chiefUid = rbacService.getChiefByClientId(clientId);
        organizationsClient.addUidsByPermalinkId(organization.getPermalinkId(), List.of(chiefUid));
    }

    @Test
    public void validOrganizationForTextBanner() {
        var banner = clientTextBanner()
                .withPermalinkId(organization.getPermalinkId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getPermalinkId(), equalTo(banner.getPermalinkId()));
    }

    @Test
    public void validOrganizationPreferedVcardFlagForTextBannerWithDisabledFeature() {
        steps.featureSteps()
                .addClientFeature(adGroupInfo.getClientId(), IS_ENABLE_PREFER_V_CARD_OVER_PERMALINK, false);

        var vcard = steps.vcardSteps().createVcard(adGroupInfo.getCampaignInfo());
        var banner = clientTextBanner()
                .withPermalinkId(organization.getPermalinkId())
                .withVcardId(vcard.getVcardId())
                .withPreferVCardOverPermalink(true)
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        TextBanner actualBanner = getBanner(id);
        assertSoftly(softly -> {
            softly.assertThat(actualBanner.getPermalinkId()).isEqualTo(organization.getPermalinkId());
            softly.assertThat(actualBanner.getPreferVCardOverPermalink())
                    .isEqualTo(false);
        });
    }

    @Test
    public void validOrganizationPreferedVcardFlagForTextBannerWithEnabledFeature() {
        steps.featureSteps()
                .addClientFeature(adGroupInfo.getClientId(), IS_ENABLE_PREFER_V_CARD_OVER_PERMALINK, true);

        var vcard = steps.vcardSteps().createVcard(adGroupInfo.getCampaignInfo());
        var banner = clientTextBanner()
                .withPermalinkId(organization.getPermalinkId())
                .withVcardId(vcard.getVcardId())
                .withPreferVCardOverPermalink(true)
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        TextBanner actualBanner = getBanner(id);
        assertSoftly(softly -> {
            softly.assertThat(actualBanner.getPermalinkId()).isEqualTo(organization.getPermalinkId());
            softly.assertThat(actualBanner.getPreferVCardOverPermalink())
                    .isEqualTo(true);
        });
    }

    @Test
    public void addBanners_withOrganization() {
        Long permalinkId = nextLong();
        organizationsClient.addUidsByPermalinkId(permalinkId, List.of(clientInfo.getUid()));

        var banner = clientTextBanner()
                .withPermalinkId(permalinkId)
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getPermalinkId(), equalTo(banner.getPermalinkId()));

        Organization organization = organizationRepository
                .getOrganizationsByBannerIds(adGroupInfo.getShard(), singletonList(id))
                .get(id);

        assertThat(organization, notNullValue());
        assertThat(organization.getPermalinkId(), is(permalinkId));
        assertThat(organization.getClientId(), is(clientInfo.getClientId()));
    }

}
