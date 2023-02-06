package ru.yandex.market.wrap.infor.functional;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.wrap.infor.repository.IdentifierMappingSequence;
import ru.yandex.market.wrap.infor.service.common.PutReferenceItemsService;
import ru.yandex.market.wrap.infor.service.identifier.mapping.IdentifierMappingResponse;

import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;


class MapReferenceItemsTest extends AbstractFunctionalTest {

    @SpyBean
    private IdentifierMappingSequence identifierMappingSequence;

    /**
     * Сценарий #1: Ожидается корректный маппинг одного SKU
     */
    @Test
    @DatabaseSetup(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/map_reference_items/common/db_state.xml"
    )
    @ExpectedDatabase(
        connection = "wrapConnection",
        value = "classpath:fixtures/functional/map_reference_items/common/db_state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void mapSingleItem() throws Exception {
        FunctionalTestScenarioBuilder.start(IdentifierMappingResponse.class)
                .sendRequestToWrap("/identifierMapping", HttpMethod.POST, "fixtures/functional/map_reference_items/1/wrap_request.xml")
                .andExpectWrapAnswerToBeEqualTo("fixtures/functional/map_reference_items/1/wrap_response.xml")
                .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
                .start();
    }

    /**
     * Сценарий #2: Ожидается корректный маппинг нескольких SKU с одинаковым id, но разным поставщиком
     */
    @Test
    @DatabaseSetup(
            connection = "wrapConnection",
            value = "classpath:fixtures/functional/map_reference_items/common/db_state.xml"
    )
    @ExpectedDatabase(
            connection = "wrapConnection",
            value = "classpath:fixtures/functional/map_reference_items/common/db_state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void mapItemsWithSameIdAndDiffVendor() throws Exception {
        FunctionalTestScenarioBuilder.start(IdentifierMappingResponse.class)
                .sendRequestToWrap("/identifierMapping", HttpMethod.POST, "fixtures/functional/map_reference_items/2/wrap_request.xml")
                .andExpectWrapAnswerToBeEqualTo("fixtures/functional/map_reference_items/2/wrap_response.xml")
                .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
                .start();
    }


    /**
     * Сценарий #3: Ожидается создание маппинга для несуществуещго SKU
     */
    @Test
    @DatabaseSetup(
            connection = "wrapConnection",
            value = "classpath:fixtures/functional/map_reference_items/common/db_state.xml"
    )
    @ExpectedDatabase(
            connection = "wrapConnection",
            value = "classpath:fixtures/functional/map_reference_items/3/db_state_after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void mapNewItems() throws Exception {
        doReturn(Set.of(5L))
                .when(identifierMappingSequence).next(1);

        FunctionalTestScenarioBuilder.start(IdentifierMappingResponse.class)
                .sendRequestToWrap("/identifierMapping", HttpMethod.POST, "fixtures/functional/map_reference_items/3/wrap_request.xml")
                .andExpectWrapAnswerToBeEqualTo("fixtures/functional/map_reference_items/3/wrap_response.xml")
                .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
                .start();
    }



}
