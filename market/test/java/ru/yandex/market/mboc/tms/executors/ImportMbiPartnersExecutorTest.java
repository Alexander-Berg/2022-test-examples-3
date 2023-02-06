package ru.yandex.market.mboc.tms.executors;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.application.monitoring.MonitoringUnit;
import ru.yandex.market.mbo.taskqueue.TaskQueueRegistrator;
import ru.yandex.market.mbo.taskqueue.TaskQueueRepository;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.eats_supplier.EatsBusinessesCacheImpl;
import ru.yandex.market.mboc.common.services.eats_supplier.YtEatsLavkaPartnersReader;
import ru.yandex.market.mboc.common.services.imports.BusinessChangesLogbrokerMessageHandler;
import ru.yandex.market.mboc.common.services.imports.SupplierImportService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.tms.service.supplier.YtMbiPartnersReader;
import ru.yandex.market.partner.event.PartnerInfo;

public class ImportMbiPartnersExecutorTest extends BaseDbTestClass {
    @Value("${mboc.beru.businessId}")
    private int beruBusinessId;

    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private MigrationStatusRepository migrationStatusRepository;
    @Autowired
    private OfferRepository offerRepository;

    private YtEatsLavkaPartnersReader ytEatsLavkaPartnersReader;
    private YtMbiPartnersReader ytMbiPartnersReaderMock;
    private ComplexMonitoring complexMonitoring;

    private ImportMbiPartnersExecutor executor;

    @Before
    public void setUp() {
        ytEatsLavkaPartnersReader = Mockito.mock(YtEatsLavkaPartnersReader.class);

        var taskQueueRepository = new TaskQueueRepository(
            namedParameterJdbcTemplate,
            transactionTemplate,
            "mboc-tms-task-queue"
        );
        var taskQueueRegistrator = new TaskQueueRegistrator(
            taskQueueRepository,
            new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
        );
        SupplierImportService supplierImportService = new SupplierImportService(
            beruBusinessId,
            supplierRepository,
            offerRepository,
            TransactionHelper.MOCK,
            migrationStatusRepository,
            categoryInfoCache,
            taskQueueRegistrator,
            new EatsBusinessesCacheImpl(ytEatsLavkaPartnersReader),
            storageKeyValueService
        );

        ytMbiPartnersReaderMock = Mockito.mock(YtMbiPartnersReader.class);

        complexMonitoring = new ComplexMonitoring();

        executor = new ImportMbiPartnersExecutor(
            ytMbiPartnersReaderMock,
            supplierImportService,
            complexMonitoring
        );
    }

    @Test
    public void testImportOK() {
        PartnerInfo.PartnerInfoEvent event = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test OK")
            .setDomain("domain")
            .setOrganizationName("some org")
            .setType(PartnerInfo.MbiPartnerType.BUSINESS)
            .build();

        mockYtMbiPartnersReader(event);

        executor.execute();

        List<Supplier> suppliers = supplierRepository.findAll();

        Assertions.assertThat(suppliers)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Supplier()
                .setId(1)
                .setName(event.getName())
                .setDomain(event.getDomain())
                .setOrganizationName(event.getOrganizationName())
                .setType(MbocSupplierType.BUSINESS)
                .setNewContentPipeline(true)
            );

        var monitoringResult = complexMonitoring.getResult(ImportMbiPartnersExecutor.MONITORING_NAME);
        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void testImportWithErrors() {
        PartnerInfo.PartnerInfoEvent eventOk = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test OK")
            .setDomain("domain")
            .setOrganizationName("some org")
            .setType(PartnerInfo.MbiPartnerType.BUSINESS)
            .build();

        // check only last 5 errors is shown in monitoring
        PartnerInfo.PartnerInfoEvent eventNoName1 = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(2L)
            .build();
        PartnerInfo.PartnerInfoEvent eventNoName2 = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(3L)
            .build();
        PartnerInfo.PartnerInfoEvent eventNoName3 = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(4L)
            .build();
        PartnerInfo.PartnerInfoEvent eventNoName4 = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(5L)
            .build();
        PartnerInfo.PartnerInfoEvent eventNoName5 = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(6L)
            .build();
        PartnerInfo.PartnerInfoEvent eventNoName6 = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(7L)
            .build();

        mockYtMbiPartnersReader(eventOk,
            eventNoName1, eventNoName2, eventNoName3, eventNoName4, eventNoName5, eventNoName6);

        executor.execute();

        List<Supplier> suppliers = supplierRepository.findAll();

        Assertions.assertThat(suppliers)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new Supplier()
                .setId(1)
                .setName(eventOk.getName())
                .setDomain(eventOk.getDomain())
                .setOrganizationName(eventOk.getOrganizationName())
                .setType(MbocSupplierType.BUSINESS)
                .setNewContentPipeline(true)
            );

        var monitoringResult = complexMonitoring.getResult(ImportMbiPartnersExecutor.MONITORING_NAME);
        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        Assertions.assertThat(monitoringResult.getMessage()).isEqualTo(
            ImportMbiPartnersExecutor.MONITORING_NAME +
                ": Failed to import 6 of 7 suppliers. Error examples: " +
                "Failed to import supplier id = [3] name = []: Name is missing; " +
                "Failed to import supplier id = [4] name = []: Name is missing; " +
                "Failed to import supplier id = [5] name = []: Name is missing; " +
                "Failed to import supplier id = [6] name = []: Name is missing; " +
                "Failed to import supplier id = [7] name = []: Name is missing"
        );
    }

    @Test
    public void testMonitoring() {
        PartnerInfo.PartnerInfoEvent eventOk = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L)
            .setName("test OK")
            .setDomain("domain")
            .setOrganizationName("some org")
            .setType(PartnerInfo.MbiPartnerType.BUSINESS)
            .build();

        PartnerInfo.PartnerInfoEvent eventNoName = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(2L)
            .build();

        MonitoringUnit monitoringLBUnit =
            complexMonitoring.getOrCreateUnit(BusinessChangesLogbrokerMessageHandler.MONITORING_NAME);
        monitoringLBUnit.critical("some error");

        mockYtMbiPartnersReader(eventOk, eventNoName);

        executor.execute();

        var monitoringResult = complexMonitoring.getResult(ImportMbiPartnersExecutor.MONITORING_NAME);
        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        Assertions.assertThat(monitoringResult.getMessage()).isEqualTo(
            ImportMbiPartnersExecutor.MONITORING_NAME +
                ": Failed to import 1 of 2 suppliers. Error examples: " +
                "Failed to import supplier id = [2] name = []: Name is missing"
        );

        var monitoringLBResult = complexMonitoring.getResult(BusinessChangesLogbrokerMessageHandler.MONITORING_NAME);
        Assertions.assertThat(monitoringLBResult.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);

        mockYtMbiPartnersReader(eventOk);

        executor.execute();

        monitoringResult = complexMonitoring.getResult(ImportMbiPartnersExecutor.MONITORING_NAME);
        Assertions.assertThat(monitoringResult.getStatus()).isEqualTo(MonitoringStatus.OK);

        monitoringLBResult = complexMonitoring.getResult(BusinessChangesLogbrokerMessageHandler.MONITORING_NAME);
        Assertions.assertThat(monitoringLBResult.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    private void mockYtMbiPartnersReader(PartnerInfo.PartnerInfoEvent... events) {
        Mockito.doAnswer(invocation -> {
            Consumer<PartnerInfo.PartnerInfoEvent> batchConsumer = invocation.getArgument(0);
            for (var event : events) {
                batchConsumer.accept(event);
            }
            return null;
        })
            .when(ytMbiPartnersReaderMock).readMbiBusinesses(Mockito.any());
    }
}
