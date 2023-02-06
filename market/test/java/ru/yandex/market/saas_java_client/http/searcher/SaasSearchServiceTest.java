package ru.yandex.market.saas_java_client.http.searcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import ru.yandex.market.saas_java_client.http.common.SaasAttr;
import ru.yandex.market.saas_java_client.http.searcher.response.SaasSearchResponse;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.DoubleKind;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.IntKind;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.IsProperty;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.NoGroup;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.NotSearchable;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.StringKind;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("checkstyle:MagicNumber")
public class SaasSearchServiceTest {
    private static final String INT_FIELD_NAME = "i_param_cnt";
    private static final String STR_FIELD_NAME = "s_id";
    private static final String DOUBLE_FIELD_NAME = "d_price";
    private static final String LIST_FIELD_NAME = "s_parsed_params";

    private static final SaasAttr<IntKind, NotSearchable, NoGroup, IsProperty> INT_FIELD
            = SaasAttr.intAttr(INT_FIELD_NAME).property();

    private static final SaasAttr<StringKind, NotSearchable, NoGroup, IsProperty> STR_FIELD
            = SaasAttr.stringAttr(STR_FIELD_NAME).property();

    private static final SaasAttr<DoubleKind, NotSearchable, NoGroup, IsProperty> DOUBLE_FIELD
            = SaasAttr.doubleAttr(DOUBLE_FIELD_NAME).property();

    //To compare doubles
    private static final double DELTA = 0.0000001;

    @Test
    public void testReadPropertiesInResponse() throws IOException {
        ObjectMapper mapper = SaasSearchService.createMapper();
        URL responseBody = getClass().getResource("/requests/search-response.json");
        assertNotNull(responseBody);

        SaasSearchResponse response = mapper.readValue(responseBody, SaasSearchResponse.class);

        assertEquals(16000, response.getTotal());
        assertEquals("4", response.getDocuments().get(0).getProperty(INT_FIELD_NAME));
        assertEquals("301cadeaf437f008262f63e2254259c8",
                response.getDocuments().get(0).getProperty(STR_FIELD_NAME));
        assertEquals(4, ((List) response.getDocuments().get(0).getProperty(LIST_FIELD_NAME)).size());
        assertEquals("1234567.89123456", response.getDocuments().get(1).getProperty(DOUBLE_FIELD_NAME));
    }

    @Test
    public void testReadCastedPropertiesInResponse() throws IOException {
        ObjectMapper mapper = SaasSearchService.createMapper();
        URL responseBody = getClass().getResource("/requests/search-response.json");
        assertNotNull(responseBody);

        SaasSearchResponse response = mapper.readValue(responseBody, SaasSearchResponse.class);

        assertEquals(16000, response.getTotal());
        assertEquals(4, response.getDocuments().get(0).getPropertyAsInt(INT_FIELD));
        assertEquals("301cadeaf437f008262f63e2254259c8",
                response.getDocuments().get(0).getPropertyAsStr(STR_FIELD).get());
        assertEquals(-1.1, response.getDocuments().get(0).getPropertyAsDouble(DOUBLE_FIELD), DELTA);
    }
}
