package ru.yandex.direct.ytwrapper.dynamic.selector;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import ru.yandex.direct.tracing.Trace;
import ru.yandex.direct.tracing.util.TraceCommentVarsHolder;
import ru.yandex.direct.ytwrapper.model.YtCluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.ytwrapper.dynamic.selector.ClusterFreshness.FRESH;
import static ru.yandex.direct.ytwrapper.dynamic.selector.ClusterFreshness.ROTTEN;
import static ru.yandex.direct.ytwrapper.dynamic.selector.ClusterFreshness.UNAVAILABLE;
import static ru.yandex.direct.ytwrapper.model.YtCluster.SENECA_MAN;
import static ru.yandex.direct.ytwrapper.model.YtCluster.SENECA_SAS;
import static ru.yandex.direct.ytwrapper.model.YtCluster.SENECA_VLA;

public class FreshnessAndHashBasedClusterChooserTest {
    private static final Collection<YtCluster> ytClusters = ImmutableSet.of(SENECA_MAN,
            SENECA_SAS, SENECA_VLA);
    private static final Map<YtCluster, ClusterFreshness> allGood = ImmutableMap.of(SENECA_MAN, FRESH,
            SENECA_VLA, FRESH,
            SENECA_SAS, FRESH);
    @Captor
    ArgumentCaptor<TimerTask> captor;
    private Map<YtCluster, ClusterFreshness> clusterFreshnessMap;
    private TimerTask timerTask;
    private Trace trace;
    private FreshnessAndHashBasedClusterChooser clusterChooser;

    @Before
    public void setUp() {
        Timer timer = mock(Timer.class);
        doNothing().when(timer).schedule(any(), eq(0L), anyLong());
        ClusterWeightFunction weightFunction = new StatClusterWeightFunc();
        clusterChooser = new FreshnessAndHashBasedClusterChooser(timer,
                Duration.ZERO, ytClusters, weightFunction);
        captor = ArgumentCaptor.forClass(TimerTask.class);
        verify(timer).schedule(captor.capture(), eq(0L), anyLong());
        timerTask = captor.getValue();
        trace = mock(Trace.class);
    }

    @Test
    public void precalculatedWithNoOperator() {
        clusterFreshnessMap = allGood;
        timerTask.run();
        // это случайный TraceId https://imgs.xkcd.com/comics/random_number.png
        when(trace.getTraceId()).thenReturn(4L);
        Trace.push(trace);
        YtCluster answer = clusterChooser.getCluster(null).orElse(null);
        // Значение для сверки может меняться при смене имплементации Random
        assertEquals(SENECA_MAN, answer);
    }

    @Test
    public void precalculatedDifferentWithNoOperator() {
        clusterFreshnessMap = allGood;
        timerTask.run();
        when(trace.getTraceId()).thenReturn(4L);
        Trace.push(trace);
        YtCluster answer = clusterChooser.getCluster(null).orElse(null);
        Trace.pop();
        // Если Random даёт то же значение, то надо менять константу ниже
        when(trace.getTraceId()).thenReturn(5L);
        Trace.push(trace);
        YtCluster answer2 = clusterChooser.getCluster(null).orElse(null);
        assertNotEquals(answer2, answer);
    }

    @Test
    public void stableWithNoOperator() {
        clusterFreshnessMap = allGood;
        timerTask.run();
        when(trace.getTraceId()).thenReturn(70789L);
        Trace.push(trace);
        YtCluster expected = clusterChooser.getCluster(null).orElse(null);
        YtCluster nextOne = clusterChooser.getCluster(null).orElse(null);
        assertEquals(expected, nextOne);
    }

    @Test
    public void precalculatedWithOperator() {
        clusterFreshnessMap = allGood;
        timerTask.run();
        when(trace.getTraceId()).thenReturn(4L);
        verifyZeroInteractions(trace);
        Trace.push(trace);
        TraceCommentVarsHolder.get().setOperator("8086");
        YtCluster answer = clusterChooser.getCluster(null).orElse(null);
        // Значение для сверки может меняться при смене имплементации Random
        assertEquals(SENECA_MAN, answer);
    }

    @Test
    public void precalculatedSingleFreshWithOperator() {
        clusterFreshnessMap = ImmutableMap.of(SENECA_MAN, ROTTEN, SENECA_VLA, FRESH, SENECA_SAS, ROTTEN);
        timerTask.run();
        when(trace.getTraceId()).thenReturn(4L);
        verifyZeroInteractions(trace);
        Trace.push(trace);
        TraceCommentVarsHolder.get().setOperator("8086");
        YtCluster answer = clusterChooser.getCluster(null).orElse(null);
        assertEquals(SENECA_VLA, answer);
    }

    @Test
    public void precalculatedNoFreshWithOperator() {
        clusterFreshnessMap = ImmutableMap.of(SENECA_MAN, ROTTEN, SENECA_VLA, ROTTEN, SENECA_SAS, UNAVAILABLE);
        timerTask.run();
        when(trace.getTraceId()).thenReturn(4L);
        verifyZeroInteractions(trace);
        Trace.push(trace);
        TraceCommentVarsHolder.get().setOperator("8086");
        YtCluster answer = clusterChooser.getCluster(null).orElse(null);
        // Значение для сверки может меняться при смене имплементации Random
        assertEquals(SENECA_MAN, answer);
    }

    @Test
    public void stableNoFreshWithOperator() {
        clusterFreshnessMap = ImmutableMap.of(SENECA_MAN, ROTTEN, SENECA_VLA, ROTTEN, SENECA_SAS, UNAVAILABLE);
        timerTask.run();
        when(trace.getTraceId()).thenReturn(4L);
        verifyZeroInteractions(trace);
        Trace.push(trace);
        TraceCommentVarsHolder.get().setOperator("8088");
        YtCluster expected = clusterChooser.getCluster(null).orElse(null);
        YtCluster nextOne = clusterChooser.getCluster(null).orElse(null);
        assertEquals(expected, nextOne);
    }

    @Test
    public void allUnavailable() {
        clusterFreshnessMap = ImmutableMap.of(SENECA_MAN, UNAVAILABLE,
                SENECA_VLA, UNAVAILABLE,
                SENECA_SAS, UNAVAILABLE);
        timerTask.run();
        when(trace.getTraceId()).thenReturn(4L);
        verifyZeroInteractions(trace);
        Trace.push(trace);
        TraceCommentVarsHolder.get().setOperator("8086");
        YtCluster answer = clusterChooser.getCluster(null).orElse(null);
        Assertions.assertThat(answer).isNull();
    }

    @After
    public void clearTrace() {
        Trace.pop();
        TraceCommentVarsHolder.get().setOperator(null);
    }

    private class StatClusterWeightFunc implements ClusterWeightFunction {
        @Override
        public ClusterFreshness apply(YtCluster ytCluster) {
            return clusterFreshnessMap.get(ytCluster);
        }
    }
}
