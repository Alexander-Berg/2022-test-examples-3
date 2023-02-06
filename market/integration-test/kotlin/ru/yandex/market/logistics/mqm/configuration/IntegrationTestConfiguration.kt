package ru.yandex.market.logistics.mqm.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Clock
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.common.util.date.TestableClock
import ru.yandex.geobase.HttpGeobase
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.operations.YtOperations
import ru.yandex.inside.yt.kosher.tables.YtTables
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.mqm.configuration.datasource.LiquibaseConfiguration
import ru.yandex.market.logistics.mqm.configuration.properties.CheckouterOrderHistoryEventConsumerProperties
import ru.yandex.market.logistics.mqm.configuration.properties.CreateClaimExecutorProperties
import ru.yandex.market.logistics.mqm.configuration.properties.PlanFactYtArchiveProperties
import ru.yandex.market.logistics.mqm.configuration.properties.ProcessLomEventsExecutorProperties
import ru.yandex.market.logistics.mqm.configuration.properties.RescheduleFailedEntitiesProperties
import ru.yandex.market.logistics.mqm.configuration.properties.SchedulingProperties
import ru.yandex.market.logistics.mqm.configuration.properties.YtProperties
import ru.yandex.market.logistics.mqm.converter.lom.LomOrderConverter
import ru.yandex.market.logistics.mqm.converter.yt.YtClaimUnitConverter
import ru.yandex.market.logistics.mqm.converter.yt.YtPlanFactAnalyticsConverter
import ru.yandex.market.logistics.mqm.converter.yt.YtPlanFactConverter
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssueForClaimPayload
import ru.yandex.market.logistics.mqm.ow.OwClient
import ru.yandex.market.logistics.mqm.queue.producer.CloseIssueLinkProducer
import ru.yandex.market.logistics.mqm.queue.producer.HandlePlanFactGroupProducer
import ru.yandex.market.logistics.mqm.queue.producer.HandlePlanFactProducer
import ru.yandex.market.logistics.mqm.queue.producer.LomCancellationRequestCreatedProducer
import ru.yandex.market.logistics.mqm.queue.producer.LomOrderDeliveryDateChangedProducer
import ru.yandex.market.logistics.mqm.queue.producer.LomOrderLastMileChangedProducer
import ru.yandex.market.logistics.mqm.queue.producer.LomOrderStatusChangedProducer
import ru.yandex.market.logistics.mqm.queue.producer.LomWaybillSegmentStatusAddedProducer
import ru.yandex.market.logistics.mqm.queue.producer.RetrieveLomOrderCombinatorRouteProducer
import ru.yandex.market.logistics.mqm.queue.producer.UpdateLomOrderCombinatorRouteProducer
import ru.yandex.market.logistics.mqm.repository.PlanFactGroupRepository
import ru.yandex.market.logistics.mqm.service.ClaimOrdersParser
import ru.yandex.market.logistics.mqm.service.ClaimPdfGeneratorService
import ru.yandex.market.logistics.mqm.service.ClaimService
import ru.yandex.market.logistics.mqm.service.PartnerService
import ru.yandex.market.logistics.mqm.service.PlanFactAggregationService
import ru.yandex.market.logistics.mqm.service.PlanFactAnalyticsService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.SchedulingLockService
import ru.yandex.market.logistics.mqm.service.logging.LogService
import ru.yandex.market.logistics.mqm.service.lom.LomOrderEventsService
import ru.yandex.market.logistics.mqm.service.lom.LomOrderService
import ru.yandex.market.logistics.mqm.service.lom.LomWaybillSegmentService
import ru.yandex.market.logistics.mqm.service.lom.QueueTaskService
import ru.yandex.market.logistics.mqm.service.monitoringevent.MonitoringEventService
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.QualityRuleAggregatedProcessor
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.QualityRuleSingleProcessor
import ru.yandex.market.logistics.mqm.service.tms.PartnerExpectedDateService
import ru.yandex.market.logistics.mqm.service.yt.PvzContactInformationCache
import ru.yandex.market.logistics.mqm.service.yt.YtClientReturnPlanFactService
import ru.yandex.market.logistics.mqm.service.yt.YtService
import ru.yandex.market.logistics.mqm.service.ytevents.CourierShiftValidateService
import ru.yandex.market.logistics.mqm.service.ytevents.reader.courier.YtCourierShiftFinishTimeReader
import ru.yandex.market.logistics.mqm.tms.AggregationExecutor
import ru.yandex.market.logistics.mqm.tms.PlanFactAnalyticsYtArchiveExecutor
import ru.yandex.market.logistics.mqm.tms.PlanFactYtArchiveExecutor
import ru.yandex.market.logistics.mqm.tms.ProcessLomEventsExecutor
import ru.yandex.market.logistics.mqm.tms.QueueStatisticsExecutor
import ru.yandex.market.logistics.mqm.tms.SchedulingPlanFactExecutor
import ru.yandex.market.logistics.mqm.tms.SchedulingPlanFactGroupExecutor
import ru.yandex.market.logistics.mqm.tms.claim.CreateExpressClaimExecutor
import ru.yandex.market.logistics.mqm.tms.claim.MerchClaimYtArchiveExecutor
import ru.yandex.market.logistics.mqm.tms.clientreturn.CreateClientReturnDeadlinesExecutor
import ru.yandex.market.logistics.mqm.tms.clientreturn.CreateClientReturnFailDeadlinesExecutor
import ru.yandex.market.logistics.mqm.tms.clientreturn.CreateClientReturnFirstCteIntakeDeadlinesExecutor
import ru.yandex.market.logistics.mqm.tms.clientreturn.CreateClientReturnPvzShipmentDeadlinesExecutor
import ru.yandex.market.logistics.mqm.tms.clientreturn.CreateClientReturnScShipmentDeadlinesExecutor
import ru.yandex.market.logistics.mqm.tms.clientreturn.CreateClientToCteReturnDeadlinesExecutor
import ru.yandex.market.logistics.mqm.tms.clientreturn.UpdateClientReturnDeadlinesFactTimeExecutor
import ru.yandex.market.logistics.mqm.tms.clientreturn.UpdateClientReturnFirstCteIntakeDeadlinesExecutor
import ru.yandex.market.logistics.mqm.tms.clientreturn.UpdateClientReturnPvzShipmentDeadlinesExecutor
import ru.yandex.market.logistics.mqm.tms.clientreturn.UpdateClientReturnScShipmentDeadlinesExecutor
import ru.yandex.market.logistics.mqm.tms.clientreturn.UpdateReturnFromClientToCteDeadlinesFactTimeExecutor
import ru.yandex.market.logistics.mqm.tms.housekeeping.RescheduleFailedGroupsExecutor
import ru.yandex.market.logistics.mqm.tms.housekeeping.RescheduleFailedPlanFactsExecutor
import ru.yandex.market.logistics.mqm.xlsx.reader.ClaimOrdersFileReader
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres
import ru.yandex.market.logistics.werewolf.client.WwClient
import ru.yandex.market.mbi.api.client.MbiApiClient
import ru.yandex.market.tms.quartz2.model.Executor
import ru.yandex.startrek.client.Session

@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Configuration
@EnableZonkyEmbeddedPostgres
@Import(
    ClockConfiguration::class,
    DbUnitTestConfiguration::class,
    LiquibaseConfiguration::class,
    RepositoryConfiguration::class,
    DbQueueConfig::class,
    YtClientConfiguration::class,
    WebMvcConfiguration::class,
    SecurityConfiguration::class,
    TestOwClientConfiguration::class,
    TestOkClientConfiguration::class,
    ConverterConfiguration::class,
    StatisticsReportConfiguration::class,
    CacheConfiguration::class,
    LesConfiguration::class
)
@MockBean(
    LMSClient::class,
    LomClient::class,
    Yt::class,
    YtTables::class,
    YtOperations::class,
    PechkinHttpClient::class,
    PvzContactInformationCache::class,
    WwClient::class,
    HttpGeobase::class,
    MdsS3Client::class,
    ResourceLocationFactory::class,
)
@MockBeans(
    MockBean(name = "aggregatedMockProcessor", classes = [QualityRuleAggregatedProcessor::class]),
    MockBean(name = "singleMockProcessor", classes = [QualityRuleSingleProcessor::class]),
    MockBean(name = "startrekSession", classes = [Session::class]),
    MockBean(name = "claimOrdersParser", classes = [ClaimOrdersParser::class]),
    MockBean(name = "claimOrdersFileReader", classes = [ClaimOrdersFileReader::class]),
    MockBean(name = "mbiApiClientLogged", classes = [MbiApiClient::class]),
    MockBean(name = "yqlJdbcTemplate", classes = [JdbcTemplate::class]),
    MockBean(name = "claimPdfGeneratorService", classes = [ClaimPdfGeneratorService::class]),
)
@SpyBean(
    TestableClock::class,
    PlanFactService::class,
    YtService::class,
    OwClient::class,
    YtCourierShiftFinishTimeReader::class,
    CourierShiftValidateService::class,
    CheckouterOrderHistoryEventConsumerProperties::class,
    PartnerService::class,
    CloseIssueLinkProducer::class,
)
@ComponentScan(
    "ru.yandex.market.logistics.mqm.converter",
    "ru.yandex.market.logistics.mqm.service",
    "ru.yandex.market.logistics.mqm.queue",
    "ru.yandex.market.logistics.mqm.controller",
    "ru.yandex.market.logistics.mqm.facade",
    "ru.yandex.market.logistics.mqm.monitoringevent",
    "ru.yandex.market.logistics.mqm.checker",
    "ru.yandex.market.logistics.mqm.repository",
    "ru.yandex.market.logistics.mqm.tms",
    "ru.yandex.market.logistics.mqm.admin",
    "ru.yandex.market.logistics.mqm.configuration.container",
)
@ConfigurationPropertiesScan("ru.yandex")
@EnableAutoConfiguration(exclude = [SecurityAutoConfiguration::class])
@TestPropertySource("classpath:integration-test.properties")
class IntegrationTestConfiguration {

    @Bean
    fun schedulingPlanFactExecutor(
        planFactService: PlanFactService,
        transactionTemplate: TransactionOperations,
        handlePlanFactProducer: HandlePlanFactProducer,
        clock: Clock,
        logService: LogService,
        @Value("\${mqm.handle_plan_fact_group_task_size}") handlePlanFactGroupTaskSize: Int,
        schedulingLockService: SchedulingLockService,
        properties: SchedulingProperties,
    ) = SchedulingPlanFactExecutor(
        planFactService = planFactService,
        transactionTemplate = transactionTemplate,
        clock = clock,
        logService = logService,
        handlePlanFactProducer = handlePlanFactProducer,
        taskSize = handlePlanFactGroupTaskSize,
        schedulingLockService = schedulingLockService,
        properties = properties,
    )

    @Bean
    fun schedulingPlanFactGroupExecutor(
        planFactService: PlanFactService,
        transactionTemplate: TransactionOperations,
        handlePlanFactGroupProducer: HandlePlanFactGroupProducer,
        clock: Clock,
        logService: LogService,
        @Value("\${mqm.handle_plan_fact_group_task_size}") handlePlanFactGroupTaskSize: Int,
        schedulingLockService: SchedulingLockService,
        properties: SchedulingProperties,
    ) = SchedulingPlanFactGroupExecutor(
        planFactService = planFactService,
        transactionTemplate = transactionTemplate,
        clock = clock,
        logService = logService,
        handlePlanFactGroupProducer = handlePlanFactGroupProducer,
        taskSize = handlePlanFactGroupTaskSize,
        schedulingLockService = schedulingLockService,
        properties = properties,
    )

    @Bean
    fun processLomEventsExecutor(
        lomOrderEventsService: LomOrderEventsService,
        lomOrderService: LomOrderService,
        lomOrderConverter: LomOrderConverter,
        lomWaybillSegmentStatusAddedProducer: LomWaybillSegmentStatusAddedProducer,
        lomOrderStatusChangedProducer: LomOrderStatusChangedProducer,
        retrieveLomOrderCombinatorRouteProducer: RetrieveLomOrderCombinatorRouteProducer,
        updateLomOrderCombinatorRouteProducer: UpdateLomOrderCombinatorRouteProducer,
        lomCancellationRequestCreatedProducer: LomCancellationRequestCreatedProducer,
        lomOrderDeliveryDateChangedProducer: LomOrderDeliveryDateChangedProducer,
        lomOrderLastMileChangedProducer: LomOrderLastMileChangedProducer,
        objectMapper: ObjectMapper,
        transactionTemplate: TransactionOperations,
        properties: ProcessLomEventsExecutorProperties,
        lomWaybillSegmentService: LomWaybillSegmentService,
    ) = ProcessLomEventsExecutor(
        ProcessLomEventsExecutor.Context(
            lomOrderEventsService,
            lomOrderService,
            lomOrderConverter,
            lomWaybillSegmentStatusAddedProducer,
            lomOrderStatusChangedProducer,
            retrieveLomOrderCombinatorRouteProducer,
            updateLomOrderCombinatorRouteProducer,
            lomCancellationRequestCreatedProducer,
            lomOrderDeliveryDateChangedProducer,
            lomOrderLastMileChangedProducer,
            objectMapper,
            2,
        ),
        transactionTemplate,
        1,
        properties,
        lomWaybillSegmentService,
    )

    @Bean
    fun createClientReturnPvzShipmentDeadlinesExecutor(
        planFactService: PlanFactService,
        ytClientReturnPlanFactService: YtClientReturnPlanFactService,
        logService: LogService
    ) = CreateClientReturnPvzShipmentDeadlinesExecutor(
        planFactService,
        ytClientReturnPlanFactService,
        logService
    )

    @Bean
    fun createClientReturnFirstCteIntakeDeadlinesExecutor(
        planFactService: PlanFactService,
        logService: LogService,
        ytClientReturnPlanFactService: YtClientReturnPlanFactService,
        ytService: YtService,
        ytProperties: YtProperties
    ): Executor {
        return CreateClientReturnFirstCteIntakeDeadlinesExecutor(
            planFactService,
            ytClientReturnPlanFactService,
            ytService,
            ytProperties,
            logService
        )
    }

    @Bean
    fun createClientReturnScShipmentDeadlinesExecutor(
        planFactService: PlanFactService,
        logService: LogService,
        ytClientReturnPlanFactService: YtClientReturnPlanFactService,
        ytService: YtService,
        ytProperties: YtProperties,
        partnerExpectedDateService: PartnerExpectedDateService
    ): Executor {
        return CreateClientReturnScShipmentDeadlinesExecutor(
            planFactService,
            ytClientReturnPlanFactService,
            ytService,
            ytProperties,
            logService,
            partnerExpectedDateService
        )
    }

    @Bean
    fun createClientReturnWaybillSegmentDeadlinesExecutor(
        ytService: YtService,
        ytProperties: YtProperties,
        planFactService: PlanFactService,
        ytClientReturnPlanFactService: YtClientReturnPlanFactService,
        logService: LogService
    ) = CreateClientReturnDeadlinesExecutor(
        ytService,
        ytProperties,
        planFactService,
        ytClientReturnPlanFactService,
        logService
    )

    @Bean
    fun createClientToCteReturnWaybillSegmentDeadlinesExecutor(
        ytService: YtService,
        ytProperties: YtProperties,
        planFactService: PlanFactService,
        logService: LogService,
        ytClientReturnPlanFactService: YtClientReturnPlanFactService,
        clock: Clock,
        partnerExpectedDateService: PartnerExpectedDateService
    ) = CreateClientToCteReturnDeadlinesExecutor(
        ytService,
        ytProperties,
        planFactService,
        logService,
        ytClientReturnPlanFactService,
        clock,
        partnerExpectedDateService
    )

    @Bean
    fun createClientReturnFailWaybillSegmentDeadlinesExecutor(
        ytService: YtService,
        ytProperties: YtProperties,
        planFactService: PlanFactService,
        logService: LogService,
        clock: Clock
    ) = CreateClientReturnFailDeadlinesExecutor(
        ytService,
        ytProperties,
        planFactService,
        logService,
        clock
    )

    @Bean
    fun updateClientReturnFirstCteIntakeDeadlinesExecutor(
        planFactService: PlanFactService,
        transactionTemplate: TransactionOperations,
        clock: Clock,
        logService: LogService,
        ytClientReturnPlanFactService: YtClientReturnPlanFactService
    ) = UpdateClientReturnFirstCteIntakeDeadlinesExecutor(
        planFactService,
        transactionTemplate,
        clock,
        logService,
        ytClientReturnPlanFactService
    )

    @Bean
    fun updateClientReturnPvzShipmentDeadlinesExecutor(
        planFactService: PlanFactService,
        transactionTemplate: TransactionOperations,
        clock: Clock,
        logService: LogService,
        ytClientReturnPlanFactService: YtClientReturnPlanFactService
    ) = UpdateClientReturnPvzShipmentDeadlinesExecutor(
        planFactService,
        transactionTemplate,
        clock,
        logService,
        ytClientReturnPlanFactService
    )

    @Bean
    fun updateClientReturnScShipmentDeadlinesExecutor(
        planFactService: PlanFactService,
        transactionTemplate: TransactionOperations,
        clock: Clock,
        logService: LogService,
        ytClientReturnPlanFactService: YtClientReturnPlanFactService,
        ytService: YtService,
        ytProperties: YtProperties
    ) = UpdateClientReturnScShipmentDeadlinesExecutor(
        planFactService,
        transactionTemplate,
        clock,
        logService,
        ytClientReturnPlanFactService,
        ytService,
        ytProperties
    )

    @Bean
    fun updateReturnFromClientToCteDeadlinesFactTimeExecutor(
        planFactService: PlanFactService,
        transactionTemplate: TransactionOperations,
        clock: Clock,
        logService: LogService,
        ytClientReturnPlanFactService: YtClientReturnPlanFactService
    ) = UpdateReturnFromClientToCteDeadlinesFactTimeExecutor(
        planFactService,
        transactionTemplate,
        clock,
        logService,
        ytClientReturnPlanFactService
    )

    @Bean
    fun updateClientReturnDeadlinesFactTimeExecutor(
        planFactService: PlanFactService,
        transactionTemplate: TransactionOperations,
        clock: Clock,
        logService: LogService,
        ytClientReturnPlanFactService: YtClientReturnPlanFactService
    ) = UpdateClientReturnDeadlinesFactTimeExecutor(
        planFactService,
        transactionTemplate,
        clock,
        logService,
        ytClientReturnPlanFactService
    )

    @Bean
    fun queueStatisticsExecutor(
        queueTaskService: QueueTaskService,
        lomOrderEventsService: LomOrderEventsService,
        clock: Clock,
    ) = QueueStatisticsExecutor(
        queueTaskService,
        lomOrderEventsService,
        clock,
    )

    @Bean
    fun planFactYtArchiveExecutor(
        planFactService: PlanFactService,
        clock: Clock,
        ytProperties: YtProperties,
        ytService: YtService,
        ytPlanFactConverter: YtPlanFactConverter,
        transactionTemplate: TransactionOperations,
        planFactYtArchiveProperties: PlanFactYtArchiveProperties,
    ) = PlanFactYtArchiveExecutor(
        planFactService,
        clock,
        ytProperties.planFactArchive,
        ytService,
        transactionTemplate,
        ytPlanFactConverter,
        planFactYtArchiveProperties,
    )

    @Bean
    fun planFactAnalyticsYtArchiveExecutor(
        planFactAnalyticsService: PlanFactAnalyticsService,
        ytPlanFactAnalyticsConverter: YtPlanFactAnalyticsConverter,
        ytProperties: YtProperties,
        ytService: YtService,
        ytPlanFactConverter: YtPlanFactConverter,
        transactionTemplate: TransactionOperations,
    ) = PlanFactAnalyticsYtArchiveExecutor(
        planFactAnalyticsService,
        ytPlanFactAnalyticsConverter,
        ytProperties.planFactAnalyticsArchive,
        ytService,
        transactionTemplate
    )

    @Bean
    fun createExpressClaimExecutor(
        claimService: ClaimService,
        lomOrderService: LomOrderService,
        monitoringEventService: MonitoringEventService<CreateStartrekIssueForClaimPayload>,
        clock: Clock,
        properties: CreateClaimExecutorProperties,
        transactionTemplate: TransactionOperations
    ) = CreateExpressClaimExecutor(
        claimService,
        lomOrderService,
        monitoringEventService,
        clock,
        properties,
        transactionTemplate
    )

    @Bean
    fun aggregationExecutor(
        planFactService: PlanFactService,
        transactionTemplate: TransactionOperations,
        clock: Clock,
        logService: LogService,
        aggregationService: PlanFactAggregationService,
    ) = AggregationExecutor(
        planFactService,
        transactionTemplate,
        clock,
        logService,
        aggregationService,
    )

    @Bean
    fun rescheduleFailedPlanFactsExecutor(
        clock: Clock,
        logService: LogService,
        properties: RescheduleFailedEntitiesProperties,
        planFactService: PlanFactService,
        transactionTemplate: TransactionOperations,
    ) = RescheduleFailedPlanFactsExecutor(
        transactionTemplate,
        clock,
        logService,
        properties,
        planFactService,
    )

    @Bean
    fun rescheduleFailedGroupsExecutor(
        transactionTemplate: TransactionOperations,
        clock: Clock,
        logService: LogService,
        properties: RescheduleFailedEntitiesProperties,
        planFactGroupRepository: PlanFactGroupRepository,
    ) = RescheduleFailedGroupsExecutor(
        transactionTemplate,
        clock,
        logService,
        properties,
        planFactGroupRepository,
    )

    @Bean
    fun claimUnitYtArchiveExecutor(
        claimService: ClaimService,
        clock: Clock,
        ytProperties: YtProperties,
        ytService: YtService,
        transactionTemplate: TransactionOperations,
        ytClaimUnitConverter: YtClaimUnitConverter
    ) = MerchClaimYtArchiveExecutor(
        claimService,
        ytProperties.merchClaimArchive,
        ytService,
        transactionTemplate,
        ytClaimUnitConverter,
    )
}
