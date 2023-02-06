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

import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.response.EmployeeCountByStatusResponse;
import ru.yandex.market.wms.timetracker.utils.FileContentUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
@ActiveProfiles("test")
class EmployeeCountByStatusResponseTest {

    @Autowired
    private JacksonTester<List<EmployeeCountByStatusResponse>> tester;

    @Test
    public void canSerialize() throws IOException {
        final List<EmployeeCountByStatusResponse> expected = List.of(
                EmployeeCountByStatusResponse.builder()
                        .status(EmployeeStatus.PLACEMENT)
                        .plan(13)
                        .fact(10)
                        .build(),
                EmployeeCountByStatusResponse.builder()
                        .status(EmployeeStatus.INVENTORIZATION)
                        .plan(816)
                        .fact(600)
                        .build()
        );

        final JsonContent<List<EmployeeCountByStatusResponse>> content = tester.write(expected);

        assertEquals(
                JsonParser.parseString(
                        FileContentUtils.getFileContent(
                                "json/employee-count-by-status-response/model.json")),
                JsonParser.parseString(content.getJson()));
    }
}
