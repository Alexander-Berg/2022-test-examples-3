package ru.yandex.crypta.graph2.model.matching.component.similarity;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.soup.props.VertexPropertiesCollector;
import ru.yandex.crypta.graph2.model.soup.props.info.ProbSocdem;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ComponentsProbSocdemSimilarityTest {

    private ProbSocdem fem1 = new ProbSocdem(Cf.map(
            "f", 0.8,
            "m", 0.2
    ), Cf.map());
    private ProbSocdem fem2 = new ProbSocdem(Cf.map(
            "f", 0.75,
            "m", 0.25
    ), Cf.map());
    private ProbSocdem boloto = new ProbSocdem(Cf.map(
            "f", 0.6,
            "m", 0.4
    ), Cf.map());
    private ProbSocdem male1 = new ProbSocdem(Cf.map(
            "f", 0.1,
            "m", 0.9
    ), Cf.map());

    private GraphInfo socdemsVps(MapF<Vertex, ProbSocdem> vertexSocdems) {
        return new GraphInfo(Cf.map(), Cf.map(), new VertexPropertiesCollector(
                Cf.map(), Cf.map(), Cf.map(), Cf.map(), vertexSocdems
        ), Cf.list());
    }

    @Test
    public void testSimilarGender() {

        Vertex v1 = new Vertex("y1", EIdType.YANDEXUID);
        Vertex v2 = new Vertex("y2", EIdType.YANDEXUID);
        Vertex v3 = new Vertex("y3", EIdType.YANDEXUID);

        GraphInfo vps = socdemsVps(Cf.map(
                v1, fem1,
                v2, boloto,
                v3, fem2
        ));

        ComponentsProbSocdemSimilarityStrategy similarity = new ComponentsProbSocdemSimilarityStrategy();

        Component female1 = new Component(Cf.set(v1, v2));
        Component female2 = new Component(Cf.set(v3));

        assertTrue(similarity.isSimilar(female1, female2, vps).first().isSimilar());
    }

    @Test
    public void testDifferentSocdem() {

        Vertex v1 = new Vertex("y1", EIdType.YANDEXUID);
        Vertex v2 = new Vertex("y2", EIdType.YANDEXUID);
        Vertex v3 = new Vertex("y3", EIdType.YANDEXUID);

        GraphInfo vps = socdemsVps(Cf.map(
                v1, fem1,
                v2, fem2,
                v3, male1
        ));

        ComponentsProbSocdemSimilarityStrategy similarity = new ComponentsProbSocdemSimilarityStrategy();

        Component female1 = new Component(Cf.set(v1, v2));
        Component female2 = new Component(Cf.set(v3));

        assertFalse(similarity.isSimilar(female1, female2, vps).first().isSimilar());
    }

    @Test
    public void testUnsure() {

        Vertex v1 = new Vertex("y1", EIdType.YANDEXUID);
        Vertex v2 = new Vertex("y2", EIdType.YANDEXUID);
        Vertex v3 = new Vertex("y3", EIdType.YANDEXUID);

        GraphInfo vps = socdemsVps(Cf.map(
                v1, fem1,
                v2, boloto,
                v3, boloto
        ));

        ComponentsProbSocdemSimilarityStrategy similarity = new ComponentsProbSocdemSimilarityStrategy();

        Component female1 = new Component(Cf.set(v1, v2));
        Component female2 = new Component(Cf.set(v3));

        assertTrue(similarity.isSimilar(female1, female2, vps).isEmpty()); // unwknown
    }

}
