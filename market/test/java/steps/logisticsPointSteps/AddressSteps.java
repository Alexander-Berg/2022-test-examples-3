package steps.logisticsPointSteps;

import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.delivery.mdbapp.integration.payload.Address;

class AddressSteps {
    private static final String CITY = "Москва";
    private static final String STREET = "Льва Толстого";
    private static final String ADDRESS_NUMBER = "16/2к3";
    private static final String BUILDING = "4";
    private static final String ESTATE = "22";
    private static final String BLOCK = "3";
    private static final String POST_CODE = "117208";

    private AddressSteps() {
    }

    static Address getAddress() {
        return new Address(
            CITY,
            STREET,
            ADDRESS_NUMBER,
            BUILDING,
            ESTATE,
            BLOCK,
            POST_CODE
        );
    }

    static Address getInletAddress() {
        return new Address(
            CITY,
            STREET,
            ADDRESS_NUMBER,
            BUILDING,
            null,
            BLOCK,
            POST_CODE
        );
    }

    static JSONObject getAddressJson() throws JSONException {
        JSONObject addressJson = new JSONObject();

        addressJson.put("city", CITY);
        addressJson.put("street", STREET);
        addressJson.put("number", ADDRESS_NUMBER);
        addressJson.put("postCode", POST_CODE);
        addressJson.put("estate", ESTATE);
        addressJson.put("block", BLOCK);
        addressJson.put("building", BUILDING);

        return addressJson;
    }
}
