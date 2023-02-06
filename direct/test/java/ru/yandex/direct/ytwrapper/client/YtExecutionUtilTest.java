package ru.yandex.direct.ytwrapper.client;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.direct.ytwrapper.exceptions.OperationRunningException;
import ru.yandex.direct.ytwrapper.model.YtCluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class YtExecutionUtilTest {
    private static final int ANSWER = 42;
    private static final List<YtCluster> CLUSTERS = ImmutableList.of(YtCluster.HAHN, YtCluster.ARNOLD);

    @Test
    public void executeWithFallback_ReturnsResult_IfAllIsOk() {
        //noinspection unchecked
        Function<YtCluster, Integer> action = mock(Function.class);
        when(action.apply(any())).thenReturn(ANSWER);

        Integer result = YtExecutionUtil.executeWithFallback(CLUSTERS, cluster -> cluster, action);
        assertThat(result).isEqualTo(ANSWER);
        verify(action, only()).apply(YtCluster.HAHN);
    }

    @Test
    public void executeWithFallback_ReturnsResult_IfFirstNotOkButSecondIsOk() {
        //noinspection unchecked
        Function<YtCluster, Integer> action = mock(Function.class);
        when(action.apply(YtCluster.HAHN)).thenThrow(ActionException.class);
        when(action.apply(YtCluster.ARNOLD)).thenReturn(ANSWER);

        Integer result = YtExecutionUtil.executeWithFallback(CLUSTERS, cluster -> cluster, action);
        assertThat(result).isEqualTo(ANSWER);
        verify(action, times(1)).apply(YtCluster.HAHN);
        verify(action, times(1)).apply(YtCluster.ARNOLD);
    }

    @Test(expected = InitializingException.class)
    public void executeWithFallback_ThrowsExceptionImmediately_IfInitializationFail() {
        //noinspection unchecked
        Function<YtCluster, YtCluster> initialization = mock(Function.class);
        when(initialization.apply(any())).thenThrow(InitializingException.class);
        YtExecutionUtil.executeWithFallback(CLUSTERS, initialization, x -> x);
    }

    @Test(expected = YtExecutionException.class)
    public void executeWithFallback_ThrowsException_IfAllAreNotOk() {
        //noinspection unchecked
        Function<YtCluster, Integer> action = mock(Function.class);
        when(action.apply(any())).thenThrow(ActionException.class);

        YtExecutionUtil.executeWithFallback(CLUSTERS, cluster -> cluster, action);
    }

    @Test(expected = YtExecutionException.class)
    public void executeWithFallback_ThrowsException_IfInterruptedExceptionIsCaught() {
        //noinspection unchecked
        Function<YtCluster, Integer> action = mock(Function.class);
        when(action.apply(any())).thenThrow(new OperationRunningException("", new InterruptedException()));

        YtExecutionUtil.executeWithFallback(CLUSTERS, cluster -> cluster, action);
    }

    private static class InitializingException extends RuntimeException {
    }

    private static class ActionException extends RuntimeException {
    }
}
