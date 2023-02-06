package ru.yandex.market.pers.tms.clustering;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;

/**
 * @author imelnikov
 */
public class SFProcessorTest {
    @Test
    public void findSimilar() {
        String text = "Суперская микроволновка. Особенно порадовал режим разморозки, не знаю у кого что да как, " +
            "но раньше ни одна микроволновка у меня не размораживала нормально, " +
            "а только как бы про варивала часть продукта, особенно мясо. " +
            "а эта просто за короткое время размораживает продукт и он нормальном сыром состоянии - " +
            "можно начинать готовить и сюрпризов не будет. " +
            "гриль тоже классный, люблю овощи гриль в маринаде делать, " +
            "и чистить потом , кстати, тоже не сложно!";
        Map<Integer, Grade> map = new HashMap<>();
        Grade g1 = new Grade(1, text, 155, 123456);
        Grade g2 = new Grade(2, text, 155, 123456);
        map.put(g1.getId(), g1);
        map.put(g2.getId(), g2);

        SFProcessor process = new SFProcessor(null, new ClusterConfig());
        process.prepareShingles(map.values());
        Map<SFProcessor.GradePair, Integer> pairs = process.mainProcess(map, new Clusterizer(null));
        assertFalse(pairs.isEmpty());
        for (SFProcessor.GradePair pair : pairs.keySet()) {
            System.out.println(pair.gradeId + " " + pair.pairGradeId);
        }
    }
}
