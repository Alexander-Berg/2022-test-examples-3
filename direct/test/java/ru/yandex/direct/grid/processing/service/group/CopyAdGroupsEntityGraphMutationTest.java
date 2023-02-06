package ru.yandex.direct.grid.processing.service.group;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.copyentity.translations.RenameProcessor;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.BannerButtonStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.repository.TestOrganizationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.group.mutation.GdCopyAdGroups;
import ru.yandex.direct.grid.processing.model.group.mutation.GdCopyAdGroupsPayload;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.i18n.I18NBundle;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupStatusArchived;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.PUBLISHED;
import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.AUTO;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.copyOrganization;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.COPY_AD_GROUPS_MUTATION_NAME;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class CopyAdGroupsEntityGraphMutationTest {

    private static final String COPY_AD_GROUPS_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    copiedAdGroupIds\n"
            + "  }\n"
            + "}";

    private static final TemplateMutation<GdCopyAdGroups, GdCopyAdGroupsPayload> COPY_MUTATION =
            new TemplateMutation<>(COPY_AD_GROUPS_MUTATION_NAME, COPY_AD_GROUPS_MUTATION_TEMPLATE,
                    GdCopyAdGroups.class, GdCopyAdGroupsPayload.class);

    private static final RecursiveComparisonConfiguration ADGROUP_COMPARE_STRATEGY =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoredFields("lastChange")
                    .build();

    private static final RecursiveComparisonConfiguration IGNORE_EXPECTED_NOT_NULL_FIELDS_STRATEGY =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreAllExpectedNullFields(true)
                    .build();

    private Integer shard;
    private User operator;
    private UserInfo userInfo;
    private GdCopyAdGroups input;
    private AdGroup expectedCopiedSameCampaignAdGroup;
    private AdGroup expectedCopiedDifferentCampaignsAdGroup;

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private BannerModifyRepository modifyRepository;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    private TestOrganizationRepository testOrganizationRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    @Autowired
    private RenameProcessor renameProcessor;

    private AdGroupInfo adGroupInfo;
    private AdGroupInfo cpmYndxFrontpageAdGroupInfo;
    private ClientInfo clientInfo;
    private Long uid;

    private AdGroup expectedCopiedCpmYndxFrontpageAdGroup;

    @Before
    public void before() {
        userInfo = steps.userSteps().createDefaultUser();
        shard = userInfo.getShard();
        operator = userInfo.getUser();
        TestAuthHelper.setDirectAuthentication(operator);
        uid = userInfo.getUid();

        testCpmYndxFrontpageRepository.fillMinBidsTestValues();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.getClientInfo());
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        clientInfo = adGroupInfo.getClientInfo();
        input = new GdCopyAdGroups()
                .withAdGroupIds(singletonList(adGroupInfo.getAdGroupId()));

        CampaignInfo cpmYndxFrontpageCampaignInfo =
                steps.campaignSteps().createActiveCpmYndxFrontpageCampaign(userInfo.getClientInfo());
        cpmYndxFrontpageAdGroupInfo = createRarelyServedCpmYndxFrontpageAdGroup(cpmYndxFrontpageCampaignInfo);
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                shard,
                cpmYndxFrontpageAdGroupInfo.getCampaignId(),
                singletonList(FrontpageCampaignShowType.FRONTPAGE));

        expectedCopiedDifferentCampaignsAdGroup =
                adGroupRepository.getAdGroups(shard, singleton(adGroupInfo.getAdGroupId())).get(0);

        expectedCopiedDifferentCampaignsAdGroup
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NO);

        expectedCopiedSameCampaignAdGroup =
                adGroupRepository.getAdGroups(shard, singleton(adGroupInfo.getAdGroupId())).get(0);

        expectedCopiedSameCampaignAdGroup
                .withName(renameProcessor.generateAdGroupCopyName(
                        expectedCopiedSameCampaignAdGroup.getName(),
                        expectedCopiedSameCampaignAdGroup.getId(),
                        RenameProcessor.getCopyLocaleByChiefUser(userInfo.getUser())))
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NO);

        expectedCopiedCpmYndxFrontpageAdGroup = getExpectedCopiedSameCampaignAdGroup(
                cpmYndxFrontpageAdGroupInfo.getAdGroupId())
                .withBsRarelyLoaded(false);
    }

    private AdGroup getExpectedCopiedSameCampaignAdGroup(Long adGroupId) {
        AdGroup expectedCopiedAdGroup = adGroupRepository.getAdGroups(shard, singleton(adGroupId)).get(0);
        return expectedCopiedAdGroup
                .withName(renameProcessor.generateAdGroupCopyName(
                        expectedCopiedAdGroup.getName(), expectedCopiedAdGroup.getId(),
                        RenameProcessor.getCopyLocaleByChiefUser(userInfo.getUser())))
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.NO);
    }

    @Test
    public void copyAdGroups_AdGroupWithBannerWithManualPermalink_ManualPermalinkCopied() {
        Long permalinkId = 1L;
        Organization organization = new Organization()
                .withPermalinkId(permalinkId)
                .withStatusPublish(PUBLISHED)
                .withClientId(adGroupInfo.getClientId());
        organizationsClient.addUidsByPermalinkId(permalinkId, List.of(uid));

        steps.bannerSteps().createBanner(activeTextBanner().withPermalinkId(organization.getPermalinkId()),
                adGroupInfo);
        organizationRepository.addOrUpdateOrganizations(shard, singletonList(organization));

        input.setDestinationCampaignId(null);

        Long copiedAdGroupId = copyAdGroupsAndCheckResult(input, expectedCopiedSameCampaignAdGroup);

        var copiedBanners = bannerTypedRepository.getBannersByGroupIds(shard, singletonList(copiedAdGroupId));
        assumeThat(copiedBanners, hasSize(1));
        Long copiedPermalinkId = ((TextBanner) copiedBanners.get(0)).getPermalinkId();

        Assert.assertThat(copiedPermalinkId, is(organization.getPermalinkId()));
    }

    @Test
    public void copyAdGroups_AdGroupWithBannerWithAutoPermalink_AutoPermalinkNotCopied() {
        Long permalinkId = 1L;
        Organization organization = new Organization()
                .withPermalinkId(permalinkId)
                .withStatusPublish(PUBLISHED)
                .withClientId(adGroupInfo.getClientId());
        organizationsClient.addUidsByPermalinkId(permalinkId, List.of(uid));

        Long bannerId =
                steps.bannerSteps().createBanner(activeTextBanner().withPermalinkId(organization.getPermalinkId()),
                        adGroupInfo)
                        .getBannerId();
        testOrganizationRepository.changePermalinkAssignType(shard, bannerId, permalinkId, AUTO);

        input.setDestinationCampaignId(null);

        Long copiedAdGroupId = copyAdGroupsAndCheckResult(input, expectedCopiedSameCampaignAdGroup);

        var copiedBanners = bannerTypedRepository.getBannersByGroupIds(shard, singletonList(copiedAdGroupId));
        assumeThat(copiedBanners, hasSize(1));
        Long copiedPermalinkId = ((TextBanner) copiedBanners.get(0)).getPermalinkId();

        Assert.assertThat(copiedPermalinkId, nullValue());
    }

    @Test
    public void copyAdGroups_AdGroupWithBannerWithAutoAndManualPermalink_OnlyManualPermalinkCopied() {
        Long manualPermalinkId = 1L;
        Long autoPermalinkId = 2L;
        Organization organization = new Organization()
                .withPermalinkId(manualPermalinkId)
                .withStatusPublish(PUBLISHED)
                .withClientId(adGroupInfo.getClientId());
        Organization anotherOrganization = copyOrganization(organization).withPermalinkId(autoPermalinkId);
        organizationsClient.addUidsByPermalinkId(manualPermalinkId, List.of(uid));
        organizationsClient.addUidsByPermalinkId(autoPermalinkId, List.of(uid));

        Long bannerId =
                steps.bannerSteps().createBanner(activeTextBanner().withPermalinkId(organization.getPermalinkId()),
                        adGroupInfo)
                        .getBannerId();
        steps.bannerSteps().createBanner(activeTextBanner().withPermalinkId(anotherOrganization.getPermalinkId()),
                clientInfo);
        organizationRepository.addOrUpdateOrganizations(shard, asList(organization, anotherOrganization));
        testOrganizationRepository.addAutoPermalink(shard, bannerId, autoPermalinkId);

        input.setDestinationCampaignId(null);

        Long copiedAdGroupId = copyAdGroupsAndCheckResult(input, expectedCopiedSameCampaignAdGroup);

        var copiedBanners = bannerTypedRepository.getBannersByGroupIds(shard, singletonList(copiedAdGroupId));
        assumeThat(copiedBanners, hasSize(1));
        Long copiedPermalinkId = ((TextBanner) copiedBanners.get(0)).getPermalinkId();

        Assert.assertThat(copiedPermalinkId, beanDiffer(organization.getPermalinkId()));
    }

    @Test
    public void copyAdGroups_AdGroupWithBannerWithButtonAndLogo() {
        LocaleContextHolder.setLocale(I18NBundle.RU);
        String imageHash = steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo()).getImageHash();

        TextBanner oldBanner = fullTextBanner()
                .withCampaignId(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withLogoImageHash(imageHash)
                .withLogoStatusModerate(BannerLogoStatusModerate.YES)
                .withButtonAction(ButtonAction.DOWNLOAD)
                .withButtonCaption("Скачать")
                .withButtonHref("https://yandex.ru")
                .withButtonStatusModerate(BannerButtonStatusModerate.YES);

        BannerRepositoryContainer repositoryContainer = new BannerRepositoryContainer(shard);
        modifyRepository.add(dslContextProvider.ppc(shard), repositoryContainer, singletonList(oldBanner));
        input.setDestinationCampaignId(null);

        Long copiedAdGroupId = copyAdGroupsAndCheckResult(input, expectedCopiedSameCampaignAdGroup);
        TextBanner newBanner = (TextBanner) bannerTypedRepository.getBannersByGroupIds(shard,
                singleton(copiedAdGroupId)).get(0);

        assertThat(newBanner.getLogoImageHash()).isEqualTo(oldBanner.getLogoImageHash());
        assertThat(newBanner.getButtonAction()).isEqualTo(oldBanner.getButtonAction());
        assertThat(newBanner.getButtonCaption()).isEqualTo(oldBanner.getButtonCaption());
        assertThat(newBanner.getButtonHref()).isEqualTo(oldBanner.getButtonHref());
    }

    @Test
    public void copyAdGroups_Success() {
        input.setDestinationCampaignId(null);

        copyAdGroupsAndCheckResult(input, expectedCopiedSameCampaignAdGroup);
    }

    @Test
    public void copyAdGroupsWithDestinationCampaignId_Success() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.getClientInfo());
        input.setDestinationCampaignId(campaignInfo.getCampaignId());
        expectedCopiedDifferentCampaignsAdGroup.setCampaignId(campaignInfo.getCampaignId());

        copyAdGroupsAndCheckResult(input, expectedCopiedDifferentCampaignsAdGroup);
    }

    @Test
    public void copyAdGroupsWithStoppedBanner_Success() {
        steps.bannerSteps().createBanner(activeTextBanner().withStatusShow(false), adGroupInfo);
        input.setDestinationCampaignId(null);

        copyAdGroupsAndCheckResult(input, expectedCopiedSameCampaignAdGroup);
    }

    @Test
    public void copyAdGroups_AdGroupWithArchivedBanner_NotCopied() {
        steps.bannerSteps().createDefaultArchivedBanner(adGroupInfo);
        input.setDestinationCampaignId(null);

        GdCopyAdGroupsPayload payload = copyAdGroups(input);
        assertThat(payload.getCopiedAdGroupIds()).isEmpty();

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdCopyAdGroups.AD_GROUP_IDS), index(0)), adGroupStatusArchived())
                .withWarnings(null);

        assertThat(payload.getValidationResult())
                .usingRecursiveComparison(IGNORE_EXPECTED_NOT_NULL_FIELDS_STRATEGY)
                .isEqualTo(expectedGdValidationResult);
    }

    @Test
    public void copyAdGroups_CpmYndxFrontpageAdGroup_EditCpmFeatureEnabled() {
        GdCopyAdGroups gdCopyAdGroups = new GdCopyAdGroups()
                .withAdGroupIds(singletonList(cpmYndxFrontpageAdGroupInfo.getAdGroupId()))
                .withDestinationCampaignId(cpmYndxFrontpageAdGroupInfo.getCampaignId());
        steps.featureSteps().addClientFeature(cpmYndxFrontpageAdGroupInfo.getClientId(),
                FeatureName.EDIT_CPM_YNDX_FRONTPAGE_IN_DNA, true);

        copyAdGroupsAndCheckResult(gdCopyAdGroups, expectedCopiedCpmYndxFrontpageAdGroup);
    }

    private GdCopyAdGroupsPayload copyAdGroups(GdCopyAdGroups input) {
        return graphQlTestExecutor.doMutationAndGetPayload(COPY_MUTATION, input, operator);
    }

    private AdGroupInfo createRarelyServedCpmYndxFrontpageAdGroup(CampaignInfo campaignInfo) {
        CpmYndxFrontpageAdGroup cpmYndxFrontpageAdGroup = activeCpmYndxFrontpageAdGroup(campaignInfo.getCampaignId())
                .withBsRarelyLoaded(true);
        return createRarelyServedAdGroup(cpmYndxFrontpageAdGroup, campaignInfo);
    }

    private AdGroupInfo createRarelyServedAdGroup(AdGroup adGroup, CampaignInfo campaignInfo) {
        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(adGroup)
                .withCampaignInfo(campaignInfo);
        AdGroupInfo result = steps.adGroupSteps().createAdGroup(adGroupInfo);
        steps.adGroupSteps().setBsRarelyLoaded(shard, result.getAdGroupId(), true);
        return result;
    }

    private Long copyAdGroupsAndCheckResult(GdCopyAdGroups input, AdGroup expectedCopiedAdGroup) {
        GdCopyAdGroupsPayload payload = copyAdGroups(input);
        validateResponseSuccessful(payload);

        GdCopyAdGroupsPayload expectedPayload = new GdCopyAdGroupsPayload()
                .withValidationResult(null);
        CompareStrategy compareStrategy = DefaultCompareStrategies.allFields()
                .forFields(newPath(GdCopyAdGroupsPayload.COPIED_AD_GROUP_IDS.name()))
                .useMatcher(allOf(everyItem(notNullValue()), iterableWithSize(1)));
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload).useCompareStrategy(compareStrategy)));

        Long copiedAdGroupId = payload.getCopiedAdGroupIds().get(0);
        AdGroup copiedAdGroup = adGroupRepository.getAdGroups(shard, payload.getCopiedAdGroupIds()).get(0);

        expectedCopiedAdGroup.setGeo(copiedAdGroup.getGeo());

        expectedCopiedAdGroup.setId(copiedAdGroupId);

        expectedCopiedAdGroup.setStatusBsSynced(StatusBsSynced.NO);
        expectedCopiedAdGroup.setPriorityId(0L);
        expectedCopiedAdGroup.setStatusAutobudgetShow(true);

        assertThat(copiedAdGroup)
                .usingRecursiveComparison(ADGROUP_COMPARE_STRATEGY)
                .isEqualTo(expectedCopiedAdGroup);

        return copiedAdGroupId;
    }
}
