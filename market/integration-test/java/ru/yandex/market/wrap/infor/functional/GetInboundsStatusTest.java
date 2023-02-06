package ru.yandex.market.wrap.infor.functional;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenContextHolder;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundsStatusResponse;
import ru.yandex.market.wrap.infor.service.inbound.InboundsStatusService;

class GetInboundsStatusTest extends AbstractFunctionalTest {

    @Autowired
    private InboundsStatusService inboundsStatusService;

    @Autowired
    private TokenContextHolder tokenContextHolder;

    @BeforeEach
    void setUp() {
        tokenContextHolder.setToken("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    }

    /**
     * Сценарий #1:
     * <p>Получаем статусы по списку partnerId</p>
     * <p>
     * В БД статусы с различными partnerId.
     * Для кажого partnerId из request возвращает последний статус по дате.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbounds_status/multipartner_history.xml",
        connection = "wmsConnection"
    )
    void withValidListOfPartnerIds() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbounds_status/1/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbounds_status/1/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #2:
     * <p>Получаем статусы по списку, содержащему только один partnerId</p>
     * <p>
     * В БД статусы с различными partnerId.
     * Получаем в response спсиок из одного элемента и
     * проверяем статус для единственного заданного partnerId.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbounds_status/multipartner_history.xml",
        connection = "wmsConnection"
    )
    void withSingleItemListOfPartnerIds() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbounds_status/2/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbounds_status/2/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #3:
     * <p>
     * Получаем статусы по списку partnerId,
     * который содержит как существующие id в БД, так и не существующие
     * </p>
     * <p>
     * В БД статусы с различными partnerId.
     * Проверяем, что возрващаются только те статусы, которые соответствуют существующим в БД partnerId.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbounds_status/multipartner_history.xml",
        connection = "wmsConnection"
    )
    void withListOfPartnerIdsThatContainsInvalidIds() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbounds_status/3/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbounds_status/3/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #4:
     * <p>
     * Получаем статусы по списку partnerId, который содержит дубликаты.
     * </p>
     * <p>
     * В БД статусы с различными partnerId.
     * Проверяем, что возрващаются статусы для partnerId's без дубликатов.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbounds_status/multipartner_history.xml",
        connection = "wmsConnection"
    )
    void withListOfPartnerIdsThatContainsDuplicates() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbounds_status/4/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbounds_status/4/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #5:
     * <p>
     * Получаем статусы по списку partnerId, который содержит только несуществующие id в БД.
     * </p>
     * <p>
     * В БД статусы с различными partnerId.
     * Проверяем, что ничего не найдено и возрващаются пустой список.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbounds_status/multipartner_history.xml",
        connection = "wmsConnection"
    )
    void withListOfInvalidPartnerIds() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbounds_status/5/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbounds_status/5/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }


    /**
     * Сценарий #6:
     * <p>
     * Пытаемся получить статусы по списку с пустым partnerId.
     * <p>
     * Проверяем, что отдана ошибка 9400..
     */
    @Test
    void withEmptyInboundId() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbounds_status/6/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbounds_status/6/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #7:
     * <p>
     * Пытаемся получить статусы по списку с отсутсвующими partnerId.
     * <p>
     * Проверяем, что отдана ошибка 9400.
     */
    @Test
    void withMissingPartnerId() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbounds_status/7/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbounds_status/7/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #8:
     * <p>
     * Пытаемся получить статусы по списку с отсутсвующими yandexId.
     * <p>
     * Проверяем, что отдана ошибка 9400.
     */
    @Test
    void withMissingYandexId() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbounds_status/8/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbounds_status/8/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #9:
     * <p>
     * Пытаемся получить статусы по списку с пустыми partnerId.
     * <p>
     * Проверяем, что отдана ошибка 9400.
     */
    @Test
    void withEmptyPartnerId() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbounds_status/9/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbounds_status/9/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #10:
     * <p>
     * Пытаемся получить статусы по списку с пустым yandexId.
     * <p>
     * В БД статусы с различными partnerId.
     * Проверяем, что отдана ошибка 9400.
     */
    @Test
    void withEmptyYandexId() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbounds_status/10/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbounds_status/10/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #11:
     * <p>Проверяем ограничение в 100 на длину входного списка.</p>
     */
    @Test
    void testScenarioOnInputListWithInvalidSize() {

        final int tooLargeElementsSize = 101;
        List<ResourceId> input = Collections.nCopies(tooLargeElementsSize, new ResourceId("1", "1"));

        Assertions.assertThatThrownBy(() -> inboundsStatusService.getInboundsStatus(input))
            .isInstanceOf(FulfillmentApiException.class);
    }

    /**
     * Сценарий #12:
     * <p>Получаем статусы по списку partnerId c учетом статусов трейлеров.</p>
     * <p>
     * В БД статусы с различными partnerId.
     * Для первой поставки - 20-ый статус будет возвращен как последний.
     * Для второй поставки - статус трейлера невалидный, поэтому не будет учитываться в response.
     * Для третьей поставки - статус трейлера неизвестный, поэтому не будет учитываться в response.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbounds_status/12/history.xml",
        connection = "wmsConnection"
    )
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbounds_status/12/trailer_statuses.xml",
        connection = "wmsConnection"
    )
    void withValidListOfPartnerIdsAndInvalidSettings() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbounds_status/12/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbounds_status/12/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }


    /**
     * Проверяет, что из истории статусов по каждому статусу будет забираться только один последний самый ранный.
     * Например в последовательности: 0, 6, 5(1), 5(2), 6 должен отобраться 5(1).
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbounds_status/multipartner_history.xml",
        connection = "wmsConnection"
    )
    void withListOfPartnerIdsAndDuplicatesAndIncorrectHistory() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbounds_status/13/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbounds_status/13/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }


    private void assertEqualScenario(String wrapRequest, String expectedWrapResponse) throws Exception {
        FunctionalTestScenarioBuilder.start(GetInboundsStatusResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }
}
