package ru.yandex.direct.grid.processing.service.campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.InetAddresses;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.differ.SimpleTypeDiffer;
import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetNotificationStatus;
import ru.yandex.direct.core.entity.campaign.model.CampaignMetatype;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomDayBudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.ContentLanguage;
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.EshowsRate;
import ru.yandex.direct.core.entity.campaign.model.EshowsSettings;
import ru.yandex.direct.core.entity.campaign.model.EshowsVideoType;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightTargetingsSnapshot;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.dialogs.repository.ClientDialogsRepository;
import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.product.service.ProductService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.IdmGroupRoleInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.data.TestGdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCmpBannerCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCpmYndxFrontpageCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddDynamicCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddSmartCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddTextCampaign;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBrandSafetyRequest;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.CAMP_FINISHED_SMS;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.NOTIFY_ORDER_MONEY_IN_SMS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_CHECK_POSITION_EVENT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_COMPANY_INFO;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ENABLE_CPC_HOLD;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_HAS_TURBO_SMARTS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_IS_ALONE_TRAFARET_ALLOWED;
import static ru.yandex.direct.core.testing.data.TestCampaigns.DEFAULT_BANNER_HREF_PARAMS;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestIdmGroups.DEFAULT_IDM_GROUP_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetRoiStrategy;
import static ru.yandex.direct.core.validation.defects.RightsDefects.noRights;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_CAMPAIGN;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.DISABLED_SSP;
import static ru.yandex.direct.grid.model.entity.campaign.converter.CampaignDataConverter.toTimeTarget;
import static ru.yandex.direct.grid.processing.data.TestGdAddCampaigns.TEST_EMAIL;
import static ru.yandex.direct.grid.processing.data.TestGdAddCampaigns.TEST_HREF;
import static ru.yandex.direct.grid.processing.data.TestGdAddCampaigns.defaultContentPromotionCampaign;
import static ru.yandex.direct.grid.processing.data.TestGdAddCampaigns.defaultCpmPriceCampaign;
import static ru.yandex.direct.grid.processing.data.TestGdAddCampaigns.defaultCpmYndxFrontpageCampaign;
import static ru.yandex.direct.grid.processing.data.TestGdAddCampaigns.defaultDynamicCampaign;
import static ru.yandex.direct.grid.processing.data.TestGdAddCampaigns.defaultTextCampaign;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.ADD_CAMPAIGNS;
import static ru.yandex.direct.grid.processing.service.constant.DefaultValuesUtils.defaultGdTimeTarget;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.DEFAULT_TIMEZONE;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.timeTarget24x7;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignMutationAddGraphqlServiceTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final RecursiveComparisonConfiguration CAMPAIGN_COMPARE_STRATEGY =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreAllExpectedNullFields(true)
                    .build();

    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    addedCampaigns {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdAddCampaigns, GdAddCampaignPayload> ADD_CAMPAIGN_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(ADD_CAMPAIGNS, MUTATION_TEMPLATE,
                    GdAddCampaigns.class, GdAddCampaignPayload.class);

    private static final String INTERNAL_IP = "12.12.12.12";
    private static final int COUNTER_ID = 5;

    private DefaultCompareStrategy getCampaignCompareStrategy() {
        return DefaultCompareStrategies
                .allFieldsExcept(newPath(CommonCampaign.CREATE_TIME.name()))
                .forFields(newPath(CommonCampaign.LAST_CHANGE.name()))
                .useMatcher(approximatelyNow())
                .forFields(newPath(CampaignWithCustomDayBudget.DAY_BUDGET_LAST_CHANGE.name()))
                .useMatcher(approximatelyNow())
                .forFields(newPath(CommonCampaign.ID.name())).useMatcher(notNullValue())
                .forFields(newPath(CommonCampaign.WALLET_ID.name())).useMatcher(notNullValue())
                .forFields(newPath(CampaignWithPackageStrategy.STRATEGY_ID.name())).useMatcher(notNullValue())
                .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer())
                .forFields(newPath(CommonCampaign.SOURCE.name())).useMatcher(equalTo(CampaignSource.DIRECT))
                .forFields(newPath(CommonCampaign.METATYPE.name())).useMatcher(equalTo(CampaignMetatype.DEFAULT_))
                .forClasses(TimeTarget.class).useDiffer(new SimpleTypeDiffer());
    }

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    private Steps steps;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;
    @Autowired
    UserRepository userRepository;

    @Autowired
    ClientDialogsRepository clientDialogsRepository;

    @Autowired
    private NetAcl netAcl;

    @Autowired
    private ProductService productService;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    protected MetrikaClientStub metrikaClientStub;

    @Autowired
    private CampaignConstantsService campaignConstantsService;

    private User operator;
    private ClientInfo clientInfo;
    private int shard;
    private IdmGroup idmGroup;

    private static CampaignAttributionModel defaultAttributionModel;

    @Before
    public void before() {
        createClient(defaultClient());
        steps.sspPlatformsSteps().addSspPlatforms(DISABLED_SSP);
        when(netAcl.isInternalIp(InetAddresses.forString(INTERNAL_IP))).thenReturn(true);
        metrikaClientStub.addUserCounter(operator.getUid(), COUNTER_ID);

        idmGroup = steps.idmGroupSteps().addIfNotExistIdmGroup(DEFAULT_IDM_GROUP_ID, IdmRequiredRole.MANAGER);

        defaultAttributionModel = campaignConstantsService.getDefaultAttributionModel();
    }

    @After
    public void after() {
        reset(netAcl);
    }

    private void createClient(Client client) {
        clientInfo = steps.clientSteps().createClient(client);

        shard = clientInfo.getShard();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.DISABLE_BILLING_AGGREGATES, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.CAMPAIGN_ALLOWED_ON_ADULT_CONTENT, true);

        operator = clientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void addTextCampaign() {
        GdAddTextCampaign textCampaign = defaultTextCampaign(defaultAttributionModel);
        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion().withTextCampaign(textCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);
    }

    @Test
    public void addTextCampaignWithBannerHrefParams() {
        GdAddTextCampaign textCampaign = defaultTextCampaign(defaultAttributionModel)
                .withBannerHrefParams(DEFAULT_BANNER_HREF_PARAMS);
        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion().withTextCampaign(textCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);
    }

    @Test
    public void addTextCampaign_byPrimaryManager() {
        UserInfo managerInfo =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER).getChiefUserInfo();
        steps.idmGroupSteps().addIdmGroupRole(
                new IdmGroupRoleInfo()
                        .withClientInfo(clientInfo)
                        .withIdmGroup(idmGroup));
        steps.idmGroupSteps().addGroupMembership(DEFAULT_IDM_GROUP_ID, managerInfo.getClientId());
        steps.idmGroupSteps().addIdmPrimaryManager(managerInfo, clientInfo);
        User manager = userRepository.fetchByUids(managerInfo.getShard(), singletonList(managerInfo.getUid())).get(0);
        User chiefUser = userRepository.fetchByUids(clientInfo.getShard(), singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(manager, chiefUser);

        GdAddTextCampaign textCampaign = defaultTextCampaign(defaultAttributionModel);
        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion().withTextCampaign(textCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));
        GdAddCampaignPayload payload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, manager, chiefUser);
        checkState(payload.getValidationResult() == null);

        Long camoaignId = payload.getAddedCampaigns().get(0).getId();
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(shard, singletonList(camoaignId)).get(0);
        assertThat(actualCampaign.getManagerUid())
                .as("manager uid equals primary manager").isEqualTo(manager.getUid());
    }

    @Test
    public void addTextCampaign_dontRepeatNotPrimaryManager() {
        UserInfo managerInfo =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER).getChiefUserInfo();
        CampaignInfo defaultCampaign = steps.campaignSteps().createActiveCampaign(clientInfo);
        steps.campaignSteps().setManager(defaultCampaign, managerInfo.getUid());

        GdAddTextCampaign textCampaign = defaultTextCampaign(defaultAttributionModel);
        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion().withTextCampaign(textCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));
        GdAddCampaignPayload payload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);
        checkState(payload.getValidationResult() == null);

        Long camoaignId = payload.getAddedCampaigns().get(0).getId();
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(shard, singletonList(camoaignId)).get(0);
        assertThat(actualCampaign.getManagerUid()).as("manager uid is null").isNull();
    }

    @Test
    public void addTextCampaign_ByOperatorWithNoRights() {
        GdAddTextCampaign textCampaign = defaultTextCampaign(defaultAttributionModel);
        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion().withTextCampaign(textCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        UserInfo subjectUser = steps.userSteps().createDefaultUser();
        User operatorWithNoRights = operator;

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operatorWithNoRights, subjectUser.getUser());

        assertThat(gdAddCampaignPayload.getValidationResult())
                .isNotNull()
                .is(matchedBy(hasErrorsWith(gridDefect(path(field(GdAddCampaigns.CAMPAIGN_ADD_ITEMS)), noRights()))));
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).isEmpty();
    }

    private void testAddDynamicCampaign(GdAddDynamicCampaign dynamicCampaign, DynamicCampaign expectedCampaign) {
        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion().withDynamicCampaign(dynamicCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));
        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);
        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);
        List<? extends BaseCampaign> campaigns =
                campaignTypedRepository.getTypedCampaigns(shard, mapList(gdAddCampaignPayload.getAddedCampaigns(),
                        GdAddCampaignPayloadItem::getId));
        assertThat(campaigns).hasSize(1);
        DynamicCampaign campaign = (DynamicCampaign) campaigns.get(0);
        assertThat(campaign)
                .usingRecursiveComparison(CAMPAIGN_COMPARE_STRATEGY)
                .isEqualTo(expectedCampaign);

    }

    @Test
    public void addDynamicCampaign() {
        GdAddDynamicCampaign dynamicCampaign = defaultDynamicCampaign(defaultAttributionModel);

        DynamicCampaign expectedCampaign = createExpectedDynamicCampaign();

        testAddDynamicCampaign(dynamicCampaign, expectedCampaign);

    }

    @Test
    @Parameters(method = "paramsAdvancedGeo")
    @TestCaseName ("Dynamic campaign, {0}, useCurrentRegion: {2}, useRegularRegion: {3}, hasExtendedGeoTargeting: {4}")
    public void testAddDynamicCampaignWithAdvancedGeoTargeting(
            String desc,
            boolean advancedGeoTargeting,
            Boolean currentRegion,
            Boolean regularRegion,
            Boolean hasExtendedGeoTargeting
    ) {
        if (advancedGeoTargeting) {
            steps.featureSteps().enableClientFeature(FeatureName.ADVANCED_GEOTARGETING);
        }
        GdAddDynamicCampaign gdAddDynamicCampaign = defaultDynamicCampaign(defaultAttributionModel);

        gdAddDynamicCampaign
                .withUseCurrentRegion(currentRegion)
                .withUseRegularRegion(regularRegion)
                .withHasExtendedGeoTargeting(hasExtendedGeoTargeting);

        DynamicCampaign expectedCampaign = createExpectedDynamicCampaign();
        expectedCampaign
                .withUseCurrentRegion(currentRegion)
                .withUseRegularRegion(regularRegion)
                .withHasExtendedGeoTargeting(hasExtendedGeoTargeting                                                                                                                                 );

        testAddDynamicCampaign(gdAddDynamicCampaign, expectedCampaign);
    }

    Iterable<Object[]> paramsAdvancedGeo() {
        return asList(new Object[][]{
                {"feature is on", true, true, true, true},
                {"feature is on", true, false, true, false},
                {"feature is on", true, true, false, true},
                {"feature is off", false, null, null, false},
        });
    }

    Iterable<Object[]> paramsAdvancedGeoSmart() {
        return asList(new Object[][]{
                {"feature is on", true, true, true, true},
                {"feature is on", true, false, true, false},
                {"feature is on", true, true, false, true},
                {"feature is off", false, null, null, null},
        });
    }

    private DynamicCampaign createExpectedDynamicCampaign() {
        DynamicCampaign expectedCampaign = new DynamicCampaign()
                .withName("new Camp")
                .withStartDate(LocalDate.now().plusDays(1))
                .withMetrikaCounters(null)
                .withHasTitleSubstitution(false)
                .withHasSiteMonitoring(false)
                .withHasExtendedGeoTargeting(false)
                .withTimeTarget(toTimeTarget(defaultGdTimeTarget()))
                .withEmail(TEST_EMAIL)
                .withSmsTime(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL)
                .withSmsFlags(EnumSet.of(CAMP_FINISHED_SMS, NOTIFY_ORDER_MONEY_IN_SMS))
                .withStrategy((DbStrategy) new DbStrategy()
                        .withPlatform(CampaignsPlatform.SEARCH)
                        .withAutobudget(CampaignsAutobudget.NO)
                        .withStrategyName(StrategyName.DEFAULT_)
                        .withStrategyData(new StrategyData()
                                .withName("default")
                                .withSum(BigDecimal.valueOf(5000))
                                .withVersion(1L)
                                .withUnknownFields(emptyMap())))
                .withEnableCpcHold(DEFAULT_ENABLE_CPC_HOLD)
                .withDayBudget(BigDecimal.ZERO.setScale(2))
                .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withEnableCompanyInfo(DEFAULT_ENABLE_COMPANY_INFO)
                .withIsAloneTrafaretAllowed(DEFAULT_IS_ALONE_TRAFARET_ALLOWED)
                .withAttributionModel(defaultAttributionModel)
                .withHref(TEST_HREF)
                .withBrandSafetyCategories(emptyList())
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withPlacementTypes(emptySet());
        return expectedCampaign;
    }

    private void testAddSmartCampaign(GdAddSmartCampaign smartCampaign, SmartCampaign expectedCampaign) {

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion().withSmartCampaign(smartCampaign);
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload response = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION, input, operator);
        checkState(response.getValidationResult() == null && response.getAddedCampaigns().size() == 1,
                "Unexpected error or list of campaign data is empty in response");

        List<? extends BaseCampaign> campaigns =
                campaignTypedRepository.getTypedCampaigns(shard, mapList(response.getAddedCampaigns(),
                        GdAddCampaignPayloadItem::getId));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(campaigns).as("list of campaign data")
                    .hasSize(1);
            soft.assertThat(campaigns.get(0))
                    .usingRecursiveComparison(CAMPAIGN_COMPARE_STRATEGY)
                    .isEqualTo(expectedCampaign);
        });
    }

    @Test
    public void addSmartCampaign() {
        GdAddSmartCampaign gdAddSmartCampaign = TestGdAddCampaigns
                .defaultSmartCampaign(DEFAULT_HAS_TURBO_SMARTS, List.of(COUNTER_ID), defaultAttributionModel);

        GdAddCampaignUnion gdAddCampaignUnion = new GdAddCampaignUnion().withSmartCampaign(gdAddSmartCampaign);
        testAddSmartCampaign(gdAddSmartCampaign, createExpectedSmartCampaign());
    }

    private SmartCampaign createExpectedSmartCampaign() {
        return TestCampaigns.defaultSmartCampaign()
                .withStrategy(defaultAutobudgetRoiStrategy(0, false))
                .withFio(operator.getFio())
                .withHasTurboSmarts(DEFAULT_HAS_TURBO_SMARTS)
                .withContextLimit(0)
                .withHasExtendedGeoTargeting(true)
                .withEnableOfflineStatNotice(true)
                .withEnablePausedByDayBudgetEvent(true)
                .withContextPriceCoef(100)
                .withHref(TEST_HREF)
                .withMetrikaCounters(List.of((long) COUNTER_ID));
    }

    @Test
    @Parameters(method = "paramsAdvancedGeoSmart")
    @TestCaseName ("Smart campaign, {0}, useCurrentRegion: {2}, useRegularRegion: {3}, hasExtendedGeoTargeting: {4}")
    public void testAddSmartCampaignWithAdvancedGeoTargeting(
            String desc,
            boolean advancedGeoTargeting,
            Boolean currentRegion,
            Boolean regularRegion,
            Boolean hasExtendedGeoTargeting
    ) {
        if (advancedGeoTargeting) {
            steps.featureSteps().enableClientFeature(FeatureName.ADVANCED_GEOTARGETING);
        }
        var gdAddSmartCampaign = TestGdAddCampaigns
                .defaultSmartCampaign(DEFAULT_HAS_TURBO_SMARTS, List.of(COUNTER_ID), defaultAttributionModel);

        gdAddSmartCampaign
                .withUseCurrentRegion(currentRegion)
                .withUseRegularRegion(regularRegion)
                .withHasExtendedGeoTargeting(hasExtendedGeoTargeting);

        var expectedCampaign = createExpectedSmartCampaign();
        expectedCampaign
                .withUseCurrentRegion(currentRegion)
                .withUseRegularRegion(regularRegion)
                .withHasExtendedGeoTargeting(hasExtendedGeoTargeting);

        testAddSmartCampaign(gdAddSmartCampaign, expectedCampaign);
    }

    @Test
    public void addContentPromotionCampaign() {
        GdAddCampaignUnion gdAddCampaignUnion =
                new GdAddCampaignUnion().withContentPromotionCampaign(defaultContentPromotionCampaign(defaultAttributionModel));
        GdAddCampaigns input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        GdAddCampaignPayload gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);

        List<? extends BaseCampaign> campaigns =
                campaignTypedRepository.getTypedCampaigns(shard, mapList(gdAddCampaignPayload.getAddedCampaigns(),
                        GdAddCampaignPayloadItem::getId));

        assertThat(campaigns).hasSize(1);
        ContentPromotionCampaign campaign = (ContentPromotionCampaign) campaigns.get(0);
        ContentPromotionCampaign expectedCampaign = new ContentPromotionCampaign()
                .withType(CampaignType.CONTENT_PROMOTION)
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong())
                .withName("new Camp")
                .withStartDate(LocalDate.now().plusDays(1))
                .withTimeTarget(toTimeTarget(defaultGdTimeTarget()))
                .withTimeZoneId(defaultGdTimeTarget().getIdTimeZone())
                .withMetrikaCounters(null)
                .withAttributionModel(defaultAttributionModel)
                .withDayBudget(BigDecimal.ZERO.setScale(2))
                .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
                .withEnableCpcHold(false)
                .withMinusKeywords(emptyList())
                .withSmsTime(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL)
                .withSmsFlags(EnumSet.of(CAMP_FINISHED_SMS, NOTIFY_ORDER_MONEY_IN_SMS))
                .withEmail(TEST_EMAIL)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                .withEnableSendAccountNews(false)
                .withEnableCompanyInfo(true)
                .withHasTitleSubstitution(true)
                .withEnableCheckPositionEvent(DEFAULT_ENABLE_CHECK_POSITION_EVENT)
                .withCheckPositionIntervalEvent(DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL)
                .withEnableCompanyInfo(true)
                .withHasTurboApp(false)
                .withIsVirtual(false)
                .withEnablePausedByDayBudgetEvent(true)
                .withDisabledSsp(emptyList())
                .withStrategy((DbStrategy) new DbStrategy()
                        .withPlatform(CampaignsPlatform.SEARCH)
                        .withAutobudget(CampaignsAutobudget.NO)
                        .withStrategyName(StrategyName.DEFAULT_)
                        .withStrategyData(new StrategyData()
                                .withName("default")
                                .withSum(BigDecimal.valueOf(5000))
                                .withVersion(1L)
                                .withUnknownFields(emptyMap())))
                .withHref(TEST_HREF)
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withBrandSafetyCategories(List.of(4_294_967_297L));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(campaign)
                    .usingRecursiveComparison(CAMPAIGN_COMPARE_STRATEGY)
                    .isEqualTo(expectedCampaign);

            soft.assertThat(
                    testCampaignRepository.getCampaignFieldValue(
                            shard, campaign.getId(), CAMP_OPTIONS.STATUS_CLICK_TRACK))
                    .isEqualTo(1L);
        });
    }

    @Test
    public void addCpmPriceCampaign() {
        createClient(defaultClient()
                .withWorkCurrency(CurrencyCode.RUB)
                .withCountryRegionId(RUSSIA_REGION_ID));
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), CPM_PRICE_CAMPAIGN, true);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        var startDate = LocalDate.of(currentYear + 1, 1, 1);
        var endDate = LocalDate.of(currentYear + 1, 12, 1);

        var pricePackage = steps.pricePackageSteps().createPricePackage(approvedPricePackage()
                .withPrice(BigDecimal.valueOf(13L))
                .withCurrency(CurrencyCode.RUB)
                .withOrderVolumeMin(100L)
                .withOrderVolumeMax(1000000L)
                .withTargetingsFixed(new TargetingsFixed()
                        .withGeo(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))
                .withTargetingsCustom(new TargetingsCustom())
                .withDateStart(startDate)
                .withDateEnd(endDate)
                .withClients(List.of(allowedPricePackageClient(clientInfo))));
        var cpmPriceCampaign =
                defaultCpmPriceCampaign(startDate, endDate, pricePackage.getPricePackageId(), defaultAttributionModel)
                        .withIsAllowedOnAdultContent(true);
        var gdAddCampaignUnion = new GdAddCampaignUnion().withCpmPriceCampaign(cpmPriceCampaign);
        var input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        var gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);

        var campaigns = campaignTypedRepository.getTypedCampaigns(shard,
                mapList(gdAddCampaignPayload.getAddedCampaigns(), GdAddCampaignPayloadItem::getId));

        var expectedProduct =
                productService.calculateProductForCampaign(CampaignType.CPM_PRICE, CurrencyCode.RUB, false);

        assertThat(campaigns).hasSize(1);
        var campaign = (CpmPriceCampaign) campaigns.get(0);
        var expectedCampaign = new CpmPriceCampaign()
                .withType(CampaignType.CPM_PRICE)
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong())
                .withAgencyId(0L)
                .withFio(operator.getFio())
                .withIsServiceRequested(false)
                .withOrderId(0L)
                .withWalletId(0L)
                .withName("New Cpm Price Campaign")
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withContentLanguage(ContentLanguage.RU)
                .withCurrency(CurrencyCode.RUB)
                .withProductId(expectedProduct.getId())
                .withTimeTarget(timeTarget24x7())
                .withTimeZoneId(DEFAULT_TIMEZONE)
                .withMetrikaCounters(null)
                .withAttributionModel(defaultAttributionModel)
                .withEnableCpcHold(false)
                .withHasTurboApp(false)
                .withHref(TEST_HREF)
                .withIsVirtual(false)
                .withEnableCompanyInfo(false)
                .withHasExtendedGeoTargeting(false)
                .withUseCurrentRegion(false)
                .withUseRegularRegion(false)
                .withHasTitleSubstitution(false)
                .withHasAddMetrikaTagToUrl(true)
                .withHasAddOpenstatTagToUrl(false)
                .withGeo(Set.of((int) RUSSIA_REGION_ID, (int) CRIMEA_REGION_ID))
                .withSmsTime(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL)
                .withSmsFlags(EnumSet.of(CAMP_FINISHED_SMS, NOTIFY_ORDER_MONEY_IN_SMS))
                .withEmail(TEST_EMAIL)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                .withEnableSendAccountNews(true)
                .withEnableOfflineStatNotice(true)
                .withEnableCheckPositionEvent(true)
                .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._60)
                .withEnablePausedByDayBudgetEvent(true)
                .withSum(BigDecimal.ZERO)
                .withSumLast(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO)
                .withSumToPay(BigDecimal.ZERO)
                .withPaidByCertificate(false)
                .withRequireFiltrationByDontShowDomains(false)
                .withDisabledSsp(emptyList())
                .withStrategy((DbStrategy) new DbStrategy()
                        .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                        .withStrategyName(StrategyName.PERIOD_FIX_BID)
                        .withStrategyData(new StrategyData()
                                .withName("period_fix_bid")
                                .withVersion(1L)
                                .withAutoProlongation(0L)
                                .withStart(startDate)
                                .withFinish(endDate)
                                .withBudget(BigDecimal.valueOf(650)))
                        .withAutobudget(CampaignsAutobudget.NO)
                        .withPlatform(CampaignsPlatform.CONTEXT))
                .withStrategyId(0L)
                .withStatusActive(false)
                .withStatusEmpty(false)
                .withStatusShow(true)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withStatusArchived(false)
                .withPricePackageId(pricePackage.getPricePackageId())
                .withFlightOrderVolume(50000L)
                .withFlightTargetingsSnapshot(new PriceFlightTargetingsSnapshot()
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))
                .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                .withFlightStatusCorrect(PriceFlightStatusCorrect.NEW)
                .withBrandSafetyCategories(emptyList())
                .withIsDraftApproveAllowed(false)
                .withIsSkadNetworkEnabled(false)
                .withIsAllowedOnAdultContent(true)
                .withMeasurers(emptyList())
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withIsCpmGlobalAbSegment(false)
                .withIsSearchLiftEnabled(false)
                .withIsBrandLiftHidden(false)
                .withAutoProlongation(false);

        assertThat(campaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(
                getCampaignCompareStrategy())));
    }

    @Test
    public void addCpmYndxFrontpageCampaign() {
        createClient(defaultClient()
                .withWorkCurrency(CurrencyCode.RUB)
                .withCountryRegionId(RUSSIA_REGION_ID));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        var startDate = LocalDate.of(currentYear + 1, 1, 1);
        var endDate = LocalDate.of(currentYear + 1, 12, 1);

        GdAddCpmYndxFrontpageCampaign cpmYndxFrontpageCampaign =
                defaultCpmYndxFrontpageCampaign(startDate, endDate, defaultAttributionModel);

        var gdAddCampaignUnion = new GdAddCampaignUnion().withCpmYndxFrontpageCampaign(cpmYndxFrontpageCampaign);
        var input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        var gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);

        var campaigns = campaignTypedRepository.getTypedCampaigns(shard,
                mapList(gdAddCampaignPayload.getAddedCampaigns(), GdAddCampaignPayloadItem::getId));

        var expectedProduct =
                productService.calculateProductForCampaign(CampaignType.CPM_YNDX_FRONTPAGE, CurrencyCode.RUB, false);

        assertThat(campaigns).hasSize(1);
        var campaign = (CpmYndxFrontpageCampaign) campaigns.get(0);
        var expectedCampaign = new CpmYndxFrontpageCampaign()
                .withType(CampaignType.CPM_YNDX_FRONTPAGE)
                .withAllowedFrontpageType(ImmutableSet.of(FrontpageCampaignShowType.FRONTPAGE))
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong())
                .withAgencyId(0L)
                .withFio(operator.getFio())
                .withIsServiceRequested(false)
                .withOrderId(0L)
                .withWalletId(0L)
                .withName("New Cpm Yndx Frontpage Campaign")
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withContentLanguage(ContentLanguage.RU)
                .withCurrency(CurrencyCode.RUB)
                .withProductId(expectedProduct.getId())
                .withTimeTarget(toTimeTarget(defaultGdTimeTarget()))
                .withTimeZoneId(DEFAULT_TIMEZONE)
                .withMetrikaCounters(null)
                .withAttributionModel(defaultAttributionModel)
                .withEnableCpcHold(false)
                .withHasTurboApp(false)
                .withIsVirtual(false)
                .withEnableCompanyInfo(false)
                .withHasExtendedGeoTargeting(false)
                .withUseCurrentRegion(false)
                .withUseRegularRegion(false)
                .withHasTitleSubstitution(false)
                .withHasSiteMonitoring(false)
                .withHasAddMetrikaTagToUrl(true)
                .withHasAddOpenstatTagToUrl(false)
                .withGeo(Set.of((int) RUSSIA_REGION_ID, (int) CRIMEA_REGION_ID))
                .withSmsTime(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL)
                .withSmsFlags(EnumSet.of(CAMP_FINISHED_SMS, NOTIFY_ORDER_MONEY_IN_SMS))
                .withEmail(TEST_EMAIL)
                .withHref(TEST_HREF)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                .withEnableSendAccountNews(true)
                .withEnableOfflineStatNotice(true)
                .withEnablePausedByDayBudgetEvent(true)
                .withSum(BigDecimal.ZERO)
                .withSumLast(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO)
                .withSumToPay(BigDecimal.ZERO)
                .withPaidByCertificate(false)
                .withStrategy((DbStrategy) new DbStrategy()
                        .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                        .withPlatform(CampaignsPlatform.CONTEXT)
                        .withStrategyName(StrategyName.CPM_DEFAULT)
                        .withAutobudget(CampaignsAutobudget.NO)
                        .withStrategyData(new StrategyData()
                                .withVersion(1L)
                                .withName(StrategyName.CPM_DEFAULT.name().toLowerCase())
                        ))
                .withStrategyId(0L)
                .withDayBudget(BigDecimal.ZERO.setScale(2))
                .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
                .withDayBudgetDailyChangeCount(0)
                .withDayBudgetNotificationStatus(CampaignDayBudgetNotificationStatus.READY)
                .withStatusActive(false)
                .withStatusEmpty(false)
                .withStatusShow(true)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withStatusArchived(false)
                .withBrandSafetyCategories(emptyList())
                .withMeasurers(emptyList())
                .withIsSkadNetworkEnabled(false)
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withIsCpmGlobalAbSegment(false)
                .withIsSearchLiftEnabled(false)
                .withIsBrandLiftHidden(false);

        assertThat(campaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(
                getCampaignCompareStrategy()
                        .forFields(newPath(CampaignWithCustomDayBudget.DAY_BUDGET_LAST_CHANGE.name()))
                        .useMatcher(nullValue()))));
    }

    @Test
    public void addCpmBannerCampaign() {
        createClient(defaultClient()
                .withWorkCurrency(CurrencyCode.RUB)
                .withCountryRegionId(RUSSIA_REGION_ID));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        var startDate = LocalDate.of(currentYear + 1, 1, 1);
        var endDate = LocalDate.of(currentYear + 1, 12, 1);

        GdAddCmpBannerCampaign cpmBannerCampaign = TestGdAddCampaigns.defaultCpmBannerCampaign(startDate, endDate,
                defaultAttributionModel)
                .withIsAllowedOnAdultContent(true)
                .withBrandSafety(new GdCampaignBrandSafetyRequest()
                        .withIsEnabled(true)
                        .withAdditionalCategories(emptySet()));

        var gdAddCampaignUnion = new GdAddCampaignUnion().withCpmBannerCampaign(cpmBannerCampaign);
        var input = new GdAddCampaigns().withCampaignAddItems(List.of(gdAddCampaignUnion));

        var gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION,
                input, operator);

        assertThat(gdAddCampaignPayload.getValidationResult()).isNull();
        assertThat(gdAddCampaignPayload.getAddedCampaigns()).hasSize(1);

        var campaigns = campaignTypedRepository.getTypedCampaigns(shard,
                mapList(gdAddCampaignPayload.getAddedCampaigns(), GdAddCampaignPayloadItem::getId));

        var expectedProduct =
                productService.calculateProductForCampaign(CampaignType.CPM_BANNER, CurrencyCode.RUB, false);

        assertThat(campaigns).hasSize(1);
        var campaign = (CpmBannerCampaign) campaigns.get(0);
        var expectedCampaign = new CpmBannerCampaign()
                .withType(CampaignType.CPM_BANNER)
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong())
                .withAgencyId(0L)
                .withFio(operator.getFio())
                .withIsServiceRequested(false)
                .withOrderId(0L)
                .withWalletId(0L)
                .withName("New Cpm Banner Campaign")
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withContentLanguage(ContentLanguage.RU)
                .withCurrency(CurrencyCode.RUB)
                .withProductId(expectedProduct.getId())
                .withTimeTarget(toTimeTarget(defaultGdTimeTarget()))
                .withTimeZoneId(DEFAULT_TIMEZONE)
                .withMetrikaCounters(null)
                .withAttributionModel(defaultAttributionModel)
                .withEnableCpcHold(false)
                .withHasTurboApp(false)
                .withIsVirtual(false)
                .withEnableCompanyInfo(true)
                .withHasExtendedGeoTargeting(false)
                .withUseCurrentRegion(false)
                .withUseRegularRegion(false)
                .withHasTitleSubstitution(true)
                .withHasAddMetrikaTagToUrl(true)
                .withHasAddOpenstatTagToUrl(false)
                .withSmsTime(CampaignConstants.DEFAULT_SMS_TIME_INTERVAL)
                .withSmsFlags(EnumSet.of(CAMP_FINISHED_SMS, NOTIFY_ORDER_MONEY_IN_SMS))
                .withEmail(TEST_EMAIL)
                .withHref(TEST_HREF)
                .withWarningBalance(CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE)
                .withEnableSendAccountNews(true)
                .withEnableOfflineStatNotice(true)
                .withEnablePausedByDayBudgetEvent(true)
                .withSum(BigDecimal.ZERO)
                .withSumLast(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO)
                .withSumToPay(BigDecimal.ZERO)
                .withStrategy((DbStrategy) new DbStrategy()
                        .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                        .withPlatform(CampaignsPlatform.CONTEXT)
                        .withStrategyName(StrategyName.CPM_DEFAULT)
                        .withAutobudget(CampaignsAutobudget.NO)
                        .withStrategyData(new StrategyData()
                                .withVersion(1L)
                                .withName(StrategyName.CPM_DEFAULT.name().toLowerCase())
                        ))
                .withStrategyId(0L)
                .withDayBudget(new BigDecimal(400))
                .withDayBudgetShowMode(DayBudgetShowMode.DEFAULT_)
                .withDayBudgetDailyChangeCount(0)
                .withDayBudgetNotificationStatus(CampaignDayBudgetNotificationStatus.READY)
                .withStatusActive(false)
                .withStatusEmpty(false)
                .withStatusShow(true)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withStatusArchived(false)
                .withBrandSafetyRetCondId(2L)
                .withImpressionStandardTime(CampaignConstants.DEFAULT_IMPRESSION_STANDARD_TIME)
                .withHasSiteMonitoring(false)
                .withPaidByCertificate(false)
                .withRequireFiltrationByDontShowDomains(false)
                .withDisabledSsp(emptyList())
                .withAllowedSsp(emptyList())
                .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._60)
                .withEnableCheckPositionEvent(false)
                .withGeo(Set.of((int) RUSSIA_REGION_ID, (int) CRIMEA_REGION_ID))
                .withEshowsSettings(
                        new EshowsSettings()
                                .withBannerRate(EshowsRate.OFF)
                                .withVideoRate(EshowsRate.ON)
                                .withVideoType(EshowsVideoType.COMPLETES)
                )
                .withIsSkadNetworkEnabled(false)
                .withIsAllowedOnAdultContent(true)
                .withMeasurers(emptyList())
                .withIsRecommendationsManagementEnabled(false)
                .withIsPriceRecommendationsManagementEnabled(false)
                .withIsCpmGlobalAbSegment(false)
                .withIsSearchLiftEnabled(false)
                .withIsBrandLiftHidden(false)
                .withCurrencyConverted(false);

        assertThat(campaign).is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(
                getCampaignCompareStrategy()
                        .forFields(newPath(CpmBannerCampaign.BRAND_SAFETY_CATEGORIES.name()))
                        .useMatcher(hasSize(7))
                        .forFields(newPath(CpmBannerCampaign.BRAND_SAFETY_RET_COND_ID.name()))
                        .useMatcher(notNullValue()))));
    }

}
