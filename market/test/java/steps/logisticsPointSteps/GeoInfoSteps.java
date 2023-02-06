package steps.logisticsPointSteps;

import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.delivery.mdbapp.integration.payload.GeoInfo;

class GeoInfoSteps {
    private static final String GPS_COORDS = "55.755826 37.6173";
    private static final Long REGION_ID = 213L;

    private GeoInfoSteps() {
    }

    static GeoInfo getGeoInfo() {
        return getGeoInfo(REGION_ID);
    }

    static GeoInfo getGeoInfo(Long regionId) {
        return new GeoInfo(GPS_COORDS, regionId);
    }

    static JSONObject getGeoInfoJson() throws JSONException {
        JSONObject geoInfoJson = new JSONObject();

        geoInfoJson.put("gpsCoords", GPS_COORDS);
        geoInfoJson.put("regionId", REGION_ID);

        return geoInfoJson;
    }
}
