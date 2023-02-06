package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.TariffType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class ParcelJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws IOException, ParseException {
        Parcel orderShipment = EntityHelper.getOrderShipment();

        String json = write(orderShipment);
        System.out.println(json);

        checkJson(json, "$." + Names.Parcel.ID, 123);
        checkJson(json, "$." + Names.Parcel.SHOP_SHIPMENT_ID, 345);
        checkJson(json, "$." + Names.Parcel.WEIGHT, 567);
        checkJson(json, "$." + Names.Parcel.WIDTH, 109);
        checkJson(json, "$." + Names.Parcel.HEIGHT, 789);
        checkJson(json, "$." + Names.Parcel.DEPTH, 901);
        checkJson(json, "$." + Names.Parcel.STATUS, ParcelStatus.NEW.name());
        checkJson(json, "$." + Names.Parcel.LABEL_URL, "labelUrl");
        checkJson(json, "$." + Names.Parcel.TRACKS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Parcel.TRACKS, hasSize(1));
        checkJson(json, "$." + Names.Parcel.ITEMS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Parcel.ITEMS, hasSize(1));
        checkJson(json, "$." + Names.Parcel.FROM_DATE, "01-01-2018");
        checkJson(json, "$." + Names.Parcel.TO_DATE, "02-01-2018");
        checkJson(json, "$." + Names.Parcel.TARIFF_TYPE, TariffType.REGISTERED.name());
    }

    @Test
    public void deserialize() throws IOException {
        String json = "{" +
                "\"id\":123," +
                "\"shopShipmentId\":345," +
                "\"weight\":567," +
                "\"height\":789," +
                "\"depth\":901," +
                "\"status\":\"NEW\"," +
                "\"labelURL\":\"labelUrl\"," +
                "\"tracks\":[{\"id\":123,\"trackCode\":\"code\",\"deliveryServiceId\":123,\"trackerId\":456," +
                "\"status\":\"STARTED\",\"creationDate\":\"02-06-72389 17:37:02\",\"checkpoints\":[{\"id\":123," +
                "\"trackerCheckpointId\":456,\"country\":\"country\",\"city\":\"city\",\"location\":\"location\"," +
                "\"message\":\"message\",\"status\":\"DELIVERED\",\"zipCode\":\"zipCode\",\"date\":\"10-07-1973 " +
                "03:11:51\",\"deliveryStatus\":123,\"translatedCountry\":\"страна\",\"translatedCity\":\"город\"," +
                "\"translatedLocation\":\"местоположение\",\"translatedMessage\":\"сообщение\"}]}]," +
                "\"items\":[{\"itemId\":123,\"count\":234}]" +
                "}";

        Parcel orderShipment = read(Parcel.class, json);

        Assertions.assertEquals(123, orderShipment.getId().longValue());
        Assertions.assertEquals(345, orderShipment.getShopShipmentId().longValue());
        Assertions.assertEquals(567, orderShipment.getWeight().longValue());
        Assertions.assertEquals(789, orderShipment.getHeight().longValue());
        Assertions.assertEquals(901, orderShipment.getDepth().longValue());
        Assertions.assertEquals(ParcelStatus.NEW, orderShipment.getStatus());
        Assertions.assertEquals("labelUrl", orderShipment.getLabelURL());
        Assertions.assertNotNull(orderShipment.getTracks());
        assertThat(orderShipment.getTracks(), hasSize(1));
        Assertions.assertNotNull(orderShipment.getParcelItems());
        assertThat(orderShipment.getParcelItems(), hasSize(1));
    }
}
