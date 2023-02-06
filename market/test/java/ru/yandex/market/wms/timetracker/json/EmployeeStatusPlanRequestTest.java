package ru.yandex.market.wms.timetracker.json;

import java.io.IOException;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.dto.EmployeeStatusPlanRequest;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.utils.FileContentUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
@ActiveProfiles("test")
class EmployeeStatusPlanRequestTest {

    @Autowired
    private JacksonTester<EmployeeStatusPlanRequest> tester;

    @Test
    public void canSerialize() throws IOException {
        final EmployeeStatusPlanRequest expected = EmployeeStatusPlanRequest.builder()
                .status(EmployeeStatus.SHIPPING)
                .count(10L).build();

        final JsonContent<EmployeeStatusPlanRequest> content = tester.write(expected);

        assertEquals(
                JsonParser.parseString(
                        FileContentUtils.getFileContent(
                                "json/employee-status-plan-request/model.json")),
                JsonParser.parseString(content.getJson()));
    }

    @Test
    public void canDeserialize() throws IOException {
        final EmployeeStatusPlanRequest expected = EmployeeStatusPlanRequest.builder()
                .status(EmployeeStatus.SHIPPING)
                .count(10L).build();

        final EmployeeStatusPlanRequest result =
                tester.readObject(
                        "/json/employee-status-plan-request/model.json");

        assertThat(result, samePropertyValuesAs(expected));
    }

}
