package ru.yandex.market.delivery.mdbapp.integration.transformer;

import java.io.IOException;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import ru.yandex.market.checkout.checkouter.jackson.CheckouterModule;
import ru.yandex.market.delivery.mdbapp.integration.enricher.delivery.json.CreateIntakeInLgwMixIn;
import ru.yandex.market.delivery.mdbapp.integration.payload.CreateIntakeInLgw;
import ru.yandex.market.delivery.mdbapp.integration.payload.ShipmentFromSC;

import static org.junit.Assert.assertEquals;

public class LgwCreateShipmentFromScTransformerTest {

    private LgwCreateShipmentFromScTransformer lgwCreateShipmentFromScTransformer;

    private ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
        .modulesToInstall(new CheckouterModule())
        .simpleDateFormat("dd-MM-yyyy HH:mm:ss")
        .timeZone(TimeZone.getDefault())
        .mixIn(CreateIntakeInLgw.class, CreateIntakeInLgwMixIn.class)
        .build()
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
        .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);

    @Before
    public void init() {
        lgwCreateShipmentFromScTransformer = new LgwCreateShipmentFromScTransformer();
    }

    @Test
    public void transformationWithAllDataIsCorrect() throws IOException {
        String requestJson = IOUtils.toString(
            this.getClass().getResourceAsStream("/create_shipment_from_sc_request.json"),
            "utf8"
        );
        ShipmentFromSC shipmentFromSc = mapper.readValue(requestJson, ShipmentFromSC.class);

        CreateIntakeInLgw actualRequest = lgwCreateShipmentFromScTransformer.transform(shipmentFromSc);

        String expectedJson = IOUtils.toString(
            this.getClass().getResourceAsStream("/create_shipment_from_sc_transformed_request.json"),
            "utf8"
        );
        CreateIntakeInLgw expectedRequest = mapper.readValue(expectedJson, CreateIntakeInLgw.class);

        assertEquals(expectedRequest, actualRequest);
    }
}
