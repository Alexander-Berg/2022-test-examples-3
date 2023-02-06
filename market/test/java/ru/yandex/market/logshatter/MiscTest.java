package ru.yandex.market.logshatter;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 01/04/15
 */
public class MiscTest {
    @Test
    public void testGlob() throws Exception {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:msh???{,.*}");
        Assert.assertTrue(pathMatcher.matches(Paths.get("msh52e")));
        Assert.assertTrue(pathMatcher.matches(Paths.get("msh52g.yandex.ru")));
        Assert.assertFalse(pathMatcher.matches(Paths.get("msh-off09e")));
        Assert.assertFalse(pathMatcher.matches(Paths.get("msh-off01ft.yandex.ru")));
        Assert.assertFalse(pathMatcher.matches(Paths.get("msh-off01.yandex.ru")));
        Assert.assertFalse(pathMatcher.matches(Paths.get("msh-off01.yandex.ru")));
        Assert.assertFalse(pathMatcher.matches(Paths.get("msh-int22g.yandex.ru")));
        Assert.assertFalse(pathMatcher.matches(Paths.get("msh-int22g")));


    }
}
