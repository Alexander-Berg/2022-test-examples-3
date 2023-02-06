package ru.yandex.chemodan.app.docviewer.adapters.imagemagick;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class TextSplitterTest {

    @Test
    public void splitText() {
        String sourceText = "Лев Николаевич Толстой\n\nВойна и мир";

        Assert.equals(
                Cf.list("Лев", "Николаевич", "Толстой", "", "Война", "и мир"),
                TextSplitter.splitLongLines(sourceText, new TextSplitOptions(5, 6)));
        Assert.equals(
                Cf.list("Лев", "Николаевич", "Толстой", "", "Война"),
                TextSplitter.splitLongLines(sourceText, new TextSplitOptions(5, 5)));
        Assert.equals(
                Cf.list("Лев", "Николаевич", "Толстой", "", "Война и мир"),
                TextSplitter.splitLongLines(sourceText, new TextSplitOptions(11, 5)));
        Assert.equals(
                Cf.list("Лев Николаевич", "Толстой", "", "Война и мир"),
                TextSplitter.splitLongLines(sourceText, new TextSplitOptions(14, 5)));
        Assert.equals(
                Cf.list("Лев Николаевич Толстой", "", "Война и мир"),
                TextSplitter.splitLongLines(sourceText, new TextSplitOptions(99, 5)));
    }

    @Test
    public void splitLongText() {
        String sourceText = "Лев Николаевич Толстой Война и мир и долгий, долгий текст на несколько миллионов строк!";

        Assert.equals(
                Cf.list("Лев Николаевич Толстой", "Война и мир и долгий,", "долгий текст на", "несколько миллионов"),
                TextSplitter.splitLongLines(sourceText, TextSplitOptions.AUTHOR));
    }
}
