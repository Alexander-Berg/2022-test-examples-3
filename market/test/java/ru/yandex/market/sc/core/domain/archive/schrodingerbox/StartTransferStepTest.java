package ru.yandex.market.sc.core.domain.archive.schrodingerbox;

import java.time.Instant;
import java.util.List;

import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.archive.ArchivingSettingsService;
import ru.yandex.market.sc.core.domain.archive.repository.Archive;
import ru.yandex.market.sc.core.domain.archive.repository.ArchiveRepository;
import ru.yandex.market.sc.core.domain.archive.repository.ArchiveStatus;
import ru.yandex.market.sc.core.domain.archive.repository.TmOperation;
import ru.yandex.market.sc.core.domain.archive.repository.TmOperationRepository;
import ru.yandex.market.sc.core.external.transfermanager.TransferManagerService;
import ru.yandex.market.sc.core.external.transfermanager.dto.TmOperationResponse;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@EmbeddedDbTest
public class StartTransferStepTest {

    private static final  int NUMBER_OF_ENTITIES_IN_MAIN_ARCHIVING_FLOW = 15;

    /**
     * Архивация заказов и сущностей связанных с ними - основной флоу
     */
    private static final String MAIN_FLOW = "FINISHED_ORDERS";

    @Autowired
    StartTransferStep transferStep;

    @Autowired
    TestFactory testFactory;

    @Autowired
    TmOperationRepository operationRepository;

    @Autowired
    ArchiveRepository archiveRepository;

    @Autowired
    ConfigurationService configurationService;

    @MockBean
    TransferManagerService transferManagerService;

    @Autowired
    ArchivingSettingsService archivingSettingsService;

    @BeforeEach
    void beforeEach() {
        enableDbPurifier();
        when(transferManagerService.scheduleTransfer(eq("order_scan_log"), anyList()))
                .thenReturn(new TmOperationResponse(
                        "dtj0ajh6ndmgosoial4o", "Upload", Instant.now(), Instant.now(),
                        "f6oupl8q139kkv6ch9qa", null, null));
        when(transferManagerService.scheduleTransfer(any(), anyList()))
                .thenReturn(new TmOperationResponse(
                        "dtj0ajh6ndmgosoial4o", "Upload", Instant.now(), Instant.now(),
                        "f6oupl8q139kkv6ch9qa", null, null));
        archivingSettingsService.setRuleSet(MAIN_FLOW);
    }

    @Transactional
    @Test
    void checkOneArchiveUploadRequestIsSent() {
        //test data preparation
        Archive archive = new Archive(ArchiveStatus.MARKING_FINISHED);
        archive = archiveRepository.save(archive);
        Archive archiveInWrongStatus = new Archive(ArchiveStatus.MARKING_IN_PROGRESS);
        archiveInWrongStatus = archiveRepository.save(archiveInWrongStatus);

        //tested method
        transferStep.scheduleTransfer();

        Mockito.verify(transferManagerService, Mockito.times(1))
                .scheduleTransfer("order_scan_log", List.of(archive.getId()));
        Mockito.verify(transferManagerService, Mockito.times(NUMBER_OF_ENTITIES_IN_MAIN_ARCHIVING_FLOW))
                .scheduleTransfer(any(), any());

        //result check
        Archive archiveAfterExecution = archiveRepository.getById(archive.getId());
        assertThat(archiveAfterExecution).isNotNull();
        assertThat(archiveAfterExecution.getArchiveStatus()).isEqualTo(ArchiveStatus.TRANSFER_SCHEDULING_FINISHED);

        Archive archiveInWrongStatusAfterExecution = archiveRepository.getById(archiveInWrongStatus.getId());
        assertThat(archiveInWrongStatusAfterExecution).isNotNull();
        assertThat(archiveInWrongStatusAfterExecution.getArchiveStatus()).isEqualTo(ArchiveStatus.MARKING_IN_PROGRESS);

        List<TmOperation> operations = operationRepository.findAll();
        assertThat(operations).size().isEqualTo(NUMBER_OF_ENTITIES_IN_MAIN_ARCHIVING_FLOW);
        TmOperation operation = operations.get(0);
        assertThat(operation.getArchives()).contains(archiveAfterExecution);
    }

    @Transactional
    @Test
    void checkUploadRequestIsResentForOneFailedArchive() {
        //test data preparation
        Archive archive = new Archive(ArchiveStatus.TRANSFER_FAILED);
        archive = archiveRepository.save(archive);
        Archive archiveInWrongStatus = new Archive(ArchiveStatus.MARKING_IN_PROGRESS);
        archiveInWrongStatus = archiveRepository.save(archiveInWrongStatus);

        //tested method
        transferStep.scheduleTransfer();

        Mockito.verify(transferManagerService, Mockito.times(1))
                .scheduleTransfer("order_scan_log", List.of(archive.getId()));
        Mockito.verify(transferManagerService, Mockito.times(NUMBER_OF_ENTITIES_IN_MAIN_ARCHIVING_FLOW))
                .scheduleTransfer(any(), any());

        //result check
        Archive archiveAfterExecution = archiveRepository.getById(archive.getId());
        assertThat(archiveAfterExecution).isNotNull();
        assertThat(archiveAfterExecution.getArchiveStatus()).isEqualTo(ArchiveStatus.TRANSFER_SCHEDULING_FINISHED);

        Archive archiveInWrongStatusAfterExecution = archiveRepository.getById(archiveInWrongStatus.getId());
        assertThat(archiveInWrongStatusAfterExecution).isNotNull();
        assertThat(archiveInWrongStatusAfterExecution.getArchiveStatus()).isEqualTo(ArchiveStatus.MARKING_IN_PROGRESS);

        List<TmOperation> operations = operationRepository.findAll();
        assertThat(operations).size().isEqualTo(NUMBER_OF_ENTITIES_IN_MAIN_ARCHIVING_FLOW);
        TmOperation operation = operations.get(0);
        assertThat(operation.getArchives()).contains(archiveAfterExecution);
    }


    @Transactional
    @Test
    void checkMulitipleArchiveUploadRequestIsSent() {
        //test data preparation
        Archive archive = new Archive(ArchiveStatus.MARKING_FINISHED);
        archive = archiveRepository.save(archive);
        Archive archive2 = new Archive(ArchiveStatus.MARKING_FINISHED);
        archive2 = archiveRepository.save(archive2);
        Archive archiveInWrongStatus = new Archive(ArchiveStatus.MARKING_IN_PROGRESS);
        archiveInWrongStatus = archiveRepository.save(archiveInWrongStatus);

        //tested method
        transferStep.scheduleTransfer();

        Mockito.verify(transferManagerService, Mockito.times(1))
                .scheduleTransfer("order_scan_log", List.of(archive.getId(), archive2.getId()));

        Mockito.verify(transferManagerService, Mockito.times(NUMBER_OF_ENTITIES_IN_MAIN_ARCHIVING_FLOW))
                .scheduleTransfer(any(), any());

        //result check
        Archive archiveAfterExecution = archiveRepository.getById(archive.getId());
        assertThat(archiveAfterExecution).isNotNull();
        assertThat(archiveAfterExecution.getArchiveStatus()).isEqualTo(ArchiveStatus.TRANSFER_SCHEDULING_FINISHED);

        Archive archiveAfterExecution2 = archiveRepository.getById(archive2.getId());
        assertThat(archiveAfterExecution2).isNotNull();
        assertThat(archiveAfterExecution2.getArchiveStatus()).isEqualTo(ArchiveStatus.TRANSFER_SCHEDULING_FINISHED);

        Archive archiveInWrongStatusAfterExecution = archiveRepository.getById(archiveInWrongStatus.getId());
        assertThat(archiveInWrongStatusAfterExecution).isNotNull();
        assertThat(archiveInWrongStatusAfterExecution.getArchiveStatus()).isEqualTo(ArchiveStatus.MARKING_IN_PROGRESS);

        List<TmOperation> operations = operationRepository.findAll();
        assertThat(operations).size().isEqualTo(NUMBER_OF_ENTITIES_IN_MAIN_ARCHIVING_FLOW);
        TmOperation operation = operations.get(0);
        assertThat(operation.getArchives()).contains(archiveAfterExecution);
    }

    @Transactional
    @Test
    void checkStatusIsNotChangedWhenExceptionOccurred() {
        //test data preparation
        Archive archive = new Archive(ArchiveStatus.MARKING_FINISHED);
        archive = archiveRepository.save(archive);
        Archive archiveInWrongStatus = new Archive(ArchiveStatus.MARKING_IN_PROGRESS);
        archiveInWrongStatus = archiveRepository.save(archiveInWrongStatus);


        when(transferManagerService.scheduleTransfer(any(), any()))
                .thenThrow(new RuntimeException());

        //tested method
        assertThatThrownBy(() -> transferStep.scheduleTransfer());

        //result check
        Mockito.verify(transferManagerService, Mockito.times(1))
                .scheduleTransfer("orders", List.of(archive.getId()));
        Mockito.verify(transferManagerService, Mockito.times(1))
                .scheduleTransfer(any(), any());
        Archive archiveAfterExecution = archiveRepository.getById(archive.getId());
        assertThat(archiveAfterExecution).isNotNull();
        assertThat(archiveAfterExecution.getArchiveStatus()).isEqualTo(ArchiveStatus.TRANSFER_FAILED);

        Archive archiveInWrongStatusAfterExecution = archiveRepository.getById(archiveInWrongStatus.getId());
        assertThat(archiveInWrongStatusAfterExecution).isNotNull();
        assertThat(archiveInWrongStatusAfterExecution.getArchiveStatus()).isEqualTo(ArchiveStatus.MARKING_IN_PROGRESS);

        List<TmOperation> operations = operationRepository.findAll();
        assertThat(operations).isEmpty();
    }

    @Transactional
    @Test
    void checkUploadRequestNotSentWhenZeroArchivesFit() {
        //test data preparation
        Archive archive = new Archive(ArchiveStatus.MARKING_IN_PROGRESS);
        archive = archiveRepository.save(archive);
        Archive archiveInWrongStatus = new Archive(ArchiveStatus.MARKING_IN_PROGRESS);
        archiveInWrongStatus = archiveRepository.save(archiveInWrongStatus);


        //tested method
        transferStep.scheduleTransfer();

        //result check
        Mockito.verify(transferManagerService, Mockito.times(0))
                .scheduleTransfer(any(), any());
        Archive archiveAfterExecution = archiveRepository.getById(archive.getId());
        assertThat(archiveAfterExecution).isNotNull();
        assertThat(archiveAfterExecution.getArchiveStatus()).isEqualTo(ArchiveStatus.MARKING_IN_PROGRESS);

        Archive archiveInWrongStatusAfterExecution = archiveRepository.getById(archiveInWrongStatus.getId());
        assertThat(archiveInWrongStatusAfterExecution).isNotNull();
        assertThat(archiveInWrongStatusAfterExecution.getArchiveStatus()).isEqualTo(ArchiveStatus.MARKING_IN_PROGRESS);

        List<TmOperation> operations = operationRepository.findAll();
        assertThat(operations).isEmpty();
    }


    private void enableDbPurifier() {
        configurationService.mergeValue(
                ConfigurationProperties.DB_ARCHIVING_ENABLED_PROPERTY, true);
    }
}
