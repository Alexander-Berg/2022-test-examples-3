package ru.yandex.calendar.logic.ics;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.misc.lang.CharsetUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class IcsUtf8RefolderTest {

    @Test
    public void empty() {
        Assert.A.hasSize(0, IcsUtf8Refolder.refold(new byte[0]));
    }

    @Test
    public void simple() {
        byte[] input = { 'a', 'b', '\n', 'd', '\r', '\n', '\n', 'e', ':', 'f' };
        Assert.A.isTrue(Arrays.equals(input, IcsUtf8Refolder.refold(input)));
    }

    @Test
    public void yuSplit() {
        byte[] yu = "ю".getBytes(CharsetUtils.UTF8_CHARSET);
        byte[] ya = "я".getBytes(CharsetUtils.UTF8_CHARSET);
        byte[] inp = { 'S', ':', yu[0], '\r', '\n', ' ', yu[1], ya[0], ya[1], '\n' };
        byte[] out = { 'S', ':', yu[0], yu[1], '\r', '\n', ' ', ya[0], ya[1], '\n' };
        Assert.A.isTrue(Arrays.equals(out, IcsUtf8Refolder.refold(inp)));
    }

    @Test
    public void yuNoSplit() {
        byte[] yu = "ю".getBytes(CharsetUtils.UTF8_CHARSET);
        byte[] ya = "я".getBytes(CharsetUtils.UTF8_CHARSET);
        byte[] inp = { 'S', ':', yu[0], yu[1], '\r', '\n', ' ', ya[0], ya[1], '\n' };
        Assert.A.isTrue(Arrays.equals(inp, IcsUtf8Refolder.refold(inp)));
    }


} //~
