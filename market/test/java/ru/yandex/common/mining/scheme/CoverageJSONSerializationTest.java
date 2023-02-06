package ru.yandex.common.mining.scheme;

import org.junit.Test;
import ru.yandex.common.util.http.HttpGetLocation;
import ru.yandex.common.util.http.navigation.Action;

import static org.junit.Assert.assertEquals;
import static ru.yandex.common.mining.bd.miner.Miner.IDENTITY;
import static ru.yandex.common.mining.scheme.NodeClass.CATEGORY;
import static ru.yandex.common.mining.scheme.NodeClass.LINK;

/**
 * Created on 02.08.2007 19:39:38
 *
 * @author Eugene Kirpichov jkff@yandex-team.ru
 */
public class CoverageJSONSerializationTest {
    private Graph g = new Graph(null);

    @Test
    public void testNothing() throws Exception {
        Coverage coverage = new Coverage();
        assertEquals("{\"nodes\":[],\"rootNode\":null,\"classes\":[],\"schemes\":[],\"schemegraph\":[],\"discovery\":[]}",
            coverage.toJSON());

        Scheme sa = new MinerScheme(LINK, IDENTITY);

        Node a = add("a", null, null, LINK, null);
        Node b;
        coverage.discovery(a, sa, b = add("b", a, sa, CATEGORY, Action.NOP), CATEGORY);
        coverage.setRootNode(a);

        assertEquals(
            "{\"nodes\":[{\"url\":\"a\",\"name\":\"a\"}," +
                "{\"url\":\"b\",\"name\":\"b\"}]," +
                "\"rootNode\":0," +
                "\"classes\":[\"Link\",\"Category\"]," +
                "%SCHEME_GOES_HERE%," +
                "\"schemegraph\":[[]]," +
                "\"discovery\":[[0,0,1,1,true]]}",
            coverage.toJSON().replaceAll("\"schemes\":\\[\".*?\"\\]", "%SCHEME_GOES_HERE%"));

        Scheme sb = new MinerScheme(NodeClass.MODEL, IDENTITY);
        sa.addNext(sb);
        Node c;
        coverage.discovery(b, sb, c = add("c", b, sb, NodeClass.MODEL, Action.NOP), NodeClass.MODEL);

        assertEquals(
            "{\"nodes\":[{\"url\":\"a\",\"name\":\"a\"}," +
                "{\"url\":\"b\",\"name\":\"b\"}," +
                "{\"url\":\"c\",\"name\":\"c\"}]," +
                "\"rootNode\":0," +
                "\"classes\":[\"Link\",\"Category\",\"Model\"]," +
                "%SCHEMES_GO_HERE%," +
                "\"schemegraph\":[[1],[]]," +
                "\"discovery\":[[0,0,1,1,true],[1,1,2,2,true]]}",
            coverage.toJSON().replaceAll("\"schemes\":\\[\".*?\",\".*?\"\\]", "%SCHEMES_GO_HERE%"));
    }

    private Node add(String url, Node parent, Scheme discoverer, NodeClass clazz, Action action) {
        return g.maybeAdd(new HttpGetLocation(url, url), action, discoverer, parent, clazz);
    }
}
