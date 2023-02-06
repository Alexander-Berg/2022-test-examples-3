package ru.yandex.market.logistics.lom.configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.ping.DBConnectionChecker;
import ru.yandex.market.common.ping.PingChecker;
import ru.yandex.market.delivery.trust.client.TrustClient;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistics.admin.serializer.CsvSerializationSettings;
import ru.yandex.market.logistics.admin.serializer.CsvSerializer;
import ru.yandex.market.logistics.admin.serializer.GridDataSerializer;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.configuration.interceptors.TraceConfiguration;
import ru.yandex.market.logistics.lom.configuration.properties.DeletedEntitiesProperties;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;
import ru.yandex.market.logistics.lom.configuration.properties.OrderCancellationProperties;
import ru.yandex.market.logistics.lom.configuration.properties.OrderUpdateRecipientErrorMonitoringProperties;
import ru.yandex.market.logistics.lom.configuration.properties.OrderValidationErrorMonitoringProperties;
import ru.yandex.market.logistics.lom.configuration.properties.RemoveOldOrdersProperties;
import ru.yandex.market.logistics.lom.configuration.properties.WaybillSegmentStatusHistoryArchivingProperties;
import ru.yandex.market.logistics.lom.configuration.properties.YtProperties;
import ru.yandex.market.logistics.lom.configuration.trust.TrustProperties;
import ru.yandex.market.logistics.lom.converter.EnumConverter;
import ru.yandex.market.logistics.lom.converter.ydb.WaybillSegmentStatusHistoryYdbConverter;
import ru.yandex.market.logistics.lom.converter.yt.YtBusinessProcessStateConverter;
import ru.yandex.market.logistics.lom.jobs.consumer.order.create.DeliveryServiceCreateOrderExternalConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.order.create.FulfillmentCreateOrderExternalConsumer;
import ru.yandex.market.logistics.lom.jobs.executor.ArchiveBusinessProcessesToYtOnlyExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.ArchiveOldBusinessProcessStatesExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.ArchiveSuccessBusinessProcessStatesExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.CheckImportExternalCreationExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.CheckOrderConfirmationExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.CheckWithdrawExternalCreationExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.CleanupDeletedEntitiesIdsExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.CombineYtChunksExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.CreateReturnRegistryExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.EventExportStatisticsExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.LogChangeOrderRequestStatisticsExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.LogExpiredOrderValidationExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.LogOrderCancellationStatisticsExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.LogStatisticsExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.PartnerLegalInfoSyncExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.ProcessLongCancellationsExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.QueueTaskStatisticsExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.RedisFromYtMigrationExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.RedisGenerationLagMetricsExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.RemoveExportedOrderHistoryEventsExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.RemoveOldShootingOrdersExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.WaybillSegmentStatusHistoryArchivingStatisticsExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.WaybillSegmentStatusHistoryYdbArchiverExecutor;
import ru.yandex.market.logistics.lom.jobs.executor.WaybillSegmentStatusHistoryYtArchiverExecutor;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessWaybillService;
import ru.yandex.market.logistics.lom.jobs.producer.CreateReturnRegistryProducer;
import ru.yandex.market.logistics.lom.jobs.producer.UpdateCancellationOrderRequestProducer;
import ru.yandex.market.logistics.lom.lms.client.LmsLomLightClient;
import ru.yandex.market.logistics.lom.lms.converter.RedisObjectConverter;
import ru.yandex.market.logistics.lom.repository.PartnerLegalInfoRepository;
import ru.yandex.market.logistics.lom.repository.QueueTaskRepository;
import ru.yandex.market.logistics.lom.repository.ReturnRegistryRepository;
import ru.yandex.market.logistics.lom.repository.ydb.BusinessProcessStateStatusHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.OrderCombinedRouteHistoryYdbRepository;
import ru.yandex.market.logistics.lom.service.async.DeliveryServiceCreateOrderAsyncResultService;
import ru.yandex.market.logistics.lom.service.async.FulfillmentCreateOrderAsyncResultService;
import ru.yandex.market.logistics.lom.service.deleted.DeletedEntitiesService;
import ru.yandex.market.logistics.lom.service.internalVariable.InternalVariableService;
import ru.yandex.market.logistics.lom.service.marketid.MarketIdService;
import ru.yandex.market.logistics.lom.service.order.ChangeOrderRequestService;
import ru.yandex.market.logistics.lom.service.order.ChangeOrderRequestStatsService;
import ru.yandex.market.logistics.lom.service.order.OrderCancellationService;
import ru.yandex.market.logistics.lom.service.order.OrderCancellationStatsService;
import ru.yandex.market.logistics.lom.service.order.OrderService;
import ru.yandex.market.logistics.lom.service.order.combinator.CombinatorGrpcClient;
import ru.yandex.market.logistics.lom.service.order.combinator.RouteRecalculationService;
import ru.yandex.market.logistics.lom.service.order.history.LogbrokerSourceService;
import ru.yandex.market.logistics.lom.service.order.history.OrderHistoryService;
import ru.yandex.market.logistics.lom.service.process.BusinessProcessStateEntityIdService;
import ru.yandex.market.logistics.lom.service.process.BusinessProcessStateService;
import ru.yandex.market.logistics.lom.service.process.SaveToYtBusinessProcessIdService;
import ru.yandex.market.logistics.lom.service.redis.RedisService;
import ru.yandex.market.logistics.lom.service.registry.RegistryService;
import ru.yandex.market.logistics.lom.service.remove.OrderRemovalService;
import ru.yandex.market.logistics.lom.service.shipment.ShipmentService;
import ru.yandex.market.logistics.lom.service.shooting.ShootingService;
import ru.yandex.market.logistics.lom.service.waybill.TransferCodesService;
import ru.yandex.market.logistics.lom.service.waybill.WaybillSegmentStatusHistoryService;
import ru.yandex.market.logistics.lom.service.ydb.WaybillSegmentStatusHistoryYdbService;
import ru.yandex.market.logistics.lom.service.yt.YtBusinessProcessStatesService;
import ru.yandex.market.logistics.lom.service.yt.YtLmsInfoService;
import ru.yandex.market.logistics.lom.service.yt.YtService;
import ru.yandex.market.logistics.lom.service.yt.YtTransferService;
import ru.yandex.market.logistics.lom.service.yt.YtWaybillSegmentStatusHistoryService;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerLegalInfoService;
import ru.yandex.market.logistics.lom.utils.OrderFlowUtils;
import ru.yandex.market.logistics.lom.utils.UuidGenerator;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;
import ru.yandex.market.logistics.util.client.tvm.client.DetailedTvmClient;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.ydb.integration.YdbTemplate;

@Configuration
@EnableZonkyEmbeddedPostgres
@Import({
    AdminConfiguration.class,
    CacheConfiguration.class,
    ClockConfiguration.class,
    CombinatorConfiguration.class,
    ConvertersConfiguration.class,
    DbQueueConfiguration.class,
    DbUnitTestConfiguration.class,
    ExecutorsConfiguration.class,
    InboundIntervalsConfiguration.class,
    LiquibaseConfiguration.class,
    LogbrokerConfiguration.class,
    LoggingConfiguration.class,
    MdsS3Configuration.class,
    MockControllerConfiguration.class,
    ObjectMapperConfiguration.class,
    PGDataSourceConfiguration.class,
    PingCheckerConfiguration.class,
    RedisTestConfiguration.class,
    RepositoryConfiguration.class,
    RetryConfiguration.class,
    SecurityConfiguration.class,
    SequenceConfiguration.class,
    TraceConfiguration.class,
    YdbConfiguration.class,
    YtClientConfiguration.class,
})
@MockBean({
    CombinatorGrpcClient.class,
    Cypress.class,
    DeliveryClient.class,
    DetailedTvmClient.class,
    FulfillmentClient.class,
    LMSClient.class,
    LesProducer.class,
    LogbrokerClientFactory.class,
    MarketIdService.class,
    MbiApiClient.class,
    MdsS3Client.class,
    MqmClient.class,
    Operation.class,
    ResourceLocationFactory.class,
    ReturnsApi.class,
    TarifficatorClient.class,
    TrackerApiClient.class,
    TrustClient.class,
    TvmClientApi.class,
    WwClient.class,
    YtOperations.class,
    YtTables.class,
})
@MockBean(name = "hahnYt", classes = Yt.class)
@MockBean(name = "arnoldYt", classes = Yt.class)
@MockBean(name = "chYtJdbcTemplate", classes = JdbcTemplate.class)
@MockBean(name = "mockPingChecker", classes = PingChecker.class)
@MockBean(name = "logbrokerIdSequence", classes = DataFieldMaxValueIncrementer.class)
@MockBean(name = "returnEventsLogbrokerIdSequence", classes = DataFieldMaxValueIncrementer.class)
@SpyBean({
    BusinessProcessStateStatusHistoryYdbRepository.class,
    DBConnectionChecker.class,
    FeatureProperties.class,
    LmsLomLightClient.class,
    OrderCombinedRouteHistoryYdbRepository.class,
    OrderService.class,
    OrderUpdateRecipientErrorMonitoringProperties.class,
    OrderValidationErrorMonitoringProperties.class,
    RouteRecalculationService.class,
    TestableClock.class,
    TransferCodesService.class,
    UuidGenerator.class,
    YdbTemplate.class,
    YtService.class,
    YtTransferService.class,
})
public class IntegrationTestConfiguration {

    @Bean
    public TrustProperties trustProperties() {
        return new TrustProperties();
    }

    @Bean
    public Executor checkOrderConfirmation(Clock clock, OrderService orderService) {
        return new CheckOrderConfirmationExecutor(clock, orderService);
    }

    @Bean
    public Executor checkWithdrawExternalCreation(Clock clock, ShipmentService shipmentService) {
        return new CheckWithdrawExternalCreationExecutor(clock, shipmentService);
    }

    @Bean
    public Executor checkImportExternalCreation(Clock clock, ShipmentService shipmentService) {
        return new CheckImportExternalCreationExecutor(clock, shipmentService);
    }

    @Bean
    public Executor createReturnRegistryExecutor(
        OrderService orderService,
        TransactionTemplate transactionTemplate,
        CreateReturnRegistryProducer createReturnRegistryProducer,
        ReturnRegistryRepository returnRegistryRepository
    ) {
        return new CreateReturnRegistryExecutor(
            orderService,
            transactionTemplate,
            createReturnRegistryProducer,
            returnRegistryRepository
        );
    }

    @Bean
    public Executor logStatisticsExecutor(ShipmentService shipmentService, RegistryService registryService) {
        return new LogStatisticsExecutor(shipmentService, registryService);
    }

    @Bean
    public Executor logOrderCancellationStatisticsExecutor(
        OrderCancellationService orderCancellationService,
        OrderCancellationStatsService orderCancellationStatsService,
        Clock clock,
        @Value("${cancellation-stats.order-processing-deadline-hours:0}") long orderProcessingDeadlineHours,
        @Value("${cancellation-stats.segment-processing-deadline-hours:0}") long segmentProcessingDeadlineHours
    ) {
        return new LogOrderCancellationStatisticsExecutor(
            orderCancellationService,
            orderCancellationStatsService,
            clock,
            Duration.ofHours(orderProcessingDeadlineHours),
            Duration.ofHours(segmentProcessingDeadlineHours)
        );
    }

    @Bean
    public Executor logChangeOrderRequestStatisticsExecutor(
        ChangeOrderRequestService changeOrderRequestService,
        ChangeOrderRequestStatsService changeOrderRequestStatsService
    ) {
        return new LogChangeOrderRequestStatisticsExecutor(
            changeOrderRequestService,
            changeOrderRequestStatsService
        );
    }

    @Bean
    public Executor logExpiredOrderValidationExecutor(OrderService orderService, Clock clock) {
        return new LogExpiredOrderValidationExecutor(orderService, clock);
    }

    @Bean
    public Executor eventExportStatisticsExecutor(OrderHistoryService orderHistoryService) {
        return new EventExportStatisticsExecutor(orderHistoryService);
    }

    @Bean
    public Executor removeExportedOrderHistoryEventsExecutor(
        OrderHistoryService orderHistoryService,
        LogbrokerSourceService logbrokerSourceService,
        Clock clock
    ) {
        return new RemoveExportedOrderHistoryEventsExecutor(orderHistoryService, logbrokerSourceService, clock);
    }

    @Bean
    public Executor queueTaskStatisticsExecutor(QueueTaskRepository queueTaskRepository, TestableClock clock) {
        return new QueueTaskStatisticsExecutor(queueTaskRepository, clock);
    }

    @Bean
    @SuppressWarnings({"ParameterNumber", "SpringJavaInjectionPointsAutowiringInspection"})
    public Executor oldBusinessProcessStatesArchiverExecutor(
        Clock clock,
        YtProperties properties,
        YtBusinessProcessStatesService ytBusinessProcessStatesService,
        YtBusinessProcessStateConverter businessProcessStateConverter,
        BusinessProcessStateService businessProcessStateService,
        BusinessProcessStateEntityIdService businessProcessStateEntityIdService,
        InternalVariableService internalVariableService,
        @Qualifier("threadPoolForOldBusinessProcessesMigration") ExecutorService executorService,
        SaveToYtBusinessProcessIdService saveToYtBusinessProcessIdService
    ) {
        return new ArchiveOldBusinessProcessStatesExecutor(
            clock,
            properties.getBusinessProcessStatesArchive(),
            ytBusinessProcessStatesService,
            businessProcessStateConverter,
            businessProcessStateService,
            businessProcessStateEntityIdService,
            internalVariableService,
            executorService,
            saveToYtBusinessProcessIdService
        );
    }

    @Bean
    @SuppressWarnings({"ParameterNumber", "SpringJavaInjectionPointsAutowiringInspection"})
    public Executor successBusinessProcessStatesArchiverExecutor(
        Clock clock,
        YtProperties properties,
        YtBusinessProcessStatesService ytBusinessProcessStatesService,
        YtBusinessProcessStateConverter businessProcessStateConverter,
        BusinessProcessStateService businessProcessStateService,
        BusinessProcessStateEntityIdService businessProcessStateEntityIdService,
        InternalVariableService internalVariableService,
        @Qualifier("threadPoolForSuccessBusinessProcessesMigration") ExecutorService executorService,
        SaveToYtBusinessProcessIdService saveToYtBusinessProcessIdService
    ) {
        return new ArchiveSuccessBusinessProcessStatesExecutor(
            clock,
            properties.getBusinessProcessStatesArchive(),
            ytBusinessProcessStatesService,
            businessProcessStateConverter,
            businessProcessStateService,
            businessProcessStateEntityIdService,
            internalVariableService,
            executorService,
            saveToYtBusinessProcessIdService
        );
    }

    @Bean
    @SuppressWarnings({"ParameterNumber", "SpringJavaInjectionPointsAutowiringInspection"})
    public Executor archiveBusinessProcessesToYtOnlyExecutor(
        Clock clock,
        YtProperties properties,
        YtBusinessProcessStatesService ytBusinessProcessStatesService,
        YtBusinessProcessStateConverter businessProcessStateConverter,
        BusinessProcessStateService businessProcessStateService,
        BusinessProcessStateEntityIdService businessProcessStateEntityIdService,
        InternalVariableService internalVariableService,
        @Qualifier("threadPoolForBusinessProcessesMigrationToYtOnly") ExecutorService executorService,
        SaveToYtBusinessProcessIdService saveToYtBusinessProcessIdService
    ) {
        return new ArchiveBusinessProcessesToYtOnlyExecutor(
            clock,
            properties.getBusinessProcessStatesArchive(),
            ytBusinessProcessStatesService,
            businessProcessStateConverter,
            businessProcessStateService,
            businessProcessStateEntityIdService,
            internalVariableService,
            executorService,
            saveToYtBusinessProcessIdService
        );
    }

    @Bean
    public Executor processLongCancellationsExecutor(
        OrderCancellationService orderCancellationService,
        OrderCancellationProperties orderCancellationProperties,
        UpdateCancellationOrderRequestProducer updateCancellationOrderRequestProducer,
        LmsLomLightClient lmsLomLightClient
    ) {
        return new ProcessLongCancellationsExecutor(
            orderCancellationService,
            orderCancellationProperties,
            updateCancellationOrderRequestProducer,
            lmsLomLightClient
        );
    }

    @Bean
    @SuppressWarnings("ParameterNumber")
    public Executor redisFromYtMigrationExecutor(
        RedisService redisService,
        YtService ytService,
        YtLmsInfoService ytLmsInfoService,
        InternalVariableService internalVariableService,
        @Qualifier("threadPoolExecutorServiceForYtToRedisMigration") ExecutorService executorService,
        LmsYtProperties lmsYtProperties,
        TestableClock clock,
        RedisObjectConverter redisObjectConverter
    ) {
        return new RedisFromYtMigrationExecutor(
            redisService,
            executorService,
            ytService,
            ytLmsInfoService,
            redisObjectConverter,
            internalVariableService,
            lmsYtProperties,
            clock
        );
    }

    @Bean
    public OrderFlowUtils.FlowCreatorFactory flowCreatorFactory(
        QueueTaskChecker queueTaskChecker,
        ProcessWaybillService processWaybillService,
        DeliveryServiceCreateOrderExternalConsumer deliveryServiceCreateOrderExternalConsumer,
        DeliveryServiceCreateOrderAsyncResultService deliveryServiceCreateOrderAsyncResultService,
        FulfillmentCreateOrderExternalConsumer fulfillmentCreateOrderExternalConsumer,
        FulfillmentCreateOrderAsyncResultService fulfillmentCreateOrderAsyncResultService
    ) {
        return new OrderFlowUtils.FlowCreatorFactory(
            queueTaskChecker,
            processWaybillService,
            deliveryServiceCreateOrderExternalConsumer,
            deliveryServiceCreateOrderAsyncResultService,
            fulfillmentCreateOrderExternalConsumer,
            fulfillmentCreateOrderAsyncResultService
        );
    }

    @Bean
    public GridDataSerializer<CsvSerializationSettings> csvSerializer() {
        return new CsvSerializer();
    }

    @Bean
    public Executor partnerLegalInfoSyncExecutor(
        YtPartnerLegalInfoService ytPartnerLegalInfoService,
        EnumConverter enumConverter,
        PartnerLegalInfoRepository partnerLegalInfoRepository,
        YtProperties ytProperties
    ) {
        return new PartnerLegalInfoSyncExecutor(
            ytPartnerLegalInfoService,
            enumConverter,
            partnerLegalInfoRepository,
            ytProperties
        );
    }

    @Bean
    public Executor waybillSegmentStatusHistoryYdbArchiverExecutor(
        WaybillSegmentStatusHistoryArchivingProperties properties,
        WaybillSegmentStatusHistoryService waybillSegmentStatusHistoryService,
        WaybillSegmentStatusHistoryYdbConverter waybillSegmentStatusHistoryYdbConverter,
        WaybillSegmentStatusHistoryYdbService waybillSegmentStatusHistoryYdbService
    ) {
        return new WaybillSegmentStatusHistoryYdbArchiverExecutor(
            properties.getYdbBatchSize(),
            waybillSegmentStatusHistoryService,
            waybillSegmentStatusHistoryYdbConverter,
            waybillSegmentStatusHistoryYdbService
        );
    }

    @Bean
    public Executor waybillSegmentStatusHistoryYtArchiverExecutor(
        YtService ytService,
        Clock clock,
        YtProperties properties,
        WaybillSegmentStatusHistoryYdbService waybillSegmentStatusHistoryYdbService,
        WaybillSegmentStatusHistoryYdbConverter waybillSegmentStatusHistoryYdbConverter,
        YtWaybillSegmentStatusHistoryService ytWaybillSegmentStatusHistoryService
    ) {
        return new WaybillSegmentStatusHistoryYtArchiverExecutor(
            ytService,
            properties.getWaybillSegmentStatusHistoryArchive(),
            clock,
            waybillSegmentStatusHistoryYdbService,
            waybillSegmentStatusHistoryYdbConverter,
            ytWaybillSegmentStatusHistoryService
        );
    }

    @Bean
    public Executor waybillSegmentStatusHistoryArchivingStatisticsExecutor(
        WaybillSegmentStatusHistoryService waybillSegmentStatusHistoryService,
        InternalVariableService internalVariableService
    ) {
        return new WaybillSegmentStatusHistoryArchivingStatisticsExecutor(
            waybillSegmentStatusHistoryService,
            internalVariableService
        );
    }

    @Bean
    public Executor redisGenerationLagMetricsExecutor(RedisService redisService, Clock clock) {
        return new RedisGenerationLagMetricsExecutor(redisService, clock);
    }

    @Bean
    public Executor cleanupDeletedEntitiesIdsExecutor(
        DeletedEntitiesProperties deletedEntitiesProperties,
        DeletedEntitiesService deletedEntitiesService,
        Clock clock
    ) {
        return new CleanupDeletedEntitiesIdsExecutor(deletedEntitiesProperties, deletedEntitiesService, clock);
    }

    @Bean
    public Executor removeOldShootingOrdersExecutor(
        OrderRemovalService orderRemovalService,
        Clock clock,
        ShootingService shootingService,
        RemoveOldOrdersProperties removeProperties
    ) {
        return new RemoveOldShootingOrdersExecutor(orderRemovalService, clock, shootingService, removeProperties);
    }

    @Bean
    public Executor combineBusinessProcessChunks(
        YtProperties properties,
        YtService ytService
    ) {
        return new CombineYtChunksExecutor(
            ytService,
            Objects.requireNonNull(properties.getBusinessProcessStatesArchive().getTablePath())
        );
    }
}
