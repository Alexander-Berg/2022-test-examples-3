package ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment;

import java.io.IOException;
import java.util.Set;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.delivery.mdbapp.integration.converter.VatConverter;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment.json.CreateLfwFfOrderMixIn;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment.json.TestOfferItemMixIn;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment.json.TestScheduleDayResponseMixIn;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment.json.serialization.TestSerializerModule;
import ru.yandex.market.delivery.mdbapp.integration.payload.CreateLgwFfOrder;
import ru.yandex.market.delivery.mdbapp.integration.payload.ExtendedOrder;
import ru.yandex.market.delivery.mdbapp.json.DeliveryDatesMixIn;
import ru.yandex.market.delivery.mdbapp.json.DeliveryMixIn;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

import static org.junit.Assert.assertEquals;

public class LgwFulfillmentOrderTransformerTest {

    private LgwFulfillmentOrderTransformer lgwFulfillmentOrderTransformer;

    private ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
        .modulesToInstall(new TestSerializerModule())
        .simpleDateFormat("dd-MM-yyyy HH:mm:ss")
        .timeZone(TimeZone.getDefault())
        .mixIn(OfferItem.class, TestOfferItemMixIn.class)
        .mixIn(Delivery.class, DeliveryMixIn.class)
        .mixIn(DeliveryDates.class, DeliveryDatesMixIn.class)
        .mixIn(CreateLgwFfOrder.class, CreateLfwFfOrderMixIn.class)
        .mixIn(ScheduleDayResponse.class, TestScheduleDayResponseMixIn.class)
        .build()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
        .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);

    @Before
    public void before() {
        lgwFulfillmentOrderTransformer = new LgwFulfillmentOrderTransformer(
            new VatConverter(),
            new FulfillmentPosteRestanteConverter(Set.of(145L))
        );
    }

    @Test
    public void testTransformationIsCorrect() throws IOException {
        transformAndAssert("/extended_order.json", "/create_lgw_ff_order_request.json");
    }

    @Test
    public void testTransformationMiddleNamePosteRestanteIsCorrect() throws IOException {
        transformAndAssert("/extended_order_poste_restante.json", "/create_lgw_ff_order_request_poste_restante.json");
    }

    private void transformAndAssert(String beforePath, String afterPath) throws IOException {
        String requestJson = IOUtils.toString(
            this.getClass().getResourceAsStream(beforePath),
            "utf8"
        );
        ExtendedOrder extendedOrder = mapper.readValue(requestJson, ExtendedOrder.class);

        CreateLgwFfOrder transformedOrder = lgwFulfillmentOrderTransformer.transform(extendedOrder);

        String expectedJson = IOUtils.toString(
            this.getClass().getResourceAsStream(afterPath),
            "utf8"
        );
        CreateLgwFfOrder expectedOrder = mapper.readValue(expectedJson, CreateLgwFfOrder.class);

        assertEquals(expectedOrder.toString(), transformedOrder.toString());
    }
}
