package ru.yandex.direct.core.entity.banner.type.organization;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.MetrikaCounter;
import ru.yandex.direct.core.entity.campaign.repository.CampMetrikaCountersRepository;
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.entity.organizations.validation.OrganizationDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestOrganizationRepository;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.testing.matchers.validation.Matchers;
import ru.yandex.direct.validation.defect.CommonDefects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithOrganizationUpdatePhoneTest
        extends BannerOldBannerInfoUpdateOperationTestBase<OldTextBanner> {

    private static final long FIRST_METRIKA_COUNTER_ID = 2L;
    private static final long SECOND_METRIKA_COUNTER_ID = 22L;
    private static final long CAMPAIGN_COUNTER_ID = 99999L;

    @Autowired
    public TestOrganizationRepository testOrganizationRepository;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ClientPhoneRepository clientPhoneRepository;

    @Autowired
    private CampMetrikaCountersRepository campMetrikaCountersRepository;

    private ClientId clientId;
    private AdGroupInfo adGroupInfo;
    private long permalinkIdFirst;
    private long permalinkIdSecond;
    ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        clientId = clientInfo.getClientId();
        steps.featureSteps().addClientFeature(clientId,
                FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, true);
        var organization1 = defaultActiveOrganization(clientId);
        var organization2 = defaultActiveOrganization(clientId);
        var chiefUid = rbacService.getChiefByClientId(clientId);
        permalinkIdFirst = organization1.getPermalinkId();
        permalinkIdSecond = organization2.getPermalinkId();
        List<Long> chiefUids = List.of(chiefUid);
        organizationsClient.addUidsAndCounterIdsByPermalinkId(permalinkIdFirst, chiefUids, FIRST_METRIKA_COUNTER_ID);
        organizationsClient.addUidsAndCounterIdsByPermalinkId(permalinkIdSecond, chiefUids, SECOND_METRIKA_COUNTER_ID);

        var campaignMetrikaCounter = new MetrikaCounter()
                .withId(CAMPAIGN_COUNTER_ID)
                .withHasEcommerce(true);

        campMetrikaCountersRepository.updateMetrikaCounters(adGroupInfo.getShard(),
                Map.of(adGroupInfo.getCampaignId(), List.of(campaignMetrikaCounter)));

        reset(organizationsClient);
    }

    @Test
    public void update_organizationWithDefaultOrganizationPhone_success() {
        Long phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        var textBanner = activeTextBanner().withPermalinkId(permalinkIdFirst).withPhoneId(phoneId);
        bannerInfo = steps.bannerSteps().createBanner(textBanner, adGroupInfo);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdSecond, TextBanner.PERMALINK_ID)
                .process(null, TextBanner.PHONE_ID);
        prepareAndApplyValid(modelChanges);
        Long bannerId = bannerInfo.getBanner().getId();
        var shard = bannerInfo.getShard();
        Long bannerPhoneId = clientPhoneRepository.getPhoneIdsByBannerIds(shard, List.of(bannerId)).get(bannerId);
        assertThat(bannerPhoneId).isNull();
    }

    @Test
    public void update_organizationWithNewPhone_success() {
        long oldPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        var textBanner = activeTextBanner().withPermalinkId(permalinkIdFirst).withPhoneId(oldPhoneId);
        bannerInfo = steps.bannerSteps().createBanner(textBanner, adGroupInfo);

        Long newPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdSecond, TextBanner.PERMALINK_ID)
                .process(newPhoneId, TextBanner.PHONE_ID);
        prepareAndApplyValid(modelChanges);
        Long bannerId = bannerInfo.getBanner().getId();
        var shard = bannerInfo.getShard();
        Long bannerPhoneId = clientPhoneRepository.getPhoneIdsByBannerIds(shard, List.of(bannerId)).get(bannerId);
        assertThat(bannerPhoneId).isEqualTo(newPhoneId);
    }

    @Test
    public void update_organizationWithSamePhone_success() {
        Long phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        var textBanner = activeTextBanner().withPermalinkId(permalinkIdFirst).withPhoneId(phoneId);
        bannerInfo = steps.bannerSteps().createBanner(textBanner, adGroupInfo);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdSecond, TextBanner.PERMALINK_ID);
        prepareAndApplyValid(modelChanges);
        Long bannerId = bannerInfo.getBanner().getId();
        var shard = bannerInfo.getShard();
        Long bannerPhoneId = clientPhoneRepository.getPhoneIdsByBannerIds(shard, List.of(bannerId)).get(bannerId);
        assertThat(bannerPhoneId).isEqualTo(phoneId);
    }

    @Test
    public void update_organizationNotAccess_addManualPhone_withoutFeature_failure() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.TELEPHONY_ALLOWED, false);
        Long newPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        MassResult<Long> result = addPhoneAndCreateOperation(newPhoneId);
        assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(TextBanner.PHONE_ID.name())),
                OrganizationDefects.hasNoAccessToOrganization())
        )));
    }

    @Test
    public void update_organizationNotAccess_addManualPhone_withFeature_failure() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.TELEPHONY_ALLOWED, true);
        Long newPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        MassResult<Long> result = addPhoneAndCreateOperation(newPhoneId);
        assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(TextBanner.PHONE_ID.name())),
                OrganizationDefects.hasNoAccessToOrganization())
        )));
    }

    private MassResult<Long> addPhoneAndCreateOperation(Long newPhoneId) {
        var organization = defaultActiveOrganization(clientId);
        var permalinkId = organization.getPermalinkId();
        long oldPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        var textBanner = activeTextBanner().withPermalinkId(permalinkId).withPhoneId(oldPhoneId);
        bannerInfo = steps.bannerSteps().createBanner(textBanner, adGroupInfo);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(newPhoneId, TextBanner.PHONE_ID);
        return createOperation(modelChanges).prepareAndApply();
    }

    @Test
    public void update_organizationNotAccess_addOrgPhone_success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.TELEPHONY_ALLOWED, true);
        var organization = defaultActiveOrganization(clientId);
        var permalinkId = organization.getPermalinkId();
        var textBanner = activeTextBanner().withPermalinkId(permalinkId);
        bannerInfo = steps.bannerSteps().createBanner(textBanner, adGroupInfo);

        Long newPhoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientInfo, permalinkId).getId();
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(newPhoneId, TextBanner.PHONE_ID);
        MassResult<Long> result = createOperation(modelChanges).prepareAndApply();
        assertThat(result.getValidationResult().hasAnyErrors()).isFalse();
    }

    @Test
    public void update_orgPhone_success() {
        var oldPhoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId, permalinkIdFirst).getId();
        var textBanner = activeTextBanner().withPermalinkId(permalinkIdFirst).withPhoneId(oldPhoneId);
        bannerInfo = steps.bannerSteps().createBanner(textBanner, adGroupInfo);

        var newPhoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId, permalinkIdFirst).getId();
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(newPhoneId, TextBanner.PHONE_ID);
        MassResult<Long> result = createOperation(modelChanges).prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(Matchers.hasNoDefectsDefinitions()));
    }

    @Test
    public void update_otherOrgPhone_failure() {
        var oldPhoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId, permalinkIdFirst).getId();
        var textBanner = activeTextBanner().withPermalinkId(permalinkIdFirst).withPhoneId(oldPhoneId);
        bannerInfo = steps.bannerSteps().createBanner(textBanner, adGroupInfo);

        var newPhoneId =
                steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId, permalinkIdSecond).getId();
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(newPhoneId, TextBanner.PHONE_ID);
        MassResult<Long> result = createOperation(modelChanges).prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(TextBanner.PHONE_ID.name())),
                CommonDefects.objectNotFound())
        )));
    }

    @Test
    public void update_changeOrgToOrgWithoutAccess_failure() {
        long phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        var textBanner = activeTextBanner().withPermalinkId(permalinkIdFirst).withPhoneId(phoneId);
        bannerInfo = steps.bannerSteps().createBanner(textBanner, adGroupInfo);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdSecond, TextBanner.PERMALINK_ID);

        // Имитация того, что у пользователя нет прав на permalinkIdSecond
        when(organizationsClient.getOrganizationsUidsWithModifyPermission(anyCollection(), anyCollection(),
                anyString(), anyString())).thenReturn(Map.of(permalinkIdSecond, Collections.emptyList()));

        MassResult<Long> result = createOperation(modelChanges).prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(TextBanner.PHONE_ID.name())),
                OrganizationDefects.hasNoAccessToOrganization())
        )));
    }

    @Test
    public void update_changeOrgToOrgWithAccess_success() {
        long phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        var textBanner = activeTextBanner().withPermalinkId(permalinkIdFirst).withPhoneId(phoneId);
        bannerInfo = steps.bannerSteps().createBanner(textBanner, adGroupInfo);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdSecond, TextBanner.PERMALINK_ID);

        MassResult<Long> result = createOperation(modelChanges).prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(Matchers.hasNoDefectsDefinitions()));
    }

    @Test
    public void update_orgWithOtherOrgPhone_failure() {
        // Создадим два баннера с привязанной организацией и номером из Справочника
        var oldPhoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId, permalinkIdFirst).getId();
        var textBanner = activeTextBanner().withPermalinkId(permalinkIdFirst).withPhoneId(oldPhoneId);
        bannerInfo = steps.bannerSteps().createBanner(textBanner, adGroupInfo);
        var textBanner2 = activeTextBanner().withPermalinkId(permalinkIdFirst).withPhoneId(oldPhoneId);
        var bannerInfo2 = steps.bannerSteps().createBanner(textBanner2, adGroupInfo);
        // Одному объявлению поменяем номер на ручной
        long newManualPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        var modelChanges2 = new ModelChanges<>(bannerInfo2.getBannerId(), TextBanner.class)
                .process(newManualPhoneId, TextBanner.PHONE_ID);
        // Другому сменим организацию, оставив sprav-номер от предыдущей -- валидация не должна это пропустить
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdSecond, TextBanner.PERMALINK_ID);
        MassResult<Long> result = createOperation(List.of(modelChanges, modelChanges2)).prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(TextBanner.PHONE_ID.name())),
                CommonDefects.objectNotFound())
        )));
    }

    @Test
    public void update_orgWithManualPhone_success() {
        var oldPhoneId1 = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        var oldPhoneId2 = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        var textBanner = activeTextBanner().withPermalinkId(permalinkIdFirst).withPhoneId(oldPhoneId1);
        bannerInfo = steps.bannerSteps().createBanner(textBanner, adGroupInfo);
        var textBanner2 = activeTextBanner().withPermalinkId(permalinkIdSecond).withPhoneId(oldPhoneId2);
        var bannerInfo2 = steps.bannerSteps().createBanner(textBanner2, adGroupInfo);
        // Меняем у объявлений организации местами
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdSecond, TextBanner.PERMALINK_ID);
        var modelChanges2 = new ModelChanges<>(bannerInfo2.getBannerId(), TextBanner.class)
                .process(permalinkIdFirst, TextBanner.PERMALINK_ID);
        MassResult<Long> result = createOperation(List.of(modelChanges, modelChanges2)).prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(Matchers.hasNoDefectsDefinitions()));
    }

    @Test
    public void update_orgWithOtherOrgPhone_success() {
        var oldPhoneId1 = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId, permalinkIdFirst).getId();
        var oldPhoneId2 = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId, permalinkIdSecond).getId();
        var textBanner = activeTextBanner().withPermalinkId(permalinkIdFirst).withPhoneId(oldPhoneId1);
        bannerInfo = steps.bannerSteps().createBanner(textBanner, adGroupInfo);
        var textBanner2 = activeTextBanner().withPermalinkId(permalinkIdSecond).withPhoneId(oldPhoneId2);
        var bannerInfo2 = steps.bannerSteps().createBanner(textBanner2, adGroupInfo);
        // Меняем у объявлений организации местами
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdSecond, TextBanner.PERMALINK_ID);
        var modelChanges2 = new ModelChanges<>(bannerInfo2.getBannerId(), TextBanner.class)
                .process(permalinkIdFirst, TextBanner.PERMALINK_ID);
        MassResult<Long> result = createOperation(List.of(modelChanges, modelChanges2)).prepareAndApply();
        assertThat(result.getValidationResult()).is(matchedBy(hasDefectWithDefinition(validationError(
                path(index(0), field(TextBanner.PHONE_ID.name())),
                CommonDefects.objectNotFound())
        )));
    }
}
