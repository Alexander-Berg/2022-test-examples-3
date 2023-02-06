package ru.yandex.market.mbisfintegration.importer.yt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class YtMockHelper {

    private final YtClient ytClient;

    @SneakyThrows
    public void mockSelectRows(String queryPart, Map<String, YTreeNode>... rows) {
        List<YTreeMapNode> treeMapNodes = Stream.of(rows)
                .map(row -> new YTreeMapNodeImpl(row, Map.of()))
                .collect(Collectors.toList());
        UnversionedRowset unversionedRowset = Mockito.mock(UnversionedRowset.class);
        Mockito.when(unversionedRowset.getYTreeRows()).thenReturn(treeMapNodes);
        CompletableFuture<UnversionedRowset> completableFuture = Mockito.mock(CompletableFuture.class);
        Mockito.when(completableFuture.get(Mockito.anyLong(), Mockito.any())).thenReturn(unversionedRowset);
        Mockito.when(ytClient.selectRows(Mockito.contains(queryPart), Mockito.any())).thenReturn(completableFuture);
    }

    public void mockSelectSingleRow(String queryPart, Object... keyValuePairs) {
        Map<String, YTreeNode> row = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            row.put((String) keyValuePairs[i], (YTreeNode) keyValuePairs[i + 1]);
        }
        mockSelectRows(queryPart, row);
    }
}
