package ru.yandex.market.delivery.mdbapp.integration.converter.delivery;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import steps.LocationSteps;
import steps.logisticsPointSteps.LogisticPointSteps;
import steps.logisticsPointSteps.PhoneNumberSteps;

import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPoint;
import ru.yandex.market.delivery.mdbapp.integration.payload.PhoneNumber;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.Warehouse;

public class LgwDsWarehouseConverterTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void convertPhones() {
        LogisticsPoint logisticsPoint = createOutletWithPhones();
        Location location = LocationSteps.getLocation();

        Warehouse warehouse = LgwDsWarehouseConverter.convertWarehouse(logisticsPoint, location, null);

        softly.assertThat(warehouse.getPhones()).containsExactlyInAnyOrder(
            new Phone("+74954567890", "123"),
            new Phone("+74954567890", "123"),
            new Phone("+78001112233", null),
            new Phone("+79006665544", null)
        );
    }

    private LogisticsPoint createOutletWithPhones() {
        LogisticsPoint outlet = LogisticPointSteps.getDefaultOutlet();

        return new LogisticsPoint(
            outlet.getId(),
            outlet.getDeliveryServiceOutletCode(),
            outlet.getAddress(),
            outlet.getGeoInfo(),
            createPhoneNumbers(),
            outlet.getScheduleLines()
        );
    }

    private List<PhoneNumber> createPhoneNumbers() {
        List<PhoneNumber> result = new ArrayList<>();
        result.addAll(PhoneNumberSteps.getPhoneNumber());
        result.addAll(PhoneNumberSteps.getInletPhoneNumber());
        result.add(new PhoneNumber(
            "+7",
            null,
            "78001112233",
            null
        ));
        result.add(new PhoneNumber(
            null,
            null,
            "79006665544",
            null
        ));
        result.add(new PhoneNumber(
            "+7",
            "495",
            null,
            null
        ));

        return result;
    }
}
