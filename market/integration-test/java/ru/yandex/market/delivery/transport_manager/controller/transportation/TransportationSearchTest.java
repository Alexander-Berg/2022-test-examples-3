package ru.yandex.market.delivery.transport_manager.controller.transportation;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DisplayName("Поиск перемещений")
@DatabaseSetup("/repository/transportation/transportation_with_partner_info_and_items.xml")
public class TransportationSearchTest extends AbstractContextualTest {

    @DisplayName("Успешный ответ")
    @Test
    void success() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/by_everything.json",
            status().isOk(),
            jsonContent("controller/transportations/search/response/result_1.json")
        );
    }

    @DisplayName("Фильтр по идентификаторам")
    @Test
    void byIds() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/by_ids.json",
            status().isOk(),
            jsonContent("controller/transportations/search/response/result_2.json")
        );
    }

    @DisplayName("Получение перемещения с тегами")
    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_with_partner_info_and_items.xml",
        "/repository/transportation/transportation_3_with_tags.xml"
    })
    void byIdsWithTags() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/by_ids_with_tags.json",
            status().isOk(),
            jsonContent("controller/transportations/search/response/result_3.json")
        );
    }

    @DisplayName("Фильтр по идентификаторам заказов отгрузок")
    @Test
    void byOutboundOrderIds() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/by_order_ids.json",
            status().isOk(),
            jsonContent("controller/transportations/search/response/result_12.json")
        );
    }

    @DisplayName("Исключить партнёров перемещений")
    @Test
    void byMovementExcludedPartnerIds() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/by_movement_exclude_partner_ids.json",
            status().isOk(),
            jsonContent("controller/transportations/search/response/result_2.json")
        );
    }

    @DisplayName("По нескольким партнёрам отгрузки")
    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_with_partner_info_and_items.xml",
        "/repository/transportation/transportation_4_different_partners.xml",
    })
    void byMultipleOutboundIds() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/outbound_partner_ids.json",
            status().isOk(),
            jsonContent("controller/transportations/search/response/result_124.json")
        );
    }

    @DisplayName("Ответ на несколько страниц")
    @Test
    @DatabaseSetup({
        "/repository/transportation/transportation_with_partner_info_and_items.xml",
        "/repository/transportation/transportation_4_different_partners.xml",
    })
    void manyPages() throws Exception {
        searchTransportationsWithPage(
            "controller/transportations/search/request/outbound_partner_ids.json",
            "1",
            "0",
            status().isOk(),
            jsonContent("controller/transportations/search/response/many_pages.json")
        );
    }

    @DisplayName("По идентификаторам логистической точки отгрузки")
    @Test
    void byOutboundLogisticPointIds() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/outbound_logistic_point_ids.json",
            status().isOk(),
            jsonContent("controller/transportations/search/response/result_2.json")
        );
    }

    @DisplayName("По идентификаторам логистической точки приёмки")
    @Test
    void byInboundLogisticPointIds() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/inbound_logistic_point_ids.json",
            status().isOk(),
            jsonContent("controller/transportations/search/response/result_2.json")
        );
    }

    @DisplayName("Пустой ответ")
    @Test
    void empty() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/empty.json",
            status().isOk(),
            jsonContent("controller/transportations/search/response/empty.json")
        );
    }

    @DisplayName("null в идентификаторах партнёров отгрузок")
    @Test
    void outboundPartnerIdsNull() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/outbound_partner_ids_null.json",
            status().isBadRequest(),
            errorMessage("Following validation errors occurred:\n" +
                "Field: 'outboundPartnerIds[]', message: 'must not be null'")
        );
    }

    @DisplayName("null в статусах отправки")
    @Test
    void outboundStatusesNull() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/statuses_null.json",
            status().isBadRequest(),
            errorMessage("Following validation errors occurred:\n" +
                "Field: 'outboundStatuses[]', message: 'must not be null'")
        );
    }

    @DisplayName("null в идентификаторах заказов отгрузок")
    @Test
    void outboundOrderIdsNull() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/order_ids_null.json",
            status().isBadRequest(),
            errorMessage("Following validation errors occurred:\n" +
                "Field: 'outboundOrderIds[]', message: 'must not be null'")
        );
    }

    @DisplayName("null в идентификаторах исключённых партнёров отгрузок")
    @Test
    void movementExcludePartnerIdsNull() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/movement_exclude_partner_ids_null.json",
            status().isBadRequest(),
            errorMessage("Following validation errors occurred:\n" +
                "Field: 'movementExcludePartnerIds[]', message: 'must not be null'")
        );
    }

    @DisplayName("null в идентификаторах логистических точек отгрузок")
    @Test
    void outboundLogisticPointIdsNull() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/outbound_logistic_point_ids_null.json",
            status().isBadRequest(),
            errorMessage("Following validation errors occurred:\n" +
                "Field: 'outboundLogisticPointIds[]', message: 'must not be null'")
        );
    }

    @DisplayName("null в идентификаторах логистических точек приёмок")
    @Test
    void inboundLogisticPointIdsNull() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/inbound_logistic_point_ids_null.json",
            status().isBadRequest(),
            errorMessage("Following validation errors occurred:\n" +
                "Field: 'inboundLogisticPointIds[]', message: 'must not be null'")
        );
    }

    @DisplayName("Невалидный формат даты")
    @Test
    void dateFromInvalidFormat() throws Exception {
        searchTransportations(
            "controller/transportations/search/request/invalid_date.json",
            status().isBadRequest(),
            errorMessage("Text '2021/03/02' could not be parsed at index 4")
        );
    }

    void searchTransportations(
        String content,
        ResultMatcher statusMatcher,
        ResultMatcher resultMatcher
    ) throws Exception {
        mockMvc.perform(
            put("/transportations/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(content))
        )
            .andExpect(statusMatcher)
            .andExpect(resultMatcher);
    }

    void searchTransportationsWithPage(
        String content,
        String pageSize,
        String pageNumber,
        ResultMatcher statusMatcher,
        ResultMatcher resultMatcher
    ) throws Exception {
        mockMvc.perform(
            put("/transportations/search")
                .param("size", pageSize)
                .param("page", pageNumber)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(content))
        )
            .andExpect(statusMatcher)
            .andExpect(resultMatcher);
    }
}
