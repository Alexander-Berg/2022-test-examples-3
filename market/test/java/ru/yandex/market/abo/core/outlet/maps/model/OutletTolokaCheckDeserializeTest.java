package ru.yandex.market.abo.core.outlet.maps.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author artemmz
 * @date 05.06.18.
 */
public class OutletTolokaCheckDeserializeTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String JSON = "{" +
            "  \"id\" : 1," +
            "  \"country\" : \"RU\"," +
            "  \"lang\" : \"RU\"," +
            "  \"checkDetails\" : {" +
            "    \"permaLink\" : 2," +
            "    \"address\" : \"msk\"," +
            "    \"price\" : \"market\"," +
            "    \"name\" : \"meow\"," +
            "    \"lat-lon\" : \"11.0,1.0\"," +
            "    \"rubric-ids\" : \"1;2\"," +
            "    \"rubric\" : \"Ready Player One;Ready Player Two\"," +
            "    \"is-auto\" : true," +
            "    \"domain\" : \"dev.null\"" +
            "  }," +
            "  \"creationTimestamp\" : 1528230196613" +
            "}";

    @Test
    public void deserialize() throws IOException {
        OutletTolokaCheck tolokaCheck = MAPPER.readValue(JSON, OutletTolokaCheck.class);
        assertNotNull(tolokaCheck);
        assertEquals(1, tolokaCheck.getId());
        assertEquals("RU", tolokaCheck.getCountry());
        assertEquals("RU", tolokaCheck.getLang());
        assertEquals(1528230196613L, tolokaCheck.getCreationTimestamp());

        OutletTolokaDetails checkDetails = tolokaCheck.getCheckDetails();
        assertEquals(2, checkDetails.getPermaLink());
        assertEquals("msk", checkDetails.getAddress());
        assertEquals("meow", checkDetails.getOutletName());
        assertEquals("1;2", checkDetails.getRubricIdsCsv());
        assertEquals("Ready Player One;Ready Player Two", checkDetails.getRubricNamesCsv());
        assertEquals("dev.null", checkDetails.getDomain());
    }
}