package ru.yandex.market.mbo.yt.transfermanager;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author danfertev
 * @since 13.05.2020
 */
public class TransferManagerServiceTest {
    private static final int MAX_TASK_COUNT = 3;
    private static final int WAIT_TIMEOUT_MILLIS = 500;
    private static final int RETRY_TIMEOUT_MILLIS = 100;
    private static final int MAX_NOT_VALID_ATTEMPTS = 3;

    private static final String SOURCE_CLUSTER = "source_cluster";
    private static final String SOURCE_PATH = "source_path";
    private static final String DESTINATION_CLUSTER = "destination_cluster";
    private static final String DESTINATION_PATH = "destination_path";

    private static final TransferStatus COMPLETED = new TransferStatus();
    private static final TransferStatus FAILED = new TransferStatus();
    private static final TransferStatus ABORTED = new TransferStatus();
    private static final TransferStatus RUNNING = new TransferStatus();
    private static final TransferStatus NOT_VALID = new TransferStatus();

    static {
        COMPLETED.setState("completed");
        FAILED.setState("failed");
        ABORTED.setState("aborted");
        RUNNING.setState("running");
    }

    private TransferManagerService transferManagerService;
    private TransferManagerClient transferManagerClient;

    @Before
    public void setUp() throws Exception {
        transferManagerClient = Mockito.mock(TransferManagerClient.class);
        Mockito.when(transferManagerClient.transfer(Mockito.any())).thenAnswer(args -> {
            TransferRequest request = args.getArgument(0);
            return request.toString();
        });
        transferManagerService = new TransferManagerService(
            transferManagerClient,
            MAX_TASK_COUNT, WAIT_TIMEOUT_MILLIS, RETRY_TIMEOUT_MILLIS, MAX_NOT_VALID_ATTEMPTS
        );
    }

    @Test
    public void transferSuccessOnCompletedResponse() {
        Mockito.when(transferManagerClient.status(Mockito.any())).thenReturn(COMPLETED);
        boolean result = transferManagerService.transferFile(
            SOURCE_CLUSTER, SOURCE_PATH, DESTINATION_CLUSTER, DESTINATION_PATH
        );

        Mockito.verify(transferManagerClient, Mockito.atLeastOnce()).transfer(Mockito.any());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void transferFailedOnFailedResponse() {
        Mockito.when(transferManagerClient.status(Mockito.any())).thenReturn(FAILED);
        boolean result = transferManagerService.transferFile(
            SOURCE_CLUSTER, SOURCE_PATH, DESTINATION_CLUSTER, DESTINATION_PATH
        );

        Mockito.verify(transferManagerClient, Mockito.atLeastOnce()).status(Mockito.any());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void transferFailedOnAbortedResponse() {
        Mockito.when(transferManagerClient.status(Mockito.any())).thenReturn(ABORTED);
        boolean result = transferManagerService.transferFile(
            SOURCE_CLUSTER, SOURCE_PATH, DESTINATION_CLUSTER, DESTINATION_PATH
        );

        Mockito.verify(transferManagerClient, Mockito.atLeastOnce()).status(Mockito.any());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void transferFailedOnTimeoutResponse() {
        Mockito.when(transferManagerClient.status(Mockito.any())).thenReturn(RUNNING);

        Assertions.assertThatThrownBy(() -> transferManagerService.transferFile(
            SOURCE_CLUSTER, SOURCE_PATH, DESTINATION_CLUSTER, DESTINATION_PATH
        )).hasCauseInstanceOf(TimeoutException.class);

        Mockito.verify(transferManagerClient, Mockito.atLeast(WAIT_TIMEOUT_MILLIS / RETRY_TIMEOUT_MILLIS))
            .status(Mockito.any());
    }

    @Test
    public void transferFailedOnNotValidResponse() {
        Mockito.when(transferManagerClient.status(Mockito.any())).thenReturn(NOT_VALID);

        boolean result = transferManagerService.transferFile(
            SOURCE_CLUSTER, SOURCE_PATH, DESTINATION_CLUSTER, DESTINATION_PATH
        );

        Mockito.verify(transferManagerClient, Mockito.atLeast(MAX_NOT_VALID_ATTEMPTS))
            .status(Mockito.any());
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void transferSuccessAfterNotValidThenCompletedResponse() {
        AtomicInteger attempts = new AtomicInteger(0);
        Mockito.when(transferManagerClient.status(Mockito.any())).then(args -> {
            int attempt = attempts.incrementAndGet();
            return attempt >= MAX_NOT_VALID_ATTEMPTS - 1 ? COMPLETED : NOT_VALID;
        });

        boolean result = transferManagerService.transferFile(
            SOURCE_CLUSTER, SOURCE_PATH, DESTINATION_CLUSTER, DESTINATION_PATH
        );

        Mockito.verify(transferManagerClient, Mockito.atLeast(MAX_NOT_VALID_ATTEMPTS - 1))
            .status(Mockito.any());
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void transferSuccessAfterRunningThenCompletedResponse() {
        AtomicInteger attempts = new AtomicInteger(0);
        Mockito.when(transferManagerClient.status(Mockito.any())).then(args -> {
            int attempt = attempts.incrementAndGet();
            return attempt >= MAX_NOT_VALID_ATTEMPTS - 1 ? COMPLETED : RUNNING;
        });

        boolean result = transferManagerService.transferFile(
            SOURCE_CLUSTER, SOURCE_PATH, DESTINATION_CLUSTER, DESTINATION_PATH
        );

        Mockito.verify(transferManagerClient, Mockito.atLeast(MAX_NOT_VALID_ATTEMPTS - 1))
            .status(Mockito.any());
        Assertions.assertThat(result).isTrue();
    }
}
