package ru.yandex.market.mbi.api.controller.reports;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.asyncreport.model.ReportRequest;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.param.model.EntityName;
import ru.yandex.market.mbi.api.client.entity.GenericCallResponse;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.asyncreport.ReportInfoDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportsControllerTest extends FunctionalTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void beforeAll() {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @DisplayName("Проверяем, что в бд создается заявка на генерацию отчёта.")
    @DbUnitDataSet(
            before = "ReportsControllerTest.generateAndGet.before.csv",
            after = "ReportsControllerTest.generateAndGet.after.csv"
    )
    void generate() throws IOException {
        ReportRequest.RequestReportInfoBuilder<ReportsType> requestBuilder = ReportRequest.<ReportsType>builder()
                .setReportType(ReportsType.STOCKS_ON_WAREHOUSES)
                .setEntityId(1111L)
                .setEntityName(EntityName.PARTNER)
                .setParams(Map.of("dateTimeFrom", "2019-09-17T03:00:00+03:00",
                        "dateTimeTo", "2019-09-19T03:00:00+03:00",
                        "useTestingVersion", true));

        ReportInfoDTO actual = mbiApiClient.requestReportGeneration(requestBuilder.build());

            String expected = JsonTestUtil.fromJsonTemplate(getClass(), "report_task_created.json")
                .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                .toString();
        JsonTestUtil.assertEquals(expected, MAPPER.writeValueAsString(actual));
    }

    @Test
    @DisplayName("Проверяем, что получаем инфо об отчете по id.")
    @DbUnitDataSet(
            before = "ReportsControllerTest.generateAndGet.before.csv",
            after = "ReportsControllerTest.generateAndGet.before.csv"
    )
    void get() throws JsonProcessingException {
        ReportInfoDTO reportInfo = mbiApiClient.getReportInfo("33");
        JsonTestUtil.assertEquals(
                MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(reportInfo),
                JsonTestUtil.fromJsonTemplate(getClass(), "get_report_info.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем, отчет с указанным id отменен.")
    @DbUnitDataSet(
            before = "ReportsControllerTest.cancel.before.csv",
            after = "ReportsControllerTest.cancel.after.csv"
    )
    void delete() {
        GenericCallResponse response = mbiApiClient.cancelReport("22");
        assertEquals(GenericCallResponse.ok(), response);
    }
}
