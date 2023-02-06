package ru.yandex.calendar.logic.ics.iv5j.vcard;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class VcfUtilsTest {

    @Test
    public void escape() {
        Assert.A.equals("", VcfUtils.escape(""));
        Assert.A.equals("hello\\, world", VcfUtils.escape("hello, world"));
        Assert.A.equals("hello\\nworld", VcfUtils.escape("hello\nworld"));
    }

} //~
