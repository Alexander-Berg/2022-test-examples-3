package ru.yandex.market.delivery.transport_manager.service.axapta;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.dto.Stock;
import ru.yandex.market.delivery.transport_manager.dto.axapta.GetPhysicalAvailQtyRequest;
import ru.yandex.market.delivery.transport_manager.dto.axapta.GetPhysicalAvailQtyResponse;
import ru.yandex.market.delivery.transport_manager.dto.axapta.ResultStatus;
import ru.yandex.market.delivery.transport_manager.provider.AxaptaClient;

@DisplayName("Запрос реального кол-ва из axapta")
@DatabaseSetup("/repository/transportation_task/additional_transportation_tasks.xml")
public class AxaptaStockQuantitySendRequestServiceTest extends AbstractContextualTest {
    @Autowired
    private AxaptaStockQuantitySendRequestService stockQuantityService;
    @Autowired
    private AxaptaClient axaptaClient;

    @DisplayName("Не отправлять запрос по не-существующим unit-ам")
    @DatabaseSetup("/repository/register/register_axapta_request_no_external_id.xml")
    @Test
    void sendStockQuantityRequestUnitsNotFound() {
        softly.assertThatThrownBy(() -> stockQuantityService.sendStockQuantityRequest(1001L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Not all register units found for request with id 1001. Missing: [1001, 1002, 1003]");
    }

    @DisplayName("Отправка запроса")
    @DatabaseSetup({
        "/repository/register/register_axapta_request_no_external_id.xml",
        "/repository/register_unit/register_unit_for_axapta_requests.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register/register_axapta_request_sent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void sendStockQuantityRequest() {
        Mockito
            .doReturn(new GetPhysicalAvailQtyResponse().setTaskId(4L).setResultStatus(ok()))
            .when(axaptaClient)
            .getPhysicalAvailQty(Mockito.any());
        stockQuantityService.sendStockQuantityRequest(1001L);
        Mockito.verify(axaptaClient)
            .getPhysicalAvailQty((List<GetPhysicalAvailQtyRequest>) MockitoHamcrest.argThat(Matchers.containsInAnyOrder(
                new GetPhysicalAvailQtyRequest("ssku1", 1, null, new Stock(1, CountType.FIT), 10),
                new GetPhysicalAvailQtyRequest("ssku1", 1, null, new Stock(1, CountType.DEFECT), 5),
                new GetPhysicalAvailQtyRequest("000111.ssku2", 2, "000111", new Stock(1, CountType.FIT), 18),
                new GetPhysicalAvailQtyRequest("ssku3", 3, null, new Stock(1, CountType.FIT), 100),
                new GetPhysicalAvailQtyRequest("ssku3", 3, null, new Stock(1, CountType.DEFECT), 3)
            )));
    }

    @DisplayName("Ошибка в теле ответа")
    @DatabaseSetup({
        "/repository/register/register_axapta_request_no_external_id.xml",
        "/repository/register_unit/register_unit_for_axapta_requests.xml"
    })
    @ExpectedDatabase(
        value = "/repository/register/register_axapta_request_no_external_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void sendStockQuantityRequestFail() {
        Mockito
            .doReturn(new GetPhysicalAvailQtyResponse().setTaskId(1024).setResultStatus(fail()))
            .when(axaptaClient)
            .getPhysicalAvailQty(Mockito.any());
        softly.assertThatThrownBy(() -> stockQuantityService.sendStockQuantityRequest(1001L))
            .hasMessage("Error message")
            .isInstanceOf(IllegalStateException.class);
    }

    private ResultStatus ok() {
        return new ResultStatus().setOk(true);
    }

    private ResultStatus fail() {
        return new ResultStatus().setOk(false).setErrorMessage("Error message");
    }
}
