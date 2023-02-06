package ru.yandex.market.logistics.iris.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MeasurementApiControllerTest extends AbstractContextualTest {

    private final static String PUT_POSITIVE_VERDICT_URL = "/measurement-api/put-positive-verdict";
    private final static String PROBABILITY_INFO_URL = "/measurement-api/get-probability-info";

    /**
     * Тест на успешное сохранение вердикта по 2-м айтемам
     */
    @Test
    @ExpectedDatabase(
            value = "classpath:fixtures/expected/measurement_api_controller/2.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSaveTwoEvents() throws Exception {
        httpOperationWithResult(
                post(PUT_POSITIVE_VERDICT_URL)
                        .content(extractFileContent(
                                "fixtures/controller/request/measurement_api/put_positive_verdict/1.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().isOk());
    }

    /**
     * Тест на сохранение пустого списка айтемов
     */
    @Test
    @ExpectedDatabase(
            value = "classpath:fixtures/expected/measurement_api_controller/2.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotSaveTwoEvents() throws Exception {
        httpOperationWithResult(
                post(PUT_POSITIVE_VERDICT_URL)
                        .content(extractFileContent(
                                "fixtures/controller/request/measurement_api/put_positive_verdict/2.json"))
                        .contentType(MediaType.APPLICATION_JSON),
                status().isBadRequest());
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/measurement/probability_info/1.xml")
    public void shouldSuccessReturnProbability() throws Exception {
        httpOperationWithResult(
                post(PROBABILITY_INFO_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(extractFileContent(
                                "fixtures/controller/request/measurement_api/get_probability_info/1.json")),
                status().isOk(),
                content().json(extractFileContent(
                        "fixtures/controller/response/measurement_api/1.json")));
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/measurement/probability_info/2.xml")
    public void shouldReturnProbabilityOnlyForExistingSku() throws Exception {
        httpOperationWithResult(
                post(PROBABILITY_INFO_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(extractFileContent(
                                "fixtures/controller/request/measurement_api/get_probability_info/2.json")),
                status().isOk(),
                content().json(extractFileContent(
                        "fixtures/controller/response/measurement_api/2.json")));
    }
}
