package ru.yandex.crypta.graph2.model.matching.component.score.extractors;

import org.junit.Test;

import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class EmailsLcsExtractorTest {
    private GraphInfo graphInfo = mock(GraphInfo.class);

    @Test
    public void testSingleEmail() throws Exception {
        EmailsLcsExtractor extractor = new EmailsLcsExtractor();

        Component cp = new Component(new Vertex("l1@ya.ru", EIdType.EMAIL));

        assertEquals(0, (int) extractor.apply(cp, graphInfo));
    }

    @Test
    public void testSingleSyntheticEmail() throws Exception {
        EmailsLcsExtractor extractor = new EmailsLcsExtractor();

        Component cp = new Component(new Vertex("phne-login@ya.ru", EIdType.EMAIL));

        assertEquals(0, (int) extractor.apply(cp, graphInfo));
    }

    @Test
    public void testComplexCase() throws Exception {
        EmailsLcsExtractor extractor = new EmailsLcsExtractor();

        Component cp = new Component();
        cp.addVertex(new Vertex("phne-login@skd.fi", EIdType.EMAIL));
        cp.addVertex(new Vertex("rkl2@mail.net", EIdType.EMAIL));
        cp.addVertex(new Vertex("gwkl3@mail.net", EIdType.EMAIL));
        cp.addVertex(new Vertex("qkl36@mail.net", EIdType.EMAIL));

        assertEquals(2, (int) extractor.apply(cp, graphInfo));
    }
}
