package ru.yandex.market.tsum.pipe.engine.runtime.config;

import org.apache.curator.framework.CuratorFramework;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.tsum.pipe.engine.curator.CuratorFactory;
import ru.yandex.market.tsum.pipe.engine.curator.CuratorValueObservable;
import ru.yandex.market.tsum.pipe.engine.runtime.JobInterruptionService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.revision.PipeStateRevisionService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.revision.StageGroupStateVersionService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Мокает CuratorFramework и всё, что от него зависит.
 *
 * Ещё есть {@link ru.yandex.market.tsum.core.config.TestZkConfig}, он поднимает почти настоящий зукипер и
 * CuratorFramework, который на него смотрит. Его можно использовать только если очень надо, потому что он поднимается
 * медленно и может вызвать ложные падения тестов если использовать его слишком много.
 *
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 15.06.2018
 */
@Configuration
public class MockCuratorConfig {
    @Bean
    public CuratorFramework curatorFramework() {
        return Mockito.mock(CuratorFramework.class);
    }

    @Bean
    public CuratorFactory curatorFactory() {
        CuratorValueObservable nodeObservable = Mockito.mock(CuratorValueObservable.class);

        CuratorFactory mock = Mockito.mock(CuratorFactory.class);
        when(mock.createValueObservable(anyString())).thenReturn(nodeObservable);

        return mock;
    }

    @Bean
    public PipeStateRevisionService pipeStateRevisionService() {
        return Mockito.mock(PipeStateRevisionService.class);
    }

    @Bean
    public StageGroupStateVersionService stageGroupStateVersionService() {
        return Mockito.mock(StageGroupStateVersionService.class);
    }

    @Bean
    public JobInterruptionService jobInterruptionService() {
        return Mockito.mock(JobInterruptionService.class);
    }
}
