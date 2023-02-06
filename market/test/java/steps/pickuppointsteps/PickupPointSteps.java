package steps.pickuppointsteps;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.entities.request.ds.DsGetReferencePickupPointsRequest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.RussianPostPickupPoint;

public class PickupPointSteps {

    private PickupPointSteps() {
        throw new UnsupportedOperationException();
    }

    public static DsGetReferencePickupPointsRequest getRequest() {
        DsGetReferencePickupPointsRequest request = new DsGetReferencePickupPointsRequest();
        request.setBoolFlag("bname");
        request.setEnumFlagName("fname");
        request.setEnumFlagValue("fvalue");

        return request;
    }

    public static List<RussianPostPickupPoint> getRussianPostPickupPoints() {
        ArrayList<RussianPostPickupPoint> pickupPoints = new ArrayList<>();

        pickupPoints.add(getRussianPostPickupPoint("1"));
        pickupPoints.add(getRussianPostPickupPoint("2"));
        pickupPoints.add(getRussianPostPickupPoint("3"));

        return pickupPoints;
    }

    public static RussianPostPickupPoint getRussianPostPickupPoint(String index) {
        RussianPostPickupPoint pickupPoint = new RussianPostPickupPoint();

        pickupPoint.setId(1);
        pickupPoint.setName("name");
        pickupPoint.setIndex(index);
        pickupPoint.setCountry("country");
        pickupPoint.setArea("area");
        pickupPoint.setSubAdminArea("sub_admin_area");
        pickupPoint.setLocality("locality");
        pickupPoint.setStreet("street");
        pickupPoint.setHouse("house");
        pickupPoint.setPhoneNumber("phone_number");
        pickupPoint.setPhoneExtension("phone_extension");
        pickupPoint.setPhoneType("phone_type");
        pickupPoint.setPhoneInfo("phone_info");
        pickupPoint.setEmail("email");
        pickupPoint.setUrl("url");
        pickupPoint.setAdditionalUrl("additional_url");
        pickupPoint.setWorkingTime("working_time");
        pickupPoint.setRubricId("rubric_id");
        pickupPoint.setActualizationDate("actualization_date");
        pickupPoint.setUpdateTime(new Date());
        pickupPoint.setEnabled(true);

        return pickupPoint;
    }

    public static HttpEntity<String> buildEntity(String request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);

        return new HttpEntity<>(request, headers);
    }

    public static Set<HashMap<String, String>> makeLocationsSet() {
        HashMap<String, String> map = new HashMap<>();
        map.put("country", "Россия");
        // @todo мы не ищем сейчас по региону, так как не у всех ПВЗ он есть и нужна доработка, чтобы искать правильно
//        map.put("region", "Ярославская область");
        map.put("locality", "город Тутаев");
        Set<HashMap<String, String>> set = new HashSet<>();
        set.add(map);

        return set;
    }
}
