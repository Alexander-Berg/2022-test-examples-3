package ru.yandex.tikaite.mimeparser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.function.FalsePredicate;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.search.document.IdentityDocumentFieldsFilter;
import ru.yandex.search.document.mail.MailMetaInfo;
import ru.yandex.tikaite.util.SearchingInputStream;
import ru.yandex.tikaite.util.SearchingOutputStream;
import ru.yandex.tikaite.util.TextExtractOptions;

public class MessageHandlerTest {
    private static final String SIMPLEST_XML = "simplest.xml.txt";
    private static final String ALTERNATIVE_PLAIN_HTML =
        "alternative.plain.html.txt";

    private static final String HELLO_AGAIN = "Hello again";

    private MimeStreamParser createParser(final Writer writer) {
        HandlerManager manager = new HandlerManager();
        RootMessageHandler handler =
            new RootMessageHandler(
                new MailMetaInfo(-1, -1, FalsePredicate.INSTANCE),
                new TextExtractOptions(),
                new MailDocumentCollectorFactory(
                    new JsonDocumentCollectorFactory(
                        new JsonWriter(writer),
                        IdentityDocumentFieldsFilter.INSTANCE,
                        null)));
        manager.push(handler);
        MimeStreamParser parser = new MimeStreamParser();
        parser.setContentHandler(manager);
        return parser;
    }

    @Test
    public void testBadInputSimplest() throws IOException, MimeException {
        StringWriter writer = new StringWriter();
        try {
            createParser(writer).parse(new SearchingInputStream(
                getClass().getResourceAsStream(SIMPLEST_XML),
                HELLO_AGAIN.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            return;
        }
        Assert.fail();
    }

    @Test(expected = IOException.class)
    public void testBadInputSimplestDetect()
        throws IOException, MimeException
    {
        StringWriter writer = new StringWriter();
        createParser(writer).parse(new SearchingInputStream(
            getClass().getResourceAsStream(SIMPLEST_XML),
            "encoding".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testBadInputAlternative() throws IOException, MimeException {
        StringWriter writer = new StringWriter();
        try {
            createParser(writer).parse(new SearchingInputStream(
                getClass().getResourceAsStream(ALTERNATIVE_PLAIN_HTML),
                "gain".getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            return;
        }
        Assert.fail();
    }

    @Test
    public void testBadOutputAlternative() throws IOException, MimeException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Writer writer =
                new OutputStreamWriter(
                    new SearchingOutputStream(
                        output,
                        "meta".getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8))
        {
            createParser(writer).parse(
                getClass().getResourceAsStream("alternative.big.txt"));
        } catch (Exception e) {
            return;
        }
        Assert.fail();
    }
}

