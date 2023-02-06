package ru.yandex.market.tsum;

import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.converters.PipelineConverter;
import ru.yandex.market.tsum.dao.PipelinesDao;
import ru.yandex.market.tsum.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsum.entity.pipeline.StoredConfigurationEntity;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 28/02/2019
 */
public class StorePipelineProviderTest {
    @Test
    public void testCaches() {
        PipelinesDao dao = mock(PipelinesDao.class);

        PipelineEntity entity = new PipelineEntity();
        entity.setCurrentConfiguration(new StoredConfigurationEntity());
        entity.getCurrentConfiguration().setVersion(1);
        entity.getCurrentConfiguration().setJobs(Collections.emptyList());

        when(dao.get(Mockito.eq("id"))).thenReturn(entity);

        PipelineConverter converter = mock(PipelineConverter.class);
        Pipeline pipeline = mock(Pipeline.class);
        when(converter.toPipeline(Mockito.any(PipelineEntity.class))).thenReturn(pipeline);

        StorePipelineProvider provider = new StorePipelineProvider(dao, converter);
        provider.get("id");
        provider.get("id");

        verify(converter, times(1)).toPipeline(Mockito.any(PipelineEntity.class));

        entity.getCurrentConfiguration().setVersion(2);
        provider.get("id");

        verify(converter, times(2)).toPipeline(Mockito.any(PipelineEntity.class));
    }
}
