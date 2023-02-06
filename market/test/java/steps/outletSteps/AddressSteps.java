package steps.outletSteps;

import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.mbi.api.client.entity.outlets.Address;

public class AddressSteps {

    private static final String CITY = "Москва";
    private static final String STREET = "Льва Толстого";
    private static final String ADDRESS_NUMBER = "16/2к3";
    private static final String BUILDING = "4";
    private static final String ESTATE = "22";
    private static final String BLOCK = "3";
    private static final int KM = 101;
    private static final String POST_CODE = "117208";
    private static final String ADDR_ADDITIONAL = "addrAdditional";

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
            KM,
            POST_CODE,
            ADDR_ADDITIONAL
        );
    }

    static JSONObject getAddressJson() throws JSONException {
        JSONObject addressJson = new JSONObject();

        addressJson.put("city", CITY);
        addressJson.put("street", STREET);
        addressJson.put("number", ADDRESS_NUMBER);
        addressJson.put("km", KM);
        addressJson.put("postCode", POST_CODE);
        addressJson.put("estate", ESTATE);
        addressJson.put("block", BLOCK);
        addressJson.put("building", BUILDING);
        addressJson.put("addrAdditional", ADDR_ADDITIONAL);

        return addressJson;
    }
}
