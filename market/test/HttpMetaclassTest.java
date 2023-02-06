package ru.yandex.market.jmf.module.http.metaclass.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.catalog.items.CatalogItem;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.http.metaclass.test.utils.HttpMetaclassUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Transactional
@SpringJUnitConfig(classes = InternalModuleHttpMetaclassTestConfiguration.class)
public class HttpMetaclassTest {

    private static final Fqn TEST_HTTP_METACLASS = Fqn.of("testHttpMetaclass");
    private static final Fqn TEST_HTTP_METACLASS_WITH_NATURAL_ID = Fqn.of("testHttpMetaclassWithNaturalId");
    private static final String STRING_ATTR_CODE = "stringAttr";
    private static final String CATALOG_ITEM_ATTR = "catalogItemAttr";
    private static final String CATALOG_ITEMS_ATTR = "catalogItemsAttr";
    private static final String OBJECTS_ATTR = "objectsAttr";
    private static final String CONST_ATTR = "constAttr";
    private static final String SCRIPT_ATTR = "scriptAttr";
    private static final String SHARE_ATR = "shareAttr";

    @Inject
    HttpMetaclassUtils utils;

    @Inject
    EntityStorageService entityStorageService;

    @Test
    public void testGetMethod() throws IOException {
        String responsePath = "/test/module/http/metaclass/responses/testGetMethod.json";

        ClientHttpResponse response = prepareResponse(responsePath);
        int scriptResult = OffsetDateTime.now().getYear();
        utils.prepareMocks("https://example.com/constValue/1?year=" + scriptResult, HttpMethod.GET, response);

        Entity entity = entityStorageService.get("testHttpMetaclass@1");
        assertEquals("testHttpMetaclass@1", entity.getGid());
        assertEquals("value", entity.getAttribute(STRING_ATTR_CODE));

        CatalogItem catalogItemAttr = entity.getAttribute(CATALOG_ITEM_ATTR);
        assertNotNull(catalogItemAttr);
        assertEquals("ITEM_1", catalogItemAttr.getCode());

        assertEquals("constValue", entity.getAttribute(CONST_ATTR));
        assertEquals("script value", entity.getAttribute(SCRIPT_ATTR));

        Set<Entity> objectsAttr = entity.getAttribute(OBJECTS_ATTR);
        assertNotNull(objectsAttr);
        assertEquals(2, objectsAttr.size());
        assertEquals(
                Set.of("stringAttrValue1", "stringAttrValue2"),
                Set.copyOf(CrmCollections.transform(objectsAttr, x -> x.getAttribute(STRING_ATTR_CODE)))
        );

        Set<CatalogItem> catalogItemsAttr = entity.getAttribute(CATALOG_ITEMS_ATTR);
        assertNotNull(catalogItemsAttr);
        assertEquals(
                Set.of("ITEM_1", "ITEM_2"),
                Set.copyOf(CrmCollections.transform(catalogItemsAttr, CatalogItem::getCode))
        );
    }

    @Test
    public void testListMethod() throws IOException {
        String responsePath = "/test/module/http/metaclass/responses/testListMethod.json";

        ClientHttpResponse response = prepareResponse(responsePath);
        int scriptResult = OffsetDateTime.now().getYear();
        utils.prepareMocks("https://example.com/constValue?page=3&pageSize=10&year=" + scriptResult,
                HttpMethod.GET, response);

        Query query = Query.of(TEST_HTTP_METACLASS)
                .withLimit(10)
                .withOffset(20);
        List<Entity> entities = entityStorageService.list(query);
        assertEquals(2, entities.size());
        assertEquals("value1", entities.get(0).getAttribute(STRING_ATTR_CODE));
        assertEquals("value", entities.get(0).getAttribute(SHARE_ATR));
        assertNotNull(entities.get(0).getGid());
        assertEquals("value2", entities.get(1).getAttribute(STRING_ATTR_CODE));
        assertEquals("value", entities.get(1).getAttribute(SHARE_ATR));
        assertNotNull(entities.get(1).getGid());
    }

    @Test
    public void testGidFromListMethodForMetaclassWithNaturalId() throws IOException {
        String responsePath = "/test/module/http/metaclass/responses/testListMethod.json";

        ClientHttpResponse response = prepareResponse(responsePath);
        utils.prepareMocks("https://example.com?page=3&pageSize=10", HttpMethod.GET, response);

        Query query = Query.of(TEST_HTTP_METACLASS_WITH_NATURAL_ID)
                .withLimit(10)
                .withOffset(20);
        List<Entity> entities = entityStorageService.list(query);
        assertEquals(2, entities.size());
        assertEquals(
                Set.of(
                        TEST_HTTP_METACLASS_WITH_NATURAL_ID.gidOf("value1"),
                        TEST_HTTP_METACLASS_WITH_NATURAL_ID.gidOf("value2")
                ),
                Set.copyOf(CrmCollections.transform(entities, Entity::getGid))
        );
    }

    private ClientHttpResponse prepareResponse(String responsePath) throws IOException {
        var httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getRawStatusCode()).thenReturn(200);
        when(response.getBody()).thenReturn(new ByteArrayInputStream(ResourceHelpers.getResource(responsePath)));
        when(response.getHeaders()).thenReturn(httpHeaders);

        return response;
    }
}
