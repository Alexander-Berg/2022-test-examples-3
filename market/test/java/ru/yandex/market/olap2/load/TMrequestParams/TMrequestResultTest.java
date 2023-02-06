package ru.yandex.market.olap2.load.TMrequestParams;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.olap2.model.LoadTaskMetrics;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TMrequestResultTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SneakyThrows
    @Test
    public void mustParseSuccessfulTMResponse() {
        MAPPER.registerModule(new JavaTimeModule());
        TMrequestResult result = MAPPER.readValue(
                this.getClass().getResourceAsStream("/testcase/tm_success.json"),
                new TypeReference<TMrequestResult>() {});
        assertThat(result.getState(), is("completed"));
        assertThat(result.getError(), Matchers.nullValue());
        assertThat(result.getExecution_log(), Matchers.containsString(""));

        TMYTOperation op1 = new TMYTOperation();
        op1.setCluster_name("hahn");
        op1.setId("66bddd52-1743202d-3fe03e8-efa11568");
        op1.setType("yt");

        TMYTOperation op2 = new TMYTOperation();
        op2.setCluster_name("vanga");
        op2.setId("9eafb86e-14ad629f-3f603e8-e0b6a576");
        op2.setType("yt");

        assertThat(result.getProgress().getOperations(), is(Arrays.asList(op1, op2)));
        assertThat(result.getZonedCreationTime(), is(ZonedDateTime.of(
                2020, 11, 3, 9, 55, 8, 223508000, ZoneId.of("UTC"))));
        assertThat(result.getZonedFinishTime(), is(ZonedDateTime.of(
                2020, 11, 3, 9, 59, 46, 725280000, ZoneId.of("UTC"))));
        assertThat(result.getZonedStartTime(), is(ZonedDateTime.of(
                2020, 11, 3, 9, 55, 10, 63251000, ZoneId.of("UTC"))));
        assertThat(TMrequestResult.parsePrepareFinishTime(result), is(ZonedDateTime.of(
                2020, 11, 3, 12, 56, 51, 0, ZoneId.of("Europe/Moscow"))));
    }

    @SneakyThrows
    @Test
    public void mustParseErrorTMResponse() {
        MAPPER.registerModule(new JavaTimeModule());
        TMrequestResult result = MAPPER.readValue(
                this.getClass().getResourceAsStream("/testcase/tm_error.json"),
                new TypeReference<TMrequestResult>() {});
        assertThat(result.getState(), is("failed"));
        assertThat(result.getError().getMessage().length(), Matchers.greaterThan(0));
    }

    @SneakyThrows
    @Test
    public void testTimeZones() {
        MAPPER.registerModule(new JavaTimeModule());
        TMrequestResult result = MAPPER.readValue(
                this.getClass().getResourceAsStream("/testcase/tm_success.json"),
                new TypeReference<TMrequestResult>() {});
        LoadTaskMetrics m = new LoadTaskMetrics();
        m.setStartTime(ZonedDateTime.of(2020, 11, 3, 12, 50, 8, 223508000,
                ZoneId.of("Europe/Moscow")));
        LoadTaskMetrics.setFromTMrequestResult(m, result);
        assertThat(LoadTaskMetrics.secLen(m.getStartTime(), m.getTmCreationTime()), is(5 * 60L));
        assertThat(LoadTaskMetrics.secLen(m.getTmCreationTime(), TMrequestResult.parsePrepareFinishTime(result)),
                is(102L));
    }
}
