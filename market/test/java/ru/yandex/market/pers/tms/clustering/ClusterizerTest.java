package ru.yandex.market.pers.tms.clustering;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author imelnikov
 */
public class ClusterizerTest {
    @Test
    public void testSimilar() {
        Map<Integer, Grade> map = new HashMap<>();
        Grade g1 = new Grade(1, "text", 155, 123456);
        Grade g2 = new Grade(2, "text", 155, 123456);
        map.put(g1.getId(), g1);
        map.put(g2.getId(), g2);
        Clusterizer clusterizer = new Clusterizer(null);
        clusterizer.cluster(g1, g2);
    }
}
