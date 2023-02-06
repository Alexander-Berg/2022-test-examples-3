package ru.yandex.market.pers.yt;

import java.util.function.Consumer;

import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.YPath;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 19.01.2021
 */
public class YtClientMocks {

    public static void baseMock(YtClient client) {
        baseMock(YtClusterType.HAHN, client);
    }

    public static void baseMock(YtClusterType clusterType, YtClient client) {
        when(client.exists(any())).thenReturn(false);
        doNothing().when(client).createTable(any(GUID.class), any());
        doNothing().when(client).createTable(any(YPath.class), any());
        doNothing().when(client).append(any(), any(), anyList());
        doAnswer(invocation -> {
            Consumer<GUID> consumer = (Consumer<GUID>) invocation.getArguments()[0];
            consumer.accept(null);
            return null;
        }).when(client).doInTransaction(any());
        doNothing().when(client).removeLink(any(), any());
        doNothing().when(client).createLink(any(), any(), any());
        when(client.getClusterType()).thenReturn(clusterType);
    }
}
