package steps.outletSteps;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.mbi.api.client.entity.outlets.Address;
import ru.yandex.market.mbi.api.client.entity.outlets.Outlet;

public class OutletSteps {
    private static final long ID = 1L;
    private static final String NAME = "defaultOutlet";
    private static final String DELIVERY_SERVICE_OUTLET_ID = "270015";
    private static final String DELIVERY_SERVICE_OUTLET_CODE = "630060";
    private static final List<String> EMAILS = Collections.singletonList("testemail@ya.ru");

    private OutletSteps() {
    }

    public static JSONObject getOutletJson() throws JSONException {
        JSONObject outletJson = new JSONObject();

        outletJson.put("id", ID);
        outletJson.put("deliveryServiceOutletId", DELIVERY_SERVICE_OUTLET_ID);
        outletJson.put("deliveryServiceOutletCode", DELIVERY_SERVICE_OUTLET_CODE);
        outletJson.put("emails", EMAILS);
        outletJson.put("address", AddressSteps.getAddressJson());
        outletJson.put("geoInfo", GeoInfoSteps.getGeoInfoJson());
        outletJson.put("name", NAME);
        outletJson.put("scheduleLines", ScheduleLineSteps.getScheduleLinesJson());
        outletJson.put("phoneNumbers", PhoneNumberSteps.getPhoneNumberJson());

        return outletJson;
    }

    public static Outlet getDefaultOutlet() {
        return getDefaultOutlet(null);
    }

    public static Outlet getDefaultOutlet(Long regionId) {
        return new Outlet(
            ID,
            NAME,
            DELIVERY_SERVICE_OUTLET_ID,
            DELIVERY_SERVICE_OUTLET_CODE,
            AddressSteps.getAddress(),
            Optional.ofNullable(regionId).map(GeoInfoSteps::getGeoInfo).orElseGet(GeoInfoSteps::getGeoInfo),
            EMAILS,
            PhoneNumberSteps.getPhoneNumber(),
            ScheduleLineSteps.getScheduleLines(),
            null
        );
    }

    public static Outlet getDefaultOutlet(String outletId, String outletCode, Long regionId) {
        return getDefaultOutlet(ID, outletId, outletCode, regionId);
    }

    public static Outlet getDefaultOutlet(Long id, String outletId, String outletCode, Long regionId) {
        return getDefaultOutlet(id, outletId, outletCode, regionId, AddressSteps.getAddress());
    }

    public static Outlet getDefaultOutlet(Long id, String outletId, String outletCode, Long regionId, Address address) {
        return new Outlet(
            id,
            NAME,
            outletId,
            outletCode,
            address,
            Optional.ofNullable(regionId).map(GeoInfoSteps::getGeoInfo).orElseGet(GeoInfoSteps::getGeoInfo),
            EMAILS,
            PhoneNumberSteps.getPhoneNumber(),
            ScheduleLineSteps.getScheduleLines(),
            null
        );
    }
}
