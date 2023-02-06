package ru.yandex.market.logistics.lom.service.yt;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.ytree.object.NullSerializationStrategy;
import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.service.yt.dto.YtCluster;
import ru.yandex.market.logistics.lom.utils.YtUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.YtUtils.getIterator;

class YtServiceTest extends AbstractContextualTest {
    private static final String TABLE_PATH = "//valid/path";
    private static final Long LOWER_ROW_INDEX = 0L;
    private static final Long UPPER_ROW_INDEX = 1L;

    @Autowired
    private YtService ytService;
    @Autowired
    private Yt hahnYt;
    @Autowired
    private Yt arnoldYt;
    @Autowired
    private YtTables ytTables;

    @BeforeEach
    void setup() {
        when(ytTables.read(any(), any())).thenReturn(getIterator(
            Set.of(YtUtils.buildMapNode(Map.of("column", "value")))
        ));
    }

    @Test
    @DisplayName("Кластер hahn работает")
    void hahnIsActive() {
        when(hahnYt.tables()).thenReturn(ytTables);
        softly.assertThat(callYtService()).containsExactly(new YtRecord("value"));
    }

    @Test
    @DisplayName("Кластер hahn не работает, arnold - работает")
    void hahnIsInactiveArnoldIsActive() {
        when(hahnYt.tables()).thenThrow(new RuntimeException("Error connecting to hahn"));
        when(arnoldYt.tables()).thenReturn(ytTables);
        softly.assertThat(callYtService()).containsExactly(new YtRecord("value"));
    }

    @Test
    @DisplayName("Все кластеры не работают")
    void allClustersAreInactive() {
        when(hahnYt.tables()).thenThrow(new RuntimeException("Error connecting to hahn"));
        when(arnoldYt.tables()).thenThrow(new RuntimeException("Error connecting to arnold"));
        softly.assertThatThrownBy(this::callYtService)
            .isInstanceOf(RuntimeException.class)
            .hasMessageContainingAll(
                "ARNOLD=java.lang.RuntimeException: Error connecting to arnold",
                "HAHN=java.lang.RuntimeException: Error connecting to hahn"
            );
    }

    @Nonnull
    private List<YtRecord> callYtService() {
        return ytService.readTableFromRowToRow(
            List.of(YtCluster.values()),
            TABLE_PATH,
            YtRecord.class,
            LOWER_ROW_INDEX,
            UPPER_ROW_INDEX,
            Set.of("column")
        );
    }

    @Value
    @Builder
    @YTreeObject(nullSerializationStrategy = NullSerializationStrategy.SERIALIZE_NULL_TO_EMPTY)
    private static class YtRecord {
        String column;
    }
}
