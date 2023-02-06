package ru.yandex.market.api.doc.impl;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by vivg on 03.04.17.
 */
public class RenderUtilsTest {


    @Test
    public void test() {

        Assert.assertEquals("<p>a</p>", RenderUtils.safeDita("a"));
        Assert.assertEquals("<p>a b</p>", RenderUtils.safeDita("a b"));
        Assert.assertEquals("<p><b>c</b></p>", RenderUtils.safeDita("<b>c</b>"));
        Assert.assertEquals("<p>a<b>c</b></p>", RenderUtils.safeDita("a<b>c</b>"));

        Assert.assertEquals("<p>a</p><p>b</p>", RenderUtils.safeDita("a<p>b"));
        Assert.assertEquals("<p>a</p><p>b</p>", RenderUtils.safeDita("<p>a</p>b"));
        Assert.assertEquals("<p>1</p><p>2</p><p>3</p>", RenderUtils.safeDita("<p>1</p>2<p>3"));

        Assert.assertEquals("<p>a</p><p>b</p>", RenderUtils.safeDita("<p>a<p>b"));
        Assert.assertEquals("<p>1<b>2</b></p><p>3</p>", RenderUtils.safeDita("<p>1<b>2</b><p>3"));
        Assert.assertEquals("<p>1<b>2<p>3</p></b></p>", RenderUtils.safeDita("<p>1<b>2<p>3</p></b>"));

        Assert.assertEquals("<p><a>1</a></p><p>3</p>", RenderUtils.safeDita("<a>1</a><p>3"));
        Assert.assertEquals("<p><a>1</a></p><p>3</p>", RenderUtils.safeDita("<a>1</a><p>3</p>"));

        Assert.assertEquals("<p>1<b>2<p>3</p>4</b></p>", RenderUtils.safeDita("<p>1<b>2<p>3</p>4</b>"));
        Assert.assertEquals("<p>1</p><p><b>2</b></p>", RenderUtils.safeDita("<p>1</p><b>2</b>"));
    }
}
