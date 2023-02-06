package ru.yandex.market.wms.timetracker.json;

import java.io.IOException;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.response.EmployeeStatusPlanDetailsRequest;
import ru.yandex.market.wms.timetracker.utils.FileContentUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
@ActiveProfiles("test")
class EmployeeStatusPlanDetailsRequestTest {

    @Autowired
    private JacksonTester<EmployeeStatusPlanDetailsRequest> tester;

    @Test
    public void canSerialize() throws IOException {
        final EmployeeStatusPlanDetailsRequest expected = EmployeeStatusPlanDetailsRequest.builder()
                .status(EmployeeStatus.SHIPPING)
                .putAwayZoneZoneId(1L)
                .hour((short) 12)
                .count(30)
                .build();

        final JsonContent<EmployeeStatusPlanDetailsRequest> content = tester.write(expected);

        assertEquals(
                JsonParser.parseString(
                        FileContentUtils.getFileContent(
                                "json/employee-status-plan-details-request/model.json")),
                JsonParser.parseString(content.getJson()));
    }

    @Test
    public void canDeserialize() throws IOException {
        final EmployeeStatusPlanDetailsRequest expected = EmployeeStatusPlanDetailsRequest.builder()
                .status(EmployeeStatus.SHIPPING)
                .putAwayZoneZoneId(1L)
                .hour((short) 12)
                .count(30)
                .build();

        final EmployeeStatusPlanDetailsRequest result =
                tester.readObject(
                        "/json/employee-status-plan-details-request/model.json");

        assertThat(result, samePropertyValuesAs(expected));
    }

}
