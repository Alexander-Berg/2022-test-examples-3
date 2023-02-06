package ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment.common;

import org.junit.Before;
import org.junit.Test;
import steps.orderSteps.AddressSteps;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.AddressLanguage;
import ru.yandex.market.checkout.checkouter.delivery.AddressType;
import ru.yandex.market.delivery.mdbapp.integration.payload.EnrichedFulfillmentOrder;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class AddressNormalizingEnricherTest {

    private AddressNormalizingEnricher addressNormalizingEnricher;

    @Before
    public void setUp() {
        addressNormalizingEnricher = new AddressNormalizingEnricher();
    }

    @Test
    public void enrichWithIncorrectAddress() {
        EnrichedFulfillmentOrder enrichedFulfillmentOrder = createEnrichedFulfillmentOrder(createIncorrectAddress());

        addressNormalizingEnricher.enrich(enrichedFulfillmentOrder);

        assertThat(enrichedFulfillmentOrder.getAddress())
            .as("Address normalization failure")
            .isEqualToComparingFieldByFieldRecursively(AddressSteps.getAddress());
    }

    @Test
    public void enrichWithNoAddress() {
        EnrichedFulfillmentOrder enrichedFulfillmentOrder = createEnrichedFulfillmentOrder(null);

        addressNormalizingEnricher.enrich(enrichedFulfillmentOrder);

        assertThat(enrichedFulfillmentOrder.getAddress())
            .isNull();
    }

    private EnrichedFulfillmentOrder createEnrichedFulfillmentOrder(Address address) {
        EnrichedFulfillmentOrder enrichedFulfillmentOrder = new EnrichedFulfillmentOrder(null);
        enrichedFulfillmentOrder.setAddress(address);

        return enrichedFulfillmentOrder;
    }

    private Address createIncorrectAddress() {
        AddressImpl address = new AddressImpl();
        address.setPostcode("630090\n ");
        address.setCity("Москва\n ");
        address.setSubway("Парк\nКультуры ");
        address.setStreet("Льва\rТолстого ");
        address.setHouse("15в/3\n ");
        address.setBlock("422\n ");
        address.setEntrance("6\n ");
        address.setFloor("4\n ");
        address.setApartment("22\n ");
        address.setRecipient("recipient\n ");
        address.setPhone("+70987654321\n ");
        address.setLanguage(AddressLanguage.RUS);
        address.setCountry("\n Россия");
        address.setKm("\n 101");
        address.setNotes("\n notes");
        address.setEstate("\n 22");
        address.setGps("\n 32.416,76.589");
        address.setType(AddressType.SHOP);
        address.setBuilding("\n 345");
        address.setEntryPhone("\n +71234567809");

        return address;
    }
}
