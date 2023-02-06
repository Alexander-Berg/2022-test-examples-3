package steps.logisticsPointSteps;

import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.delivery.mdbapp.integration.payload.PhoneNumber;

public class PhoneNumberSteps {
    private static final String CITY = "495";
    private static final String COUNTRY = "+7";
    private static final String PHONE_NUMBER = "4567890";
    private static final String EXTENSION = "123";
    public static final String INLET_NUMBER = "+74954567890";

    private PhoneNumberSteps() {
    }

    public static List<PhoneNumber> getPhoneNumber() {
        return Collections.singletonList(new PhoneNumber(
            COUNTRY,
            CITY,
            PHONE_NUMBER,
            EXTENSION
        ));
    }

    public static List<PhoneNumber> getInletPhoneNumber() {
        return Collections.singletonList(new PhoneNumber(
            null,
            null,
            INLET_NUMBER,
            EXTENSION
        ));
    }

    static JSONArray getPhoneNumberJson() throws JSONException {
        JSONArray phoneNumbersJsonArray = new JSONArray();
        JSONObject phoneNumberJson = new JSONObject();

        phoneNumberJson.put("country", COUNTRY);
        phoneNumberJson.put("number", PHONE_NUMBER);
        phoneNumberJson.put("extension", EXTENSION);
        phoneNumberJson.put("city", CITY);

        phoneNumbersJsonArray.put(phoneNumberJson);
        return phoneNumbersJsonArray;
    }

    static JSONArray getInletPhoneNumberJson() throws JSONException {
        JSONArray phoneNumbersJsonArray = new JSONArray();
        JSONObject phoneNumberJson = new JSONObject();

        phoneNumberJson.put("number", INLET_NUMBER);
        phoneNumberJson.put("extension", EXTENSION);

        phoneNumbersJsonArray.put(phoneNumberJson);
        return phoneNumbersJsonArray;
    }
}
