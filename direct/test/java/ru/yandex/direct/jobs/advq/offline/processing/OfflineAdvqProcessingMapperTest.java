package ru.yandex.direct.jobs.advq.offline.processing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.operations.Yield;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.jobs.advq.offline.processing.OfflineAdvqTestUtils.resultRowNoPid;
import static ru.yandex.direct.ytwrapper.model.YtTableRow.TABLE_INDEX_ATTR_NAME;

class OfflineAdvqProcessingMapperTest {
    @Mock
    private Yield<YTreeMapNode> yield;

    @Captor
    private ArgumentCaptor<YTreeMapNode> captor;

    private OfflineAdvqProcessingMapper mapper;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);

        mapper = new OfflineAdvqProcessingMapper();
    }

    @Test
    void testPidIsSet() {
        long pid = 123;

        YTreeMapNode node = resultRowNoPid(pid, 321L, "123", "keyword", 9876L).getData();
        node.putAttribute(TABLE_INDEX_ATTR_NAME, YTree.integerNode(1L));
        mapper.map(node, yield, null);

        verify(yield).yield(eq(0), captor.capture());

        assertThat(OfflineAdvqProcessingTemporaryTableRow.PID.extractValue(captor.getValue()))
                .isEqualTo(pid);
    }
}
