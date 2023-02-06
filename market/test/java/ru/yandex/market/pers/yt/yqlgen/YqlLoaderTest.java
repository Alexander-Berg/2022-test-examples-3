package ru.yandex.market.pers.yt.yqlgen;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Ilya Kislitsyn / ilyakis@ / 15.06.2021
 */
public class YqlLoaderTest {

    @Test
    public void testIncludes() {
        String yql = YqlLoader.readYql("/yql/source.sql");

        Assertions.assertEquals("#INCLUDE(/yql/inc1.sql)\n" +
            "original text\n" +
            "#INCLUDE(/yql/inc2.sql)\n" +
            "#INCLUDE(/yql/inc1.sql)\n", yql);

        String fullYql = YqlLoader.handleImports(yql);

        Assertions.assertEquals("initialization\n\noriginal text\n\nsome more text to $test includes\n" +
            "and some more, more\n\n\n", fullYql);
    }

    @Test
    public void testIncludesPyCode() {
        String yql = YqlLoader.readYql("/yql/script_user.sql");
        String fullYql = YqlLoader.handleImports(yql);

        Assertions.assertEquals("$fun = Python3::fun(Callable<(List<String>)->List<String>>, @@\n" +
            "def fun(arg):\n" +
            "    return function(arg)\\\n" +
            "    .newline_method()\n" +
            "\n" +
            "@@);", fullYql);
    }

}
