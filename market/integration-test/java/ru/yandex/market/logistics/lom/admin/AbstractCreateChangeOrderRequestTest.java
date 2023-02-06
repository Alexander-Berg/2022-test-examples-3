package ru.yandex.market.logistics.lom.admin;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.service.businessProcess.AbstractBusinessProcessStateYdbServiceTest;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.logistics.util.client.tvm.client.TvmServiceTicket;
import ru.yandex.market.logistics.util.client.tvm.client.TvmTicketStatus;
import ru.yandex.market.logistics.util.client.tvm.client.TvmUserTicket;

import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание заявки на изменение опций доставки")
abstract class AbstractCreateChangeOrderRequestTest extends AbstractBusinessProcessStateYdbServiceTest {
    public static final long REQUEST_CAN_BE_CREATED_FOR_ORDER_ID = 4;

    @Autowired
    private TvmClientApi tvmClientApi;

    @Test
    @ExpectedDatabase(
        value = "/controller/admin/change-request/before/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Заказ по идентификатору не найден")
    void orderNotFound() throws Exception {
        createChangeOrderRequest(0)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with id [0]"));
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/admin/change-request/before/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Заказ уже создан в некоторых партнерах")
    void orderCreatedAtPartner() throws Exception {
        createChangeOrderRequest(1)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Заявки не созданы из-за ошибок валидации: " +
                    "Заказ создан хотя бы в одном партнере прямого маршрута (orderIds = [1])"
            ));
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/admin/change-request/before/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Заказ в неподходящем статусе")
    void inappropriateOrderStatus() throws Exception {
        createChangeOrderRequest(2)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Заявки не созданы из-за ошибок валидации: " +
                    "Заказ в неподходящем статусе PROCESSING. Ожидаемый статус - PROCESSING_ERROR (orderIds = [2])"
            ));
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/admin/change-request/before/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Заявка на изменение опций доставки уже есть")
    void requestAlreadyExists() throws Exception {
        createChangeOrderRequest(3)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Заявки не созданы из-за ошибок валидации: " +
                    "Заявка на изменение опции доставки уже существует (orderIds = [3])"
            ));
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/admin/change-request/after/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное создание заявки")
    void requestCreated() throws Exception {
        when(tvmClientApi.checkUserTicket("test-user-ticket"))
            .thenReturn(new TvmUserTicket(19216801L, TvmTicketStatus.OK));
        when(tvmClientApi.checkServiceTicket("test-service-ticket"))
            .thenReturn(new TvmServiceTicket(1010, TvmTicketStatus.OK, ""));

        createChangeOrderRequest(REQUEST_CAN_BE_CREATED_FOR_ORDER_ID)
            .andExpect(status().isOk())
            .andExpect(changeRequestCreatedResponseBodyMatcher());
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/admin/change-request/before/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Есть необработанные фоновые процессы")
    void orderWithActiveBusinessProcesses() throws Exception {
        createChangeOrderRequest(5)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Заявки не созданы из-за ошибок валидации: " +
                    "По заказу есть активные бизнес-процессы. Дождитесь их завершения (orderIds = [5])"
            ));
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/admin/change-request/before/order_2_processing_error_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup("/controller/admin/change-request/before/order_2_processing_error_status.xml")
    @DisplayName("Есть необработанные фоновые процессы в YDB")
    void orderWithActiveBusinessProcessesInYdb() throws Exception {
        insertProcessToYdb(11111L, BusinessProcessStatus.ENQUEUED);

        createChangeOrderRequest(2)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Заявки не созданы из-за ошибок валидации: " +
                    "По заказу есть активные бизнес-процессы. Дождитесь их завершения (orderIds = [2])"
            ));
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/admin/change-request/before/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Неподдерживаемый клиент платформы")
    void unsupportedPlatformClient() throws Exception {
        createChangeOrderRequest(6)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Заявки не созданы из-за ошибок валидации: " +
                    "Заказ неподдерживаемого клиента платформы YANDEX_DELIVERY. " +
                    "Поддерживаемый тип - BERU (orderIds = [6])"
            ));
    }

    @Nonnull
    abstract ResultActions createChangeOrderRequest(long orderId) throws Exception;

    @Nonnull
    abstract ResultMatcher changeRequestCreatedResponseBodyMatcher();
}
