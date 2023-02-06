package ru.yandex.market.logistics.lom.controller.order;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.dto.ItemInstancesDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateItemInstancesRequestDto;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обновление маркировок товаров")
@ParametersAreNonnullByDefault
class OrderItemInstancesUpdateTest extends AbstractContextualTest {
    @Test
    @DisplayName("Успешное обновление маркировок")
    @DatabaseSetup("/controller/order/update_item_instances/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_item_instances/after/instances_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSuccess() throws Exception {
        performRequest(createRequest(1L, 1L, 2L))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/update_item_instances/after/change_order_request.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Заказ не найден")
    @DatabaseSetup("/controller/order/update_item_instances/before/setup.xml")
    void orderNotFound() throws Exception {
        performRequest(createRequest(2L, 1L, 2L))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with id [2]"));
    }

    @Test
    @DisplayName("Существует активная заявка на изменение")
    @DatabaseSetup("/controller/order/update_item_instances/before/setup.xml")
    @DatabaseSetup(
        value = "/controller/order/update_item_instances/before/active_request_exists.xml",
        type = DatabaseOperation.INSERT
    )
    void activeRequestExists() throws Exception {
        performRequest(createRequest(1L, 1L, 2L))
            .andExpect(status().isConflict())
            .andExpect(errorMessage(
                "Active Change Request with type = UPDATE_ITEMS_INSTANCES is already exists for order barcode-1"
            ));
    }

    @Test
    @DisplayName("Не обновляем маркировки после 110 чп")
    @DatabaseSetup("/controller/order/update_item_instances/before/setup.xml")
    @DatabaseSetup(
        value = "/controller/order/update_item_instances/before/sc_status_in.xml",
        type = DatabaseOperation.INSERT
    )
    void orderStartedAtSc() throws Exception {
        performRequest(createRequest(1L, 1L, 2L))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Order has IN status on not FF segment"));
    }

    @Test
    @DisplayName("Статус IN на FF сегменте, обновляем маркировки")
    @DatabaseSetup(value = "/controller/order/update_item_instances/before/setup_ff.xml")
    void orderStartedAtFF() throws Exception {
        performRequest(createRequest(1L, 1L, 2L))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/update_item_instances/after/change_order_request.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Партнер СД не может обновлять маркировки товаров")
    @DatabaseSetup("/controller/order/update_item_instances/before/setup.xml")
    @DatabaseSetup(
        value = "/controller/order/update_item_instances/before/partner_not_update_instances.xml",
        type = DatabaseOperation.UPDATE
    )
    void partnerNotUpdateInstances() throws Exception {
        performRequest(createRequest(1L, 1L, 2L))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Partner 1003 can't update item instances"));
    }

    @Test
    @DisplayName("Не все товары указаны в запросе")
    @DatabaseSetup("/controller/order/update_item_instances/before/setup.xml")
    void notAllItemsInRequest() throws Exception {
        performRequest(createRequest(1L, 1L))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Items with ids [ItemKey(vendorId=1002, article=article-2)] not found in request"));
    }

    @Test
    @DisplayName("Товар не найден в заказе")
    @DatabaseSetup("/controller/order/update_item_instances/before/setup.xml")
    void itemNotFoundInOrder() throws Exception {
        performRequest(createRequest(1L, 1L, 2L, 3L))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER_ITEM] with id [ItemKey(vendorId=1003, article=article-3)]"));
    }

    @Test
    @DisplayName("Дублирующийся товар в запросе, маркировки записываются в один товар")
    @DatabaseSetup("/controller/order/update_item_instances/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_item_instances/after/instances_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void duplicateItemInRequest() throws Exception {
        performRequest(
            UpdateItemInstancesRequestDto.builder()
                .orderId(1L)
                .itemInstances(
                    List.of(
                        ItemInstancesDto.builder()
                            .vendorId(1001L)
                            .article("article-1")
                            .instances(List.of(Map.of("UIT", "2-1-UIT")))
                            .build(),
                        ItemInstancesDto.builder()
                            .vendorId(1001L)
                            .article("article-1")
                            .instances(List.of(Map.of("CIS", "2-1-CIS")))
                            .build(),
                        ItemInstancesDto.builder()
                            .vendorId(1002L)
                            .article("article-2")
                            .instances(List.of(Map.of("UIT", "2-2-UIT"), Map.of("CIS", "2-2-CIS")))
                            .build()
                    )
                )
                .build()
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/update_item_instances/after/change_order_request_with_duplicate.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Дублирующийся товар в заказе, маркировки раскидываются по товарам")
    @DatabaseSetup("/controller/order/update_item_instances/before/setup_duplicate.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_item_instances/after/instances_updated_duplicate.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void duplicateItemInOrder() throws Exception {
        performRequest(createRequest(1L, 1L, 2L))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/update_item_instances/after/change_order_request.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("В запросе маркировок больше, чем количество товаров")
    @DatabaseSetup("/controller/order/update_item_instances/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_item_instances/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void instancesMoreThanItemsCount() throws Exception {
        performRequest(
            UpdateItemInstancesRequestDto.builder()
                .orderId(1L)
                .itemInstances(
                    List.of(
                        ItemInstancesDto.builder()
                            .vendorId(1001L)
                            .article("article-1")
                            .instances(List.of(Map.of("UIT", "2-1-UIT"), Map.of("UIT", "2-1-CIS")))
                            .build(),
                        ItemInstancesDto.builder()
                            .vendorId(1002L)
                            .article("article-2")
                            .instances(List.of(Map.of("CIS", "1-CIS"), Map.of("CIS", "2-CIS"), Map.of("CIS", "3-CIS")))
                            .build()
                    )
                )
                .build()
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "There are more instances than item count for item with id ItemKey(vendorId=1002, article=article-2)"
            ));
    }

    @Test
    @DisplayName("Запрос с пустыми списками маркировок")
    @DatabaseSetup("/controller/order/update_item_instances/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/order/update_item_instances/after/instances_updated_empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void emptyInstances() throws Exception {
        performRequest(
            UpdateItemInstancesRequestDto.builder()
                .orderId(1L)
                .itemInstances(
                    List.of(
                        ItemInstancesDto.builder()
                            .vendorId(1001L)
                            .article("article-1")
                            .instances(null)
                            .build(),
                        ItemInstancesDto.builder()
                            .vendorId(1002L)
                            .article("article-2")
                            .instances(null)
                            .build()
                    )
                )
                .build()
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/update_item_instances/after/change_order_request_empty.json",
                "created",
                "updated"
            ));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
    }

    @Nonnull
    private UpdateItemInstancesRequestDto createRequest(Long orderId, Long... itemIds) {
        return UpdateItemInstancesRequestDto.builder()
            .orderId(orderId)
            .itemInstances(
                Arrays.stream(itemIds)
                    .map(itemId -> ItemInstancesDto.builder()
                        .vendorId(1000 + itemId)
                        .article("article-" + itemId)
                        .instances(List.of(
                            Map.of("UIT", "2-" + itemId + "-UIT"),
                            Map.of("CIS", "2-" + itemId + "-CIS")
                        ))
                        .build()
                    )
                    .collect(Collectors.toList())
            )
            .build();
    }

    @Nonnull
    private ResultActions performRequest(UpdateItemInstancesRequestDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/updateItemInstances", request));
    }
}
