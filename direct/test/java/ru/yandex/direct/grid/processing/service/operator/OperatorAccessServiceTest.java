package ru.yandex.direct.grid.processing.service.operator;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestClients;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.processing.model.client.GdClientAccess;
import ru.yandex.direct.grid.processing.model.client.GdClientFeatures;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.client.GdTestClientAccess;
import ru.yandex.direct.grid.processing.model.client.GdUserInfo;
import ru.yandex.direct.model.Model;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.model.ClientsRelation;
import ru.yandex.direct.rbac.model.ClientsRelationType;
import ru.yandex.direct.regions.GeoTreeType;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.client.model.PhoneVerificationStatus.VERIFIED;
import static ru.yandex.direct.core.entity.feature.PerlFeatureNames.PREVIEW_ACCESS_TO_NEW_FEATURE_CLIENTS_IDS_MEMBERS_3;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaign;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.CONTENT_PROMOTION;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.CPM_BANNER;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.CPM_DEALS;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.CPM_PRICE;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.CPM_YNDX_FRONTPAGE;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.DYNAMIC;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.INTERNAL_AUTOBUDGET;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.INTERNAL_DISTRIB;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.INTERNAL_FREE;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.MCBANNER;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.MOBILE_CONTENT;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.PERFORMANCE;
import static ru.yandex.direct.grid.model.campaign.GdCampaignType.TEXT;
import static ru.yandex.direct.grid.processing.service.client.ClientDataService.createClientInfo;
import static ru.yandex.direct.grid.processing.service.operator.UserDataConverter.toGdUserInfo;
import static ru.yandex.direct.grid.processing.util.UserHelper.defaultClientNds;

@RunWith(Parameterized.class)
public class OperatorAccessServiceTest extends OperatorAccessServiceBaseTest {
    private static final int TEST_SHARD = RandomNumberUtils.nextPositiveInteger(22);
    private static final ClientId TEST_CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final ClientId TEST_CLIENT_ID_TWO = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final long TEST_USER_ID = RandomNumberUtils.nextPositiveLong();
    private static final long TEST_CHIEF_USER_ID = RandomNumberUtils.nextPositiveLong();
    private static final long TEST_CHIEF_CLIENT_ID = RandomNumberUtils.nextPositiveLong();
    private static final long AGENCY_USER_ID = RandomNumberUtils.nextPositiveLong();
    private static final long AGENCY_USER_ID_TWO = RandomNumberUtils.nextPositiveLong();
    private static final long AGENCY_CLIENT_ID = RandomNumberUtils.nextPositiveLong();
    private static final long FREELANCER_USER_ID = RandomNumberUtils.nextPositiveLong();
    private static final long LIMITED_SUPPORT_USER_ID = RandomNumberUtils.nextPositiveLong();
    private static final ClientId FREELANCER_CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final ClientId LIMITED_SUPPORT_CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final Set<GdCampaignType> DEFAULT_CAN_CREATE_CAMPAIGN_TYPES = Set.of(
            TEXT, PERFORMANCE, MOBILE_CONTENT, DYNAMIC, MCBANNER, CPM_BANNER, CPM_YNDX_FRONTPAGE);
    private static final Set<GdCampaignType> SUPER_CAN_CREATE_CAMPAIGN_TYPES =
            Sets.union(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES, Set.of(CPM_DEALS));
    private static final Set<GdCampaignType> EMPTY_CAN_CREATE_CAMPAIGN_TYPES = Set.of();
    private static final ClientId MCC_CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final long MCC_USER_ID = RandomNumberUtils.nextPositiveLong();

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    static User operator() {
        return TestUsers.defaultUser()
                .withId(TEST_USER_ID)
                .withClientId(TEST_CLIENT_ID_TWO)
                .withChiefUid(TEST_CHIEF_USER_ID)
                .withUseCampDescription(false)
                .withMetrikaCountersNum(0);
    }

    static GdUserInfo chiefGdInfo() {
        return toGdUserInfo(
                TestUsers.defaultUser()
                        .withId(TEST_CHIEF_USER_ID)
                        .withClientId(TEST_CLIENT_ID_TWO)
                        .withChiefUid(TEST_CHIEF_USER_ID)
                        .withUseCampDescription(false)
                        .withMetrikaCountersNum(0)
        );
    }

    private static GdClientInfo clientInfo() {
        return createClientInfo(TEST_SHARD, TestClients.defaultClient()
                        .withId(TEST_CLIENT_ID.asLong())
                        .withChiefUid(TEST_CHIEF_USER_ID),
                Map.of(TEST_CHIEF_USER_ID, chiefGdInfo()), null, emptyMap(), emptyMap(),
                defaultClientNds(TEST_CLIENT_ID.asLong()), GeoTreeType.GLOBAL,
                null, emptySet(), emptySet(), false, emptyMap(), emptyMap(), false, VERIFIED, false);
    }

    static GdClientFeatures features() {
        return new GdClientFeatures()
                .withIsVideoConstructorEnabled(false)
                .withIsVideoConstructorCreateFromScratchEnabled(false)
                .withIsVideoConstructorFeedEnabled(false)
                .withIsVideoConstructorBeruruTemplateEnabled(false)
                .withHasEcommerce(false)
                .withHasCampaignsWithCPAStrategy(false)
                .withHasMetrikaCounters(false)
                .withIsWalletEnabled(false)
                .withCampaignIdForVCardsManagement(null)
                .withHasCampaignsWithShows(false)
                .withHasCampaignsWithStats(false)
                .withUsedCampaignTypes(Collections.emptySet())
                .withCampaignManagers(Collections.emptySet())
                .withIsInternalAdsAllowed(false)
                .withIsCpmCampaignsEnabled(false)
                .withIsMinusWordsLibEnabled(false)
                .withIsContentPromotionVideoInGridSupported(false)
                .withIsContentPromotionCollectionInGridSupported(false)
                .withIsCpmYndxFrontpageOnGridEnabled(false)
                .withIsCpmPriceCampaignEnabled(false)
                .withIsContentPromotionVideoEnabled(false)
                .withIsClientAllowedToRemoderate(false)
                .withIsConversionCenterEnabled(false)
                .withIsCrrStrategyAllowed(false);
    }

    private static GdTestClientAccess clientAccess() {
        return new GdTestClientAccess()
                .withOperatorIsClient(false)
                .withOperatorIsClientChief(false)
                .withCanCreateCampaign(false)
                .withCanCopyCampaign(false)
                .withCanMassCopyCampaign(false)
                .withCanManageFavoriteCampaigns(false)
                .withCanTransferMoney(false)
                .withCanPayByCash(false)
                .withCanViewOfflineReports(false)
                .withCanManageVCards(false)
                .withCanManageRetargertingConditions(false)
                .withCanManageTurboLandings(true)
                .withCanManageCreatives(false)
                .withCanManageFeeds(false)
                .withCanManageUsingXLS(true)
                .withCanManageUsingCommander(false)
                .withCanManageUsingApi(false)
                .withCanCheckCampaignsStats(false)
                .withCanMonitorPhrasesPositions(false)
                .withCanUploadDocuments(false)
                .withCanSeeClientRepresentatives(false)
                .withCanRequestFlService(false)
                .withCanCreateCampaignTypes(Collections.emptySet())
                .withCanViewMobileApps(true)
                .withCanViewMobileAppDomain(false)
                .withCanEditMobileApps(true)
                .withCanEditMobileAppDomain(false)
                .withCanViewInternalAds(false)
                .withCanEditInternalAds(false)
                .withCanOnlyManageInternalAds(false)
                .withCanExportToExcel(false)
                .withCanSeeDealsLinkEverywhere(false)
                .withCanSeeAgencyClients(false)
                .withCanSeeManagerClients(false)
                .withCanSeeCampaignsServicedState(false)
                .withCanMassEditAdGroupRegions(false)
                .withOperatorCanEditClientSettings(false)
                .withOperatorCanEditUserSettings(false)
                .withInfoblockEnabled(false)
                .withCanManageLibMinusWords(false)
                .withCanViewDocumentsAndPayments(false)
                .withCanBlockUser(false)
                .withCanResetFlightStatusApprove(false)
                .withCanSelfSendAdsOnRemoderation(false)
                .withCanViewConversionSources(false)
                .withCanEditConversionSources(false)
                .withCanCreateFeedFromSite(true);
    }

    @Parameterized.Parameter()
    public String testName;

    @Parameterized.Parameter(1)
    public User operator;

    @Parameterized.Parameter(2)
    public User subjectUser;

    @Parameterized.Parameter(3)
    public GdClientInfo clientInfo;

    @Parameterized.Parameter(4)
    public List<String> openFeatures;

    @Parameterized.Parameter(5)
    public GdClientFeatures usedFeatures;

    @Parameterized.Parameter(6)
    public Collection<GdiCampaign> campaigns;

    @Parameterized.Parameter(7)
    public GdClientAccess expectedAccess;

    @Parameterized.Parameter(8)
    public Condition<GdClientAccess> accessCondition;

    @Parameterized.Parameter(9)
    public Optional<ClientsRelation> internalAdManagerProductRelation;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        "Супер может создать все",
                        operator()
                                .withRole(RbacRole.SUPER),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanCreateCampaignTypes(SUPER_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanCreateCampaign(true)
                                .withCanCopyCampaign(true)
                                .withCanMassCopyCampaign(true)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanViewOfflineReports(true)
                                .withCanManageTurboLandings(true)
                                .withCanViewMobileAppDomain(true)
                                .withCanEditMobileAppDomain(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanManageCreatives(true)
                                .withCanManageFeeds(true)
                                .withCanSeeClientRepresentatives(true)
                                .withCanViewUserLogs(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withInfoblockEnabled(true)
                                .withCanUseCampDescription(true)
                                .withCanSeeCampaignsServicedState(true)
                                .withCanManageInternalAdAccessRights(true)
                                .withCanBlockUser(true),
                        null,
                        Optional.empty()
                },
                {
                        "Супер может создать все, если MCBANNER включено у клиента",
                        operator()
                                .withRole(RbacRole.SUPER),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo(),
                        singletonList(PREVIEW_ACCESS_TO_NEW_FEATURE_CLIENTS_IDS_MEMBERS_3.name()),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanCreateCampaignTypes(SUPER_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanCreateCampaign(true)
                                .withCanCopyCampaign(true)
                                .withCanMassCopyCampaign(true)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanViewOfflineReports(true)
                                .withCanManageTurboLandings(true)
                                .withCanViewMobileAppDomain(true)
                                .withCanEditMobileAppDomain(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanManageCreatives(true)
                                .withCanManageFeeds(true)
                                .withCanSeeClientRepresentatives(true)
                                .withCanViewUserLogs(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withInfoblockEnabled(true)
                                .withCanUseCampDescription(true)
                                .withCanSeeCampaignsServicedState(true)
                                .withCanManageInternalAdAccessRights(true)
                                .withCanBlockUser(true),
                        null,
                        Optional.empty()
                },
                {
                        "Суперридер и иже с ним не умеют создавать",
                        operator()
                                .withRole(RbacRole.SUPERREADER),
                        operator()
                                .withUseCampDescription(false),
                        clientInfo(),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanManageFavoriteCampaigns(false)
                                .withCanViewOfflineReports(true)
                                .withCanViewMobileAppDomain(true)
                                .withCanEditMobileApps(false)
                                .withCanSeeClientRepresentatives(true)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanCheckCampaignsStats(false)
                                .withCanViewUserLogs(true)
                                .withCanMassEditAdGroupRegions(false)
                                .withInfoblockEnabled(true)
                                .withCanManageInternalAdAccessRights(false)
                                .withOperatorCanEditUserSettings(true)
                                .withCanUseCampDescription(false)
                                .withOperatorCanEditClientSettings(true),
                        null,
                        Optional.empty()
                },
                {
                        "Саппорт может создавать часть кампаний (тест с фичами)",
                        operator()
                                .withRole(RbacRole.SUPPORT),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo(),
                        singletonList(PREVIEW_ACCESS_TO_NEW_FEATURE_CLIENTS_IDS_MEMBERS_3.name()),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanCreateCampaign(true)
                                .withCanMassCopyCampaign(true)
                                .withCanManageFavoriteCampaigns(false)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanManageFeeds(true)
                                .withCanSeeClientRepresentatives(true)
                                .withCanViewUserLogs(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withCanUseCampDescription(true)
                                .withCanManageInternalAdAccessRights(false)
                                .withCanViewMobileAppDomain(true)
                                .withCanEditMobileAppDomain(true)
                                .withCanBlockUser(true),
                        null,
                        Optional.empty()
                },
                {
                        "Саппорт может создавать часть кампаний (тест без фич)",
                        operator()
                                .withRole(RbacRole.SUPPORT),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanCreateCampaign(true)
                                .withCanMassCopyCampaign(true)
                                .withCanManageFavoriteCampaigns(false)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanManageFeeds(true)
                                .withCanSeeClientRepresentatives(true)
                                .withCanViewUserLogs(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withCanUseCampDescription(true)
                                .withCanManageInternalAdAccessRights(false)
                                .withCanViewMobileAppDomain(true)
                                .withCanEditMobileAppDomain(true)
                                .withCanBlockUser(true),
                        null,
                        Optional.empty()
                },
                {
                        "Клиент может создавать себе все, что разрешено (тест без фич)",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withOperatorIsClient(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanCreateCampaign(true)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanViewUserLogs(true)
                                .withCanRequestFlService(false)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withOperatorCanEditUserSettings(true)
                                .withInfoblockEnabled(true)
                                .withCanUseCampDescription(true)
                                .withCanManageInternalAdAccessRights(false),
                        null,
                        Optional.empty()
                },
                {
                        "Клиент может создавать себе все, что разрешено (тест с фичами)",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo(),
                        ImmutableList.of(FeatureName.CPM_BANNER.getName(),
                                PREVIEW_ACCESS_TO_NEW_FEATURE_CLIENTS_IDS_MEMBERS_3.name()),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withOperatorIsClient(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanCreateCampaign(true)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanViewUserLogs(true)
                                .withCanRequestFlService(false)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withOperatorCanEditUserSettings(true)
                                .withInfoblockEnabled(true)
                                .withCanUseCampDescription(true)
                                .withCanManageInternalAdAccessRights(false),
                        null,
                        Optional.empty()
                },
                {
                        "Главный представитель клиента может взаимодействовать с фрилансером",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID)
                                .withUid(TEST_CHIEF_USER_ID),
                        operator()
                                .withUseCampDescription(false),
                        clientInfo(),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withOperatorIsClient(true)
                                .withOperatorIsClientChief(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanCreateCampaign(true)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanSeeClientRepresentatives(true)
                                .withCanViewUserLogs(true)
                                .withCanRequestFlService(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withOperatorCanEditUserSettings(true)
                                .withInfoblockEnabled(true)
                                .withCanUseCampDescription(false)
                                .withCanManageInternalAdAccessRights(false),
                        null,
                        Optional.empty()
                },
                {
                        "Клиент под агентством не может создавать себе кампании",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo()
                                .withAgencyUserId(AGENCY_USER_ID)
                                .withAgencyClientId(AGENCY_CLIENT_ID),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withOperatorIsClient(true)
                                .withCanCreateCampaign(false)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanViewOfflineReports(true)
                                .withCanEditMobileApps(false)
                                .withCanManageUsingXLS(false)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanViewUserLogs(true)
                                .withCanMassEditAdGroupRegions(false)
                                .withInfoblockEnabled(true)
                                .withCanManageInternalAdAccessRights(false)
                                .withCanUseCampDescription(true)
                                .withOperatorCanEditUserSettings(true),
                        null,
                        Optional.empty()
                },
                {
                        "Агентство может создавать кампании своему клиенту",
                        operator()
                                .withRole(RbacRole.AGENCY)
                                .withClientId(TEST_CLIENT_ID),
                        operator()
                                .withUseCampDescription(false),
                        clientInfo()
                                .withAgencyUserId(TEST_USER_ID)
                                .withAgencyClientId(TEST_CLIENT_ID.asLong()),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanCreateCampaign(true)
                                .withCanMassCopyCampaign(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanViewOfflineReports(true)
                                .withCanViewUserLogs(true)
                                .withCanSeeDealsLinkEverywhere(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withCanSeeAgencyClients(true)
                                .withInfoblockEnabled(true)
                                .withCanManageInternalAdAccessRights(false)
                                .withOperatorCanEditUserSettings(true)
                                .withCanUseCampDescription(true)
                                .withOperatorCanEditClientSettings(true),
                        null,
                        Optional.empty()
                },
                {
                        "Супер может создавать кампании клиенту агентства",
                        operator()
                                .withRole(RbacRole.SUPER),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo()
                                .withAgencyUserId(TEST_USER_ID)
                                .withAgencyClientId(TEST_CLIENT_ID.asLong()),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanCreateCampaign(true)
                                .withCanCopyCampaign(true)
                                .withCanMassCopyCampaign(true)
                                .withCanCreateCampaignTypes(SUPER_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanViewMobileAppDomain(true)
                                .withCanEditMobileAppDomain(true)
                                .withCanViewOfflineReports(true)
                                .withCanViewUserLogs(true)
                                .withCanManageTurboLandings(true)
                                .withCanSeeClientRepresentatives(true)
                                .withCanSeeAgencyClients(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withInfoblockEnabled(true)
                                .withCanManageInternalAdAccessRights(true)
                                .withOperatorCanEditUserSettings(true)
                                .withCanUseCampDescription(true)
                                .withCanSeeCampaignsServicedState(true)
                                .withOperatorCanEditClientSettings(true)
                                .withCanBlockUser(true),
                        null,
                        Optional.empty()
                },
                {
                        "Менеджер может создавать кампании своему клиенту",
                        operator()
                                .withRole(RbacRole.MANAGER)
                                .withClientId(TEST_CLIENT_ID),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo()
                                .withManagerUserId(TEST_USER_ID)
                                .withManagersInfo(singletonList(new GdUserInfo().withUserId(TEST_USER_ID))),
                        ImmutableList.of(FeatureName.CPM_BANNER.getName(),
                                PREVIEW_ACCESS_TO_NEW_FEATURE_CLIENTS_IDS_MEMBERS_3.name()),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanCreateCampaign(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageCreatives(true)
                                .withCanManageFeeds(true)
                                .withCanViewOfflineReports(true)
                                .withCanViewMobileAppDomain(true)
                                .withCanEditMobileAppDomain(true)
                                .withCanSeeClientRepresentatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanMassCopyCampaign(true)
                                .withCanViewUserLogs(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withCanUseCampDescription(true)
                                .withCanSeeCampaignsServicedState(true)
                                .withCanManageInternalAdAccessRights(false),
                        null,
                        Optional.empty()
                },
                {
                        "Менеджер может чуть больше, если есть кампании",
                        operator()
                                .withRole(RbacRole.MANAGER)
                                .withClientId(TEST_CLIENT_ID),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo()
                                .withManagerUserId(TEST_USER_ID)
                                .withManagersInfo(singletonList(new GdUserInfo().withUserId(TEST_USER_ID))),
                        ImmutableList.of(FeatureName.CPM_BANNER.getName(),
                                PREVIEW_ACCESS_TO_NEW_FEATURE_CLIENTS_IDS_MEMBERS_3.name()),
                        features(),
                        Collections.singleton(defaultCampaign()),
                        clientAccess()
                                .withCanCreateCampaign(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageCreatives(true)
                                .withCanManageFeeds(true)
                                .withCanViewOfflineReports(true)
                                .withCanViewMobileAppDomain(true)
                                .withCanEditMobileAppDomain(true)
                                .withCanManageUsingCommander(true)
                                .withCanUploadDocuments(true)
                                .withCanSeeClientRepresentatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanMassCopyCampaign(true)
                                .withCanViewUserLogs(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withCanUseCampDescription(true)
                                .withCanSeeCampaignsServicedState(true)
                                .withCanManageInternalAdAccessRights(false),
                        null,
                        Optional.empty()
                },
                {
                        "Суперу показываем ссылку на клиентов менеджера",
                        operator()
                                .withRole(RbacRole.SUPER),
                        operator(),
                        clientInfo()
                                .withManagerUserId(RandomNumberUtils.nextPositiveLong()),
                        emptyList(),
                        features(),
                        List.of(defaultCampaign()),
                        null,
                        propertyEqualTo(GdTestClientAccess::getCanSeeManagerClients, "getCanSeeManagerClients", true),
                        Optional.empty()
                },
                {
                        "Клиент может видеть и редактировать свои моб. приложения, но не может редактировать и видеть" +
                                " домен",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features(),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewMobileApps,
                                        "getCanViewMobileApps", true),
                                propertyEqualTo(GdTestClientAccess::getCanViewMobileAppDomain,
                                        "getCanViewMobileAppDomain", false),
                                propertyEqualTo(GdTestClientAccess::getCanEditMobileApps,
                                        "getCanEditMobileApps", true),
                                propertyEqualTo(GdTestClientAccess::getCanEditMobileAppDomain,
                                        "getCanEditMobileAppDomain", false)
                        ),
                        Optional.empty()
                },
                {
                        "Субклиент-агентства может видеть, но не редактировать моб. приложения и видеть домен",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo()
                                .withAgencyUserId(AGENCY_USER_ID)
                                .withAgencyClientId(AGENCY_CLIENT_ID),
                        emptyList(),
                        features(),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewMobileApps,
                                        "getCanViewMobileApps", true),
                                propertyEqualTo(GdTestClientAccess::getCanViewMobileAppDomain,
                                        "getCanViewMobileAppDomain", false),
                                propertyEqualTo(GdTestClientAccess::getCanEditMobileApps,
                                        "getCanEditMobileApps", false),
                                propertyEqualTo(GdTestClientAccess::getCanEditMobileAppDomain,
                                        "getCanEditMobileAppDomain", false)
                        ),
                        Optional.empty()
                },
                {
                        "Супер может видеть и редактировать моб. приложения клиента с фичей и может редактировать " +
                                "домен",
                        operator()
                                .withRole(RbacRole.SUPER),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features(),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewMobileApps,
                                        "getCanViewMobileApps", true),
                                propertyEqualTo(GdTestClientAccess::getCanViewMobileAppDomain,
                                        "getCanViewMobileAppDomain", true),
                                propertyEqualTo(GdTestClientAccess::getCanEditMobileApps,
                                        "getCanEditMobileApps", true),
                                propertyEqualTo(GdTestClientAccess::getCanEditMobileAppDomain,
                                        "getCanEditMobileAppDomain", true)
                        ),
                        Optional.empty()
                },
                {
                        "Суперридер может видеть, но не редактировать моб. приложения клиента",
                        operator()
                                .withRole(RbacRole.SUPERREADER),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features(),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewMobileApps,
                                        "getCanViewMobileApps", true),
                                propertyEqualTo(GdTestClientAccess::getCanViewMobileAppDomain,
                                        "getCanViewMobileAppDomain", true),
                                propertyEqualTo(GdTestClientAccess::getCanEditMobileApps,
                                        "getCanEditMobileApps", false),
                                propertyEqualTo(GdTestClientAccess::getCanEditMobileAppDomain,
                                        "getCanEditMobileAppDomain", false)
                        ),
                        Optional.empty()
                },
                {
                        "MEDIA может видеть, но не редактировать моб. приложения клиента",
                        operator()
                                .withRole(RbacRole.MEDIA),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features(),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewMobileApps,
                                        "getCanViewMobileApps", true),
                                propertyEqualTo(GdTestClientAccess::getCanViewMobileAppDomain,
                                        "getCanViewMobileAppDomain", true),
                                propertyEqualTo(GdTestClientAccess::getCanEditMobileApps,
                                        "getCanEditMobileApps", false),
                                propertyEqualTo(GdTestClientAccess::getCanEditMobileAppDomain,
                                        "getCanEditMobileAppDomain", false)
                        ),
                        Optional.empty()
                },
                {
                        "PLACER может видеть, но не редактировать моб. приложения клиента",
                        operator()
                                .withRole(RbacRole.PLACER),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features(),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewMobileApps,
                                        "getCanViewMobileApps", true),
                                propertyEqualTo(GdTestClientAccess::getCanViewMobileAppDomain,
                                        "getCanViewMobileAppDomain", true),
                                propertyEqualTo(GdTestClientAccess::getCanEditMobileApps,
                                        "getCanEditMobileApps", false),
                                propertyEqualTo(GdTestClientAccess::getCanEditMobileAppDomain,
                                        "getCanEditMobileAppDomain", false)
                        ),
                        Optional.empty()
                },
                {
                        "Клиента с внутренней рекламой может смотреть суперридер",
                        operator().withRole(RbacRole.SUPERREADER),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features().withIsInternalAdsAllowed(true),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewInternalAds,
                                        "getCanViewInternalAds", true),
                                propertyEqualTo(GdTestClientAccess::getCanEditInternalAds,
                                        "getCanEditInternalAds", false)
                        ),
                        Optional.empty()
                },
                {
                        "Клиента с внутренней рекламой может редактировать сам клиент с фичей",
                        operator().withRole(RbacRole.CLIENT).withClientId(TEST_CLIENT_ID),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features().withIsInternalAdsAllowed(true),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewInternalAds,
                                        "getCanViewInternalAds", true),
                                propertyEqualTo(GdTestClientAccess::getCanEditInternalAds,
                                        "getCanEditInternalAds", true)
                        ),
                        Optional.empty()
                },
                {
                        "Клиента с внутренней рекламой может редактировать супер",
                        operator().withRole(RbacRole.SUPER),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features().withIsInternalAdsAllowed(true),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewInternalAds,
                                        "getCanViewInternalAds", true),
                                propertyEqualTo(GdTestClientAccess::getCanEditInternalAds,
                                        "getCanEditInternalAds", true)
                        ),
                        Optional.empty()
                },
                {
                        "Внутренняя реклама не доступна даже суперу, если у клиента нет фичи",
                        operator().withRole(RbacRole.SUPER),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features(),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewInternalAds,
                                        "getCanViewInternalAds", false),
                                propertyEqualTo(GdTestClientAccess::getCanEditInternalAds,
                                        "getCanEditInternalAds", false)
                        ),
                        Optional.empty()
                },
                {
                        "Экспорт в эксель доступен клиенту, если клиенту доступен показ Внутренней рекламы",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features().withIsInternalAdsAllowed(true),
                        Collections.emptySet(),
                        null,
                        propertyEqualTo(GdTestClientAccess::getCanExportToExcel, "getCanExportToExcel", true),
                        Optional.empty()
                },
                {
                        "Экспорт в эксель недоступен клиенту, если клиенту недоступен показ Внутренней рекламы",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features().withIsInternalAdsAllowed(false),
                        Collections.emptySet(),
                        null,
                        propertyEqualTo(GdTestClientAccess::getCanExportToExcel, "getCanExportToExcel", false),
                        Optional.empty()
                },
                {
                        "Охватная реклама не доступна клиенту, если у клиента нет фичи",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features().withIsCpmCampaignsEnabled(false),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanCreateCampaignTypes,
                                        "getCanCreateCampaignTypes",
                                        DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                        ),
                        Optional.empty()
                },
                {
                        "Охватная реклама доступна клиенту, если у клиента есть фича",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features().withIsCpmCampaignsEnabled(true),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanCreateCampaignTypes,
                                        "getCanCreateCampaignTypes",
                                        Sets.union(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES, singleton(CPM_BANNER)))
                        ),
                        Optional.empty()
                },
                {
                        "CMP_DEALS недоступна оператору, если у него нет фичи",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID),
                        operator(),
                        clientInfo(),
                        emptyList(),
                        features(),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanCreateCampaignTypes,
                                        "getCanCreateCampaignTypes", DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                        ),
                        Optional.empty()
                },
                {
                        "Фрилансер может создавать кампании своему клиенту",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withId(FREELANCER_USER_ID)
                                .withClientId(FREELANCER_CLIENT_ID),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withOperatorIsClient(false)
                                .withCanCreateCampaign(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageCreatives(true)
                                .withCanManageFeeds(true)
                                .withCanViewOfflineReports(false)
                                .withCanSeeClientRepresentatives(false)
                                .withCanManageRetargertingConditions(true)
                                .withCanCopyCampaign(false)
                                .withCanViewUserLogs(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withOperatorCanEditClientSettings(true)
                                .withOperatorCanEditUserSettings(true)
                                .withInfoblockEnabled(true)
                                .withCanUseCampDescription(true)
                                .withCanManageInternalAdAccessRights(false),
                        null,
                        Optional.empty()
                },
                {
                        "limited support может открывать страницу редактирования клиента",
                        operator()
                                .withRole(RbacRole.LIMITED_SUPPORT)
                                .withId(LIMITED_SUPPORT_USER_ID)
                                .withClientId(LIMITED_SUPPORT_CLIENT_ID),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanCreateCampaign(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanManageLibMinusWords(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withOperatorCanEditClientSettings(true)
                                .withOperatorCanEditUserSettings(true)
                                .withCanManageCreatives(true)
                                .withCanManageFeeds(true)
                                .withCanSeeClientRepresentatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanCopyCampaign(false)
                                .withCanViewUserLogs(true)
                                .withCanUseCampDescription(true),
                        null,
                        Optional.empty()
                },
                {
                        "Права администратора внутренней рекламы на обычного клиента",
                        operator().withRole(RbacRole.INTERNAL_AD_ADMIN),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanOnlyManageInternalAds(true)
                                .withCanManageTurboLandings(false)
                                .withCanViewMobileApps(false)
                                .withCanEditMobileApps(false)
                                .withCanViewLibMinusWords(false),
                        null,
                        Optional.empty()
                },
                {
                        "Права администратора внутренней рекламы на продукт внутренней рекламы",
                        operator().withRole(RbacRole.INTERNAL_AD_ADMIN),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features().withIsInternalAdsAllowed(true),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanOnlyManageInternalAds(true)
                                .withCanManageTurboLandings(false)
                                .withCanViewMobileApps(false)
                                .withCanEditMobileApps(false)
                                .withCanEditInternalAds(true)
                                .withCanViewInternalAds(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanExportToExcel(true)
                                .withCanUseCampDescription(true)
                                .withCanCreateCampaignTypes(Set.of(
                                        INTERNAL_AUTOBUDGET,
                                        INTERNAL_DISTRIB,
                                        INTERNAL_FREE))
                                .withCanCreateCampaign(true)
                                .withCanViewLibMinusWords(false),
                        null,
                        Optional.empty()
                },
                {
                        "Права администратора внутренней рекламы на самого себя",
                        operator()
                                .withRole(RbacRole.INTERNAL_AD_ADMIN)
                                .withUid(TEST_CHIEF_USER_ID)
                                .withChiefUid(TEST_CHIEF_USER_ID)
                                .withClientId(TEST_CLIENT_ID),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanOnlyManageInternalAds(true)
                                .withCanManageTurboLandings(false)
                                .withCanViewMobileApps(false)
                                .withCanEditMobileApps(false)
                                .withCanEditInternalAds(false)
                                .withCanViewInternalAds(false)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanExportToExcel(false)
                                .withCanUseCampDescription(true)
                                .withCanCreateCampaignTypes(emptySet())
                                .withCanCreateCampaign(false)
                                .withCanViewLibMinusWords(false),
                        null,
                        Optional.empty()
                },
                {
                        "Права маркетолога внутренней рекламы на продукт внутренней рекламы, к которому нет доступа",
                        operator().withRole(RbacRole.INTERNAL_AD_MANAGER),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features().withIsInternalAdsAllowed(true),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanOnlyManageInternalAds(true)
                                .withCanManageTurboLandings(false)
                                .withCanViewMobileApps(false)
                                .withCanEditMobileApps(false)
                                .withCanEditInternalAds(false)
                                .withCanViewInternalAds(true)
                                .withCanMassEditAdGroupRegions(false)
                                .withCanExportToExcel(true)
                                .withCanUseCampDescription(true)
                                .withCanCreateCampaign(false)
                                .withCanViewLibMinusWords(false),
                        null,
                        Optional.empty()
                },
                {
                        "Права маркетолога внутренней рекламы на продукт внутренней рекламы, к которому " +
                                "доступ только на чтение",
                        operator().withRole(RbacRole.INTERNAL_AD_MANAGER),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features().withIsInternalAdsAllowed(true),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanOnlyManageInternalAds(true)
                                .withCanManageTurboLandings(false)
                                .withCanViewMobileApps(false)
                                .withCanEditMobileApps(false)
                                .withCanEditInternalAds(false)
                                .withCanViewInternalAds(true)
                                .withCanMassEditAdGroupRegions(false)
                                .withCanExportToExcel(true)
                                .withCanUseCampDescription(true)
                                .withCanCreateCampaign(false)
                                .withCanViewLibMinusWords(false),
                        null,
                        Optional.of(new ClientsRelation()
                                .withClientIdFrom(TEST_CLIENT_ID_TWO.asLong())
                                .withClientIdTo(TEST_CHIEF_CLIENT_ID)
                                .withRelationType(ClientsRelationType.INTERNAL_AD_READER))
                },
                {
                        "Права маркетолога внутренней рекламы на продукт внутренней рекламы, к которому " +
                                "доступ на чтение и запись",
                        operator().withRole(RbacRole.INTERNAL_AD_MANAGER),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features().withIsInternalAdsAllowed(true),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanOnlyManageInternalAds(true)
                                .withCanManageTurboLandings(false)
                                .withCanViewMobileApps(false)
                                .withCanEditMobileApps(false)
                                .withCanEditInternalAds(true)
                                .withCanViewInternalAds(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanExportToExcel(true)
                                .withCanUseCampDescription(true)
                                .withCanCreateCampaignTypes(Set.of(INTERNAL_AUTOBUDGET,
                                        INTERNAL_DISTRIB,
                                        INTERNAL_FREE))
                                .withCanCreateCampaign(true)
                                .withCanViewLibMinusWords(false),
                        null,
                        Optional.of(new ClientsRelation()
                                .withClientIdFrom(TEST_CLIENT_ID_TWO.asLong())
                                .withClientIdTo(TEST_CHIEF_CLIENT_ID)
                                .withRelationType(ClientsRelationType.INTERNAL_AD_PUBLISHER))
                },
                {
                        "Права суперсмотрителя внутренней рекламы на обычного клиента",
                        operator().withRole(RbacRole.INTERNAL_AD_SUPERREADER),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanOnlyManageInternalAds(true)
                                .withCanManageTurboLandings(false)
                                .withCanViewMobileApps(false)
                                .withCanEditMobileApps(false)
                                .withCanViewLibMinusWords(false),
                        null,
                        Optional.empty()
                },
                {
                        "Права суперсмотрителя внутренней рекламы на продукт внутренней рекламы",
                        operator().withRole(RbacRole.INTERNAL_AD_SUPERREADER),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features().withIsInternalAdsAllowed(true),
                        Collections.emptySet(),
                        clientAccess()
                                .withCanOnlyManageInternalAds(true)
                                .withCanManageTurboLandings(false)
                                .withCanViewMobileApps(false)
                                .withCanEditMobileApps(false)
                                .withCanEditInternalAds(false)
                                .withCanViewInternalAds(true)
                                .withCanMassEditAdGroupRegions(false)
                                .withCanExportToExcel(true)
                                .withCanUseCampDescription(true)
                                .withCanCreateCampaignTypes(emptySet())
                                .withCanCreateCampaign(false)
                                .withCanViewLibMinusWords(false),
                        null,
                        Optional.empty()
                },
                {
                        "Если у клиента включена фича CPM_PRICE_CAMPAIGNS - можно создавать прайсовые кампании. " +
                                "Оператор - клиент",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features()
                                .withIsCpmPriceCampaignEnabled(true),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanCreateCampaignTypes,
                                        "getCanCreateCampaignTypes",
                                        Sets.union(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES, singleton(CPM_PRICE))),
                                propertyEqualTo(GdTestClientAccess::getCanResetFlightStatusApprove,
                                        "getCanResetFlightStatusApprove", false)
                        ),
                        Optional.empty()
                },
                {
                        "Если у клиента включена фича CPM_PRICE_CAMPAIGNS - можно создавать прайсовые кампании. " +
                                "Оператор - супер",
                        operator()
                                .withRole(RbacRole.SUPER),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features()
                                .withIsCpmPriceCampaignEnabled(true),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanCreateCampaignTypes,
                                        "getCanCreateCampaignTypes",
                                        Sets.union(SUPER_CAN_CREATE_CAMPAIGN_TYPES, singleton(CPM_PRICE))),
                                propertyEqualTo(GdTestClientAccess::getCanResetFlightStatusApprove,
                                        "getCanResetFlightStatusApprove", true)
                        ),
                        Optional.empty()
                },
                {
                        "Если у клиента включена фича content_promotion_video - можно создавать CONTENT_PROMOTION " +
                                "кампании. " +
                                "Оператор - клиент",
                        operator()
                                .withRole(RbacRole.CLIENT),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features()
                                .withIsContentPromotionVideoEnabled(true),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanCreateCampaignTypes,
                                        "getCanCreateCampaignTypes",
                                        Sets.union(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES, singleton(CONTENT_PROMOTION)))
                        ),
                        Optional.empty()
                },
                {
                        "Если у клиента включена фича content_promotion_video - можно создавать CONTENT_PROMOTION " +
                                "кампании. " +
                                "Оператор - супер",
                        operator()
                                .withRole(RbacRole.SUPER),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features()
                                .withIsContentPromotionVideoEnabled(true),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanCreateCampaignTypes,
                                        "getCanCreateCampaignTypes",
                                        Sets.union(SUPER_CAN_CREATE_CAMPAIGN_TYPES, singleton(CONTENT_PROMOTION)))
                        ),
                        Optional.empty()
                },
                {
                        "Охватная реклама на главной не доступна клиенту, если у клиента нет фичи",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features().withIsCpmYndxFrontpageOnGridEnabled(false),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanCreateCampaignTypes,
                                        "getCanCreateCampaignTypes",
                                        DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                        ),
                        Optional.empty()
                },
                {
                        "Охватная реклама на главной доступна клиенту, если у клиента есть фича",
                        operator(),
                        operator().withUseCampDescription(true),
                        clientInfo(),
                        emptyList(),
                        features().withIsCpmYndxFrontpageOnGridEnabled(true),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanCreateCampaignTypes,
                                        "getCanCreateCampaignTypes",
                                        Sets.union(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES, singleton(CPM_YNDX_FRONTPAGE)))
                        ),
                        Optional.empty()
                },
                {
                        "Клиенту показываем ссылку на платежи и документы",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID),
                        operator(),
                        clientInfo(),
                        emptyList(),
                        features(),
                        List.of(defaultCampaign()),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewDocumentsAndPayments,
                                        "getCanViewDocumentsAndPayments", true)
                        ),
                        Optional.empty()
                },
                {
                        "Не клиенту не показываем ссылку на платежи и документы",
                        operator().withRole(RbacRole.AGENCY),
                        operator(),
                        clientInfo(),
                        emptyList(),
                        features(),
                        List.of(defaultCampaign()),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewDocumentsAndPayments,
                                        "getCanViewDocumentsAndPayments", false)
                        ),
                        Optional.empty()
                },
                {
                        "Агентству показываем возможность подключить клиента",
                        operator().withRole(RbacRole.AGENCY),
                        operator(),
                        clientInfo(),
                        emptyList(),
                        features(),
                        List.of(defaultCampaign()),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanBindClientToAgency,
                                        "getCanBindClientToAgency", true),
                                propertyEqualTo(GdTestClientAccess::getIsAgencyServiceStopped,
                                        "getIsAgencyServiceStopped", false)
                        ),
                        Optional.empty()
                },
                {
                        "Клиенту не показываем возможность подключить клиента",
                        operator().withRole(RbacRole.CLIENT),
                        operator(),
                        clientInfo(),
                        emptyList(),
                        features(),
                        List.of(defaultCampaign()),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanBindClientToAgency,
                                        "getCanBindClientToAgency", false),
                                propertyEqualTo(GdTestClientAccess::getIsAgencyServiceStopped,
                                        "getIsAgencyServiceStopped", false)
                        ),
                        Optional.empty()
                },
                {
                        "Клиент с фичей видит источники конверсий и может их редактировать",
                        operator().withRole(RbacRole.CLIENT),
                        operator(),
                        clientInfo(),
                        emptyList(),
                        features().withIsConversionCenterEnabled(true),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewConversionSources,
                                        "getCanViewConversionSources", true),
                                propertyEqualTo(GdTestClientAccess::getCanEditConversionSources,
                                        "getCanEditConversionSources", true)
                        ),
                        Optional.empty()
                },
                {
                        "Клиент без фичи не видит источники конверсий",
                        operator().withRole(RbacRole.CLIENT),
                        operator(),
                        clientInfo(),
                        emptyList(),
                        features().withIsConversionCenterEnabled(false),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewConversionSources,
                                        "getCanViewConversionSources", false),
                                propertyEqualTo(GdTestClientAccess::getCanEditConversionSources,
                                        "getCanEditConversionSources", false)
                        ),
                        Optional.empty()
                },
                {
                        "Суперридер видит источники конверсий для клиента с фичей, но не может их редактировать",
                        operator().withRole(RbacRole.SUPERREADER),
                        operator(),
                        clientInfo(),
                        emptyList(),
                        features().withIsConversionCenterEnabled(true),
                        Collections.emptySet(),
                        null,
                        allOf(
                                propertyEqualTo(GdTestClientAccess::getCanViewConversionSources,
                                        "getCanViewConversionSources", true),
                                propertyEqualTo(GdTestClientAccess::getCanEditConversionSources,
                                        "getCanEditConversionSources", false)
                        ),
                        Optional.empty()
                },
                {
                        "Если оператор - агентство, а subjectUser - не менеджер, возвращается " +
                                "showAgencyControls:true, showManagerControls:false ",
                        operator()
                                .withRole(RbacRole.AGENCY)
                                .withClientId(TEST_CLIENT_ID),
                        operator()
                                .withUseCampDescription(false),
                        clientInfo()
                                .withAgencyUserId(TEST_USER_ID)
                                .withAgencyClientId(TEST_CLIENT_ID.asLong()),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withShowManagerControls(false)
                                .withShowAgencyControls(true)

                                .withCanCreateCampaign(true)
                                .withCanMassCopyCampaign(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanViewOfflineReports(true)
                                .withCanSeeDealsLinkEverywhere(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withCanSeeAgencyClients(true)
                                .withInfoblockEnabled(true)
                                .withOperatorCanEditUserSettings(true)
                                .withOperatorCanEditClientSettings(true)
                                .withCanSelfSendAdsOnRemoderation(false),
                        null,
                        Optional.empty()
                },
                {
                        "Если оператор - агентство, а subjectUser - менеджер, возвращается showAgencyControls:false, " +
                                "showManagerControls:true",
                        operator()
                                .withRole(RbacRole.AGENCY)
                                .withClientId(TEST_CLIENT_ID),
                        operator()
                                .withRole(RbacRole.MANAGER)
                                .withUseCampDescription(false),
                        clientInfo()
                                .withAgencyUserId(TEST_USER_ID)
                                .withAgencyClientId(TEST_CLIENT_ID.asLong()),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withShowManagerControls(true)
                                .withShowAgencyControls(false)

                                .withCanCreateCampaign(true)
                                .withCanMassCopyCampaign(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanViewOfflineReports(true)
                                .withCanSeeDealsLinkEverywhere(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true)
                                .withCanSeeAgencyClients(true)
                                .withInfoblockEnabled(true)
                                .withOperatorCanEditUserSettings(true)
                                .withOperatorCanEditClientSettings(true),
                        null,
                        Optional.empty()
                },
                {
                        "Если оператор - менеджер, а subjectUser - не агенство, возвращается " +
                                "showAgencyControls:false, showManagerControls:true",
                        operator()
                                .withRole(RbacRole.MANAGER)
                                .withClientId(TEST_CLIENT_ID),
                        operator()
                                .withUseCampDescription(false),
                        clientInfo()
                                .withManagerUserId(TEST_USER_ID)
                                .withManagersInfo(singletonList(new GdUserInfo().withUserId(TEST_USER_ID))),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withShowManagerControls(true)
                                .withShowAgencyControls(false)

                                .withCanCreateCampaign(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanEditMobileAppDomain(true)
                                .withCanSeeClientRepresentatives(true)
                                .withCanSeeCampaignsServicedState(true)
                                .withCanViewMobileAppDomain(true)
                                .withCanMassCopyCampaign(true)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanViewOfflineReports(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true),
                        null,
                        Optional.empty()
                },
                {
                        "Если оператор - менеджер, а subjectUser - агенство, возвращается showAgencyControls:true, " +
                                "showManagerControls:false",
                        operator()
                                .withRole(RbacRole.MANAGER)
                                .withClientId(TEST_CLIENT_ID),
                        operator()
                                .withRole(RbacRole.AGENCY)
                                .withUseCampDescription(false),
                        clientInfo()
                                .withManagerUserId(TEST_USER_ID)
                                .withManagersInfo(singletonList(new GdUserInfo().withUserId(TEST_USER_ID))),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withShowManagerControls(false)
                                .withShowAgencyControls(true)

                                .withCanCreateCampaign(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanEditMobileAppDomain(true)
                                .withCanSeeClientRepresentatives(true)
                                .withCanSeeCampaignsServicedState(true)
                                .withCanViewMobileAppDomain(true)
                                .withCanMassCopyCampaign(true)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanViewOfflineReports(true)
                                .withCanMassEditAdGroupRegions(true)
                                .withCanManageLibMinusWords(true),
                        null,
                        Optional.empty()
                },
                {
                        "Управляющий МСС-аккаунт для клиента",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(MCC_CLIENT_ID)
                                .withUid(MCC_USER_ID),
                        operator(),
                        clientInfo(),
                        emptyList(),
                        features(),
                        emptySet(),
                        clientAccess()
                                .withCanCreateCampaign(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageLibMinusWords(true)
                                .withCanManageUsingXLS(true)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanViewUserLogs(true)
                                .withCanPayByYandexMoney(false)
                                .withCanPayCampaigns(false)
                                .withCanMassEditAdGroupRegions(true)
                                .withInfoblockEnabled(true)
                                .withCanEditUserSettings(false)
                                .withOperatorCanEditUserSettings(false)
                                .withOperatorCanEditClientSettings(false)
                                .withShowBalanceLink(false)
                                .withWalletAllowPay(false),
                        null,
                        Optional.empty()
                },
                {
                        "Управляющий МСС-аккаунт для агентского супер субклиента",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(MCC_CLIENT_ID)
                                .withUid(MCC_USER_ID),
                        operator(),
                        clientInfo()
                                .withAgencyUserId(AGENCY_USER_ID)
                                .withAgencyClientId(AGENCY_CLIENT_ID),
                        emptyList(),
                        features(),
                        emptySet(),
                        clientAccess()
                                .withCanCreateCampaign(true)
                                .withCanCreateCampaignTypes(DEFAULT_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageLibMinusWords(true)
                                .withCanManageUsingXLS(false)
                                .withCanManageFeeds(true)
                                .withCanManageCreatives(true)
                                .withCanManageRetargertingConditions(true)
                                .withCanViewUserLogs(true)
                                .withCanPayByYandexMoney(false)
                                .withCanPayCampaigns(false)
                                .withCanMassEditAdGroupRegions(true)
                                .withInfoblockEnabled(true)
                                .withCanEditUserSettings(false)
                                .withOperatorCanEditUserSettings(false)
                                .withOperatorCanEditClientSettings(false)
                                .withShowBalanceLink(false)
                                .withWalletAllowPay(false)
                                .withCanViewOfflineReports(true),
                        null,
                        Optional.empty()
                },
                {
                        "Управляющий МСС-аккаунт для агентского субклиента без права редактирования",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(MCC_CLIENT_ID)
                                .withUid(MCC_USER_ID),
                        operator(),
                        clientInfo()
                                .withAgencyUserId(AGENCY_USER_ID_TWO)
                                .withAgencyClientId(AGENCY_CLIENT_ID),
                        emptyList(),
                        features(),
                        emptySet(),
                        clientAccess()
                                .withOperatorCanWrite(false)
                                .withCanCreateCampaign(false)
                                .withCanCreateCampaignTypes(EMPTY_CAN_CREATE_CAMPAIGN_TYPES)
                                .withCanEditMobileApps(false)
                                .withCanManageFavoriteCampaigns(true)
                                .withCanManageLibMinusWords(false)
                                .withCanManageUsingXLS(false)
                                .withCanManageFeeds(false)
                                .withCanManageCreatives(false)
                                .withCanManageRetargertingConditions(false)
                                .withCanViewUserLogs(true)
                                .withCanPayByYandexMoney(false)
                                .withCanPayCampaigns(false)
                                .withCanMassEditAdGroupRegions(false)
                                .withInfoblockEnabled(true)
                                .withCanEditUserSettings(false)
                                .withOperatorCanEditUserSettings(false)
                                .withOperatorCanEditClientSettings(false)
                                .withShowBalanceLink(false)
                                .withWalletAllowPay(false)
                                .withCanViewOfflineReports(true),
                        null,
                        Optional.empty()
                },
                {
                        "Ограниченный представитель клиента",
                        operator()
                                .withRole(RbacRole.CLIENT)
                                .withClientId(TEST_CLIENT_ID)
                                .withRepType(RbacRepType.READONLY)
                                .withIsReadonlyRep(true),
                        operator()
                                .withUseCampDescription(true),
                        clientInfo(),
                        Collections.emptyList(),
                        features(),
                        Collections.emptySet(),
                        clientAccess()
                                .withOperatorIsClient(true)
                                .withCanCreateCampaignTypes(emptySet())
                                .withCanCreateCampaign(false)
                                .withCanManageFavoriteCampaigns(false)
                                .withCanManageFeeds(false)
                                .withCanManageCreatives(false)
                                .withCanManageRetargertingConditions(false)
                                .withCanViewUserLogs(true)
                                .withCanRequestFlService(false)
                                .withCanMassEditAdGroupRegions(false)
                                .withCanManageLibMinusWords(false)
                                .withOperatorCanEditUserSettings(false)
                                .withOperatorCanWrite(false)
                                .withInfoblockEnabled(true)
                                .withCanUseCampDescription(true)
                                .withCanEditMobileApps(false)
                                .withCanManageUsingXLS(false)
                                .withCanManageInternalAdAccessRights(false),
                        null,
                        Optional.empty()
                },
        });
    }


    @Test
    public void getAccess() {
        doReturn(Collections.emptySet())
                .when(clientService).clientIdsWithApiEnabled(any());
        doReturn(ImmutableMap.of(operator.getChiefUid(), false))
                .when(userService).getUsersAgencyDisallowMoneyTransfer(Collections.singleton(operator.getChiefUid()));
        doReturn(Collections.emptySet())
                .when(featureService).getEnabledForClientId(any(ClientId.class));
        doReturn(Collections.emptyList())
                .when(freelancerService).getFreelancers(anyCollection());
        doReturn(true)
                .when(autopayService).canUseAutopay(anyInt(), anyLong(), any());
        doReturn(true)
                .when(rbacService).isRelatedClient(FREELANCER_CLIENT_ID, TEST_CLIENT_ID);
        doReturn(true)
                .when(rbacService).isRelatedClient(MCC_CLIENT_ID, TEST_CLIENT_ID);
        doReturn(true)
                .when(rbacClientsRelations).isSupportForClientRelation(LIMITED_SUPPORT_CLIENT_ID, TEST_CLIENT_ID);
        doReturn(Set.of(TEST_CLIENT_ID))
                .when(agencyClientRelationService).getAllowableToBindClients(any(), anyCollection());
        doReturn(Set.of(TEST_CLIENT_ID.asLong()))
                .when(agencyClientRelationService).getUnArchivedAgencyClients(any(), anyCollection());
        doReturn(internalAdManagerProductRelation)
                .when(rbacClientsRelations)
                .getInternalAdProductRelation(operator.getClientId(), ClientId.fromLong(clientInfo.getId()));
        doReturn(new ClientLimits())
                .when(clientLimitsService)
                .getClientLimits(any(ClientId.class));
        doReturn(true)
                .when(rbacService).isOperatorMccForClient(MCC_USER_ID, TEST_CLIENT_ID.asLong());
        if ((operator.getRole().equals(RbacRole.CLIENT) && clientInfo.getAgencyClientId() == null && !operator.getIsReadonlyRep())
                || (operator.getRole() == RbacRole.INTERNAL_AD_ADMIN
                && operator.getClientId().asLong() == clientInfo.getId())) {
            doReturn(true)
                    .when(rbacService).canWrite(operator.getUid(), clientInfo.getChiefUserId());
        }
        if (operator.getRole().anyOf(
                RbacRole.SUPER,
                RbacRole.SUPERREADER,
                RbacRole.SUPPORT,
                RbacRole.LIMITED_SUPPORT,
                RbacRole.CLIENT,
                RbacRole.MANAGER,
                RbacRole.AGENCY)) {
            doReturn(true)
                    .when(rbacService).canRead(operator.getUid(), clientInfo.getChiefUserId());
        }

        if (operator.getUid() == MCC_USER_ID && clientInfo.getAgencyUserId() != null && clientInfo.getAgencyUserId() == AGENCY_USER_ID) {
            doReturn(true)
                    .when(clientService).isSuperSubclient(TEST_CLIENT_ID);
        }

        doReturn(ImmutableList.builder().addAll(campaigns).build())
                .when(campaignInfoService).getAllBaseCampaigns(any(ClientId.class));
        doReturn(usedFeatures)
                .when(clientDataService)
                .getClientFeatures(any(ClientId.class), any(User.class), any());

        GdClientAccess access = operatorAccessService.getAccess(
                operator,
                clientInfo.withChiefUser(new GdUserInfo()
                        .withUserId(TEST_CHIEF_USER_ID)
                        .withClientId(TEST_CHIEF_CLIENT_ID)),
                subjectUser,
                Instant.now());
        GdTestClientAccess clientAccess = JsonUtils.fromJson(JsonUtils.toJson(access), GdTestClientAccess.class);

        if (accessCondition != null) {
            assertThat(clientAccess).is(accessCondition);
        } else {
            assertThat(clientAccess)
                    .usingRecursiveComparison()
                    .ignoringExpectedNullFields()
                    .isEqualTo(expectedAccess);
        }
    }

    private static <T, M extends Model> Condition<M> propertyEqualTo(Function<M, T> property, String name, T value) {
        return new Condition<M>(
                obj -> Objects.equals(property.apply(obj), value),
                "property %s is equal to %s", name, String.valueOf(value));
    }
}
