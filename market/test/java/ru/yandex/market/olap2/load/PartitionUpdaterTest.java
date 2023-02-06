package ru.yandex.market.olap2.load;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.leader.LeaderElector;
import ru.yandex.market.olap2.model.YtCluster;
import ru.yandex.market.olap2.yt.YtTableService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PartitionUpdaterTest {

    private PartitionDataUpdater tg;
    @Mock
    private MetadataDao metadataDaoImpl;

    @Mock
    private YtTableService ytTableService;

    @Mock
    private LeaderElector le;

    @Before
    public void setup() {
        tg = new PartitionDataUpdater(metadataDaoImpl, ytTableService, le);
    }

    @Test
    public void testWorkflow() {
        when(ytTableService.exists(any(), any())).thenReturn(true);
        when(ytTableService.getTableRowCount(any(), any())).thenReturn(10L);
        when(ytTableService.getTableSize(any(), any())).thenReturn(100L);

        tg.updateAttrs("//cubes/my_round_cube/2020-08", new YtCluster("hahn"));
        verify(metadataDaoImpl).updateAttributes(eq("//cubes/my_round_cube/2020-08"), eq("hahn"), eq(10L), eq(100L),
                any());

    }
}
