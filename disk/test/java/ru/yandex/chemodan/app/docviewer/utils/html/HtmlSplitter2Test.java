package ru.yandex.chemodan.app.docviewer.utils.html;

import java.util.List;

import org.dom4j.Document;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom4j.Dom4jUtils;

/**
 * @author vlsergey
 * @author akirakozov
 *
 */
public class HtmlSplitter2Test extends DocviewerSpringTestBase {

    @Autowired
    private HtmlSplitter htmlSplitter;

    @Test
    public void testFlatPs() {
        htmlSplitter.setMaxSizePart(DataSize.fromBytes(4000));
        htmlSplitter.setMaxSizeSingle(DataSize.fromBytes(4000));

        Document source = Dom4jUtils.read(new UrlInputStreamSource(TestResources.HTML_FLAT));
        List<byte[]> result = htmlSplitter.splitHtml(source, false, Option.empty(), false);

        Assert.A.equals(Integer.valueOf(3), Integer.valueOf(result.size()));

        Assert.assertContains(new String(result.get(0)), "sentence number 1");
        Assert.assertContains(new String(result.get(0)), "sentence number 2");
        Assert.assertContains(new String(result.get(0)), "sentence number 3");
        Assert.assertContains(new String(result.get(0)), "sentence number 4");

        Assert.assertContains(new String(result.get(1)), "sentence number 5");
        Assert.assertContains(new String(result.get(1)), "sentence number 6");
        Assert.assertContains(new String(result.get(1)), "sentence number 7");
        Assert.assertContains(new String(result.get(1)), "sentence number 8");

        Assert.assertContains(new String(result.get(2)), "sentence number 9");
        Assert.assertContains(new String(result.get(2)), "sentence number 10");
    }

    @Test
    public void testTable() {
        htmlSplitter.setMaxSizePart(DataSize.fromBytes(5000));
        htmlSplitter.setMaxSizeSingle(DataSize.fromBytes(5000));

        Document source = Dom4jUtils.read(new UrlInputStreamSource(TestResources.HTML_TABLE));
        List<byte[]> result = htmlSplitter.splitHtml(source, false, Option.empty(), false);

        Assert.A.equals(Integer.valueOf(3), Integer.valueOf(result.size()));

        Assert.assertContains(new String(result.get(0)), "sentence number 1");
        Assert.assertContains(new String(result.get(0)), "sentence number 2");
        Assert.assertContains(new String(result.get(0)), "sentence number 3");
        Assert.assertContains(new String(result.get(0)), "sentence number 4");
        Assert.assertContains(new String(result.get(0)), "sentence number 5");
        Assert.assertContains(new String(result.get(0)), "sentence number 9");

        Assert.assertContains(new String(result.get(1)), "sentence number 3");
        Assert.assertContains(new String(result.get(1)), "sentence number 6");
        Assert.assertContains(new String(result.get(1)), "sentence number 7");
        Assert.assertContains(new String(result.get(1)), "sentence number 8");
        Assert.assertContains(new String(result.get(1)), "sentence number 9");

        Assert.assertContains(new String(result.get(2)), "sentence number 10");
    }
}
