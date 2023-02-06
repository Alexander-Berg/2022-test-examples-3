package ru.yandex.market.core.abo._public.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.abo._public.ResolveShopProblemResult;

/**
 * @author zoom
 */
public class ResolveShopProblemResultParserTest extends AbstractParserTest {

    @Test
    public void shouldNotContainsAnyErrorsWhenParseOkResult() throws IOException, SAXException {
        try (InputStream in = getContentStream("OK-result.xml")) {
            ResolveShopProblemResult actual = new ResolveTicketResultParser().parseStream(in);
            Assert.assertFalse(actual.hasErrors());
        }
    }

    @Test
    public void shouldContainsErrorsWhenParseErrorResult() throws IOException, SAXException {
        try (InputStream in = getContentStream("ERROR-result.xml")) {
            ResolveShopProblemResult actual = new ResolveTicketResultParser().parseStream(in);
            Assert.assertTrue(actual.hasErrors());
            Assert.assertEquals(Arrays.asList("problemId was not found"), actual.getErrors());
        }
    }

    @Test
    public void shouldContainsErrorsWhenParseUnknownErrorResult() throws IOException, SAXException {
        try (InputStream in = getContentStream("ERROR-unknown-result.xml")) {
            ResolveShopProblemResult actual = new ResolveTicketResultParser().parseStream(in);
            Assert.assertTrue(actual.hasErrors());
            Assert.assertEquals(Arrays.asList("Unknown error"), actual.getErrors());
        }
    }

}