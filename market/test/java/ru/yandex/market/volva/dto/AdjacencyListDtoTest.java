package ru.yandex.market.volva.dto;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;
import ru.yandex.market.volva.serializer.VolvaJsonUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class AdjacencyListDtoTest {

    @Test
    public void serializationTest() throws Exception {
        Node n1 = new Node("puid-1", IdType.PUID);
        Node n2 = new Node("uuid-2", IdType.UUID);
        Node n3 = new Node("puid-3", IdType.PUID);
        Node n4 = new Node("yandexuid-4", IdType.YANDEXUID);
        AdjacencyListDto adjacencyListDto = new AdjacencyListDto(
                Map.of(
                        n1, Set.of(n2, n3),
                        n2, Set.of(n1, n4),
                        n3, Set.of(n1),
                        n4, Set.of(n2)));
        String json = VolvaJsonUtils.toJson(adjacencyListDto);
        AdjacencyListDto newAld = VolvaJsonUtils.OBJECT_MAPPER.readValue(json, AdjacencyListDto.class);
        assertThat(newAld).isEqualTo(adjacencyListDto);
    }
}
