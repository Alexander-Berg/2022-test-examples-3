package ru.yandex.market.wms.timetracker.json;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.response.EmployeeCountByStatusHistoryResponse;
import ru.yandex.market.wms.timetracker.response.EmployeeCountByStatusResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
@ActiveProfiles("test")
class EmployeeCountByStatusHistoryResponseTest {

    @Autowired
    private ObjectMapper mapper;

    @Test
    void canSerialize() throws JsonProcessingException {

        final List<EmployeeCountByStatusHistoryResponse> result = List.of(EmployeeCountByStatusHistoryResponse.builder()
                .period(LocalDateTime.of(2021, 12, 17, 15, 0))
                .data(List.of(
                        EmployeeCountByStatusResponse.builder()
                                .status(EmployeeStatus.SHIPPING)
                                .plan(10)
                                .fact(15)
                                .build(),
                        EmployeeCountByStatusResponse.builder()
                                .status(EmployeeStatus.CONSOLIDATION)
                                .plan(100)
                                .fact(150)
                                .build()
                ))
                .build(),
                EmployeeCountByStatusHistoryResponse.builder()
                        .period(LocalDateTime.of(2021, 12, 17, 16, 0))
                        .data(List.of(
                                EmployeeCountByStatusResponse.builder()
                                        .status(EmployeeStatus.SHIPPING)
                                        .plan(10)
                                        .fact(15)
                                        .build(),
                                EmployeeCountByStatusResponse.builder()
                                        .status(EmployeeStatus.CONSOLIDATION)
                                        .plan(100)
                                        .fact(150)
                                        .build()
                        ))
                        .build());

        final String expected = "[" +
                "{" +
                "\"period\" : \"2021-12-17T15:00:00\", " +
                "\"data\" : [" +
                "    {" +
                "        \"status\": \"SHIPPING\"," +
                "        \"child\": []," +
                "        \"plan\": 10," +
                "        \"fact\": 15" +
                "    }," +
                "    {" +
                "        \"status\": \"CONSOLIDATION\"," +
                "        \"child\": []," +
                "        \"plan\": 100," +
                "        \"fact\": 150" +
                "    }" +
                "]" +
                "}," +
                "{" +
                "\"period\" : \"2021-12-17T16:00:00\", " +
                "\"data\" : [" +
                "    {" +
                "        \"status\": \"SHIPPING\"," +
                "        \"child\": []," +
                "        \"plan\": 10," +
                "        \"fact\": 15" +
                "    }," +
                "    {" +
                "        \"status\": \"CONSOLIDATION\"," +
                "        \"child\": []," +
                "        \"plan\": 100," +
                "        \"fact\": 150" +
                "    }" +
                "]" +
                "}" +
                "]";

        assertEquals(
                JsonParser.parseString(expected),
                JsonParser.parseString(mapper.writeValueAsString(result)));
    }
}
