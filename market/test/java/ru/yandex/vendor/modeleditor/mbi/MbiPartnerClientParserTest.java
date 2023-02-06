package ru.yandex.vendor.modeleditor.mbi;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MbiPartnerClientParserTest {

    @Test
    public void testParseListOfAllowedActions() {
        MbiPartnerClientParser parser = new MbiPartnerClientParser();
        InputStream inputStream = getInputStreamResource("/testParseListOfAllowedActions/mbiPartnerResponse.json");
        List<String> allowedActions = parser.parseGetAllowedActionsStream(inputStream);
        assertEquals(4, allowedActions.size());
        assertTrue(allowedActions.contains("/shop/model/write"));
        assertTrue(allowedActions.contains("/shop/model/read"));
        assertTrue(allowedActions.contains("/shop/model/batch/write"));
        assertTrue(allowedActions.contains("/shop/model/batch/read"));
    }

    private InputStream getInputStreamResource(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        return classLoader.getResourceAsStream("ru/yandex/vendor/modeledit/mbi/" + getClass().getSimpleName() + filename);
    }
}
