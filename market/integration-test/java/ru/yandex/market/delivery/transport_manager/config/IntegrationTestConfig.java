package ru.yandex.market.delivery.transport_manager.config;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.market.abo.api.client.AboAPI;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.mds.s3.spring.configuration.MdsS3BasicConfiguration;
import ru.yandex.market.delivery.transport_manager.EntityFactory;
import ru.yandex.market.delivery.transport_manager.config.abo.AboClientProperties;
import ru.yandex.market.delivery.transport_manager.config.admin.AdminConfig;
import ru.yandex.market.delivery.transport_manager.config.admin.DownloadProperties;
import ru.yandex.market.delivery.transport_manager.config.axapta.AxaptaConfiguration;
import ru.yandex.market.delivery.transport_manager.config.cache.CacheConfig;
import ru.yandex.market.delivery.transport_manager.config.datasource.DatabaseSequenceIncrementerConfig;
import ru.yandex.market.delivery.transport_manager.config.datasource.LiquibaseConfig;
import ru.yandex.market.delivery.transport_manager.config.dbqueue.DbQueueConfig;
import ru.yandex.market.delivery.transport_manager.config.dbqueue.DbQueueProperties;
import ru.yandex.market.delivery.transport_manager.config.distribution_center.DcClientConfig;
import ru.yandex.market.delivery.transport_manager.config.distribution_center.DcProperties;
import ru.yandex.market.delivery.transport_manager.config.jackson.RoutingJacksonConfig;
import ru.yandex.market.delivery.transport_manager.config.les.TmLesSqsProperties;
import ru.yandex.market.delivery.transport_manager.config.logbroker.FfwfEventConsumerConfiguration;
import ru.yandex.market.delivery.transport_manager.config.logbroker.FfwfEventConsumerProperties;
import ru.yandex.market.delivery.transport_manager.config.logbroker.LogbrokerProducerProperties;
import ru.yandex.market.delivery.transport_manager.config.logbroker.LogbrokerProperties;
import ru.yandex.market.delivery.transport_manager.config.logbroker.LomEventConsumerConfiguration;
import ru.yandex.market.delivery.transport_manager.config.logbroker.LomEventConsumerProperties;
import ru.yandex.market.delivery.transport_manager.config.lrm.LrmProperties;
import ru.yandex.market.delivery.transport_manager.config.mdm.MdmConfig;
import ru.yandex.market.delivery.transport_manager.config.pechkin.PechkinConfiguration;
import ru.yandex.market.delivery.transport_manager.config.pechkin.PechkinProperties;
import ru.yandex.market.delivery.transport_manager.config.properties.FeatureProperties;
import ru.yandex.market.delivery.transport_manager.config.properties.LmsExtraProperties;
import ru.yandex.market.delivery.transport_manager.config.properties.TmProperties;
import ru.yandex.market.delivery.transport_manager.config.startrek.StartrekConfiguration;
import ru.yandex.market.delivery.transport_manager.config.startrek.StartrekProperties;
import ru.yandex.market.delivery.transport_manager.config.tms.DefaultTmsDataSourceConfig;
import ru.yandex.market.delivery.transport_manager.config.tms.TmsConfig;
import ru.yandex.market.delivery.transport_manager.config.tpl.TplProperties;
import ru.yandex.market.delivery.transport_manager.config.tracker.TmTrackerProperties;
import ru.yandex.market.delivery.transport_manager.config.tsum.TsumProperties;
import ru.yandex.market.delivery.transport_manager.config.tsup.TsupProperties;
import ru.yandex.market.delivery.transport_manager.config.yard.YardProperties;
import ru.yandex.market.delivery.transport_manager.converter.LomConverter;
import ru.yandex.market.delivery.transport_manager.converter.trn.TrnInformationConverter;
import ru.yandex.market.delivery.transport_manager.event.les.order.listener.OrderBindEventListener;
import ru.yandex.market.delivery.transport_manager.event.unit.status.listener.AcceptedRequestListener;
import ru.yandex.market.delivery.transport_manager.event.unit.status.listener.UpdateEntitiesStatusListener;
import ru.yandex.market.delivery.transport_manager.facade.FfWfInboundFacade;
import ru.yandex.market.delivery.transport_manager.facade.FfWfOutboundFacade;
import ru.yandex.market.delivery.transport_manager.facade.TransportationUpdateFacade;
import ru.yandex.market.delivery.transport_manager.facade.register.RegisterPlanFacade;
import ru.yandex.market.delivery.transport_manager.facade.shipment.ShipmentFacade;
import ru.yandex.market.delivery.transport_manager.facade.transportation.TransportationFacade;
import ru.yandex.market.delivery.transport_manager.provider.AxaptaClient;
import ru.yandex.market.delivery.transport_manager.queue.task.axapta.document.SendAxaptaDocumentRequestProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.booking.BookingSlotConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.booking.BookingSlotProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.cancelation.CancelBookedSlotProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.cancelation_and_retry.CancelBookedSlotAndRetryProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.distribution_center.axapta_request.SendAxaptaRequestToDcProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.les.event_received.LesEventReceivedProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.les.trip.TripToYardLesProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.les.trn.TrnReadyLesProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.order.deletion.OrderDeletionProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.order.event.OrderEventIdsProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.order_route.BindOrderProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.request.external_id.RequestExternalIdQueueConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.request.external_id.RequestExternalIdQueueProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.request.status.RequestStatusQueueConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.request.status.RequestStatusQueueProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.create.TicketCreationProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.tracker.process.ProcessTrackProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.tracker.register.RegisterTrackProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.cancellation.CancellationService;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.cancellation.CancellationUnitProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.cancellation.TransportationCancellationProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.checker.TransportationCheckerProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.deletion.TransportationDeletionProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.inbound.PutInboundProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.master.NewSchemeTransportationRouter;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.master.TransportationMasterConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.master.TransportationMasterProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.master.TransportationProducersRouter;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.courier.RefreshCourierProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.get.GetMovementProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.put.PutMovementProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.outbound.PutOutboundProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.CreateRegisterProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.FetchRegisterProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.FetchRegisterUnitsProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.external_id.EnrichRegisterExternalIdProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.ffwf_error.GetFfwfRegisterErrorProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.PutInboundRegisterProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.abo.TransferRegisterAboProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.ffwf.ReturnRegisterTaskProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.outbound.PutOutboundRegisterProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.palet.EnrichRegisterWithPalletsProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.transfer.ffwf.TransferRegisterFfwfConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.transfer.ffwf.TransferRegisterFfwfProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.shipment.ShipmentProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.xdoc.CancelXDocOutboundPlanProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.xdoc.ProcessXdocOutboundFactRegisterProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation_task.encrich.EnrichTransportationTaskProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation_task.splitting.TransportationTaskSplittingProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation_task.validation.ValidateTransportationTaskProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.trn.BuildTransportationTrnProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TagMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.service.AxaptaStatusEventService;
import ru.yandex.market.delivery.transport_manager.service.CustomCsvSerializer;
import ru.yandex.market.delivery.transport_manager.service.LegalInfoService;
import ru.yandex.market.delivery.transport_manager.service.PartnerInfoService;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.service.TransportationService;
import ru.yandex.market.delivery.transport_manager.service.TransportationStatusService;
import ru.yandex.market.delivery.transport_manager.service.checker.TransportationChecker;
import ru.yandex.market.delivery.transport_manager.service.checker.TransportationExternalInfoSaver;
import ru.yandex.market.delivery.transport_manager.service.checker.validation.TransportationValidator;
import ru.yandex.market.delivery.transport_manager.service.core.TmEventPublisher;
import ru.yandex.market.delivery.transport_manager.service.distribution_center.client.DcClient;
import ru.yandex.market.delivery.transport_manager.service.event.ffwf.RequestStatusHandlerService;
import ru.yandex.market.delivery.transport_manager.service.external.lgw.LgwClientExecutor;
import ru.yandex.market.delivery.transport_manager.service.external.marketd.MarketIdService;
import ru.yandex.market.delivery.transport_manager.service.health.event.TrnEventWriter;
import ru.yandex.market.delivery.transport_manager.service.interwarehouse.enrichment.TransportSearchService;
import ru.yandex.market.delivery.transport_manager.service.logistic_point.LogisticPointSearchService;
import ru.yandex.market.delivery.transport_manager.service.movement.MovementApprover;
import ru.yandex.market.delivery.transport_manager.service.movement.courier.MovementCourierRefresher;
import ru.yandex.market.delivery.transport_manager.service.movement.courier.MovementCourierService;
import ru.yandex.market.delivery.transport_manager.service.movement.tracking.entity.MovementTrackerCheckpointProcessor;
import ru.yandex.market.delivery.transport_manager.service.order.OrderBindingService;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;
import ru.yandex.market.delivery.transport_manager.service.ticket.service.StEntityErrorTicketService;
import ru.yandex.market.delivery.transport_manager.service.ticket.service.StInterwarehouseTicketService;
import ru.yandex.market.delivery.transport_manager.service.ticket.service.StReturnDropoffTicketService;
import ru.yandex.market.delivery.transport_manager.service.transportation_unit.TransportationUnitService;
import ru.yandex.market.delivery.transport_manager.service.transportation_unit.TransportationUnitUpdater;
import ru.yandex.market.delivery.transport_manager.service.trn.Templater;
import ru.yandex.market.delivery.transport_manager.service.trn.TrnTemplaterService;
import ru.yandex.market.delivery.transport_manager.service.yt.YtCommonReader;
import ru.yandex.market.delivery.transport_manager.service.yt.YtScheduleReader;
import ru.yandex.market.delivery.transport_manager.task.RefreshTransportationsByConfigTask;
import ru.yandex.market.ff.client.FulfillmentWorkflowAsyncClientApi;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.FulfillmentWorkflowReturnRegistryClientApi;
import ru.yandex.market.logbroker.consumer.LogbrokerReader;
import ru.yandex.market.logbroker.consumer.config.LogbrokerClientFactoryConfiguration;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConnectionParams;
import ru.yandex.market.logbroker.consumer.util.LbReaderOffsetDao;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnablePooledZonkyEmbeddedPostgres;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.mdm.http.MasterDataService;
import ru.yandex.market.tms.quartz2.spring.config.DatabaseSchedulerFactoryConfig;
import ru.yandex.market.tpl.client.dropoff.TplDropoffCargoClient;
import ru.yandex.market.tpl.common.lrm.client.api.ReturnsApi;
import ru.yandex.passport.tvmauth.BlackboxEnv;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.passport.tvmauth.roles.Roles;
import ru.yandex.startrek.client.AttachmentsClient;
import ru.yandex.startrek.client.IssuesClient;
import ru.yandex.startrek.client.StartrekClient;

@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Configuration
@EnablePooledZonkyEmbeddedPostgres
@Import({
    FeatureProperties.class,
    DbUnitTestConfiguration.class,
    AdditionalDatasourceConfiguration.class,
    LiquibaseConfig.class,
    MyBatisConfig.class,
    CacheConfig.class,
    YtClientConfiguration.class,
    YtScheduleReader.class,
    CustomPropertyEditorConfiguration.class,
    DbQueueProperties.class,
    DbQueueConfig.class,
    TransportationFacade.class,
    YtScheduleReader.class,
    TestClockConfiguration.class,
    LomEventConsumerProperties.class,
    LogbrokerProducerProperties.class,
    LomEventConsumerConfiguration.class,
    ShipmentFacade.class,
    TransportationMasterConsumer.class,
    TransportationProducersRouter.class,
    FfWfInboundFacade.class,
    FfWfOutboundFacade.class,
    AdminConfig.class,
    DownloadProperties.class,
    LogbrokerConfiguration.class,
    FfwfEventConsumerConfiguration.class,
    FfwfEventConsumerProperties.class,
    RequestStatusQueueConsumer.class,
    RequestExternalIdQueueConsumer.class,
    TransferRegisterFfwfConsumer.class,
    RequestStatusHandlerService.class,
    TmProperties.class,
    TmTrackerProperties.class,
    TmsConfig.class,
    DefaultTmsDataSourceConfig.class,
    DatabaseSchedulerFactoryConfig.class,
    FakeTmsTasks.class,
    StartrekConfiguration.class,
    StartrekProperties.class,
    LmsExtraProperties.class,
    MdmConfig.class,
    LmsExtraProperties.class,
    TplProperties.class,
    AboClientProperties.class,
    YardProperties.class,
    LrmProperties.class,
    TsupProperties.class,
    TsumProperties.class,
    AxaptaConfiguration.class,
    PechkinProperties.class,
    PechkinConfiguration.class,
    BookingSlotConsumer.class,
    FulfillmentWorkflowConfiguration.class,
    DcClientConfig.class,
    DcProperties.class,
    SpringSchedulerMonitoringConfiguration.class,
    StatusChangeEventConfig.class,
    TmLesSqsProperties.class,
    DatabaseSequenceIncrementerConfig.class,
    RoutingJacksonConfig.class,
    MdsS3BasicConfiguration.class,
    TestListenerConfig.class,
    MdsS3Configuration.class,
})
@SpyBean({
    LomConverter.class,
    FeatureProperties.class,
    OrderEventIdsProducer.class,
    BindOrderProducer.class,
    PutInboundProducer.class,
    ShipmentProducer.class,
    PutOutboundProducer.class,
    PutMovementProducer.class,
    TransportationMasterProducer.class,
    CreateRegisterProducer.class,
    FetchRegisterProducer.class,
    RequestStatusQueueProducer.class,
    RequestExternalIdQueueProducer.class,
    EnrichRegisterExternalIdProducer.class,
    TransportationFacade.class,
    TransportationUpdateFacade.class,
    RefreshTransportationsByConfigTask.class,
    LgwClientExecutor.class,
    TransportationChecker.class,
    TransportationCheckerProducer.class,
    TicketCreationProducer.class,
    FetchRegisterUnitsProducer.class,
    TransferRegisterFfwfProducer.class,
    TransferRegisterAboProducer.class,
    ReturnRegisterTaskProducer.class,
    PutOutboundRegisterProducer.class,
    PutInboundRegisterProducer.class,
    OrderDeletionProducer.class,
    TransportationDeletionProducer.class,
    EnrichTransportationTaskProducer.class,
    EnrichRegisterWithPalletsProducer.class,
    TransportationTaskSplittingProducer.class,
    ValidateTransportationTaskProducer.class,
    TransportationValidator.class,
    TransportationExternalInfoSaver.class,
    NewSchemeTransportationRouter.class,
    TransportSearchService.class,
    AxaptaClient.class,
    TransportationStatusService.class,
    TransportationValidator.class,
    RegisterPlanFacade.class,
    StEntityErrorTicketService.class,
    ProcessXdocOutboundFactRegisterProducer.class,
    AcceptedRequestListener.class,
    ProcessXdocOutboundFactRegisterProducer.class,
    CancelBookedSlotAndRetryProducer.class,
    CancelBookedSlotProducer.class,
    TransportationUnitService.class,
    CancellationService.class,
    RefreshCourierProducer.class,
    MovementCourierRefresher.class,
    CancellationService.class,
    TransportationCancellationProducer.class,
    SendAxaptaDocumentRequestProducer.class,
    DcClient.class,
    YtCommonReader.class,
    CustomCsvSerializer.class,
    StReturnDropoffTicketService.class,
    BookingSlotProducer.class,
    GetMovementProducer.class,
    TransportationUnitUpdater.class,
    GetMovementProducer.class,
    CancelXDocOutboundPlanProducer.class,
    TmPropertyService.class,
    RegisterService.class,
    MovementTrackerCheckpointProcessor.class,
    AxaptaStatusEventService.class,
    UpdateEntitiesStatusListener.class,
    FfWfInboundFacade.class,
    OrderBindEventListener.class,
    DataFieldMaxValueIncrementer.class,
    TrnReadyLesProducer.class,
    LesEventReceivedProducer.class,
    Templater.class,
    LegalInfoService.class,
    MovementCourierService.class,
    LogisticPointSearchService.class,
    TrnInformationConverter.class,
    TmEventPublisher.class,
    MovementApprover.class,
    TrnTemplaterService.class,
    TransportationService.class,
    SendAxaptaRequestToDcProducer.class,
    TransportationMasterProducer.class,
    GetFfwfRegisterErrorProducer.class,
    OrderBindingService.class,
    CancellationUnitProducer.class,
    TripToYardLesProducer.class,
    AxaptaStatusEventService.class,
    StInterwarehouseTicketService.class,
    TagMapper.class,
    TrnEventWriter.class,
    PartnerInfoService.class,
    RegisterTrackProducer.class,
    ProcessTrackProducer.class,
    BuildTransportationTrnProducer.class,
    RegisterMapper.class
})
@MockBean({
    LMSClient.class,
    LomClient.class,
    DeliveryClient.class,
    LogbrokerReader.class,
    MarketIdService.class,
    LogbrokerProperties.class,
    LogbrokerConnectionParams.class,
    LogbrokerClientFactoryConfiguration.class,
    FulfillmentWorkflowClientApi.class,
    FulfillmentWorkflowReturnRegistryClientApi.class,
    FulfillmentWorkflowAsyncClientApi.class,
    FulfillmentClient.class,
    TrackerApiClient.class,
    StartrekClient.class,
    IssuesClient.class,
    AttachmentsClient.class,
    LogbrokerClientFactory.class,
    LbReaderOffsetDao.class,
    PechkinHttpClient.class,
    MasterDataService.class,
    AboAPI.class,
    ReturnsApi.class,
    CalendaringServiceClientApi.class,
    LogbrokerClientFactory.class,
    TplDropoffCargoClient.class,
    LesProducer.class,
    ResourceLocationFactory.class,
    MdsS3Client.class,
})
@ComponentScan({
    "ru.yandex.market.delivery.transport_manager.provider",
    "ru.yandex.market.delivery.transport_manager.converter",
    "ru.yandex.market.delivery.transport_manager.admin.converter",
    "ru.yandex.market.delivery.transport_manager.admin.controller",
    "ru.yandex.market.delivery.transport_manager.queue.task.transportation.register",
    "ru.yandex.market.delivery.transport_manager.config.health",
    "ru.yandex.market.delivery.gruzin.controller",
    "ru.yandex.market.delivery.gruzin.facade",
    "ru.yandex.market.delivery.gruzin.converter",
    "ru.yandex.market.delivery.gruzin.service",
    "ru.yandex.market.delivery.gruzin.task",
})
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class
})
@TestPropertySource("classpath:application.properties")
public class IntegrationTestConfig {

    @Bean
    public EntityFactory entityFactory() {
        return new EntityFactory();
    }

    @Bean
    public AsyncProducerConfig csMetaUpdateAsyncProducerConfig() {
        return Mockito.mock(AsyncProducerConfig.class);
    }

    @Bean
    public LogbrokerProducerProperties csMetaUpdateProducerProperties() {
        return Mockito.mock(LogbrokerProducerProperties.class);
    }

    @Bean
    public TvmTicketProvider tvmTicketProvider() {
        return new TvmTicketProvider() {
            @Override
            public String provideServiceTicket() {
                return "test-service-ticket";
            }

            @Override
            public String provideUserTicket() {
                return "test-user-ticket";
            }
        };
    }

    @Bean
    public TvmClient iTvmClient() {
        return new TvmClient() {
            @Override
            public ClientStatus getStatus() {
                return null;
            }

            @Override
            public String getServiceTicketFor(String alias) {
                return "service-ticket";
            }

            @Override
            public String getServiceTicketFor(int clientId) {
                return null;
            }

            @Override
            public CheckedServiceTicket checkServiceTicket(String ticketBody) {
                return null;
            }

            @Override
            public CheckedUserTicket checkUserTicket(String ticketBody) {
                return null;
            }

            @Override
            public CheckedUserTicket checkUserTicket(String ticketBody, BlackboxEnv env) {
                return null;
            }

            @Override
            public Roles getRoles() {
                return null;
            }

            @Override
            public void close() {

            }
        };
    }
}
