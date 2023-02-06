package ru.yandex.calendar.frontend.ews.xml;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;

import ru.yandex.bolts.function.Function2;
import ru.yandex.misc.io.IoUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class NullReplacingReaderTest {

    @Test
    public void testReplaced() {
        Function2<String, Integer, String> replace = (input, bufferSize) -> {
            Reader reader = new NullReplacingReader(new StringReader(input));
            StringWriter writer = new StringWriter();

            char[] chars = new char[bufferSize];
            try {
                for (;;) {
                    int count = reader.read(chars);
                    if (count < 0) break;

                    writer.write(chars, 0, count);
                }
                return writer.toString();

            } catch (IOException e) {
                throw IoUtils.translate(e);
            }
        };
        String input = "trule:Microsoft/Registry/UTC+03:00&#x0;&#x0;&#x0;&#x0;&#x0;&#x0;/1-Standard&#x";
        String output = input.replace("&#x0;", "?");

        for (int size = 1; size < input.length() + 1; ++size) {
            Assert.equals(output, replace.apply(input, size), "Buffer size " + size);
        }
    }

    @Test
    public void weient() throws IOException {
        String input = "<t:WebClientReadFormQueryString>";

        Reader reader = new NullReplacingReader(new StringReader(input));
        StringWriter writer = new StringWriter();

        char[] chars = new char[20];

        writer.write(chars, 1, reader.read(chars, 1, 5));
        writer.write(chars, 3, reader.read(chars, 3, 17));
        writer.write(chars, 5, reader.read(chars, 5, 15));

        Assert.equals(input, writer.toString());
    }
}
