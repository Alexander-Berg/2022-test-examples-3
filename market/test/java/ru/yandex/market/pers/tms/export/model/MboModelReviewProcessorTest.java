package ru.yandex.market.pers.tms.export.model;

import java.io.File;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.pers.tms.MockedPersTmsTest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 10.06.16
 */
public class MboModelReviewProcessorTest extends MockedPersTmsTest {
    @Test
    public void testGetCounts() throws Exception {
        MboModelReviewProcessor processor = new MboModelReviewProcessor(
            new File(this.getClass().getClassLoader()
                .getResource("model_params_model_review.xml").toURI()));
        List<ModelCount> countList = processor.getCounts();
        assertNotNull(countList);
        assertTrue(countList.size() > 0);
        for (ModelCount modelCount : countList) {
            assertTrue(modelCount.count > 0);
        }
    }
}
