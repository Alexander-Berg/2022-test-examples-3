package ru.yandex.market.wrap.infor.functional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundHistoryResponse;


class GetInboundHistoryTest extends AbstractFunctionalTest {

    /**
     * Сценарий #1:
     * <p>Получаем список статусов по partnerId</p>
     * <p>
     * В БД статусы с различными partnerId. Для искомого partnerId в БД содержится 3 различных статуса,
     * которые должны получить в response с соответствующим маппингом кодов статусов.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/multipartner_history.xml",
        connection = "wmsConnection"
    )
    void withDifferentStatuses() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/1/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/1/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #2:
     * <p>Пытаемся получить историю по partnerId, для которого в БД соответсвует единственная запись</p>
     * <p>
     * В ответе должен быть список с одним найденным статусом.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/multipartner_history.xml",
        connection = "wmsConnection"
    )
    void withSingleStatus() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/2/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/2/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #3:
     * <p>Попытка получить историю для несуществующего partnerId</p>
     * <p>
     * В ответе проверяем, что возвращена ошибка 9999.
     * TODO: возвращать 9404.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/multipartner_history.xml",
        connection = "wmsConnection"
    )
    void withUnknownPartnerId() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/3/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/3/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #4:
     * <p>Проверка маппинга всех статусов из infor в fulfillment</p>
     * <p>
     * В БД записи для одного partnerId со всеми возможными статусами Infor'a.
     * Проверяем в ответе валидный маппинг кодов статусов.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/singlepartner_history_with_all_statuses.xml",
        connection = "wmsConnection"
    )
    void withAllStatuses() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/4/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/4/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #5:
     * <p>Попытка обработать неизвестный статутс ошибки от Infor</p>
     * <p>
     * В БД статус с неподдерживаемым кодом.
     * Проверяем, что в ответе будет статус UNKNOWN(-1).
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/history_with_invalid_status.xml",
        connection = "wmsConnection"
    )
    void withInvalidStatus() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/5/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/5/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #6:
     * <p>Попытка обработать request без тега partnerId</p>
     * <p>
     * Проверяем, что отдана ошибка 9400.
     */
    @Test
    void withRequestThatNotContainsPartnerId() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/6/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/6/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #7:
     * <p>Попытка обработать request без тега yandexId</p>
     * <p>
     * Проверяем, что отдана ошибка 9400.
     */
    @Test
    void withRequestThatNotContainsYandexId() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/7/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/7/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }


    /**
     * Сценарий #8:
     * <p>Попытка обработать request с пустым partnerId</p>
     * <p>
     * Проверяем, что отдана ошибка 9400.
     */
    @Test
    void withRequestThatContainsEmptyPartnerId() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/8/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/8/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #9:
     * <p>Попытка обработать request с пустым yandexId</p>
     * <p>
     * Проверяем, что отдана ошибка 9400.
     */
    @Test
    void withRequestThatContainsEmptyYandexId() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/9/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/9/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #10:
     * <p>Получаем список статусов по partnerId cо статусом прибытия (20 - ARRIVED)</p>
     * <p>
     * В БД заданы статусы с различными partnerId и трейлеры для каждой поставки.
     * Для искомого partnerId в response с должны получить 4 статуса.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/multipartner_history.xml",
        connection = "wmsConnection"
    )
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/trailer_statuses.xml",
        connection = "wmsConnection"
    )
    void withReceiptStatusesAndTrailerStatuses() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/10/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/10/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #11:
     * <p>Получаем список статусов по partnerId cо статусом прибытия (20 - ARRIVED),
     * который является последним.</p>
     * <p>
     * В БД заданы статусы с различными partnerId и трейлер для искомой поставки.
     * Для искомого partnerId в response с должны получить 2 статуса, первый из которых - 20-ый.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/multipartner_history.xml",
        connection = "wmsConnection"
    )
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/11/trailer_statuses.xml",
        connection = "wmsConnection"
    )
    void withReceiptStatusesAndTrailerStatusWhichIsTheLastOne() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/11/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/11/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #12:
     * <p>Получаем список статусов по partnerId cо статусом прибытия (20 - ARRIVED),
     * который является первым (проверяем валидность сортировки статусов по дате)</p>
     * <p>
     * <p>
     * В БД заданы статусы с различными partnerId и трейлер для искомой поставки.
     * Для искомого partnerId в response с должны получить 2 статуса, последний из которых - 20-ый.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/multipartner_history.xml",
        connection = "wmsConnection"
    )
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/12/trailer_statuses.xml",
        connection = "wmsConnection"
    )
    void withReceiptStatusesAndTrailerStatusWhichIsTheFirstOne() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/12/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/12/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #13:
     * <p>Проверяем, что неправильный статус трейлера не будет отражен в response.</p>
     * <p>
     * В БД заданы статусы с различными partnerId и трейлеры для каждой поставки.
     * Для искомой поставки статус трейлера отличный 5COMP.
     * Для искомого partnerId в response должны получить 3 статуса, в котором нет ни одного с кодом 20.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/multipartner_history.xml",
        connection = "wmsConnection"
    )
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/trailer_statuses.xml",
        connection = "wmsConnection"
    )
    void withReceiptStatusesAndUnmappableTrailerStatus() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/13/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/13/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #14:
     * <p>Проверяем, что неизвестный статус трейлера не будет отражен в response.</p>
     * <p>
     * В БД заданы статусы с различными partnerId и трейлеры для каждой поставки.
     * Для искомой поставки у трейлера указан неизвестный статутс.
     * Для искомого partnerId в response должен быть возвращен 1 статус,
     * в котором нет ни одного с кодом 20.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/multipartner_history.xml",
        connection = "wmsConnection"
    )
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/trailer_statuses.xml",
        connection = "wmsConnection"
    )
    void withReceiptStatusesAndUnknownTrailerStatus() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/14/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/14/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #15:
     * <p>Проверка работы схлопывания всех статусов в случае идущих подряд повторяющихся статусов</p>
     * Проверяем в ответе отсутствие дубликатов идущих подряд.
     * Могут добавляться трейлерные статусы из {@link ru.yandex.market.wrap.infor.entity.TrailerStatusType} -
     * они не сквошатся вместе с текущим набором статусов.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/singlepartner_history_with_statuses_flap.xml",
        connection = "wmsConnection"
    )
    void withReceiptStatusPalletAcceptance() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/15/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/15/wrap_response.xml";

        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    /**
     * Сценарий #16:
     * <p>Проверка того, что мы не смотрим статусы {@link ru.yandex.market.wrap.infor.entity.TrailerStatusType}
     * в случае, если присутствует {@link ru.yandex.market.wrap.infor.entity.ReceiptStatusType#PALLET_ACCEPTANCE}.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/16/singlepartner_history_with_arrived_status.xml",
        connection = "wmsConnection"
    )

    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_inbound_history/16/trailer_statuses.xml",
        connection = "wmsConnection"
    )
    void palletAcceptanceWithoutTrailerStatusEnriching() throws Exception {
        String wrapRequest = "fixtures/functional/get_inbound_history/16/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_inbound_history/16/wrap_response.xml";
        assertEqualScenario(wrapRequest, expectedWrapResponse);
    }

    private void assertEqualScenario(String wrapRequest, String expectedWrapResponse) throws Exception {
        FunctionalTestScenarioBuilder.start(GetInboundHistoryResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }
}
