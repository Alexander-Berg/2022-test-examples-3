package ru.yandex.common.util.sitegraph;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author imelnikov
 */
public class SiteGraphReaderTest {

    @Test
    public void readGraph() throws Exception {
        SiteGraphReader reader = new SiteGraphReader();
        File file = new File(getClass().getResource("/graph/dump.txt").getFile());
        SiteGraph graph = reader.readGraph(file);

        assertNotNull(graph.getNode("/blah/blah.html"));
    }
}
