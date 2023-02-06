package ru.yandex.ir.dao.jobs;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import org.junit.Test;
import ru.yandex.ir.modelsclusterizer.utils.CategoriesSerializer;

import static org.junit.Assert.assertEquals;

/**
 * todo описать предназначение.
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class CategoriesSerializerTest {
    @Test
    public void testSerializeCategories() throws Exception {
        checkSerialization("");
        checkSerialization("1", 1);
        checkSerialization("1\t2", 1, 2);
        checkSerialization("1\t2\t3\t4", 1, 2, 3, 4);
    }

    private void checkSerialization(String expected, int... catIds) {
        final String serialization = CategoriesSerializer.serializeCategories(new IntArrayList(catIds));
        assertEquals(expected, serialization);
    }

    @Test
    public void testDeserializeCategories() throws Exception {
        checkDeserialization("");
        checkDeserialization("1", 1);
        checkDeserialization("1\t2", 1, 2);
        checkDeserialization("1\t2\t3\t4", 1, 2, 3, 4);

        checkDeserialization("\t");
        checkDeserialization("\t1", 1);
        checkDeserialization("\t1\t2", 1, 2);
        checkDeserialization("\t1\t2\t3\t4", 1, 2, 3, 4);

        checkDeserialization("\t\t");
        checkDeserialization("\t1\t", 1);
        checkDeserialization("\t1\t2\t", 1, 2);
        checkDeserialization("\t1\t2\t3\t\t4\t", 1, 2, 3, 4);
    }

    private void checkDeserialization(String serialization, int... expectedCatIds) {
        final IntCollection actialCatIds = CategoriesSerializer.deserializeCategories(serialization);
        final IntArrayList expected = new IntArrayList(expectedCatIds);
        assertEquals(expected, actialCatIds);
    }
}
