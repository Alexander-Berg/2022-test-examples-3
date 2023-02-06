package ru.yandex.market.sc.core.domain.archive.schrodingerbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.archive.repository.Archive;
import ru.yandex.market.sc.core.domain.archive.repository.ArchiveRepository;
import ru.yandex.market.sc.core.domain.archive.repository.ArchiveStatus;
import ru.yandex.market.sc.core.domain.archive.repository.TmOperation;
import ru.yandex.market.sc.core.domain.archive.repository.TmOperationRepository;
import ru.yandex.market.sc.core.domain.archive.repository.TmOperationStatus;
import ru.yandex.market.sc.core.external.transfermanager.dto.TmTransferStatus;
import ru.yandex.market.sc.core.external.transfermanager.TransferManagerService;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@EmbeddedDbTest
public class FinishTransferStepTest {
    @Autowired
    FinishTransferStep finishTransferStep;

    @Autowired
    ArchiveRepository archiveRepository;

    @Autowired
    TmOperationRepository operationRepository;

    @Autowired
    ConfigurationService configurationService;

    @MockBean
    TransferManagerService transferManagerService;

    private static final String OPERATION_FINISHED = "OPERATION_FINISHED";
    private static final String OPERATION_IN_PROGRESS = "OPERATION_IN_PROGRESS";
    private static final String OPERATION_FAILED = "OPERATION_FAILED";
    private static final String OPERATION_NOT_FOUND = "OPERATION_NOT_FOUND";

    @BeforeEach
    void beforeEach() {
        enableDbPurifier();

        when(transferManagerService.getTransferStatus(OPERATION_FINISHED))
                .thenReturn(TmTransferStatus.FINISHED_SUCCESSFULLY);
        when(transferManagerService.getTransferStatus(OPERATION_IN_PROGRESS))
                .thenReturn(TmTransferStatus.IN_PROGRESS);
        when(transferManagerService.getTransferStatus(OPERATION_FAILED))
                .thenReturn(TmTransferStatus.FINISHED_FAULTILY);
        when(transferManagerService.getTransferStatus(OPERATION_NOT_FOUND))
                .thenReturn(TmTransferStatus.NOT_FOUND);
    }


    @Transactional
    @ParameterizedTest
    @ArgumentsSource(ArchiveStatusDataProvider.class)
    void checkForFinishedOperation(ArchiveStatus notEligibleArchiveStatus) {
        checkArchiveAndOperationStatuesAfterProcessing(OPERATION_FINISHED, TmOperationStatus.FINISHED,
                ArchiveStatus.TRANSFER_FINISHED, notEligibleArchiveStatus);
    }


    @Transactional
    @ParameterizedTest
    @ArgumentsSource(ArchiveStatusDataProvider.class)
    void checkForOperationInProgress(ArchiveStatus notEligibleArchiveStatus) {
        checkArchiveAndOperationStatuesAfterProcessing(OPERATION_IN_PROGRESS, TmOperationStatus.IN_PROGRESS,
                ArchiveStatus.TRANSFER_SCHEDULING_FINISHED, notEligibleArchiveStatus);
    }


    @Transactional
    @ParameterizedTest
    @ArgumentsSource(ArchiveStatusDataProvider.class)
    void checkForOperationFailed(ArchiveStatus notEligibleArchiveStatus) {
        checkArchiveAndOperationStatuesAfterProcessing(OPERATION_FAILED, TmOperationStatus.FAILED,
                ArchiveStatus.TRANSFER_FAILED, notEligibleArchiveStatus);
    }


    @Transactional
    @ParameterizedTest
    @ArgumentsSource(ArchiveStatusDataProvider.class)
    void checkForOperationNotFound(ArchiveStatus notEligibleArchiveStatus) {
        checkArchiveAndOperationStatuesAfterProcessing(OPERATION_NOT_FOUND, TmOperationStatus.FAILED,
                ArchiveStatus.TRANSFER_FAILED, notEligibleArchiveStatus);
    }


    void checkArchiveAndOperationStatuesAfterProcessing(String operationId, TmOperationStatus newOperationStatus,
                                                        ArchiveStatus newArchiveStatus,
                                                        ArchiveStatus notEligibleArchiveStatus) {
        //test data preparation
        Archive archive = new Archive(ArchiveStatus.TRANSFER_SCHEDULING_FINISHED);
        archive = archiveRepository.save(archive);
        TmOperation uploadOperation = new TmOperation(operationId, TmOperationStatus.IN_PROGRESS,
                new ArrayList<>(List.of(archive)));
        uploadOperation = operationRepository.save(uploadOperation);


        Archive archiveWithOtherOperations = new Archive(ArchiveStatus.TRANSFER_SCHEDULING_FINISHED);
        archiveWithOtherOperations = archiveRepository.save(archiveWithOtherOperations);
        TmOperation failedOperation = new TmOperation(operationId, TmOperationStatus.FAILED,
                new ArrayList<>(List.of(archiveWithOtherOperations)));
        failedOperation = operationRepository.save(failedOperation);
        TmOperation finishedOperation = new TmOperation(operationId, TmOperationStatus.FINISHED,
                new ArrayList<>(List.of(archiveWithOtherOperations)));
        finishedOperation = operationRepository.save(finishedOperation);


        Archive archiveInWrongStatus = new Archive(notEligibleArchiveStatus);
        archiveInWrongStatus = archiveRepository.save(archiveInWrongStatus);


        //tested method
        finishTransferStep.finishTransfer();

        Mockito.verify(transferManagerService, Mockito.times(1))
                .getTransferStatus(uploadOperation.getExternalId());
        Mockito.verify(transferManagerService, Mockito.times(1))
                .getTransferStatus(any());

        //result check
        Archive archiveAfterExecution = archiveRepository.getById(archive.getId());
        assertThat(archiveAfterExecution).isNotNull();
        assertThat(archiveAfterExecution.getArchiveStatus()).isEqualTo(newArchiveStatus);


        List<TmOperation> operations = operationRepository.findAll();
        assertThat(operations).size().isEqualTo(3);
        TmOperation operation = operationRepository.getById(uploadOperation.getId());
        assertThat(operation.getArchives()).contains(archiveAfterExecution);
        assertThat(operation.getStatus()).isEqualTo(newOperationStatus);

        //check other operations and archives not affected
        Archive archiveInWrongStatusAfter = archiveRepository.getById(archiveInWrongStatus.getId());
        assertThat(archiveInWrongStatusAfter).isNotNull();
        assertThat(archiveInWrongStatusAfter.getArchiveStatus()).isEqualTo(notEligibleArchiveStatus);

        Archive archiveWithOtherOperationsAfter =
                archiveRepository.getById(archiveWithOtherOperations.getId());
        assertThat(archiveWithOtherOperationsAfter).isNotNull();
        assertThat(archiveWithOtherOperationsAfter.getArchiveStatus()).isEqualTo(archiveWithOtherOperations.getArchiveStatus());

        TmOperation failedOperationAfter = operationRepository.getById(failedOperation.getId());
        assertThat(failedOperationAfter.getArchives()).contains(archiveWithOtherOperations);
        assertThat(failedOperationAfter.getStatus()).isEqualTo(TmOperationStatus.FAILED);

        TmOperation finishedOperationAfter = operationRepository.getById(finishedOperation.getId());
        assertThat(finishedOperationAfter.getArchives()).contains(archiveWithOtherOperations);
        assertThat(finishedOperationAfter.getStatus()).isEqualTo(TmOperationStatus.FINISHED);


    }

    static class ArchiveStatusDataProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Arrays.stream(ArchiveStatus.values())
                    .filter(s -> !ArchiveStatus.TRANSFER_SCHEDULING_FINISHED.equals(s))
                    .map(Arguments::of);
        }
    }

    private void enableDbPurifier() {
        configurationService.mergeValue(
                ConfigurationProperties.DB_ARCHIVING_ENABLED_PROPERTY, true);
    }
}
