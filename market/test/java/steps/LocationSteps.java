package steps;

import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.delivery.mdbapp.components.geo.Location;

public class LocationSteps {
    private static final int ID = 213;
    private static final String COUNTRY = "Россия";
    private static final String FEDERAL_DISTRICT = "Центральный федеральный округ";
    private static final String REGION = "Москва и Московская область";
    private static final String SUB_REGION = "Москва";
    private static final String LOCALITY = "Москва";

    private LocationSteps() {
    }

    public static Location getLocation() {
        return getLocation(ID);
    }

    public static Location getLocation(long id) {
        Location defaultLocation = new Location();

        defaultLocation.setId((int) id);
        defaultLocation.setCountry(COUNTRY);
        defaultLocation.setFederalDistrict(FEDERAL_DISTRICT);
        defaultLocation.setRegion(REGION);
        defaultLocation.setSubRegion(SUB_REGION);
        defaultLocation.setLocality(LOCALITY);

        return defaultLocation;
    }

    public static JSONObject getLocationJson() throws JSONException {
        JSONObject locationJson = new JSONObject();

        locationJson.put("id", ID);
        locationJson.put("country", COUNTRY);
        locationJson.put("federalDistrict", FEDERAL_DISTRICT);
        locationJson.put("region", REGION);
        locationJson.put("subRegion", SUB_REGION);
        locationJson.put("locality", LOCALITY);

        return locationJson;
    }
}
