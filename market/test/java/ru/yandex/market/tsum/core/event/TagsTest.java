package ru.yandex.market.tsum.core.event;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 29/09/16
 */
public class TagsTest {

    private List<Tag> normalized;
    private List<Tag> denormalized;

    @Before
    public void setUp() throws Exception {
        normalized = Tags.parse(Arrays.asList("a:b:c", "a:b:d", "a:c"));
        denormalized = Tags.parse(Arrays.asList("a", "a:b", "a:b:c", "a:b:d", "a:c"));
    }

    @Test
    public void denormalize() throws Exception {
        Assert.assertEquals(denormalized, Tags.denormalize(normalized));
    }

    @Test
    public void normalize() throws Exception {
        Assert.assertEquals(normalized, Tags.normalize(denormalized));
    }

}