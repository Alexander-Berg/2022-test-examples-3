package ru.yandex.market.wms.timetracker.json;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.dto.AssignTaskRequest;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.utils.FileContentUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
@ActiveProfiles("test")
class AssignTaskRequestTest {

    @Autowired
    private JacksonTester<AssignTaskRequest> tester;

    @Test
    void canDeserialize() throws IOException {
        final AssignTaskRequest expected = AssignTaskRequest.builder()
                .assigner("assigner")
                .duration(15L)
                .status(EmployeeStatus.SHIPPING)
                .userNames(List.of("test1", "test2"))
                .build();

        final AssignTaskRequest result =
                tester.readObject("/json/assign-task-request/model.json");

        assertThat(result, samePropertyValuesAs(expected));
    }

    @Test
    void canSerialize() throws IOException {
        final AssignTaskRequest expected = AssignTaskRequest.builder()
                .assigner("assigner")
                .duration(15L)
                .status(EmployeeStatus.SHIPPING)
                .userNames(List.of("test1", "test2"))
                .build();

        final JsonContent<AssignTaskRequest> content = tester.write(expected);

        assertEquals(
                JsonParser.parseString(
                        FileContentUtils.getFileContent("json/assign-task-request/model.json")),
                JsonParser.parseString(content.getJson()));
    }

}
