package ru.yandex.direct.core.entity.banner.type.phone;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestOrganizationRepository;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.StatusBsSynced.NO;
import static ru.yandex.direct.core.entity.banner.model.BannerWithOrganizationAndPhone.PERMALINK_ID;
import static ru.yandex.direct.core.entity.banner.model.BannerWithOrganizationAndPhone.PHONE_ID;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;


@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithOrganizationAndPhoneUpdatePositiveTest extends BannerNewBannerInfoUpdateOperationTestBase {

    @Autowired
    public TestOrganizationRepository testOrganizationRepository;

    private ClientInfo defaultClient;
    private long chiefUid;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    public OrganizationRepository organizationRepository;

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createDefaultClient();
        chiefUid = rbacService.getChiefByClientId(defaultClient.getClientId());
    }

    @Test
    public void linkPhoneIdsToBanners() {
        bannerInfo = steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withBanner(fullTextBanner()
                        .withPermalinkId(null)
                        .withPhoneId(null))
                .withClientInfo(defaultClient));
        var organization = defaultActiveOrganization(bannerInfo.getClientId());
        organizationsClient.addUidsByPermalinkId(organization.getPermalinkId(), List.of(chiefUid));

        ClientPhone phone = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(defaultClient.getClientId(),
                organization.getPermalinkId());
        long newPhoneId = phone.getId();
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(organization.getPermalinkId(), PERMALINK_ID)
                .process(newPhoneId, PHONE_ID);
        Long id = prepareAndApplyValid(modelChanges);

        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getPhoneId(), equalTo(newPhoneId));
    }

    @Test
    public void unlinkPhoneIdFromBanner() {
        var organization = defaultActiveOrganization(defaultClient.getClientId());
        organizationsClient.addUidsByPermalinkId(organization.getPermalinkId(), List.of(chiefUid));

        Long oldPhoneId = 1L;
        Long newPhoneId = null;
        bannerInfo = steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withBanner(fullTextBanner()
                        .withPermalinkId(organization.getPermalinkId())
                        .withPreferVCardOverPermalink(false)
                        .withPhoneId(oldPhoneId))
                .withClientInfo(defaultClient));

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(newPhoneId, PHONE_ID);
        Long id = prepareAndApplyValid(modelChanges);

        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getPhoneId(), equalTo(newPhoneId));
        assertThat(actualBanner.getStatusBsSynced(), equalTo(NO));
    }

    @Test
    public void prepareAndApply_BannerWithOrganization_PhoneIdChanged() {
        var organization = defaultActiveOrganization(defaultClient.getClientId());
        organizationsClient.addUidsByPermalinkId(organization.getPermalinkId(), List.of(chiefUid));

        Long oldPhoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(defaultClient.getClientId(),
                organization.getPermalinkId()).getId();
        Long newPhoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(defaultClient.getClientId(),
                organization.getPermalinkId()).getId();
        bannerInfo = steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withBanner(fullTextBanner()
                        .withPermalinkId(organization.getPermalinkId())
                        .withPreferVCardOverPermalink(false)
                        .withPhoneId(oldPhoneId))
                .withClientInfo(defaultClient));

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(newPhoneId, PHONE_ID);
        Long id = prepareAndApplyValid(modelChanges);

        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getPhoneId(), equalTo(newPhoneId));
        assertThat(actualBanner.getStatusBsSynced(), equalTo(NO));
    }


    @Test
    public void update_titleWithNotAccessPermalink_success() {
        ClientId clientId = defaultClient.getClientId();
        var organization = defaultActiveOrganization(clientId);
        Long permalinkId = organization.getPermalinkId();

        long phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        var banner = fullTextBanner()
                .withPermalinkId(permalinkId)
                .withPreferVCardOverPermalink(false)
                .withPhoneId(phoneId);
        bannerInfo = steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withBanner(banner)
                .withClientInfo(defaultClient));
        steps.featureSteps().addClientFeature(clientId,
                FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, true);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process("new title", TextBanner.TITLE);
        MassResult<Long> result = createOperation(modelChanges).prepareAndApply();
        assertThat(result.getValidationResult().hasAnyErrors(), is(false));
    }


}
