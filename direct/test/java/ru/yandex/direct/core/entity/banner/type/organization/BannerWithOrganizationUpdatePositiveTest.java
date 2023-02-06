package ru.yandex.direct.core.entity.banner.type.organization;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithOrganization;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestOrganizationRepository;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.TextBanner.PERMALINK_ID;
import static ru.yandex.direct.core.entity.banner.model.TextBanner.PREFER_V_CARD_OVER_PERMALINK;
import static ru.yandex.direct.core.entity.banner.model.TextBanner.VCARD_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;
import static ru.yandex.direct.feature.FeatureName.IS_ENABLE_PREFER_V_CARD_OVER_PERMALINK;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithOrganizationUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithOrganization> {

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
    public void addPermalinkIdForTextBanner() {
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), defaultClient);
        var organization = defaultActiveOrganization(bannerInfo.getClientId());
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(organization.getPermalinkId(), PERMALINK_ID);
        organizationsClient.addUidsByPermalinkId(organization.getPermalinkId(), List.of(chiefUid));
        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getPermalinkId(), equalTo(organization.getPermalinkId()));
    }

    @Test
    public void changePermalinkIdForTextBanner() {
        var organization1 = defaultActiveOrganization(defaultClient.getClientId());
        var organization2 = defaultActiveOrganization(defaultClient.getClientId());
        organizationsClient.addUidsByPermalinkId(organization1.getPermalinkId(), List.of(chiefUid));
        organizationsClient.addUidsByPermalinkId(organization2.getPermalinkId(), List.of(chiefUid));

        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner()
                .withPermalinkId(organization1.getPermalinkId()), defaultClient);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(organization2.getPermalinkId(), PERMALINK_ID);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getPermalinkId(), equalTo(organization2.getPermalinkId()));
    }

    @Test
    public void changePreferedVcardFlagForTextBanner() {
        steps.featureSteps()
                .addClientFeature(defaultClient.getClientId(), IS_ENABLE_PREFER_V_CARD_OVER_PERMALINK, true);

        var organization = defaultActiveOrganization(defaultClient.getClientId());
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner()
                .withPermalinkId(organization.getPermalinkId())
                .withPreferVCardOverPermalink(false), defaultClient);

        var vcard = steps.vcardSteps().createVcard(bannerInfo.getCampaignInfo());

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(true, PREFER_V_CARD_OVER_PERMALINK)
                .process(vcard.getVcardId(), VCARD_ID);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertSoftly(softly -> {
            softly.assertThat(actualBanner.getPermalinkId()).isEqualTo(organization.getPermalinkId());
            softly.assertThat(actualBanner.getPreferVCardOverPermalink())
                    .isEqualTo(true);
        });
    }

    @Test
    public void addPermalinkIdWithExistingAutoPermalinkForTextBanner() {
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), defaultClient);
        var organization = defaultActiveOrganization(bannerInfo.getClientId());
        testOrganizationRepository.addAutoPermalink(bannerInfo.getShard(), bannerInfo.getBannerId(),
                organization.getPermalinkId());
        organizationsClient.addUidsByPermalinkId(organization.getPermalinkId(), List.of(chiefUid));

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(organization.getPermalinkId(), PERMALINK_ID);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getPermalinkId(), equalTo(organization.getPermalinkId()));
    }

    @Test
    public void addPermalinkIdWithExistingAnotherAutoPermalinkForTextBanner() {
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), defaultClient);
        var organization1 = defaultActiveOrganization(bannerInfo.getClientId());
        var organization2 = defaultActiveOrganization(bannerInfo.getClientId());
        testOrganizationRepository.addAutoPermalink(bannerInfo.getShard(), bannerInfo.getBannerId(),
                organization1.getPermalinkId());
        organizationsClient.addUidsByPermalinkId(organization1.getPermalinkId(), List.of(chiefUid));
        organizationsClient.addUidsByPermalinkId(organization2.getPermalinkId(), List.of(chiefUid));

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(organization2.getPermalinkId(), PERMALINK_ID);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getPermalinkId(), equalTo(organization2.getPermalinkId()));
    }

    @Test
    public void updateOneOrganization() {
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), defaultClient);

        var organizationId = 123L;
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(organizationId, PERMALINK_ID);
        organizationsClient.addUidsByPermalinkId(organizationId, List.of(chiefUid));
        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getPermalinkId(), equalTo(organizationId));

        Organization result = organizationRepository
                .getOrganizationsByBannerIds(defaultClient.getShard(), defaultClient.getClientId(),
                        singletonList(bannerInfo.getBannerId()))
                .get(bannerInfo.getBannerId());
        assertThat(result, notNullValue());
        assertThat(result.getPermalinkId(), is(organizationId));
    }

    @Test
    public void updateTwoOrganizations() {
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), defaultClient);
        var bannerInfo2 = steps.bannerSteps().createBanner(activeTextBanner(), bannerInfo.getAdGroupInfo());
        var organizationId = 123L;
        var modelChanges1 = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(organizationId, PERMALINK_ID);
        var modelChanges2 = new ModelChanges<>(bannerInfo2.getBannerId(), TextBanner.class)
                .process(organizationId, PERMALINK_ID);
        organizationsClient.addUidsByPermalinkId(organizationId, List.of(chiefUid));
        List<Long> ids = prepareAndApplyValid(List.of(modelChanges1, modelChanges2));
        for (var id : ids) {
            TextBanner actualBanner = getBanner(id, TextBanner.class);
            assertThat(actualBanner.getPermalinkId(), equalTo(organizationId));

            Organization result = organizationRepository
                    .getOrganizationsByBannerIds(defaultClient.getShard(), defaultClient.getClientId(),
                            singletonList(id))
                    .get(id);
            assertThat(result, notNullValue());
            assertThat(result.getPermalinkId(), is(organizationId));
        }
    }

    @Test
    public void deleteOrganization() {
        var organization = defaultActiveOrganization(defaultClient.getClientId());
        organizationsClient.addUidsByPermalinkId(organization.getPermalinkId(), List.of(chiefUid));

        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner()
                .withPermalinkId(organization.getPermalinkId()), defaultClient);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(null, PERMALINK_ID);
        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getPermalinkId(), nullValue());

        Organization result = organizationRepository
                .getOrganizationsByBannerIds(defaultClient.getShard(), defaultClient.getClientId(),
                        singletonList(bannerInfo.getBannerId()))
                .get(bannerInfo.getBannerId());
        assertThat(result, nullValue());
    }

}
