package ru.yandex.crypta.graph2.model.matching.component.similarity;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.soup.props.VertexPropertiesCollector;
import ru.yandex.crypta.graph2.model.soup.props.info.ExactSocdem;
import ru.yandex.crypta.graph2.model.soup.props.info.ExactSocdem.Gender;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ComponentsExactSocdemSimilarityTest {

    private GraphInfo socdemsVps(MapF<Vertex, ListF<ExactSocdem>> vertexSocdems) {
        return new GraphInfo(Cf.map(), Cf.map(), new VertexPropertiesCollector(
                Cf.map(), Cf.map(), Cf.map(), vertexSocdems, Cf.map()
        ), Cf.list());
    }

    @Test
    public void testSameSocdem() {

        Vertex y1 = new Vertex("y1", EIdType.YANDEXUID);
        Vertex y2 = new Vertex("y2", EIdType.YANDEXUID);

        GraphInfo sameSocdemsVps = socdemsVps(Cf.map(
                y1, Cf.list(new ExactSocdem(Option.of(1984), Option.of(Gender.MALE), Option.empty())),
                y2, Cf.list(new ExactSocdem(Option.of(1985), Option.of(Gender.MALE), Option.empty()))
        ));

        ComponentsSimilarityStrategy similarity = new ComponentsExactSocdemSimilarityStrategy();
        boolean sameSocdem = similarity.isSimilar(new Component(y1), new Component(y2), sameSocdemsVps)
                .first().isSimilar();
        assertTrue(sameSocdem);

    }

    @Test
    public void testAgeDifferent() {

        Vertex y1 = new Vertex("y1", EIdType.YANDEXUID);
        Vertex y2 = new Vertex("y2", EIdType.YANDEXUID);

        GraphInfo ageIsDifferent = socdemsVps(Cf.map(
                y1, Cf.list(new ExactSocdem(Option.of(1982), Option.of(Gender.MALE), Option.empty())),
                y2, Cf.list(new ExactSocdem(Option.of(1985), Option.of(Gender.MALE), Option.empty()))
        ));

        ComponentsSimilarityStrategy similarity = new ComponentsExactSocdemSimilarityStrategy();
        boolean sameSocdem = similarity.isSimilar(new Component(y1), new Component(y2), ageIsDifferent)
                .first().isSimilar();
        assertFalse(sameSocdem);

    }

    @Test
    public void testAgeDifferentDoGender() {

        Vertex y1 = new Vertex("y1", EIdType.YANDEXUID);
        Vertex y2 = new Vertex("y2", EIdType.YANDEXUID);

        GraphInfo ageIsDifferentNoAge = socdemsVps(Cf.map(
                y1, Cf.list(new ExactSocdem(Option.of(1982), Option.empty(), Option.empty())),
                y2, Cf.list(new ExactSocdem(Option.of(1985), Option.empty(), Option.empty()))
        ));

        ComponentsSimilarityStrategy similarity = new ComponentsExactSocdemSimilarityStrategy();
        boolean sameSocdem = similarity.isSimilar(new Component(y1), new Component(y2), ageIsDifferentNoAge)
                .first().isSimilar();
        assertFalse(sameSocdem);

    }

    @Test
    public void testGenderDifferentNoAge() {

        Vertex y1 = new Vertex("y1", EIdType.YANDEXUID);
        Vertex y2 = new Vertex("y2", EIdType.YANDEXUID);

        GraphInfo ageIsDifferentNoAge = socdemsVps(Cf.map(
                y1, Cf.list(new ExactSocdem(Option.empty(), Option.of(Gender.FEMALE), Option.empty())),
                y2, Cf.list(new ExactSocdem(Option.empty(), Option.of(Gender.FEMALE), Option.empty()))
        ));

        ComponentsSimilarityStrategy similarity = new ComponentsExactSocdemSimilarityStrategy();
        boolean sameSocdem = similarity.isSimilar(new Component(y1), new Component(y2), ageIsDifferentNoAge)
                .first().isSimilar();
        assertTrue(sameSocdem);

    }

}
