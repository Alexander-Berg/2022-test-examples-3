package ru.yandex.market.fulfillment.wrap.marschroute.transformation;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.marschroute.api.ProductsClient;
import ru.yandex.market.fulfillment.wrap.marschroute.model.converter.ProductsResponseDataToItemReferenceConverter;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.MarschrouteProductsResponse;
import ru.yandex.market.fulfillment.wrap.marschroute.service.MarschrouteProductsService;
import ru.yandex.market.logistic.api.model.fulfillment.Inbound;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.fail;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class LifetimeCheckerTest {
    private static class MockMarschrouteProductsService extends MarschrouteProductsService {

        public MockMarschrouteProductsService(ProductsClient productsClient) {
            super(productsClient);
        }

        @NotNull
        @Override
        public MarschrouteProductsResponse execute(@NotNull Set<UnitId> unitIds) {
            MarschrouteProductsResponse marschrouteProductsResponse = new MarschrouteProductsResponse();
            try {
                marschrouteProductsResponse =
                        mapper.readValue(extractFileContent(MARSCHROUTE_PRODUCT_SERVICE_RESPONSE_OBJECT_PATH),
                                MarschrouteProductsResponse.class);
                return marschrouteProductsResponse;
            } catch (IOException e) {
                fail("Failed to read model response");
            }

            return marschrouteProductsResponse;
        }
    }

    private static LifetimeChecker lifetimeChecker;
    private static ObjectMapper mapper;
    private static final String INBOUND_OBJECT_PATH = "transformation/inbound.json";
    private static final String MARSCHROUTE_PRODUCT_SERVICE_RESPONSE_OBJECT_PATH =
            "transformation/marchroute_product_service_response.json";

    @BeforeAll
    public static void init(){
        mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setVisibility(
                VisibilityChecker.Std.defaultInstance()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY));

        MarschrouteProductsService mockService = new MockMarschrouteProductsService(null);
        ProductsResponseDataToItemReferenceConverter converter =
                new ProductsResponseDataToItemReferenceConverter(null);

        lifetimeChecker = new LifetimeChecker(mockService, converter, 500);
    }

    @Test
    void updateLifetime() throws Exception {
        Inbound inbound = mapper.readValue(extractFileContent(INBOUND_OBJECT_PATH), Inbound.class);
        Inbound updated = lifetimeChecker.updateLifetime(inbound);

        //количество поставок не должно измениться после проверки наличия hasLifeTime
        assertEquals("Consignments number should not change",
                inbound.getConsignments().size(), updated.getConsignments().size());

        //В стоке есть информация, флаг hasLifeTime сброшен
        Boolean sourceLt0 = inbound.getConsignments().get(0).getItem().getHasLifeTime();
        Boolean updatedLt0 = updated.getConsignments().get(0).getItem().getHasLifeTime();
        assertNotSame("HasLifeTime flag should change for " +
                "UnitId{id='100304632761', vendorId=469929, article='5010415331281'}", sourceLt0, updatedLt0);

        assertFalse("HasLifeTime flag should be false for " +
                "UnitId{id='100304632761', vendorId=469929, article='5010415331281'}", updatedLt0);

        //В стоке есть информация, флаг hasLifeTime установлен
        Boolean sourceLt1 = inbound.getConsignments().get(1).getItem().getHasLifeTime();
        Boolean updatedLt1 = updated.getConsignments().get(1).getItem().getHasLifeTime();
        assertNotSame("HasLifeTime flag should change for " +
                "UnitId{id='100304721351', vendorId=469929, article='2000015052276'}", sourceLt1, updatedLt1);

        assertTrue("HasLifeTime flag should be false for " +
                "UnitId{id='100304721351', vendorId=469929, article='2000015052276'}", updatedLt1);

        //В стоке нет информации, флаг hasLifeTime сохраняет свое значение
        Boolean sourceLt2 = inbound.getConsignments().get(2).getItem().getHasLifeTime();
        Boolean updatedLt2 = updated.getConsignments().get(2).getItem().getHasLifeTime();
        assertSame("HasLifeTime flag should not change for " +
                "UnitId{id='100304721352', vendorId=469929, article='2000015052269'}", sourceLt2, updatedLt2);

        assertNull("HasLifeTime flag should be null for " +
                "UnitId{id='100304721352', vendorId=469929, article='2000015052269'}", updatedLt2);

    }

}
