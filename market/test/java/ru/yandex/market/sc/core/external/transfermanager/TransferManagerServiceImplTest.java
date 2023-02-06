package ru.yandex.market.sc.core.external.transfermanager;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import ru.yandex.market.sc.core.external.transfermanager.dto.TmError;
import ru.yandex.market.sc.core.external.transfermanager.dto.TmOperationResponse;
import ru.yandex.market.sc.core.external.transfermanager.dto.TmTableRequest;
import ru.yandex.market.sc.core.external.transfermanager.dto.TmTransferStatus;
import ru.yandex.market.sc.core.external.transfermanager.dto.TmUploadRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class TransferManagerServiceImplTest {
    private static final String TRANSFER_ID = "transfer-id-1";
    private static final String TABLE_NAME = "order_scan_log";

    private final TransferManagerClient clientMock = mock(TransferManagerClient.class);
    private final TransferManagerService tmService = new TransferManagerServiceImpl(TRANSFER_ID, clientMock);

    @Test
    void testUploadRequested() {
        int archiveId = 10;
        String operationId = "operation-id";

        TmUploadRequest uploadRequest = new TmUploadRequest(
                List.of(
                        new TmTableRequest(
                                MessageFormat.format("archive_id in ({0})", archiveId),
                                TABLE_NAME,
                                TransferManagerServiceImpl.DEFAULT_SCHEMA
                        )
                ),
                TRANSFER_ID
        );

        TmOperationResponse uploadResponse = new TmOperationResponse(operationId, "description",
                Instant.now(), Instant.now(), "created_by", true, new TmError(1000, "message")
        );

        when(clientMock.createUpload(uploadRequest)).thenReturn(uploadResponse);

        //tested method
        TmOperationResponse operation = tmService.scheduleTransfer(TABLE_NAME, archiveId);


        //Result verification
        assertThat(operation).isNotNull();
        assertThat(operation.getId()).isEqualTo(uploadResponse.getId());
        assertThat(operation.getDone()).isEqualTo(uploadResponse.getDone());
        assertThat(operation.getError()).isEqualTo(uploadResponse.getError());
        assertThat(operation.getCreatedAt()).isEqualTo(uploadResponse.getCreatedAt());
        assertThat(operation.getModifiedAt()).isEqualTo(uploadResponse.getModifiedAt());
        assertThat(operation.getCreatedBy()).isEqualTo(uploadResponse.getCreatedBy());
        assertThat(operation.getDescription()).isEqualTo(uploadResponse.getDescription());

        verify(clientMock, times(1)).createUpload(uploadRequest);
        verify(clientMock, times(1)).createUpload(any());
    }

    @Test
    void testBadUploadRequested() {
        int archiveId = 10;
        String operationId = "operation-id";

        TmUploadRequest uploadRequest = new TmUploadRequest(
                List.of(
                        new TmTableRequest(
                                MessageFormat.format("archive_id in ({0})", archiveId),
                                TABLE_NAME,
                                TransferManagerServiceImpl.DEFAULT_SCHEMA
                        )
                ),
                TRANSFER_ID
        );

        TmOperationResponse uploadResponse = new TmOperationResponse(operationId, "description",
                Instant.now(), Instant.now(), "created_by", true, new TmError(1000, "message")
        );

        when(clientMock.createUpload(uploadRequest)).thenThrow(new RuntimeException());

        //tested method
        assertThatThrownBy(() -> tmService.scheduleTransfer(TABLE_NAME, archiveId));
    }

    @Test
    void checkUploadExceptions() {
        Long archiveId = 10L;

        assertThatThrownBy(() -> tmService.scheduleTransfer(TABLE_NAME, Collections.emptyList()));
        assertThatThrownBy(() -> tmService.scheduleTransfer(TABLE_NAME, null));
        assertThatThrownBy(() -> tmService.scheduleTransfer(null, List.of(archiveId)));
        assertThatThrownBy(() -> tmService.scheduleTransfer("   \t", List.of(archiveId)));
    }

    @DisplayName("Статус трансфера в зависимости от результата операции")
    @ParameterizedTest
    @ArgumentsSource(TestDataProvider.class)
    void testGetTransferStatus(TmOperationResponse operation, TmTransferStatus status) {
        String id = Optional.ofNullable(operation).map(TmOperationResponse::getId).orElse(null);
        when(clientMock.getOperation(id)).thenReturn(operation);


        //tested method
        TmTransferStatus transferStatus = tmService.getTransferStatus(id);
        assertThat(transferStatus).isEqualTo(status);

        verify(clientMock, times(1)).getOperation(id);
        verify(clientMock, times(1)).getOperation(any());
    }

    static class TestDataProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.of(
                    createOperation(true, null),
                    TmTransferStatus.FINISHED_SUCCESSFULLY
                ),
                Arguments.of(
                    createOperation(true, new TmError()),
                    TmTransferStatus.FINISHED_FAULTILY
                ),
                Arguments.of(
                    createOperation(false, null),
                    TmTransferStatus.IN_PROGRESS
                ),
                Arguments.of(
                    null,
                    TmTransferStatus.NOT_FOUND
                )
            );
        }
    }

    static TmOperationResponse createOperation(boolean done, TmError error) {
        return new TmOperationResponse(
                "operation-id",
                "desc",
                Instant.now(),
                Instant.now(),
                "created-by",
                done,
                error
        );
    }

}
