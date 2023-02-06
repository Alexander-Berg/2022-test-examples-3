package ru.yandex.common.util;

import static junit.framework.Assert.assertEquals;
import org.junit.Test;

/**
 * Created on 25.05.2007 16:20:23
 *
 * @author Eugene Kirpichov jkff@yandex-team.ru
 */
public class CanonicalCharsetTest {
    @Test
    public void test() {
        assertEquals("windows-1251", CanonicalCharset.forName("win-1251").name());
    }
}
