package ru.yandex.market.delivery.transport_manager.yt;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.yt.DropoffRelationDto;
import ru.yandex.market.delivery.transport_manager.domain.yt.LogisticsPointChangeDto;
import ru.yandex.market.delivery.transport_manager.service.yt.YtCommonReader;
import ru.yandex.market.delivery.transport_manager.service.yt.YtReader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YtCommonReaderTest extends AbstractContextualTest {

    private static final String TABLE = "//path";

    @Autowired
    private YtCommonReader reader;

    @Test
    void testDropoffRelations() {
        List<YTreeMapNode> ytRelations = List.of(
            YTree.mapBuilder()
                .key("scPointId").value(1)
                .key("dropoffPointId").value(11)
                .key("deliveryId").value(3)
                .key("scPartnerId").value(7)
                .key("dropoffPartnerId").value(9)
                .buildMap()
        );
        mockYt(ytRelations);
        Set<DropoffRelationDto> relations = reader.getTableData(DropoffRelationDto.class, TABLE);
        softly.assertThat(relations).isEqualTo(Set.of(
            new DropoffRelationDto(1L, 11L, 3L, 7L, 9L)
        ));
    }

    @Test
    void testPointChanges() {
        List<YTreeMapNode> ytRelations = List.of(
            YTree.mapBuilder()
                .key("old_logistics_point_id").value(1)
                .key("new_logistics_point_id").value(2)
                .key("created").value("2021-12-12T01:00:00Z")
                .buildMap()
        );
        mockYt(ytRelations);
        Set<LogisticsPointChangeDto> changes = reader.getTableData(LogisticsPointChangeDto.class, TABLE);
        softly.assertThat(changes).isEqualTo(Set.of(
            new LogisticsPointChangeDto(1L, 2L, Instant.parse("2021-12-12T01:00:00Z")
            )));
    }

    private void mockYt(List<YTreeMapNode> nodes) {
        Yt ytMock = mock(Yt.class);
        YtTables tablesMock = mock(YtTables.class);
        Cypress cypressMock = mock(Cypress.class);

        reader = new YtCommonReader<>(new YtReader(ytMock, ytMock));

        when(ytMock.tables()).thenReturn(tablesMock);
        when(ytMock.cypress()).thenReturn(cypressMock);
        doAnswer((invocation) -> {
            Consumer<YTreeMapNode> argument = invocation.getArgument(2);
            nodes.forEach(argument::accept);
            return null;
        }).when(tablesMock).read(any(), any(), any(Consumer.class));
    }

}
