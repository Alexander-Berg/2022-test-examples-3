package ru.yandex.market.fulfillment.stockstorage.service.health;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.fulfillment.stockstorage.repository.replica.ReplicaMonitoringRepository;
import ru.yandex.market.fulfillment.stockstorage.service.lms.LmsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CrossdockSkuDivergenceCheckerTest {

    @Mock
    private LmsService lmsService;

    @Mock
    private ReplicaMonitoringRepository replicaMonitoringRepository;

    @InjectMocks
    private CrossdockSkuDivergenceChecker crossdockSkuDivergenceChecker;

    private SoftAssertions assertions;

    @BeforeEach
    public void setup() {
        assertions = new SoftAssertions();

        MockitoAnnotations.initMocks(this);

        setupLmsResponse(ImmutableMap.of(1L, ImmutableSet.of(10L, 20L, 30L)));

        when(replicaMonitoringRepository.findAllCrossdockSkuDivergences(any(), any(), any()))
                .thenReturn(Collections.emptyList());
    }

    @AfterEach
    public void assertAll() {
        assertions.assertAll();
    }

    @Test
    public void noCrossdockSuppliersTest() {
        when(lmsService.getCrossdockSuppliersIdsByFulfillmentId()).thenReturn(new HashMap<>());

        CheckResult result = crossdockSkuDivergenceChecker.check();

        assertions.assertThat(result).isNotNull();
        assertions.assertThat(result.getLevel()).isEqualTo(CheckResult.Level.CRITICAL);
    }

    @Test
    public void noCrossdockSkuDivergence() {
        CheckResult result = crossdockSkuDivergenceChecker.check();

        assertions.assertThat(result).isNotNull();
        assertions.assertThat(result.getLevel()).isEqualTo(CheckResult.Level.OK);
    }

    @Test
    public void twoFulfillmentServicesTestNoDivergence() {
        setupLmsResponse(ImmutableMap.of(
                1L, ImmutableSet.of(10L, 20L, 30L),
                2L, ImmutableSet.of(40L, 50L, 60L)));

        CheckResult result = crossdockSkuDivergenceChecker.check();

        assertions.assertThat(result).isNotNull();
        assertions.assertThat(result.getLevel()).isEqualTo(CheckResult.Level.OK);
    }

    @Test
    public void twoFulfillmentServicesTestWithDivergence() {
        setupLmsResponse(ImmutableMap.of(
                1L, ImmutableSet.of(10L, 20L, 30L),
                2L, ImmutableSet.of(40L, 50L, 60L)));

        when(replicaMonitoringRepository.findAllCrossdockSkuDivergences(any(), any(), any()))
                .thenReturn(Arrays.asList("a", "b", "c"));

        CheckResult result = crossdockSkuDivergenceChecker.check();

        assertions.assertThat(result).isNotNull();
        assertions.assertThat(result.getLevel()).isEqualTo(CheckResult.Level.CRITICAL);
        verify(replicaMonitoringRepository, times(2)).findAllCrossdockSkuDivergences(any(), any(), any());
    }

    @Test
    public void twoBatches() {
        setupLmsResponse(ImmutableMap.of(
                172L,
                LongStream.rangeClosed(0, CrossdockSkuDivergenceChecker.PARTNERS_BATCH_SIZE)
                        .boxed()
                        .collect(Collectors.toSet())));

        CheckResult result = crossdockSkuDivergenceChecker.check();

        assertions.assertThat(result).isNotNull();
        assertions.assertThat(result.getLevel()).isEqualTo(CheckResult.Level.OK);
        verify(replicaMonitoringRepository, times(2)).findAllCrossdockSkuDivergences(any(), any(), any());
    }

    @Test
    public void crossdockSkuDivergenceWasFound() {
        when(replicaMonitoringRepository.findAllCrossdockSkuDivergences(any(), any(), any()))
                .thenReturn(Arrays.asList("a", "b", "c"));

        CheckResult result = crossdockSkuDivergenceChecker.check();

        assertions.assertThat(result).isNotNull();
        assertions.assertThat(result.getLevel()).isEqualTo(CheckResult.Level.CRITICAL);
        assertions.assertThat(result.getMessage()).contains("3");
    }

    private void setupLmsResponse(Map<Long, Set<Long>> ids) {
        when(lmsService.getCrossdockSuppliersIdsByFulfillmentId()).thenReturn(ids);
    }
}
