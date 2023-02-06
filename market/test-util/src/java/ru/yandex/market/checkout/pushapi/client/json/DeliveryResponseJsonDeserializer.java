package ru.yandex.market.checkout.pushapi.client.json;

import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.json.JsonDeserializer;
import ru.yandex.market.checkout.common.json.JsonReader;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;

import java.io.IOException;
import java.util.*;

@Component
public class DeliveryResponseJsonDeserializer implements JsonDeserializer<DeliveryResponse> {

    @Override
    public DeliveryResponse deserialize(JsonReader reader) throws IOException {
        final DeliveryResponse delivery = new DeliveryResponse();
        delivery.setType(reader.getEnum("type", DeliveryType.class));
        delivery.setPrice(reader.getBigDecimal("price"));
        delivery.setServiceName(reader.getString("serviceName"));
        delivery.setId(reader.getString("id"));
        if(reader.getBool("paymentAllow") != null)
            delivery.setPaymentAllow(reader.getBool("paymentAllow"));

        final JsonReader datesReader = reader.getReader("dates");
        if(datesReader != null) {
            final Date toDate = datesReader.getDate("toDate");
            final Date fromDate = datesReader.getDate("fromDate");
            delivery.setDeliveryDates(new DeliveryDates(fromDate, toDate));
        }

        final List<JsonReader> outletsReaders = reader.getReaderList("outlets");
        if(outletsReaders != null) {
            delivery.setOutletIds(new ArrayList<Long>());
            for(JsonReader jsonReader : outletsReaders) {
                delivery.getOutletIds().add(jsonReader.getLong("id"));
            }
        }

        delivery.setPaymentOptions(deserializePaymentMethods(reader));

        return delivery;
    }

    public static Set<PaymentMethod> deserializePaymentMethods(JsonReader reader) throws IOException {
        List<PaymentMethod> methods = reader.getEnumList("paymentMethods", PaymentMethod.class);
        return methods == null ? null : new HashSet<>(methods);
    }
}
