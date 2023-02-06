package steps.logisticsPointSteps;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.JSONException;
import org.json.JSONObject;
import steps.utils.TestUtils;

import ru.yandex.market.delivery.mdbapp.integration.payload.Address;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPoint;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

public class LogisticPointSteps {
    private static final long ID = 1L;
    private static final String NAME = "defaultOutlet";
    private static final String DELIVERY_SERVICE_OUTLET_CODE = "630060";

    private LogisticPointSteps() {
    }

    public static JSONObject getLogisticsPointJson() throws JSONException {
        JSONObject outletJson = new JSONObject();

        outletJson.put("id", ID);
        outletJson.put("deliveryServiceOutletCode", DELIVERY_SERVICE_OUTLET_CODE);
        outletJson.put("address", AddressSteps.getAddressJson());
        outletJson.put("geoInfo", GeoInfoSteps.getGeoInfoJson());
        outletJson.put("name", NAME);
        outletJson.put("scheduleLines", ScheduleLineSteps.getScheduleLinesJson());
        outletJson.put("phoneNumbers", PhoneNumberSteps.getPhoneNumberJson());

        return outletJson;
    }

    public static LogisticsPoint getDefaultOutlet() {
        return getDefaultOutlet(null);
    }

    public static LogisticsPoint getDefaultOutlet(Long regionId) {
        return new LogisticsPoint(
            ID,
            DELIVERY_SERVICE_OUTLET_CODE,
            AddressSteps.getAddress(),
            Optional.ofNullable(regionId).map(GeoInfoSteps::getGeoInfo).orElseGet(GeoInfoSteps::getGeoInfo),
            PhoneNumberSteps.getPhoneNumber(),
            ScheduleLineSteps.getScheduleLines()
        );
    }

    public static LogisticsPoint getDefaultOutlet(String outletCode, Long regionId) {
        return getDefaultOutlet(ID, outletCode, regionId);
    }

    public static LogisticsPoint getDefaultOutlet(Long id, String outletCode, Long regionId) {
        return getDefaultOutlet(id, outletCode, regionId, AddressSteps.getAddress());
    }

    public static LogisticsPoint getDefaultOutlet(
        Long id,
        String outletCode,
        Long regionId,
        Address address
    ) {
        return new LogisticsPoint(
            id,
            outletCode,
            address,
            Optional.ofNullable(regionId).map(GeoInfoSteps::getGeoInfo).orElseGet(GeoInfoSteps::getGeoInfo),
            PhoneNumberSteps.getPhoneNumber(),
            ScheduleLineSteps.getScheduleLines()
        );
    }

    public static LogisticsPoint getDefaultInlet() {
        return getDefaultInlet(null);
    }

    public static LogisticsPoint getDefaultInlet(Long regionId) {
        return new LogisticsPoint(
            ID,
            DELIVERY_SERVICE_OUTLET_CODE,
            AddressSteps.getInletAddress(),
            Optional.ofNullable(regionId).map(GeoInfoSteps::getGeoInfo).orElseGet(GeoInfoSteps::getGeoInfo),
            PhoneNumberSteps.getInletPhoneNumber(),
            ScheduleLineSteps.getInletScheduleLines()
        );
    }

    public static List<LogisticsPointResponse> getWarehouseResponse() throws IOException {
        String body = TestUtils.readFile("/logistics_point.json");

        LogisticsPointResponse logisticsPointResponse = TestUtils.getObjectMapper()
            .readValue(body, LogisticsPointResponse.class);

        return Collections.singletonList(logisticsPointResponse);
    }

    public static LogisticsPointResponse outletFromLms() {
        return LogisticsPointResponse.newBuilder()
            .id(ID)
            .externalId(DELIVERY_SERVICE_OUTLET_CODE)
            .build();
    }
}
