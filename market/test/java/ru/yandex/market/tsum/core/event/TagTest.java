package ru.yandex.market.tsum.core.event;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 25/09/16
 */
public class TagTest {
    @Test
    public void isParentOf() throws Exception {
        Tag parent = Tags.parse("a:b");
        Tag child = Tags.parse("a:b:c");
        Tag notChild = Tags.parse("a:c:e");
        Assert.assertTrue(parent.isParentOf(child));
        Assert.assertFalse(parent.isParentOf(notChild));
        Assert.assertFalse(parent.isParentOf(parent));

    }

}