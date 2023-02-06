package ru.yandex.crypta.graph2.model.matching.component;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.graph.engine.proto.TGraph;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class ComponentTest {

    @Test
    public void testComponentConverter() {

        String cryptaId = "123";
        Vertex vertex1 = new Vertex("1234567891585685152", EIdType.YANDEXUID);
        Vertex vertex2 = new Vertex("00000000-0000-0000-0000-111111111100", EIdType.IDFA);
        Vertex vertex3 = new Vertex("aba@yandex.ru", EIdType.EMAIL);

        Edge edge1 = new Edge(
                vertex1.getId(),
                vertex1.getIdType(),
                vertex2.getId(),
                vertex2.getIdType(),
                ESourceType.APP_METRICA,
                ELogSourceType.METRIKA_MOBILE_LOG,
                Cf.list("2020-03-20", "2020-03-21")
        );
        Edge edge2 = new Edge(
                vertex1.getId(),
                vertex1.getIdType(),
                vertex3.getId(),
                vertex3.getIdType(),
                ESourceType.WEBVISOR,
                ELogSourceType.WEBVISOR_LOG,
                Cf.list()
        );
        Component component = new Component(cryptaId);
        component.addAllVertices(Cf.list(vertex1, vertex2, vertex3));
        component.addInnerEdges(Cf.list(edge1, edge2));

        TGraph graph = component.computeTGraph();

        assertEquals(graph.getId(), 123);
        assertEquals(graph.getVerticesCount(), 3);
        assertEquals(graph.getEdgesCount(), 2);
    }
}
