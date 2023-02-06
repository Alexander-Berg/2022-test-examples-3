package ru.yandex.market.modelparams;

import static junit.framework.Assert.assertEquals;
import org.junit.Test;
import ru.yandex.market.modelparams.model.DocumentRecord;
import ru.yandex.market.modelparams.model.ModelDocuments;

import java.sql.Date;
import java.util.Arrays;

/**
 * Created on 09.06.2007 20:47:11
 *
 * @author Eugene Kirpichov jkff@yandex-team.ru
 */
public class XmlConversionTest {
    @Test
    public void testConvertDocs() {
        ModelDocuments docs = new ModelDocuments(
            123, Arrays.asList(
            new DocumentRecord(
                111, 123, "m&m's", "en",
                "www.example.com/?a=1&b=2", "www.yandex.ru/?a=1&b=2",
                new Date(0), new Date(0), "zip", 1024),
            new DocumentRecord(
                112, 123, "doc1", "en",
                "www.example.com/?a=1&b=2", "www.yandex.ru/?a=1&b=2",
                new Date(0), new Date(0), null, 1024))
        );
        StringBuilder sb = new StringBuilder();
        docs.toXml(sb);
        assertEquals(
            "<model-docs id=\"123\">" +
                "<doc filetype=\"zip\" size=\"1024\" xml:lang=\"en\" " +
                "name=\"m&amp;m&apos;s\" vendorUrl=\"www.example.com/?a=1&amp;b=2\" " +
                "cachedUrl=\"www.yandex.ru/?a=1&amp;b=2\" " +
                "docid=\"111\"/>" +
                "<doc filetype=\"\" size=\"1024\" xml:lang=\"en\" " +
                "name=\"doc1\" vendorUrl=\"www.example.com/?a=1&amp;b=2\" " +
                "cachedUrl=\"www.yandex.ru/?a=1&amp;b=2\" " +
                "docid=\"112\"/>" +
                "</model-docs>"
            , sb.toString());
    }

    @Test
    public void testConvertPostCount() {

    }
}
