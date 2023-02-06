package ru.yandex.chemodan.app.docviewer.adapters.imagemagick;

import org.junit.Test;

import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class ImageAnnotatorTest {

    private static void assertContains(String expected, String string) {
        Assert.isTrue(string.contains(expected), "Expected '" + expected + "' in string: ", string);
    }

    @Test
    public void checkOffsetParams() {
        Assert.equals("+0", ImageAnnotator.offsetParam(0));
        Assert.equals("+10", ImageAnnotator.offsetParam(10));
        Assert.equals("-10", ImageAnnotator.offsetParam(-10));
    }

    @Test
    public void checkAnnotateScript() {
        ImageAnnotator annotator = new ImageAnnotator("convert");

        String cmds = annotator.createAnnotateScripts(
                    new File2("source.png"), new File2("target.jpg"),
                    Tuple2List.fromPairs("Hello\nWorld!", new AnnotateImageOptions("arial", 0, 40, "black", 12))
                ).mkString(" ");

        assertContains("-fill black", cmds);
        assertContains("-font arial", cmds);
        assertContains("-pointsize 12", cmds);
        assertContains("-gravity North", cmds);
        assertContains("-annotate +0+40", cmds);
        assertContains("Hello\\nWorld!", cmds);
    }

}
