package ru.yandex.market.wms.timetracker.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.model.enums.OperationThreshold;
import ru.yandex.market.wms.timetracker.response.EmployeeStatusResponse;
import ru.yandex.market.wms.timetracker.utils.FileContentUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
@ActiveProfiles("test")
class TimexStatusResponseTest {

    @Autowired
    private JacksonTester<EmployeeStatusResponse> tester;

    @Test
    public void canSerialize() throws IOException {
        final EmployeeStatusResponse expected = EmployeeStatusResponse.builder()
                .userName("sof-test")
                .status(EmployeeStatus.PLACEMENT)
                .assignStatus(EmployeeStatus.SHIPPING)
                .lastUpdatedTs(LocalDateTime.parse("2021-11-12T12:00:00"))
                .assigner("assigner-test")
                .finishTs(LocalDateTime.parse("2021-11-12T15:00:00"))
                .threshold(OperationThreshold.BAD)
                .area("area")
                .overall(BigDecimal.valueOf(0.8))
                .company("company")
                .newbie(false)
                .build();

        final JsonContent<EmployeeStatusResponse> content = tester.write(expected);

        assertEquals(
                JsonParser.parseString(
                        FileContentUtils.getFileContent("json/employee-status-response/model.json")),
                JsonParser.parseString(content.getJson()));
    }

}
