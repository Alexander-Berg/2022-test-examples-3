package ru.yandex.market.delivery.mdbapp.components.service.yt;

import java.util.HashMap;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.delivery.mdbapp.configuration.YtProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class YtLogisticsPointServiceTest {

    private static final Long PARTNER_ID = 123L;
    private static final String OUTLET_PATH = "/";
    private static final String EXTERNAL_ID = "externalId";

    @Mock
    private YtTables ytTables;

    @Mock
    private YtTables backupYtTables;

    @Mock
    private Yt yt;

    @Mock(name = "backup")
    private Yt backupYt;

    @Mock
    private YtProperties ytProperties;

    @Test
    public void getLogisticsPointIdUsingBackup() {
        long lmsId = 10000000003L;
        YtLogisticsPointService ytLogisticsPointService = new YtLogisticsPointService(yt, backupYt, ytProperties);

        when(yt.tables()).thenReturn(ytTables);
        when(backupYt.tables()).thenReturn(backupYtTables);
        when(ytProperties.getYtOutletPath()).thenReturn(OUTLET_PATH);

        doThrow(new RuntimeException())
            .when(ytTables).selectRows(any(), any(), Mockito.<Consumer<YTreeMapNode>>any());

        doAnswer(invocation -> {
            Consumer<YTreeMapNode> consumer = invocation.getArgument(2);
            HashMap<String, YTreeNode> data  = new HashMap<>();
            data.put(YtLogisticsPointService.LMS_ID_COLUMN, YTree.longNode(lmsId));
            consumer.accept(new YTreeMapNodeImpl(data, null));
            return true;
        })
            .when(backupYtTables).selectRows(any(), any(), Mockito.<Consumer<YTreeMapNode>>any());

        Assertions.assertEquals(
            lmsId,
            ytLogisticsPointService.getLogisticsPointId(PARTNER_ID, EXTERNAL_ID)
        );
        verify(backupYtTables).selectRows(
            eq(String.format(
                YtLogisticsPointService.SELECT_QUERY_TEMPLATE,
                YtLogisticsPointService.LMS_ID_COLUMN,
                OUTLET_PATH,
                PARTNER_ID,
                EXTERNAL_ID)
            ),
            any(),
            Mockito.<Consumer<YTreeMapNode>>any()
        );
    }
}
