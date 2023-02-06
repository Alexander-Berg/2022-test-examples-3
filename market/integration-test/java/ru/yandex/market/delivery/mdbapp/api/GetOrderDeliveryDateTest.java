package ru.yandex.market.delivery.mdbapp.api;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.logging.json.GetOrdersDeliveryDateResultLogger;
import ru.yandex.market.delivery.mdbclient.model.delivery.OrderDeliveryDate;
import ru.yandex.market.delivery.mdbclient.model.delivery.ResourceId;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrdersDeliveryDateError;
import ru.yandex.market.delivery.mdbclient.model.request.GetOrdersDeliveryDateResult;

import static org.mockito.Mockito.when;

public class GetOrderDeliveryDateTest extends MockContextualTest {
    private final ObjectMapper om = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    File logsDir;
    File logFile;
    File errLogFile;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthManager healthManager;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();
    private OffsetDateTime deliveryDate = OffsetDateTime.of(
        2020, 7, 14, 10, 0, 0, 0, ZoneOffset.ofHours(5));


    @Before
    public void setUp() {
        logsDir = new File(loggerContext.getProperty("LOG_PATH"));
        logFile = FileUtils.getFile(logsDir.getAbsoluteFile(), "json-reports", "godd-result-log-json.log");
        errLogFile = FileUtils.getFile(logsDir.getAbsoluteFile(), "json-reports", "godd-error-log-json.log");
        logFile.delete();
        errLogFile.delete();
        when(healthManager.isHealthyEnough()).thenReturn(true);
    }

    @Test
    public void deliveryDateSuccessLogging() throws Exception {
        OrderDeliveryDate dd1 = createOrderDeliveryDate("1", "1", "aaa");
        OrderDeliveryDate dd2 = createOrderDeliveryDate("2", "1", "bbb");
        OrderDeliveryDate dd3 = createOrderDeliveryDate("3", "2", "ccc");

        mockMvc.perform(MockMvcRequestBuilders
            .post("/orders/getOrdersDeliveryDateSuccess", 1)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(om.writeValueAsString(new GetOrdersDeliveryDateResult("1", 1234L, List.of(dd1, dd2)))))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        mockMvc.perform(MockMvcRequestBuilders
            .post("/orders/getOrdersDeliveryDateSuccess", 1)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(om.writeValueAsString(new GetOrdersDeliveryDateResult("1", 1234L, List.of(dd3)))))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());


        try (Reader r = new FileReader(logFile)) {
            List<String> lines = IOUtils.readLines(r);
            softly.assertThat(lines.size()).isEqualTo(3);

            softly.assertThat(om.readValue(lines.get(0), GetOrdersDeliveryDateResultLogger.OrderDeliveryDateDto.class))
                .isEqualTo(new GetOrdersDeliveryDateResultLogger.OrderDeliveryDateDto("1", 1234L, dd1, 0L));
            softly.assertThat(om.readValue(lines.get(1), GetOrdersDeliveryDateResultLogger.OrderDeliveryDateDto.class))
                .isEqualTo(new GetOrdersDeliveryDateResultLogger.OrderDeliveryDateDto("1", 1234L, dd2, 0L));
            softly.assertThat(om.readValue(lines.get(2), GetOrdersDeliveryDateResultLogger.OrderDeliveryDateDto.class))
                .isEqualTo(new GetOrdersDeliveryDateResultLogger.OrderDeliveryDateDto("1", 1234L, dd3, 0L));
        }
    }

    @Test
    public void deliveryDateErrorLogging() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders
            .post("/orders/getOrdersDeliveryDateError", 1)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(om.writeValueAsString(new GetOrdersDeliveryDateError(
                "1",
                1234L,
                List.of(
                    new ResourceId("1", "1")
                ),
                "Error msg"))))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        try (Reader r = new FileReader(errLogFile)) {
            List<String> lines = IOUtils.readLines(r);
            softly.assertThat(lines.size()).isEqualTo(1);
            Map<String, Object> err = om.readValue(lines.get(0), new TypeReference<Map<String, Object>>() {
            });
            softly.assertThat(err.get("deliveryServiceId")).isEqualTo(1234);
            softly.assertThat(err.get("deliveryId")).isEqualTo("1");
            softly.assertThat(err.get("yandexId")).isEqualTo("1");
            softly.assertThat(err.get("error")).isEqualTo("Error msg");
        }
    }

    @NotNull
    private OrderDeliveryDate createOrderDeliveryDate(String yandexId, String deliveryId, String description) {
        return new OrderDeliveryDate(
            new ResourceId(yandexId, deliveryId),
            deliveryDate,
            deliveryDate.toOffsetTime(),
            deliveryDate.toOffsetTime(),
            description);
    }
}
