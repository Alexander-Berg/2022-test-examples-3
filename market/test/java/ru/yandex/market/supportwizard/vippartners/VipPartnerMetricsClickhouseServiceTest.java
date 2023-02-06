package ru.yandex.market.supportwizard.vippartners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.storage.EnvironmentRepository;
import ru.yandex.market.supportwizard.yqlapi.OperationDto;
import ru.yandex.market.supportwizard.yqlapi.ProgramSyntax;
import ru.yandex.market.supportwizard.yqlapi.Status;
import ru.yandex.market.supportwizard.yqlapi.YqlApiService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;

@DbUnitDataSet
public class VipPartnerMetricsClickhouseServiceTest extends BaseFunctionalTest {

    @Autowired
    EnvironmentRepository environmentRepository;
    @Autowired
    YqlApiService yqlApiService;
    @Autowired
    private VipPartnerMetricsClickhouseService vipPartnerMetricsClickhouseService;

    @BeforeEach
    void init() {
        Mockito.when(yqlApiService.postOperation(contains("min(ts) ts"), eq(ProgramSyntax.CLICKHOUSE))).thenReturn(
                new OperationDto("maxtimestamp1", Status.COMPLETED));
        Mockito.when(yqlApiService.postOperation(ArgumentMatchers.argThat(
                s -> s != null && s.contains("page_id as page") && s.contains("1628139600") && s
                        .contains("1628154000")),
                eq(ProgramSyntax.CLICKHOUSE))).thenReturn(
                new OperationDto("page1", Status.COMPLETED)
        );

        Mockito.when(yqlApiService.postOperation(ArgumentMatchers.argThat(
                s -> s != null && s.contains("page_id as page") && s.contains("1628154000") && s
                        .contains("1628164800")),
                eq(ProgramSyntax.CLICKHOUSE))).thenReturn(
                new OperationDto("page2", Status.COMPLETED)
        );

        Mockito.when(yqlApiService.postOperation(ArgumentMatchers.argThat(
                s -> s != null && s.contains("request_method") && s.contains("1628139600") && s.contains("1628154000")),
                eq(ProgramSyntax.CLICKHOUSE))).thenReturn(
                new OperationDto("method1", Status.COMPLETED)
        );

        Mockito.when(yqlApiService.postOperation(ArgumentMatchers.argThat(
                s -> s != null && s.contains("request_method") && s.contains("1628154000") && s.contains("1628164800")),
                eq(ProgramSyntax.CLICKHOUSE))).thenReturn(
                new OperationDto("method2", Status.COMPLETED)
        );

        Mockito.when(yqlApiService.getOperationStatus("maxtimestamp1")).thenReturn(Status.COMPLETED);
        Mockito.when(yqlApiService.getOperationStatus("page1")).thenReturn(Status.COMPLETED);
        Mockito.when(yqlApiService.getOperationStatus("method1")).thenReturn(Status.COMPLETED);
        Mockito.when(yqlApiService.getOperationStatus("page2")).thenReturn(Status.COMPLETED);
        Mockito.when(yqlApiService.getOperationStatus("method2")).thenReturn(Status.COMPLETED);

        Mockito.when(yqlApiService.getOperationResult(eq("maxtimestamp1"), any())).thenReturn(1628164800L);
        Mockito.when(yqlApiService.getOperationResult(eq("page1"), any())).thenReturn(
                Collections
                        .singletonList(new VipPartnerMetric("page-1", MetricType.PAGE, new TreeSet<>(Set.of(1L)), Arrays.asList("a/b", "c/d"), 2L)));
        Mockito.when(yqlApiService.getOperationResult(eq("page2"), any())).thenReturn(
                Collections.singletonList(
                        new VipPartnerMetric("page-2", MetricType.PAGE, new TreeSet<>(Set.of(1L, 2L)), Arrays.asList("e/f", "g/h"), 4L)));
        Mockito.when(yqlApiService.getOperationResult(eq("method1"), any())).thenReturn(
                Collections.singletonList(
                        new VipPartnerMetric("method-1", MetricType.METHOD, new TreeSet<>(Set.of(4L)), Arrays.asList("k/l", "m/n"), 8L)));
        Mockito.when(yqlApiService.getOperationResult(eq("method2"), any())).thenReturn(
                Collections.singletonList(
                        new VipPartnerMetric("method-2", MetricType.METHOD, new TreeSet<>(Set.of(8L, 16L)), Arrays.asList("o/p", "q/r"), 16L)));
    }

    @Test
    @DisplayName("Проверить корректность процедуры забора метрик из кликхауса двумя пачками")
    @DbUnitDataSet(before = "processClickhouseMetrics.before.csv", after = "processClickhouseMetrics.after.csv")
    void testGetMetricsWithTwoBatches() {
        List<VipPartnerMetric> partnerMetrics = new ArrayList<>();
        vipPartnerMetricsClickhouseService.processPartnerMetrics((yql, vipPartnerMetrics) -> {
            partnerMetrics.addAll(vipPartnerMetrics);
            return null;
        });
        assertThat(partnerMetrics, containsInAnyOrder(
                new VipPartnerMetric("page-1", MetricType.PAGE, new TreeSet<>(Set.of(1L)), Arrays.asList("a/b", "c/d"), 2L),
                new VipPartnerMetric("page-2", MetricType.PAGE, new TreeSet<>(Set.of(1L, 2L)), Arrays.asList("e/f", "g/h"), 4L),
                new VipPartnerMetric("method-1", MetricType.METHOD, new TreeSet<>(Set.of(4L)), Arrays.asList("k/l", "m/n"), 8L),
                new VipPartnerMetric("method-2", MetricType.METHOD, new TreeSet<>(Set.of(8L, 16L)), Arrays.asList("o/p", "q/r"), 16L)
        ));
    }
}



