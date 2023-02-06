package ru.yandex.market.pers.yt;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.CloseableIterator;
import ru.yandex.inside.yt.kosher.tables.TableReaderOptions;
import ru.yandex.inside.yt.kosher.tables.YtTables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.03.2021
 */
public class YtClientTest {
    @Mock
    private final Cypress cypress = mock(Cypress.class);
    private final YtTables tables = mock(YtTables.class);

    private final Yt yt = buildYtMock(cypress, tables);
    private final YtClient client = new YtClient(YtClusterType.HAHN, yt);

    private static Yt buildYtMock(Cypress cypress, YtTables tables) {
        Yt mock = mock(Yt.class);
        when(mock.cypress()).thenReturn(cypress);
        when(mock.tables()).thenReturn(tables);
        return mock;
    }

    @BeforeEach
    public void init() {
        reset(cypress, tables);
    }

    @Test
    public void testBatchConsume() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode array = mapper.readTree("[{\"field\":\"value\"}, {\"field\":\"value2\"}, {\"field\":\"test\"}]");

        List<Object> es = List.of(array.get(0), array.get(1), array.get(2));
        when(tables.read(any(), anyBoolean(), any(), any(), any(TableReaderOptions.class))).thenReturn(
             CloseableIterator.wrap(es)
        );

        List<String> result = new ArrayList<>();
        client.consumeTableBatched(YPath.cypressRoot(), 2, node->node.get("field").textValue(), result::addAll);

        assertEquals(List.of("value", "value2", "test"), result);
    }
}
