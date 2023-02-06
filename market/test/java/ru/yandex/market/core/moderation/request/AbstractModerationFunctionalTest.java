package ru.yandex.market.core.moderation.request;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.common.framework.core.MultipartRemoteFile;
import ru.yandex.common.framework.core.RemoteFile;
import ru.yandex.market.common.balance.BalanceConstants;
import ru.yandex.market.common.test.db.SingleFileCsvProducer;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.IdsResolver;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.complex.ComplexCampaignService;
import ru.yandex.market.core.cutoff.CutoffNotificationService;
import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.delivery.tariff.service.DeliveryTariffService;
import ru.yandex.market.core.ds.DatasourceCreationService;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.core.ds.DatasourceTransactionTemplate;
import ru.yandex.market.core.ds.model.DatasourceInfo;
import ru.yandex.market.core.feature.FeatureCutoffService;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureCutoffInfo;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.cutoff.FeatureCustomCutoffType;
import ru.yandex.market.core.feed.FeedService;
import ru.yandex.market.core.feed.datacamp.FeedParsingType;
import ru.yandex.market.core.feed.model.FeedInfo;
import ru.yandex.market.core.feed.model.FeedSiteType;
import ru.yandex.market.core.feed.model.FeedUpload;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.model.RegionConstants;
import ru.yandex.market.core.moderation.ModerationService;
import ru.yandex.market.core.moderation.ShopsModeratedSupplier;
import ru.yandex.market.core.moderation.TestingShop;
import ru.yandex.market.core.moderation.approve.ModerationStatusApprovingEntryPoint;
import ru.yandex.market.core.moderation.feed.passed.ModerationFeedsLoadCheckPassedEntryPoint;
import ru.yandex.market.core.moderation.passed.PassModerationEntryPoint;
import ru.yandex.market.core.moderation.qc.result.Message;
import ru.yandex.market.core.moderation.qc.result.PremoderationResult;
import ru.yandex.market.core.moderation.qc.result.PremoderationResultEntryPoint;
import ru.yandex.market.core.moderation.recommendation.service.PartnerSettingsRecommendationService;
import ru.yandex.market.core.moderation.sandbox.SandboxRepository;
import ru.yandex.market.core.moderation.sandbox.SandboxStateFactory;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.orginfo.OrganizationInfoService;
import ru.yandex.market.core.orginfo.model.OrganizationInfo;
import ru.yandex.market.core.orginfo.model.OrganizationInfoSource;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.NumberParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.StringParamValue;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.SystemActionContext;
import ru.yandex.market.core.protocol.model.UIDActionContext;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingService;
import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.Functional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
class AbstractModerationFunctionalTest extends FunctionalTest {
    //    static final long ACTION_ID = 789;
    static final long USER_ID = 456;

    private static final RemoteFile FILE_FOR_UPLOADING =
            new MultipartRemoteFile(new MockMultipartFile("test.file.name", "test.content".getBytes()));

    @Autowired
    Clock clock;

    @Autowired
    ModerationRequestEntryPoint moderationRequestEntryPoint;

    @Autowired
    TestingService testingService;

    @Autowired
    ShopsModeratedSupplier shopsModeratedSupplier;

    @Autowired
    ProtocolService protocolService;

    @Autowired
    CutoffService cutoffService;

    @Autowired
    CutoffNotificationService cutoffNotificationService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    DataSource dataSource;

    @Autowired
    ModerationService moderationService;

    @Autowired
    SandboxRepository sandboxRepository;

    @Autowired
    DatasourceCreationService datasourceCreationService;

    @Autowired
    DatasourceService datasourceService;

    @Autowired
    ModerationStatusApprovingEntryPoint moderationStatusApprover;

    @Autowired
    ModerationFeedsLoadCheckPassedEntryPoint moderationFeedsLoadCheckPassedEntryPoint;

    @Autowired
    PremoderationResultEntryPoint premoderationResultEntryPoint;

    @Autowired
    ParamService paramService;

    @Autowired
    FeedService feedService;

    @Autowired
    CampaignService campaignService;

    @Autowired
    DatasourceTransactionTemplate transactionTemplate;

    @Autowired
    IdsResolver idsResolver;

    @Autowired
    ComplexCampaignService complexCampaignService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    SandboxStateFactory sandboxStateFactory;

    @Autowired
    RegionService regionService;

    @Autowired
    EnvironmentService environmentService;

    @Autowired
    PartnerTypeAwareService partnerTypeAwareService;

    @Autowired
    FeatureService featureService;

    @Autowired
    FeatureCutoffService featureCutoffService;

    PassModerationEntryPoint passModerationEntryPoint;

    PartnerSettingsRecommendationService partnerSettingsRecommendationService;

    @Autowired
    DeliveryTariffService deliveryTariffService;

    @Autowired
    OrganizationInfoService organizationInfoService;


    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(SingleFileCsvProducer.Functions.sysdate(1).toInstant());

        partnerSettingsRecommendationService = mock(PartnerSettingsRecommendationService.class);

        passModerationEntryPoint = new PassModerationEntryPoint(
                transactionTemplate,
                sandboxRepository,
                cutoffService,
                paramService,
                datasourceService,
                idsResolver,
                complexCampaignService,
                notificationService,
                sandboxStateFactory,
                partnerSettingsRecommendationService,
                regionService,
                partnerTypeAwareService,
                applicationEventPublisher,
                featureCutoffService);
    }

    void assertThatAllProgramsAreOn(long datasourceID, Set<ShopProgram> programs) {
        assertThat(programs).noneMatch(program -> isCuttedOff(datasourceID, program));
    }

    void assertThatAllProgramsAreCuttedOff(long datasourceID, Set<ShopProgram> programs) {
        assertThat(programs).allMatch(program -> isCuttedOff(datasourceID, program));
    }

    void withinAction(Functional.ThrowingConsumer<Long> actionIdCallback) {
        protocolService.operationInTransaction(
                new UIDActionContext(ActionType.START_MODERATION, USER_ID),
                (status, actionId) -> actionIdCallback.accept(actionId)
        );
    }

    void openCutoff(ShopActionContext context, CutoffType cutoffType) {
        cutoffService.openCutoff(context, cutoffType);
    }

    void closeCutoff(ShopActionContext context, CutoffType cutoffType) {
        cutoffService.closeCutoff(context, cutoffType);
    }

    private boolean isCuttedOff(long datasourceID, ShopProgram program) {
        Set<CutoffType> allCutoffs = getAllCutoffs(datasourceID);
        return allCutoffs.stream().anyMatch(c -> c.cutsOff(program));
    }

    @Nonnull
    protected Set<CutoffType> getAllCutoffs(long datasourceID) {
        return cutoffService.getCutoffs(datasourceID, CutoffType.ALL_CUTOFFS).keySet();
    }

    @Nonnull
    protected Set<FeatureCustomCutoffType> getAllFeatureCutoffs(long datasourceID, FeatureType featureType) {
        return featureService.getCutoffs(datasourceID, featureType).stream()
                .map(FeatureCutoffInfo::getFeatureCutoffType)
                .collect(Collectors.toUnmodifiableSet());
    }

    long createDatasource(long actionId, Set<ShopProgram> programs) {
        return createDatasource(actionId, programs, true, false);
    }

    long createDatasource(
            long actionId,
            Set<ShopProgram> programs,
            boolean shouldPassModeration,
            boolean createCampaign
    ) {
        long datasourceID = createDatasourceWithProgramsDisabled(actionId, true, createCampaign, programs);
        createFeed(actionId, datasourceID);
        partnerTypeAwareService.activateCpcProgram(actionId, datasourceID);

        ShopActionContext context = new ShopActionContext(actionId, datasourceID);
        if (shouldPassModeration) {
            passModeration(context, programs);
        }
        return datasourceID;
    }

    long createDatasourceWithUploadFeed(long actionId, Set<ShopProgram> programs, Date uploadDate) throws IOException {
        long datasourceID = createDatasourceWithProgramsDisabled(actionId, false, false, programs);
        createUploadFeed(actionId, datasourceID, uploadDate);
        partnerTypeAwareService.activateCpcProgram(actionId, datasourceID);
        return datasourceID;
    }

    void requestRequiredModeration(ShopActionContext context) {
        moderationRequestEntryPoint.requestRequiredModeration(context);
    }

    long createDatasourceWithProgramsDisabled(
            long actionId,
            boolean createDatafeed,
            boolean createCampaign,
            Set<ShopProgram> programs
    ) {
        DatasourceInfo datasource = new DatasourceInfo();
        datasource.setInternalName("test1.shop.ru");
        datasource.setManagerId(USER_ID);
        if (programs.contains(ShopProgram.CPC)) {
            datasource.setPlacementTypes(List.of(PartnerPlacementProgramType.CPC));
        }
        datasourceCreationService.createDatasource(datasource, actionId, CampaignType.SHOP);

        long datasourceId = datasource.getId();
        if (createDatafeed) {
            createFeed(actionId, datasourceId);
        }

        if (createCampaign) {
            campaignService.createCampaign(new CampaignInfo(datasourceId, 1L),
                    1L,
                    actionId,
                    BalanceConstants.SHOP_BALANCE_ORDER_PRODUCT_ID);
        }

        paramService.setParam(
                new NumberParamValue(ParamType.HOME_REGION, datasourceId,
                        BigDecimal.valueOf(RegionConstants.RUSSIA)),
                actionId
        );
        paramService.setParam(
                new NumberParamValue(ParamType.LOCAL_DELIVERY_REGION, datasourceId,
                        BigDecimal.valueOf(RegionConstants.MOSCOW)),
                actionId
        );
        paramService.setParam(
                new StringParamValue(ParamType.DATASOURCE_DOMAIN, datasourceId, "www.test.ru"),
                actionId
        );
        OrganizationInfo orgInfo = new OrganizationInfo();
        orgInfo.setDatasourceId(datasourceId);
        orgInfo.setType(OrganizationType.NONE);
        orgInfo.setName("name");
        orgInfo.setJuridicalAddress("juridical_address");
        orgInfo.setOgrn("1027700132195");
        orgInfo.setRegistrationNumber("reg_num");
        orgInfo.setInfoSource(OrganizationInfoSource.YA_MONEY);
        organizationInfoService.createOrUpdateOrganizationInfo(orgInfo, actionId);
        return datasourceId;
    }

    void passModeration(ShopActionContext context, Set<ShopProgram> programs) {
        moderationRequestEntryPoint.requestRequiredModeration(context);
        passAlreadyStartedModeration(context, programs);
    }

    void assertDoesNotHaveDatasourcesInTesting(long datasourceID, ShopProgram program) {
        assertThat(testingService.getTestingStatus(datasourceID, program)).isNull();
    }

    void assertDoesNotHaveCutoff(long datasourceID, CutoffType cutoffType) {
        assertThat(cutoffService.getCutoff(datasourceID, cutoffType)).isNull();
    }

    void assertHasCutoff(long datasourceID, CutoffType cutoffType) {
        assertThat(cutoffService.getCutoff(datasourceID, cutoffType)).isNotNull();
    }

    void assertTestingStatePushReadyButtonCount(int count, TestingState testingState) {
        assertThat(testingState.getPushReadyButtonCount()).isEqualTo(count);
    }

    void assertTestingStateTestingStatus(TestingStatus status, TestingState testingState) {
        assertThat(testingState.getStatus()).isEqualTo(status);
    }

    void assertTestingState(long datasourceID, ShopProgram program, Consumer<TestingState> consumer) {
        TestingState testingStatus = testingService.getTestingStatus(datasourceID, program);
        assertThat(testingStatus).isNotNull();
        consumer.accept(testingStatus);
    }

    protected void passAlreadyStartedModeration(ShopActionContext context, Set<ShopProgram> programs) {
        long datasourceID = context.getShopId();
        for (ShopProgram program : programs) {
            skipModerationDelayAndConfirmModerationRequest(datasourceID, program);
            skipModerationDelayAndStartMainModerationProcess(datasourceID, program);
            confirmModerationSandboxFeedLoad(datasourceID, program);
            submitModerationResult(datasourceID, program, ModerationResult.PASSED);
            confirmModerationSandboxFeedLoad(datasourceID, program);
            finishPassedModeration(datasourceID, program);
        }
    }

    void skipModerationDelayAndConfirmModerationRequest(long datasourceID, ShopProgram program) {
        TestingState testingStatus = testingService.getTestingStatus(datasourceID, program);
        if (testingStatus.getStatus() == TestingStatus.READY_FOR_CHECK) {
            assertThat(shopsModeratedSupplier.getWaitingStartApprovingShopIds())
                    .anyMatch(t -> t.getTestingId() == testingStatus.getId());
            moderationStatusApprover.approveCheckStart(
                    new SystemActionContext(ActionType.TEST_ACTION),
                    new TestingShop(testingStatus.getId(), datasourceID));
        }
    }

    void skipModerationDelayAndStartMainModerationProcess(long datasourceID, ShopProgram program) {
        TestingState testingStatus = testingService.getTestingStatus(datasourceID, program);
        if (testingStatus.getStatus() == TestingStatus.PENDING_CHECK_START) {
            protocolService.operationInTransaction(
                    new SystemActionContext(ActionType.START_MODERATION),
                    (transactionStatus, actionId) -> moderationRequestEntryPoint.startTesting(new ShopActionContext(actionId, datasourceID), program)
            );
        }
    }

    void confirmModerationSandboxFeedLoad(long datasourceID, ShopProgram program) {
        TestingState testingStatus = testingService.getTestingStatus(datasourceID, program);
        moderationFeedsLoadCheckPassedEntryPoint.pass(
                new SystemActionContext(ActionType.TEST_ACTION),
                new TestingShop(testingStatus.getId(), datasourceID));
    }

    void confirmModerationSandboxFeedLoadForced(long datasourceID, ShopProgram program) {
        TestingState testingStatus = testingService.getTestingStatus(datasourceID, program);
        moderationFeedsLoadCheckPassedEntryPoint.passForced(
                new SystemActionContext(ActionType.TEST_ACTION),
                new TestingShop(testingStatus.getId(), datasourceID));
    }

    void submitModerationResult(long datasourceID, ShopProgram program, ModerationResult result) {
        TestingState testingStatus = testingService.getTestingStatus(datasourceID, program);
        TestingType testingType = testingStatus.getTestingType();
        premoderationResultEntryPoint.accept(
                new SystemActionContext(ActionType.TEST_ACTION),
                PremoderationResult.of(
                        datasourceID,
                        testingType,
                        result.cloneCheck,
                        result.qualityCheck,
                        result.orderCheck,
                        result.offersCheck,
                        result.message
                                .map(message -> Message.of(0, "test", message, Collections.emptyList()))
                                .orElse(null),
                        null));
    }

    void finishPassedModeration(long datasourceID, ShopProgram program) {
        TestingState testingStatus = testingService.getTestingStatus(datasourceID, program);
        assertThat(shopsModeratedSupplier.getPassModerationShopIds())
                .anyMatch(t -> t.getTestingId() == testingStatus.getId());
        passModerationEntryPoint.pass(
                new SystemActionContext(ActionType.TEST_ACTION),
                new TestingShop(testingStatus.getId(), datasourceID));
    }

    void finishFailedModeration(long datasourceID, ShopProgram program) {
        TestingState testingStatus = testingService.getTestingStatus(datasourceID, program);
        assertThat(shopsModeratedSupplier.getWaitingFailApprovingShopIds())
                .anyMatch(t -> t.getTestingId() == testingStatus.getId());
        moderationStatusApprover.approveFail(
                new SystemActionContext(ActionType.TEST_ACTION),
                new TestingShop(testingStatus.getId(), datasourceID));
    }

    void createUploadFeed(long actionId, long datasourceId, Date uploadDate) {
        FeedUpload feedUpload = new FeedUpload(datasourceId, "uploadFeed", uploadDate);
        try {
            feedUpload = feedService.uploadFeed(feedUpload, FILE_FOR_UPLOADING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        FeedInfo feed = new FeedInfo();
        feed.setUrl("http://test1.shop.ru/feed.xml");
        feed.setEnabled(true);
        feed.setDatasourceId(datasourceId);
        feed.setSiteType(FeedSiteType.MARKET);
        feed.setUpload(feedUpload.getId(), null);
        feedService.createFeed(feed, FeedParsingType.COMPLETE_FEED, actionId);
    }

    public void createFeed(long actionId, long datasourceId) {
        final FeedInfo feed = new FeedInfo();
        feed.setUrl("http://test1.shop.ru/feed.xml");
        feed.setEnabled(true);
        feed.setDatasourceId(datasourceId);
        feed.setSiteType(FeedSiteType.MARKET);
        feedService.createFeed(feed, FeedParsingType.COMPLETE_FEED, actionId);
    }

    static class ModerationResult {
        static final ModerationResult PASSED =
                new ModerationResult(
                        PremoderationResult.Status.PASSED,
                        PremoderationResult.Status.PASSED,
                        PremoderationResult.Status.PASSED,
                        PremoderationResult.Status.PASSED);

        private final PremoderationResult.Status cloneCheck;
        private final PremoderationResult.Status qualityCheck;
        private final PremoderationResult.Status orderCheck;
        private final PremoderationResult.Status offersCheck;
        private final Optional<String> message;

        ModerationResult(PremoderationResult.Status cloneCheck,
                         PremoderationResult.Status qualityCheck,
                         PremoderationResult.Status orderCheck,
                         PremoderationResult.Status offersCheck,
                         @Nullable String message) {

            this.cloneCheck = cloneCheck;
            this.qualityCheck = qualityCheck;
            this.orderCheck = orderCheck;
            this.offersCheck = offersCheck;
            this.message = Optional.ofNullable(message);
        }

        ModerationResult(PremoderationResult.Status cloneCheck,
                         PremoderationResult.Status qualityCheck,
                         PremoderationResult.Status orderCheck,
                         PremoderationResult.Status offersCheck) {
            this(cloneCheck, qualityCheck, orderCheck, offersCheck, null);
        }
    }
}
