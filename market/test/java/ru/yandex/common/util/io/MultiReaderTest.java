package ru.yandex.common.util.io;

import junit.framework.TestCase;
import ru.yandex.common.util.IOUtils;

import java.io.Reader;
import java.io.StringReader;

import static ru.yandex.common.util.collections.CollectionFactory.list;

/**
 * Created by pulser at Apr 15, 2010 6:38:40 PM
 */
public class MultiReaderTest extends TestCase {

    public void testCompositeReaders() throws Exception {
        final MultiReader r = new MultiReader(list((Reader)
                new StringReader("foo"),
                new StringReader("bar"),
                new StringReader("boo"))
        );
        assertEquals("foobarboo", IOUtils.readLine(r));
    }

}
