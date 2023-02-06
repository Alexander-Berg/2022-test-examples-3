package ru.yandex.chemodan.uploader.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.uploader.status.strategy.LoadingStatusStrategy;
import ru.yandex.chemodan.uploader.status.strategy.LoadingStatusStrategyType;
import ru.yandex.misc.test.Assert;

import static org.mockito.Mockito.when;

/**
 * @author nshmakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = LoadingStatusManagerTest.Config.class)
public class LoadingStatusManagerTest {

    private static final LoadingStatusStrategy loadingStatusStrategy1 = Mockito.mock(LoadingStatusStrategy.class);
    private static final LoadingStatusStrategyType loadingStatusStrategy1Type = LoadingStatusStrategyType.DISK_IO;
    private static final LoadingStatusStrategy loadingStatusStrategy2 = Mockito.mock(LoadingStatusStrategy.class);
    private static final LoadingStatusStrategyType loadingStatusStrategy2Type = LoadingStatusStrategyType.NETWORK_IO;

    @Autowired
    private LoadingStatusManager sut;

    @Test
    public void shouldComputeLoadingStatusForAllStrategies() {
        when(loadingStatusStrategy1.compute()).thenReturn(1000L);
        when(loadingStatusStrategy2.compute()).thenReturn(5000L);

        MapF<LoadingStatusStrategyType, Long> actual =
                sut.computeLoadingStatus();

        Assert.equals(Cf.map(loadingStatusStrategy1Type, 1000L, loadingStatusStrategy2Type, 5000L), actual);
    }

    @Configuration
    public static class Config {

        @Bean
        public LoadingStatusManager loadingStatusManager() {
            return new LoadingStatusManager();
        }

        @Bean
        public LoadingStatusStrategy loadingStatusStrategy1() {
            when(loadingStatusStrategy1.getType()).thenReturn(loadingStatusStrategy1Type);
            return loadingStatusStrategy1;
        }

        @Bean
        public LoadingStatusStrategy loadingStatusStrategy2() {
            when(loadingStatusStrategy2.getType()).thenReturn(loadingStatusStrategy2Type);
            return loadingStatusStrategy2;
        }
    }
}
