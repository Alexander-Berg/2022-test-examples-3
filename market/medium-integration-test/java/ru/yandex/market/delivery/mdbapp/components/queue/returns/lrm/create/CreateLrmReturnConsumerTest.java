package ru.yandex.market.delivery.mdbapp.components.queue.returns.lrm.create;

import java.time.ZonedDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnApi;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnRequest;
import ru.yandex.market.delivery.mdbapp.AbstractMediumContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterReturnService;
import ru.yandex.market.delivery.mdbapp.components.service.lrm.LogisticReturnService;
import ru.yandex.market.delivery.mdbapp.integration.converter.CreateReturnRequestConverter;
import ru.yandex.market.delivery.mdbapp.integration.service.PersonalDataService;
import ru.yandex.market.delivery.mdbapp.integration.service.ReturnRequestService;
import ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils;
import ru.yandex.market.logistics.mdb.lrm.client.api.ReturnsApi;
import ru.yandex.market.logistics.mdb.lrm.client.model.CreateReturnRequest;
import ru.yandex.market.logistics.mdb.lrm.client.model.CreateReturnResponse;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.LRM_RETURN_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ORDER_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.lrmCreateReturnRequest;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DatabaseSetup("/components/queue/returns/lrm/create_return/before/setup.xml")
class CreateLrmReturnConsumerTest extends AbstractMediumContextualTest {

    @Autowired
    private CheckouterOrderService checkouterOrderService;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private CheckouterReturnApi checkouterReturnApi;

    @Autowired
    private ReturnRequestService returnRequestService;

    @Autowired
    private CheckouterReturnService checkouterReturnService;

    @Autowired
    private LogisticReturnService logisticReturnService;

    @Autowired
    private ReturnsApi returnsApi;

    @Autowired
    private CreateReturnRequestConverter createReturnRequestConverter;

    @Autowired
    private PersonalDataService personalDataService;

    private CreateLrmReturnConsumer consumer;

    @BeforeEach
    void setup() {
        consumer = new CreateLrmReturnConsumer(
            checkouterOrderService,
            checkouterReturnService,
            returnRequestService,
            logisticReturnService,
            createReturnRequestConverter
        );
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(returnsApi);
    }

    @Test
    @DisplayName("Успешное создание возврата в ПВЗ")
    void pickupSuccess() {
        mockCheckouterOrder();
        mockCheckouterReturn(ReturnRequestTestUtils.checkouterReturnWithDelivery());

        when(returnsApi.createReturn(any(CreateReturnRequest.class), eq(true)))
            .thenReturn(new CreateReturnResponse().id(LRM_RETURN_ID));

        assertResult(TaskExecutionResult.finish());

        verify(returnsApi).createReturn(lrmCreateReturnRequest(true), true);
    }

    @Test
    @DisplayName("Успешное создание возврата курьером")
    void courierSuccess() {
        mockCheckouterOrder();
        mockCheckouterReturn(ReturnRequestTestUtils.checkouterCourierReturn());
        mockPersonal();

        assertResult(TaskExecutionResult.finish());
        verify(returnsApi).createClientCourierReturn(ReturnRequestTestUtils.lrmCreateClientCourierReturnRequest());
    }

    @Test
    @DisplayName("Отсутствует заказ в чекаутере")
    void noCheckouterOrder() {
        assertResult(TaskExecutionResult.fail());
    }

    @Test
    @DisplayName("Отсутствует возврат в чекаутере")
    void noCheckouterReturn() {
        mockCheckouterOrder();

        assertResult(TaskExecutionResult.fail());
    }

    @Test
    @DisplayName("Ошибка создания в LRM")
    void lrmCreateFail() {
        mockCheckouterOrder();
        mockCheckouterReturn(ReturnRequestTestUtils.checkouterReturnWithDelivery());

        when(returnsApi.createReturn(any(CreateReturnRequest.class), eq(true)))
            .thenThrow(new RuntimeException("request exception"));

        assertResult(TaskExecutionResult.fail());

        verify(returnsApi).createReturn(lrmCreateReturnRequest(true), true);
    }

    private void mockCheckouterOrder() {
        when(checkouterAPI.getOrder(eq(ORDER_ID), eq(ClientRole.SYSTEM), eq(null)))
            .thenReturn(ReturnRequestTestUtils.checkouterOrder());
    }

    private void mockCheckouterReturn(Return checkouterReturn) {
        when(checkouterReturnApi.getReturn(
            safeRefEq(new RequestClientInfo(ClientRole.SYSTEM, 0L)),
            safeRefEq(ReturnRequest.builder(RETURN_ID, ORDER_ID).build())
        )).thenReturn(checkouterReturn);
    }

    public void mockPersonal() {
        when(personalDataService.convertToLogisticsAddressId(ReturnRequestTestUtils.SENDER_ADDRESS_PERSONAL_ADDRESS_ID))
            .thenReturn(ReturnRequestTestUtils.LOGISTICS_PERSONAL_ADDRESS_ID);
    }

    private void assertResult(TaskExecutionResult result) {
        softly.assertThat(consumer.execute(new Task<>(
                new QueueShardId("id"),
                new CreateLrmReturnDto(String.valueOf(RETURN_ID)),
                1L,
                ZonedDateTime.now(),
                null,
                null
            )))
            .isEqualTo(result);
    }

}
