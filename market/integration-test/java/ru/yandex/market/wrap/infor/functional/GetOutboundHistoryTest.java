package ru.yandex.market.wrap.infor.functional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundHistoryResponse;

class GetOutboundHistoryTest extends AbstractFunctionalTest {


    /**
     * Сценарий #1:
     * <p>
     * В теле запроса отсутствует orderId.partnerId.
     * <p>
     * Ожидаем получить ответ с ошибкой 9400 BAD REQUEST и сообщением об отсутствующем partnerId.
     */
    @Test
    void noPartnerIdAvailable() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_history/1/request.xml",
            "fixtures/functional/get_outbound_history/1/response.xml"
        );
    }

    /**
     * Сценарий #2:
     * <p>
     * В БД отсутствует информация по статусам заказа с идентификатором из запроса.
     * <p>
     * В ответ должна вернуться ошибка с информацией о том, что нам не удалось найти заказ с указанным ID.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_outbound_history/2/state.xml",
        connection = "wmsConnection"
    )
    void noHistoryAvailable() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_history/2/request.xml",
            "fixtures/functional/get_outbound_history/2/response.xml"
        );
    }


    /**
     * Сценарий #3:
     * В БД для указанного заказа присутствует только 1 статус.
     * В ответ должна вернуться история, состоящая из 1 статуса.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_outbound_history/3/state.xml",
        connection = "wmsConnection"
    )
    void singleStatusesHistory() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_history/3/request.xml",
            "fixtures/functional/get_outbound_history/3/response.xml"
        );
    }

    /**
     * Сценарий #4:
     * В БД для указанного заказа присутствует полноценная история с множеством статусов.
     * <p>
     * В ответ должна вернуться эта история, но с сортировкой по дате статуса по убыванию
     * и со схлопыванием статусов.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_outbound_history/4/state.xml",
        connection = "wmsConnection"
    )
    void multipleStatusesHistory() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_history/4/request.xml",
            "fixtures/functional/get_outbound_history/4/response.xml"
        );
    }

    /**
     * Сценарий #5:
     * В БД для указанного заказа присутствует полноценная история с множеством статусов,
     * которые маппятся в единственный статаус.
     * <p>
     * В ответ должна вернуться история, состоящая из 1 статуса.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_outbound_history/5/state.xml",
        connection = "wmsConnection"
    )
    void multipleInforStatusesToSingleStatusHistory() throws Exception {
        executeScenario(
            "fixtures/functional/get_outbound_history/5/request.xml",
            "fixtures/functional/get_outbound_history/5/response.xml"
        );
    }

    /**
     * Сценарий #6:
     * В БД для указанного заказа присутствует полноценная история с множеством статусов.
     * При этом сортировка по serialKey и по addDate отличается,
     * то есть для максимального addDate минимальный serialKey.
     * <p>
     * В ответ должна вернуться эта история, но с сортировкой по ДАТЕ статуса по убыванию
     * и со схлопыванием статусов.
     */
    @Test
    @DatabaseSetup(
            value = "classpath:fixtures/functional/get_outbound_history/6/state.xml",
            connection = "wmsConnection"
    )
    void multipleStatusesHistoryWithDifferentAddDateAndSerialKeySortOrder() throws Exception {
        executeScenario(
                "fixtures/functional/get_outbound_history/6/request.xml",
                "fixtures/functional/get_outbound_history/6/response.xml"
        );
    }

    private void executeScenario(String wrapRequest, String expectedWrapResponse) throws Exception {
        FunctionalTestScenarioBuilder.start(GetOutboundHistoryResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }
}
