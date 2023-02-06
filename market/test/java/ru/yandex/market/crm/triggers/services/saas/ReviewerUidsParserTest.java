package ru.yandex.market.crm.triggers.services.saas;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class ReviewerUidsParserTest {

    private ReviewerUidsParser parser = new ReviewerUidsParser();

    @Test
    public void testParse() throws IOException {
        byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("saas-reviews-response.json"));
        List<Long> res = parser.parse(bytes);
        assertEquals(2, res.size());
        assertEquals(asList(111L, 222L), res);
    }
}