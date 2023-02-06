package ru.yandex.market.wrap.infor.functional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenContextHolder;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundsStatusResponse;
import ru.yandex.market.wrap.infor.service.outbound.OutboundsStatusService;

class GetOutboundsStatusTest extends AbstractFunctionalTest {

    @Autowired
    private OutboundsStatusService outboundsStatusService;

    @Autowired
    private TokenContextHolder tokenContextHolder;

    @BeforeEach
    void setUp() {
        tokenContextHolder.setToken("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    }

    /**
     * Сценарий #1:
     * <p>
     * Запрашиваем актуальный статус для 1 идентификатора.
     * Информация по этому идентификатору отсутствует в БД.
     * <p>
     * В ответ должно вернуться тело с пустым набором статусов.
     */
    @Test
    void noStatusesAvailableForSingleId() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbounds_status/1/request.xml",
            "fixtures/functional/get_outbounds_status/1/response.xml"
        );
    }

    /**
     * Сценарий #2:
     * <p>
     * Запрашиваем актуальный статус для 1 идентификатора.
     * По этому идентификатору присутствует ровно 1 статус в БД.
     * <p>
     * В ответ должна вернуться информация со статусом из БД.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_outbounds_status/2/state.xml",
        connection = "wmsConnection"
    )
    void singleStatusAvailableForSingleId() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbounds_status/2/request.xml",
            "fixtures/functional/get_outbounds_status/2/response.xml"
        );
    }


    /**
     * Сценарий #3:
     * <p>
     * Запрашиваем актуальный статус для 1 идентификатора.
     * По этому идентификатору присутствует более 1 статуса в БД.
     * <p>
     * В ответ должна вернуться информация с самым актуальным из статусов из БД.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_outbounds_status/3/state.xml",
        connection = "wmsConnection"
    )
    void multipleStatusesAvailableForSingleId() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbounds_status/3/request.xml",
            "fixtures/functional/get_outbounds_status/3/response.xml"
        );
    }

    /**
     * Сценарий #4:
     * <p>
     * Запрашиваем актуальные статусы по множеству идентификаторов.
     * В БД присутствует ровно по 1 статусу для каждого из них.
     * <p>
     * В ответ должны вернуться соответствующие статусы из бд.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_outbounds_status/4/state.xml",
        connection = "wmsConnection"
    )
    void singleStatusAvailableForMultipleIds() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbounds_status/4/request.xml",
            "fixtures/functional/get_outbounds_status/4/response.xml"
        );
    }

    /**
     * Сценарий #5:
     * <p>
     * Запрашиваем актуальные статусы по множеству идентификаторов.
     * Информация по этому набору идентификаторов отсутствует в БД.
     * <p>
     * В ответ должен вернуться пустой ответ.
     */
    @Test
    void noStatusesAvailableForMultiple() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbounds_status/5/request.xml",
            "fixtures/functional/get_outbounds_status/5/response.xml"
        );
    }

    /**
     * Сценарий #6:
     * <p>
     * Запрашиваем актуальные статусы по множеству идентификаторов.
     * В БД присутствует ровно по несколько статусов для каждого из них.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_outbounds_status/6/state.xml",
        connection = "wmsConnection"
    )
    void multipleStatusesAvailableForMultipleIds() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbounds_status/6/request.xml",
            "fixtures/functional/get_outbounds_status/6/response.xml"
        );
    }

    /**
     * Сценарий #7:
     * <p>
     * Запрашиваем актуальные статусы по множеству идентификаторов,
     * размер которого превышает лимит в 100 элементов.
     */
    @Test
    void tooLongInputListOfIds() {
        final int tooLargeElementsSize = 101;
        List<ResourceId> input = IntStream.range(0, tooLargeElementsSize)
            .mapToObj(it -> new ResourceId("yandexId" + it, "partnerId" + it))
            .collect(Collectors.toList());

        Assertions.assertThatThrownBy(() -> outboundsStatusService.getOutboundsStatus(input))
            .isInstanceOf(FulfillmentApiException.class);
    }

    /**
     * Сценарий #8:
     * <p>
     * Запрашиваем актуальный статус для 1 идентификатора.
     * По этому идентификатору присутствует более 1 статуса в БД.
     * При этом сортировка по serialKey и по addDate отличается,
     * то есть для максимального addDate минимальный serialKey.
     * <p>
     * В ответ должна вернуться информация с самым актуальным из статусов (по addDate) из БД.
     */
    @Test
    @DatabaseSetup(
            value = "classpath:fixtures/functional/get_outbounds_status/8/state.xml",
            connection = "wmsConnection"
    )
    void multipleStatusesAvailableForSingleIdWithAddDateAndSerialKeySortDifferent() throws Exception {
        executeScenario(
                "fixtures/functional/get_outbounds_status/8/request.xml",
                "fixtures/functional/get_outbounds_status/8/response.xml"
        );
    }

    private void executeScenario(String wrapRequest, String expectedWrapResponse) throws Exception {
        FunctionalTestScenarioBuilder.start(GetOutboundsStatusResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

}
