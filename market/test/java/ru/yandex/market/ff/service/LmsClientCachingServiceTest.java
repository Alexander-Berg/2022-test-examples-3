package ru.yandex.market.ff.service;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.util.concurrent.UncheckedExecutionException;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.model.bo.PartnerRelation;
import ru.yandex.market.ff.service.implementation.LmsClientCachingServiceImpl;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link LmsClientCachingService}.
 */
public class LmsClientCachingServiceTest {

    private static final long PARTNER_FROM = 1;
    private static final long PARTNER_TO = 2;
    private static final PartnerRelation PARTNER_RELATION = new PartnerRelation(PARTNER_FROM, PARTNER_TO);
    private static final CutoffResponse FIRST_CUTOFF =
        CutoffResponse.newBuilder().cutoffTime(LocalTime.of(10, 10)).build();
    private static final CutoffResponse SECOND_CUTOFF =
        CutoffResponse.newBuilder().cutoffTime(LocalTime.of(11, 0)).build();
    private static final List<CutoffResponse> CUTOFF_TIMES = asList(FIRST_CUTOFF, SECOND_CUTOFF);
    private static final long PARTNER_ID = 172;

    private LmsClientCachingService lmsClientCachingService;
    private LmsClientCachingService lmsClientCachingServiceWithOneSecondInvalidationTime;
    private LMSClient lmsClient;
    private SoftAssertions assertions;

    @BeforeEach
    public void init() {
        lmsClient = Mockito.mock(LMSClient.class);
        lmsClientCachingService = new LmsClientCachingServiceImpl(lmsClient, 600, 600, 1);
        lmsClientCachingServiceWithOneSecondInvalidationTime = new LmsClientCachingServiceImpl(lmsClient, 1, 1, 1);
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void after() {
        lmsClientCachingService.invalidateCache();
        assertions.assertAll();
    }

    @Test
    public void returnPreviouslyCachedValueIfFailedToComputeNewOneForCutoff() throws Exception {
        Mockito.when(lmsClient.getCutoffs(PARTNER_FROM, PARTNER_TO))
            .thenReturn(CUTOFF_TIMES)
            .thenThrow(new RuntimeException("Connection timeout"));
        List<CutoffResponse> cutoffTimes =
            lmsClientCachingServiceWithOneSecondInvalidationTime.getCutoffTimes(PARTNER_RELATION);
        assertions.assertThat(cutoffTimes).isEqualTo(CUTOFF_TIMES);
        Thread.sleep(1001);
        cutoffTimes = lmsClientCachingServiceWithOneSecondInvalidationTime.getCutoffTimes(PARTNER_RELATION);
        assertions.assertThat(cutoffTimes).isEqualTo(CUTOFF_TIMES);
        Mockito.verify(lmsClient, Mockito.times(2)).getCutoffs(PARTNER_FROM, PARTNER_TO);
    }

    @Test
    public void secondRequestCachedForCutoff() {
        Mockito.when(lmsClient.getCutoffs(PARTNER_FROM, PARTNER_TO)).thenReturn(CUTOFF_TIMES);
        List<CutoffResponse> cutoffTimes = lmsClientCachingService.getCutoffTimes(PARTNER_RELATION);
        assertions.assertThat(cutoffTimes).isEqualTo(CUTOFF_TIMES);
        cutoffTimes = lmsClientCachingService.getCutoffTimes(PARTNER_RELATION);
        assertions.assertThat(cutoffTimes).isEqualTo(CUTOFF_TIMES);
        Mockito.verify(lmsClient).getCutoffs(PARTNER_FROM, PARTNER_TO);
    }

    @Test
    public void requestAfterCacheInvalidationExecutedForCutoff() {
        Mockito.when(lmsClient.getCutoffs(PARTNER_FROM, PARTNER_TO))
            .thenReturn(CUTOFF_TIMES)
            .thenReturn(CUTOFF_TIMES);
        List<CutoffResponse> cutoffTimes = lmsClientCachingService.getCutoffTimes(PARTNER_RELATION);
        assertions.assertThat(cutoffTimes).isEqualTo(CUTOFF_TIMES);
        lmsClientCachingService.invalidateCache();
        cutoffTimes = lmsClientCachingService.getCutoffTimes(PARTNER_RELATION);
        assertions.assertThat(cutoffTimes).isEqualTo(CUTOFF_TIMES);
        Mockito.verify(lmsClient, Mockito.times(2)).getCutoffs(PARTNER_FROM, PARTNER_TO);
    }

    @Test
    public void exceptionIsThrownIfFailedToFindOnTheFirstAttemptForCutoff() {
        Mockito.when(lmsClient.getCutoffs(PARTNER_FROM, PARTNER_TO))
            .thenThrow(new RuntimeException("Connection timeout"));
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            lmsClientCachingService.getCutoffTimes(PARTNER_RELATION));
        assertions.assertThat(exception.getCause().getCause()).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void returnPreviouslyCachedValueIfFailedToComputeNewOneBecauseOfExceptionForLogisticPoint()
        throws Exception {
        LogisticsPointResponse expectedLogisticPoint = createLogisticPointResponse();
        Mockito.when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class)))
            .thenReturn(Collections.singletonList(expectedLogisticPoint))
            .thenThrow(new RuntimeException("Connection timeout"));
        LogisticsPointResponse logisticPoint =
            lmsClientCachingServiceWithOneSecondInvalidationTime.getLogisticPoint(PARTNER_ID);
        assertions.assertThat(logisticPoint).isEqualTo(expectedLogisticPoint);
        Thread.sleep(1001);
        logisticPoint = lmsClientCachingServiceWithOneSecondInvalidationTime.getLogisticPoint(PARTNER_ID);
        assertions.assertThat(logisticPoint).isEqualTo(expectedLogisticPoint);
        Mockito.verify(lmsClient, Mockito.times(2))
            .getLogisticsPoints(any(LogisticsPointFilter.class));
    }

    @Test
    public void returnPreviouslyCachedValueIfFailedToComputeNewOneBecauseOfIncorrectDateForLogisticPoint()
        throws Exception {
        LogisticsPointResponse expectedLogisticPoint = createLogisticPointResponse();
        Mockito.when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class)))
            .thenReturn(Collections.singletonList(expectedLogisticPoint))
            .thenReturn(Collections.emptyList());
        LogisticsPointResponse logisticPoint =
            lmsClientCachingServiceWithOneSecondInvalidationTime.getLogisticPoint(PARTNER_ID);
        assertions.assertThat(logisticPoint).isEqualTo(expectedLogisticPoint);
        Thread.sleep(1001);
        logisticPoint = lmsClientCachingServiceWithOneSecondInvalidationTime.getLogisticPoint(PARTNER_ID);
        assertions.assertThat(logisticPoint).isEqualTo(expectedLogisticPoint);
        Mockito.verify(lmsClient, Mockito.times(2))
            .getLogisticsPoints(any(LogisticsPointFilter.class));
    }

    @Test
    public void secondRequestCachedForLogisticPoint() {
        LogisticsPointResponse expectedLogisticPoint = createLogisticPointResponse();
        Mockito.when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class)))
            .thenReturn(Collections.singletonList(expectedLogisticPoint));
        LogisticsPointResponse logisticPoint = lmsClientCachingService.getLogisticPoint(PARTNER_ID);
        assertions.assertThat(logisticPoint).isEqualTo(expectedLogisticPoint);
        logisticPoint = lmsClientCachingService.getLogisticPoint(PARTNER_ID);
        assertions.assertThat(logisticPoint).isEqualTo(expectedLogisticPoint);
        Mockito.verify(lmsClient).getLogisticsPoints(any(LogisticsPointFilter.class));
    }

    @Test
    public void requestAfterCacheInvalidationExecutedForLogisticPoint() {
        LogisticsPointResponse expectedLogisticPoint = createLogisticPointResponse();
        Mockito.when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class)))
            .thenReturn(Collections.singletonList(expectedLogisticPoint))
            .thenReturn(Collections.singletonList(expectedLogisticPoint));
        LogisticsPointResponse logisticPoint = lmsClientCachingService.getLogisticPoint(PARTNER_ID);
        assertions.assertThat(logisticPoint).isEqualTo(expectedLogisticPoint);
        lmsClientCachingService.invalidateCache();
        logisticPoint = lmsClientCachingService.getLogisticPoint(PARTNER_ID);
        assertions.assertThat(logisticPoint).isEqualTo(expectedLogisticPoint);
        Mockito.verify(lmsClient, Mockito.times(2)).getLogisticsPoints(any(LogisticsPointFilter.class));
    }

    @Test
    public void exceptionIsThrownIfFailedToFindOnTheFirstAttemptForLogisticPointsBecauseOfException() {
        Mockito.when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class)))
            .thenThrow(new RuntimeException("Connection timeout"));
        RuntimeException exception =
            assertThrows(RuntimeException.class, () -> lmsClientCachingService.getLogisticPoint(PARTNER_ID));
        Throwable cause = exception.getCause();
        assertions.assertThat(cause).isInstanceOf(UncheckedExecutionException.class);
        assertions.assertThat(cause.getCause()).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void exceptionIsThrownIfFailedToFindOnTheFirstAttemptForLogisticPointsBecauseOfIncorrectData() {
        Mockito.when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class)))
            .thenReturn(Arrays.asList(createLogisticPointResponse(), createLogisticPointResponse()));
        RuntimeException exception =
            assertThrows(RuntimeException.class, () -> lmsClientCachingService.getLogisticPoint(PARTNER_ID));
        Throwable cause = exception.getCause();
        assertions.assertThat(cause).isInstanceOf(UncheckedExecutionException.class);
        assertions.assertThat(cause.getCause()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getEmails() {
        PartnerExternalParamResponse partnerExternalParam =
                PartnerExternalParamResponse.newBuilder().partnerId(145L).key(PartnerExternalParamType.SERVICE_EMAILS)
                        .value("test@yandex.ru").build();
        when(lmsClient.getPartnerExternalParam(eq(145L), eq(PartnerExternalParamType.SERVICE_EMAILS)))
                .thenReturn(partnerExternalParam);

        String email = lmsClientCachingService.getPartnerEmails(145L);

        assertions.assertThat(email).isEqualTo("test@yandex.ru");
        Mockito.verify(lmsClient, Mockito.times(1)).getPartnerExternalParam(anyLong(), any());
    }

    @Test
    public void getEmailsIsCached() {
        PartnerExternalParamResponse partnerExternalParam =
                PartnerExternalParamResponse.newBuilder().partnerId(145L).key(PartnerExternalParamType.SERVICE_EMAILS)
                        .value("test@yandex.ru").build();

        when(lmsClient.getPartnerExternalParam(145L, PartnerExternalParamType.SERVICE_EMAILS))
                .thenReturn(partnerExternalParam);

        String first = lmsClientCachingService.getPartnerEmails(145L);
        String second = lmsClientCachingService.getPartnerEmails(145L);

        assertions.assertThat(first).isEqualTo("test@yandex.ru");
        assertions.assertThat(second).isEqualTo("test@yandex.ru");
        Mockito.verify(lmsClient, Mockito.times(1)).getPartnerExternalParam(anyLong(), any());
    }

    @Test
    public void getEmailsIsCachedAndThenThrown() throws Exception {
        PartnerExternalParamResponse partnerExternalParam =
                PartnerExternalParamResponse.newBuilder().partnerId(145L).key(PartnerExternalParamType.SERVICE_EMAILS)
                        .value("test@yandex.ru").build();

        when(lmsClient.getPartnerExternalParam(145L, PartnerExternalParamType.SERVICE_EMAILS))
                .thenReturn(partnerExternalParam).thenThrow(new RuntimeException("test cache with exception"));

        String first = lmsClientCachingService.getPartnerEmails(145L);
        Thread.sleep(1001);
        String second = lmsClientCachingService.getPartnerEmails(145L);

        assertions.assertThat(first).isEqualTo("test@yandex.ru");
        assertions.assertThat(second).isEqualTo("test@yandex.ru");
        Mockito.verify(lmsClient, Mockito.times(1)).getPartnerExternalParam(anyLong(), any());
    }

    private LogisticsPointResponse createLogisticPointResponse() {
        return LogisticsPointResponse.newBuilder().build();
    }
}
