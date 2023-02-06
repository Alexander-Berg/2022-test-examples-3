package ru.yandex.direct.grid.processing.service.operator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.common.util.TuneSecretKey;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.office.CityContact;
import ru.yandex.direct.core.entity.client.model.office.OfficeContact;
import ru.yandex.direct.core.entity.client.model.office.RegionContact;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.client.GdClientMccCommonInfo;
import ru.yandex.direct.grid.processing.model.client.GdOfficeContactData;
import ru.yandex.direct.grid.processing.model.client.GdOfficeInfo;
import ru.yandex.direct.grid.processing.model.client.GdOperatorAccess;
import ru.yandex.direct.grid.processing.model.client.GdOperatorAction;
import ru.yandex.direct.grid.processing.model.client.GdOperatorFeatures;
import ru.yandex.direct.grid.processing.model.client.GdOperatorInfo;
import ru.yandex.direct.grid.processing.model.client.GdUserInfo;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.processing.service.operator.UserDataConverter.USER_YAPIC_AVATAR_TEMPLATE;
import static ru.yandex.direct.grid.processing.service.operator.UserDataConverter.toGdClientRepresentativeType;
import static ru.yandex.direct.grid.processing.service.operator.UserDataConverter.toGdUserInfo;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OperatorDataServiceTest {

    private Client client;
    private User user;

    private User readonlyRep;
    private ClientInfo clientInfo;

    @Autowired
    private OperatorDataService operatorDataService;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private Steps steps;

    @Before
    public void initTestData() {
        var httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getHeader(eq("X-Real-IP"))).thenReturn("12.12.12.12");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));

        clientInfo = steps.clientSteps().createDefaultClient();
        user = clientInfo.getChiefUserInfo().getUser();
        readonlyRep = steps.userSteps().createReadonlyRepresentative(clientInfo).getUser();
        client = clientInfo.getClient();
    }

    @After
    public void afterTest() {
        RequestContextHolder.resetRequestAttributes();
    }


    @Test
    public void checkGdOperatorInfo() {
        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(user);

        var expectedGdOperatorInfo = getExpectedGdOperatorInfoForClient(user, user);
        expectedGdOperatorInfo.getClientMccCommonInfo().withCanUseClientMccCommonInfo(false);
        assertThat(gdOperatorInfo)
                .usingRecursiveComparison()
                .isEqualTo(expectedGdOperatorInfo);
    }

    @Test
    public void checkGdOperatorInfo_ForSuper() {
        user.setRole(RbacRole.SUPER);
        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(user);

        var expectedGdOperatorInfo = getExpectedGdOperatorInfoForClient();
        expectedGdOperatorInfo.getAccess()
                .withCanSearchPhrasesDuplicates(true)
                .withCanEditInternalAdManagers(true)
                .withCanEditInternalAdProducts(true)
                .withCanViewListOfInternalAdManagers(true)
                .withCanViewListOfInternalAdProducts(true)
                .withCanViewInternalAdsInSomeClients(true)
                .withCanEditAdDomain(true)
                .withCanViewCrmLink(true)
                .withCanAddTechAppMetrikaGoals(true)
                .withCanViewModerationDocuments(true)
                .withCanViewCmdLogs(true)
                .withCanViewMailLogs(true)
                .withCanViewManagerLogs(true)
                .withCanViewCampaignBalance(true)
                .withCanCreateCampaignBalance(true)
                .withCanViewLogviewerLink(true)
                .withCanViewModerationLink(true)
                .withCanCopyCampaignConversionStrategy(true)
                .withCanCopyCampaignsWithReport(true)
                .withCanStopCopiedCampaigns(true)
                .withCanSetCampaignDisallowedPageIds(true);

        assertThat(gdOperatorInfo)
                .usingRecursiveComparison()
                .isEqualTo(expectedGdOperatorInfo);
    }

    @Test
    public void checkGdOperatorInfo_ForSuperReader() {
        user.setRole(RbacRole.SUPERREADER);
        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(user);

        var expectedGdOperatorInfo = getExpectedGdOperatorInfoForClient();
        expectedGdOperatorInfo.getAccess()
                .withCanPay(false)
                .withCanResumeAutopay(false)
                .withCanViewPayCampaigns(true)
                .withCanSearchPhrasesDuplicates(true)
                .withCanViewListOfInternalAdManagers(true)
                .withCanViewListOfInternalAdProducts(true)
                .withCanViewInternalAdsInSomeClients(true)
                .withCanEditAdDomain(true)
                .withCanViewCrmLink(true)
                .withCanViewModerationDocuments(true)
                .withCanViewCmdLogs(true)
                .withCanViewMailLogs(true)
                .withCanViewManagerLogs(true)
                .withCanViewCampaignBalance(true)
                .withCanCreateCampaignBalance(true)
                .withCanViewLogviewerLink(true)
                .withCanViewModerationLink(true)
                .withCanEditAutopay(false);

        assertThat(gdOperatorInfo)
                .usingRecursiveComparison()
                .isEqualTo(expectedGdOperatorInfo);
    }

    @Test
    public void checkGdOperatorInfo_ForSupport() {
        user.setRole(RbacRole.SUPPORT);
        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(user);

        var expectedGdOperatorInfo = getExpectedGdOperatorInfoForClient();
        expectedGdOperatorInfo.getAccess()
                .withCanSearchPhrasesDuplicates(true)
                .withCanEditAdDomain(true)
                .withCanViewCrmLink(true)
                .withCanAddTechAppMetrikaGoals(false)
                .withCanViewPayCampaigns(true)
                .withCanResumeAutopay(false)
                .withCanViewModerationDocuments(true)
                .withCanViewCmdLogs(true)
                .withCanViewMailLogs(true)
                .withCanViewManagerLogs(true)
                .withCanViewCampaignBalance(true)
                .withCanViewLogviewerLink(true)
                .withCanViewModerationLink(true)
                .withCanCopyCampaignConversionStrategy(true)
                .withCanCopyCampaignsWithReport(true)
                .withCanStopCopiedCampaigns(true)
                .withCanSetCampaignDisallowedPageIds(true);

        assertThat(gdOperatorInfo)
                .usingRecursiveComparison()
                .isEqualTo(expectedGdOperatorInfo);
    }

    @Test
    public void checkGdOperatorInfo_ForManager() {
        user.setRole(RbacRole.MANAGER);
        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(user);

        var expectedGdOperatorInfo = getExpectedGdOperatorInfoForClient();
        expectedGdOperatorInfo.getAccess()
                .withCanResumeAutopay(false)
                .withCanViewPayCampaigns(true)
                .withCanSearchPhrasesDuplicates(true)
                .withCanEditAdDomain(true)
                .withCanViewCrmLink(true)
                .withCanAddTechAppMetrikaGoals(true)
                .withCanViewModerationDocuments(true)
                .withCanViewCmdLogs(true)
                .withCanViewMailLogs(true)
                .withCanViewManagerLogs(true)
                .withCanViewCampaignBalance(true)
                .withCanCreateCampaignBalance(true)
                .withCanViewModerationLink(true)
                .withCanCopyCampaignConversionStrategy(true)
                .withCanCopyCampaignsWithReport(true)
                .withCanStopCopiedCampaigns(true)
                .withCanSetCampaignDisallowedPageIds(true);

        assertThat(gdOperatorInfo)
                .usingRecursiveComparison()
                .isEqualTo(expectedGdOperatorInfo);
    }

    @Test
    public void checkGdOperatorInfo_ForLimitedSupport() {
        user.setRole(RbacRole.LIMITED_SUPPORT);
        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(user);

        var expectedGdOperatorInfo = getExpectedGdOperatorInfoForClient();
        expectedGdOperatorInfo.getAccess()
                .withCanPay(true)
                .withCanResumeAutopay(false)
                .withCanViewPayCampaigns(true)
                .withCanSearchPhrasesDuplicates(true)
                .withCanViewCrmLink(true)
                .withCanViewModerationDocuments(true)
                .withCanViewCmdLogs(true)
                .withCanViewMailLogs(true)
                .withCanViewManagerLogs(true)
                .withCanViewCampaignBalance(true)
                .withCanCreateCampaignBalance(true)
                .withCanViewLogviewerLink(true)
                .withCanViewModerationLink(true)
                .withCanCopyCampaignConversionStrategy(true)
                .withCanStopCopiedCampaigns(true)
                .withCanSetCampaignDisallowedPageIds(true);

        assertThat(gdOperatorInfo)
                .usingRecursiveComparison()
                .isEqualTo(expectedGdOperatorInfo);
    }

    @Test
    public void checkGdOperatorInfo_ForReadonlyRep() {
        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(readonlyRep);

        var expectedGdOperatorInfo = getExpectedGdOperatorInfoForClient(readonlyRep, user);
        expectedGdOperatorInfo.getAccess()
                .withCanPay(false)
                .withCanResumeAutopay(false)
                .withCanViewPayCampaigns(false)
                .withCanSearchPhrasesDuplicates(false)
                .withCanViewListOfInternalAdManagers(false)
                .withCanViewListOfInternalAdProducts(false)
                .withCanViewInternalAdsInSomeClients(false)
                .withCanEditAdDomain(false)
                .withCanViewCrmLink(false)
                .withCanViewModerationDocuments(false)
                .withCanViewCmdLogs(false)
                .withCanViewMailLogs(false)
                .withCanViewManagerLogs(false)
                .withCanViewCampaignBalance(false)
                .withCanCreateCampaignBalance(false)
                .withCanViewLogviewerLink(false)
                .withCanViewModerationLink(false)
                .withCanEditAutopay(false);

        assertThat(gdOperatorInfo)
                .usingRecursiveComparison()
                .isEqualTo(expectedGdOperatorInfo);
    }
    @Test
    public void checkGdOperatorInfo_isFreelancerTrue_forFreelancer() {
        // делаем тестового клиента фрилансером
        steps.freelancerSteps().createFreelancer(
                new FreelancerInfo().withClientInfo(clientInfo));

        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(user);

        assertThat(gdOperatorInfo.getIsFreelancer())
                .describedAs("gdOperatorInfo.getIsFreelancer")
                .isTrue();
    }

    @Test
    public void checkGdOperatorInfo_isOlderThanWeekTrue_forOldUser() {
        user.setCreateTime(LocalDateTime.now().minusDays(7).minusMinutes(1));

        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(user);

        var expectedGdOperatorInfo = getExpectedGdOperatorInfoForClient().withIsOlderThanWeek(true);
        expectedGdOperatorInfo.getClientMccCommonInfo().withCanUseClientMccCommonInfo(false);
        assertThat(gdOperatorInfo)
                .usingRecursiveComparison()
                .isEqualTo(expectedGdOperatorInfo);
    }

    @Test
    public void checkGdOperatorInfo_isOlderThanWeekFalse_forNonOldUser() {
        user.setCreateTime(LocalDateTime.now().minusDays(7).plusMinutes(1));

        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(user);

        var expectedGdOperatorInfo = getExpectedGdOperatorInfoForClient().withIsOlderThanWeek(false);
        expectedGdOperatorInfo.getClientMccCommonInfo().withCanUseClientMccCommonInfo(false);
        assertThat(gdOperatorInfo)
                .usingRecursiveComparison()
                .isEqualTo(expectedGdOperatorInfo);
    }

    @Test
    @SuppressWarnings("null")
    public void checkOfficeInfo() {
        GdOfficeInfo gdOfficeInfo = operatorDataService.toGdOfficeInfo(OfficeContact.MSK);

        CityContact cityContact = OfficeContact.MSK.getCityContact();
        RegionContact regionContact = OfficeContact.MSK.getRegionContact();
        GdOfficeInfo expectedGdOfficeInfo = new GdOfficeInfo()
                .withCityContact(new GdOfficeContactData()
                        .withPhone(Objects.requireNonNull(cityContact).getPhone())
                        .withPhoneExtension(cityContact.getPhoneExtension())
                        .withCityName(translationService.translate(cityContact.getCity()))
                        .withDescription(translationService.translate(cityContact.getDescription()))
                )
                .withRegionContact(new GdOfficeContactData()
                        .withPhone(Objects.requireNonNull(regionContact).getPhone())
                        .withPhoneExtension(regionContact.getPhoneExtension())
                        .withDescription(translationService.translate(regionContact.getDescription()))
                );

        assertThat(gdOfficeInfo)
                .usingRecursiveComparison()
                .isEqualTo(expectedGdOfficeInfo);
    }

    @Test
    public void checkOperatorAllowedActions() {
        user.setRole(RbacRole.SUPPORT);
        GdOperatorAccess operatorAccess = operatorDataService.getOperatorAccess(user);

        Set<GdOperatorAction> expectedActions = OperatorAllowedActionsUtils.getActions(user);
        assertThat(operatorAccess.getActions())
                .usingRecursiveComparison()
                .isEqualTo(expectedActions);
    }

    @Test
    public void checkOfficeInfoForTr() {
        GdOfficeInfo gdOfficeInfo = operatorDataService.toGdOfficeInfo(OfficeContact.TR);
        assertThat(gdOfficeInfo)
                .usingRecursiveComparison()
                .isEqualTo(new GdOfficeInfo());
    }

    @Test
    public void checkNotMccOperatorClientMccCommonInfoTest() {
        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(user);
        assertSoftly(sa -> {
            assertThat(gdOperatorInfo.getClientMccInfo()).isEmpty();
            assertThat(gdOperatorInfo.getClientMccCommonInfo()).isEqualTo(
                    new GdClientMccCommonInfo()
                            .withHasManagedClients(false)
                            .withHasControlRequests(false)
                            .withCanManageControlRequests(false)
                            .withCanUseClientMccCommonInfo(false)
            );
        });
    }

    @Test
    public void checkMccOperatorClientMccCommonInfoTest() {
        var controlUserInfo = steps.userSteps().createDefaultUser();
        var managedClient1UserInfo = steps.userSteps().createDefaultUser();
        var managedClient2UserInfo = steps.userSteps().createDefaultUser();
        var managedClientId3 = steps.userSteps().createDefaultUser().getClientId();
        var managedClientId4 = steps.userSteps().createDefaultUser().getClientId();

        Set.of(managedClient1UserInfo, managedClient2UserInfo)
                .forEach(info -> steps.clientMccSteps().createClientMccLink(controlUserInfo.getClientId(), info.getClientId()));

        Set.of(managedClientId3, managedClientId4)
                .forEach(id -> steps.clientMccSteps().addMccRequest(controlUserInfo.getClientId(), id));

        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(controlUserInfo.getUser());
        assertSoftly(sa -> {
            assertThat(gdOperatorInfo.getClientMccInfo())
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyInAnyOrderElementsOf(
                    Stream.of(controlUserInfo, managedClient1UserInfo, managedClient2UserInfo)
                            .map(info -> toGdUserInfo(info.getUser()))
                            .collect(Collectors.toList())
            );
            assertThat(gdOperatorInfo.getClientMccCommonInfo()).isEqualTo(
                    new GdClientMccCommonInfo()
                            .withHasManagedClients(true)
                            .withHasControlRequests(true)
                            .withCanManageControlRequests(true)
                            .withCanUseClientMccCommonInfo(false)
            );
        });
    }

    @Test
    public void checkMccOperatorClientMccWithoutManagedClientsCommonInfoTest() {
        var controlUserInfo = steps.userSteps().createDefaultUser();
        var managedClientId1 = steps.userSteps().createDefaultUser().getClientId();
        var managedClientId2 = steps.userSteps().createDefaultUser().getClientId();

        Set.of(managedClientId1, managedClientId2)
                .forEach(id -> steps.clientMccSteps().addMccRequest(controlUserInfo.getClientId(), id));

        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(controlUserInfo.getUser());
        assertSoftly(sa -> {
            assertThat(gdOperatorInfo.getClientMccInfo()).isEmpty();
            assertThat(gdOperatorInfo.getClientMccCommonInfo()).isEqualTo(
                    new GdClientMccCommonInfo()
                            .withHasManagedClients(false)
                            .withHasControlRequests(true)
                            .withCanManageControlRequests(true)
                            .withCanUseClientMccCommonInfo(false)
            );
        });
    }

    @Test
    public void checkMccOperatorClientMccWithoutControlRequestsCommonInfoTest() {
        var controlUserInfo = steps.userSteps().createDefaultUser();
        var managedClient1UserInfo = steps.userSteps().createDefaultUser();
        var managedClient2UserInfo = steps.userSteps().createDefaultUser();

        Set.of(managedClient1UserInfo, managedClient2UserInfo)
                .forEach(info -> steps.clientMccSteps().createClientMccLink(controlUserInfo.getClientId(), info.getClientId()));

        GdOperatorInfo gdOperatorInfo = operatorDataService.getOperatorInfo(controlUserInfo.getUser());
        assertSoftly(sa -> {
            assertThat(gdOperatorInfo.getClientMccInfo())
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyInAnyOrderElementsOf(
                            Stream.of(controlUserInfo, managedClient1UserInfo, managedClient2UserInfo)
                                    .map(info -> toGdUserInfo(info.getUser()))
                                    .collect(Collectors.toList())
                    );
            assertThat(gdOperatorInfo.getClientMccCommonInfo()).isEqualTo(
                    new GdClientMccCommonInfo()
                            .withHasManagedClients(true)
                            .withHasControlRequests(false)
                            .withCanManageControlRequests(false)
                            .withCanUseClientMccCommonInfo(false)
            );
        });
    }

    private GdOperatorInfo getExpectedGdOperatorInfoForClient() {
        return getExpectedGdOperatorInfoForClient(user, user);
    }
    private GdOperatorInfo getExpectedGdOperatorInfoForClient(User operator, User operatorChief) {
        GdOfficeInfo gdOfficeInfo =
                operatorDataService.toGdOfficeInfo(operatorDataService.getOfficeContact(null));

        GdUserInfo operatorInfo = new GdUserInfo()
                .withClientId(operator.getClientId().asLong())
                .withUserId(operator.getUid())
                .withName(operator.getFio())
                .withLogin(operator.getLogin())
                .withEmail(operator.getEmail())
                .withPhone(operator.getPhone())
                .withRepresentativeType(toGdClientRepresentativeType(operator.getRepType()))
                .withAvatarUrl(String.format(USER_YAPIC_AVATAR_TEMPLATE, operator.getUid()))
                .withMetrikaCountersNum(0);

        GdUserInfo operatorChiefInfo = new GdUserInfo()
                .withClientId(operatorChief.getClientId().asLong())
                .withUserId(operatorChief.getUid())
                .withName(operatorChief.getFio())
                .withLogin(operatorChief.getLogin())
                .withEmail(operatorChief.getEmail())
                .withPhone(operatorChief.getPhone())
                .withRepresentativeType(toGdClientRepresentativeType(operatorChief.getRepType()))
                .withAvatarUrl(String.format(USER_YAPIC_AVATAR_TEMPLATE, operatorChief.getUid()))
                .withMetrikaCountersNum(0);

        return new GdOperatorInfo()
                .withOfficeInfo(gdOfficeInfo)
                .withTuneSecretKey(TuneSecretKey.generateSecretKey(operator.getUid()))
                .withIsAgency(false)
                .withIsSelfAgency(false)
                .withIsFreelancer(false)
                .withIsOlderThanWeek(false)
                .withInfo(operatorInfo)
                .withChiefInfo(operatorChiefInfo)
                .withManagerInfo(null)
                .withAgencyInfo(null)
                .withWorkCurrency(client.getWorkCurrency())
                .withClientMccInfo(List.of())
                .withClientMccCommonInfo(new GdClientMccCommonInfo()
                        .withHasManagedClients(false)
                        .withHasControlRequests(false)
                        .withCanManageControlRequests(false)
                        .withCanUseClientMccCommonInfo(!operator.getIsReadonlyRep()))
                .withFeatures(new GdOperatorFeatures()
                        .withIsCampaignCreationStepsEnabledForDna(false)
                        .withIsAdsMassEditAllowed(false)
                        .withIsBannerAimingAllowed(false)
                        .withIsBannerAimingCpmAllowed(false)
                        .withIsBannerAimingCpmYndxFrontpageAllowed(false)
                        .withIsBannerUpdateAllowed(false)
                        .withIsCpmBannerUpdateAllowed(false)
                        .withIsPerformanceAdEditAllowed(false)
                        .withIsGridEnabled(false)
                        .withIsRmpGroupsUpdateAllowed(false)
                        .withIsRmpBannersUpdateAllowed(false)
                        .withIsWebvisorEnabledForDna(false)
                        .withUcDesignForDnaEditEnabled(false)
                        .withUcDesignForDnaGridEnabled(false)
                        .withEnablePreloadAssets(false)
                        .withEnablePrefetchAssets(false)
                        .withEnableLongTermCaching(false)
                        .withIsCpmAdGroupUpdateAllowed(false)
                        .withIsAggregatedStatusAllowed(false)
                        .withIsAggregatedStatusDebugAllowed(false)
                        .withIsAggregatedStatusOpenBetaAllowed(false)
                        .withIsRetargetingGridEnabled(false)
                        .withIsUserProfilePageAllowed(false)
                        .withIsOldTextSmartEditHidden(false)
                        .withIsVideoGroupsEditAllowed(false)
                        .withIsAudioGroupsEditAllowed(false)
                        .withIsOutdoorGroupsEditAllowed(false)
                        .withIsIndoorGroupsEditAllowed(false)
                        .withIsGeoproductGroupsEditAllowed(false)
                        .withIsMcBannerDnaEnabled(false)
                        .withIsMcBannerCampaignDnaEnabled(false)
                        .withIsServiceWorkerAllowed(false)
                        .withIsTargetTagsAllowed(false)
                        .withIsKeywordsEditOnBannersPageAllowed(false)
                        .withIsPerformanceCampaignsEditAllowed(false)
                        .withIsDynamicCampaignsGroupsAndAdsEditAllowed(false)
                        .withIsGoalsOnlyWithCampaignCountersUsed(false)
                        .withIsDefaultAutobudgetAvgCpaEnabled(false)
                        .withIsDefaultAutobudgetAvgClickWithWeekBudgetEnabled(false)
                        .withIsDefaultAutobudgetRoiEnabled(false)
                        .withIsOldShowCampsHidden(false)
                        .withIsShowDnaByDefaultEnabled(false)
                        .withIsShowCampLinkByCellHoverEnabledForDna(false)
                        .withIsShowCampLinkByNameClickEnabledForDna(false)
                        .withIsShowCampLinkInGridCellEnabledForDna(false)
                        .withIsShowCampLinkInPopupEnabledForDna(false)
                        .withIsBulkOpsCampaignsEditStrategiesEnabled(false)
                        .withIsBulkOpsCampaignsEditOrganizationEnabled(false)
                        .withIsBulkOpsCampaignsEditDayBudgetEnabled(false)
                        .withIsFilterShortcutsEnabled(false)
                        .withIsFilterShortcutsForCampaignEnabled(false)
                        .withIsSuggestGeneratedGroupPhrasesForOperator(false)
                        .withIsSuggestGeneratedTitleAndSnippetForOperator(false)
                        .withIsSuggestGeneratedPhrasesBySnippetForOperator(false)
                        .withIsSuggestGeneratedImagesForOperator(false)
                        .withIsSuggestGeneratedSitelinksForOperator(false)
                        .withIsSuggestGeneratedRegionsForOperator(false)
                        .withIsShowSidebarForAllEnabledForDna(false)
                        .withIsLoadingDnaScriptsBeforeOldInterfaceScriptsEnabled(false)
                        .withIsSpellerOnEditAdEnabled(false)
                        .withIsInterClientCampaignCopyAllowed(false)
                        .withIsHoverableMenuEnabled(false)
                        .withIsInternalLinksInSameWindowEnabled(false)
                        .withIsSetCampaignDisallowedPageIdsEnabled(false)
                        .withAvailableOperatorCoreFeatures(emptySet())
                        .withIsBrandLiftCpmYndxFrontpageAllowed(false))
                .withAccess(new GdOperatorAccess()
                        .withActions(OperatorAllowedActionsUtils.getActions(operator))
                        .withCanEditAdDomain(false)
                        .withCanEditInternationalization(false)
                        .withCanSearchPhrasesDuplicates(false)
                        .withCanViewInternalAdsInSomeClients(false)
                        .withCanEditInternalAdManagers(false)
                        .withCanViewListOfInternalAdManagers(false)
                        .withCanEditInternalAdProducts(false)
                        .withCanEditAutopay(true)
                        .withCanResumeAutopay(true)
                        .withCanPay(true)
                        .withWalletDontShowWalletLink(false)
                        .withCanViewListOfInternalAdProducts(false)
                        .withCanViewCrmLink(false)
                        .withCanAddTechAppMetrikaGoals(false)
                        .withCanViewPayCampaigns(false)
                        .withCanViewMailLogs(false)
                        .withCanViewManagerLogs(false)
                        .withCanViewCmdLogs(false)
                        .withCanViewModerationDocuments(false)
                        .withCanCreateCampaignBalance(false)
                        .withCanViewCampaignBalance(false)
                        .withCanViewLogviewerLink(false)
                        .withCanViewModerationLink(false)
                        .withCanOnlyCopyWithinSubclient(false)
                        .withCanCopyCampaignConversionStrategy(false)
                        .withCanCopyCampaignsWithReport(false)
                        .withCanStopCopiedCampaigns(false)
                        .withCanSetCampaignDisallowedPageIds(false));
    }

}
