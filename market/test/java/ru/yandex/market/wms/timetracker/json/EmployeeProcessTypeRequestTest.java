package ru.yandex.market.wms.timetracker.json;

import java.io.IOException;
import java.time.Instant;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.model.enums.AssigmentType;
import ru.yandex.market.wms.timetracker.model.enums.ProcessType;
import ru.yandex.market.wms.timetracker.response.EmployeeProcessTypeRequest;
import ru.yandex.market.wms.timetracker.utils.FileContentUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
@ActiveProfiles("test")
class EmployeeProcessTypeRequestTest {

    @Autowired
    private JacksonTester<EmployeeProcessTypeRequest> tester;

    @Test
    public void canSerialize() throws IOException {
        final EmployeeProcessTypeRequest expected = EmployeeProcessTypeRequest.builder()
                .processType(ProcessType.PLACEMENT)
                .assigmentType(AssigmentType.SYSTEM)
                .putAwayZoneName(null)
                .assigner("assigner")
                .eventTime(Instant.parse("2021-11-01T00:00:00Z"))
                .expectedEndTime(Instant.parse("2021-11-01T12:00:00Z"))
                .user("test").build();

        final JsonContent<EmployeeProcessTypeRequest> content = tester.write(expected);

        assertEquals(
                JsonParser.parseString(
                        FileContentUtils.getFileContent(
                                "json/employee-process-type-dto/model.json")),
                JsonParser.parseString(content.getJson()));
    }

    @Test
    public void canDeserialize() throws IOException {
        final EmployeeProcessTypeRequest expected = EmployeeProcessTypeRequest.builder()
                .processType(ProcessType.PLACEMENT)
                .assigmentType(AssigmentType.SYSTEM)
                .putAwayZoneName(null)
                .assigner("assigner")
                .eventTime(Instant.parse("2021-11-01T00:00:00Z"))
                .expectedEndTime(Instant.parse("2021-11-01T12:00:00Z"))
                .user("test").build();

        final EmployeeProcessTypeRequest result =
                tester.readObject(
                        "/json/employee-process-type-dto/model.json");

        assertThat(result, samePropertyValuesAs(expected));
    }
}
