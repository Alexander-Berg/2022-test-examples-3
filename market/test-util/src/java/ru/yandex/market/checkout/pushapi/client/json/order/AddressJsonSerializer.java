package ru.yandex.market.checkout.pushapi.client.json.order;

import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.common.json.JsonSerializer;
import ru.yandex.market.checkout.common.json.JsonWriter;

import java.io.IOException;

@Component
public class AddressJsonSerializer implements JsonSerializer<Address> {

    @Override
    public void serialize(Address value, JsonWriter writer) throws IOException {
        writer.startObject();

        writer.setAttribute("country", value.getCountry());
        writer.setAttribute("postcode", value.getPostcode());
        writer.setAttribute("city", value.getCity());
        writer.setAttribute("subway", value.getSubway());
        writer.setAttribute("street", value.getStreet());
        writer.setAttribute("house", value.getHouse());
        writer.setAttribute("block", value.getBlock());
        writer.setAttribute("entrance", value.getEntrance());
        writer.setAttribute("entryphone", value.getEntryphone());
        writer.setAttribute("floor", value.getFloor());
        writer.setAttribute("apartment", value.getApartment());
        writer.setAttribute("recipient", value.getRecipient());
        writer.setAttribute("phone", value.getPhone());

        writer.endObject();
    }

}
