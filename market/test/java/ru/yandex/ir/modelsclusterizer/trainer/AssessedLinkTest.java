package ru.yandex.ir.modelsclusterizer.trainer;

import org.junit.Test;
import ru.yandex.ir.modelsclusterizer.train.AssessedLink;

import static org.junit.Assert.assertEquals;

/**
 * @author mkrasnoperov
 */
public class AssessedLinkTest {
    @Test
    public void getCategoryId() throws Exception {
        // Чтобы хотя бы один тест был в пакете
        assertEquals(new AssessedLink(3, "a", "b", true).getCategoryId(), 3);
    }

}