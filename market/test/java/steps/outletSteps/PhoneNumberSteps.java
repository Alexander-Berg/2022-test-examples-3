package steps.outletSteps;

import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.market.mbi.api.client.entity.outlets.PhoneNumber;

public class PhoneNumberSteps {

    private static final String CITY = "495";
    private static final String COUNTRY = "+7";
    private static final String PHONE_NUMBER = "4567890";
    private static final String EXTENSION = "123";
    private static final String COMMENTS = "test";
    private static final PhoneType PHONE_TYPE = PhoneType.PHONE;

    private PhoneNumberSteps() {
    }

    static List<PhoneNumber> getPhoneNumber() {
        return Collections.singletonList(new PhoneNumber(
            COUNTRY,
            CITY,
            PHONE_NUMBER,
            EXTENSION,
            COMMENTS,
            PHONE_TYPE
        ));
    }

    static JSONArray getPhoneNumberJson() throws JSONException {
        JSONArray phoneNumbersJsonArray = new JSONArray();
        JSONObject phoneNumberJson = new JSONObject();

        phoneNumberJson.put("country", COUNTRY);
        phoneNumberJson.put("number", PHONE_NUMBER);
        phoneNumberJson.put("extension", EXTENSION);
        phoneNumberJson.put("phoneType", PHONE_TYPE);
        phoneNumberJson.put("city", CITY);
        phoneNumberJson.put("comments", COMMENTS);

        phoneNumbersJsonArray.put(phoneNumberJson);
        return phoneNumbersJsonArray;
    }
}
