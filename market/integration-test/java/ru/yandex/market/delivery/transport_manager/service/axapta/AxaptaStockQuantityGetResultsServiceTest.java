package ru.yandex.market.delivery.transport_manager.service.axapta;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.dto.Stock;
import ru.yandex.market.delivery.transport_manager.dto.axapta.GetPhysicalAvailQtyResult;
import ru.yandex.market.delivery.transport_manager.dto.axapta.ResultStatus;
import ru.yandex.market.delivery.transport_manager.provider.AxaptaClient;

@SuppressWarnings("checkstyle:ParameterNumber")
@DisplayName("Получение реального кол-ва из axapta")
@DatabaseSetup("/repository/transportation_task/additional_transportation_tasks.xml")
public class AxaptaStockQuantityGetResultsServiceTest extends AbstractContextualTest {
    @Autowired
    private AxaptaStockQuantityGetResultsService stockQuantityService;
    @Autowired
    private AxaptaClient axaptaClient;

    @DisplayName("Успешное получение реального кол-ва из axapta")
    @DatabaseSetup({
        "/repository/transportation_task/additional_transportation_tasks.xml",
        "/repository/register/register_axapta_request_sent.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register/register_axapta_request_with_response.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void checkForStockQuantityResponse() {
        Mockito
            .doReturn(Optional.of(
                new GetPhysicalAvailQtyResult.Result()
                    .setResultStatus(ok())
                    .setResults(List.of(
                        unit("ssku1", 1L, null, 1, CountType.FIT, 10, 100, null, null),
                        unit("ssku1", 1L, null, 1, CountType.DEFECT, 5, 100, false, null),
                        unit("ssku2", 2L, null, 1, CountType.FIT, 18, 100, false, 50),
                        unit("123.ssku3", 3L, "123", 1, CountType.FIT, 100, 5, true, null),
                        unit("ssku3", 3L, null, 1, CountType.DEFECT, 4, 0, true, 10),
                        unit("ssku3", 3L, null, 1, CountType.DEFECT, 4, 0, true, 10)
                    ))
            ))
            .when(axaptaClient)
            .getResult(4L);

        softly.assertThat(stockQuantityService.checkForStockQuantityResponse(1001L)).isTrue();
    }

    @DisplayName("Ошибка в ответе от axapta (несмотря на не-пустые results)")
    @DatabaseSetup("/repository/register/register_axapta_request_sent.xml")
    @ExpectedDatabase(
        value = "/repository/register/register_axapta_request_sent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void checkForStockQuantityResponseFail() {
        Mockito
            .doReturn(Optional.of(
                new GetPhysicalAvailQtyResult.Result()
                    .setResultStatus(fail())
                    .setResults(List.of(
                        unit("ssku1", 1L, null, 1, CountType.FIT, 10, 100, false, null),
                        unit("ssku1", 1L, null, 1, CountType.DEFECT, 5, 100, false, null),
                        unit("ssku2", 2L, null, 1, CountType.FIT, 18, 100, false, 50),
                        unit("ssku3", 3L, null, 1, CountType.FIT, 100, 5, true, null),
                        unit("ssku3", 3L, null, 1, CountType.DEFECT, 4, 0, true, 10)
                    ))
            ))
            .when(axaptaClient)
            .getResult(4L);

        softly.assertThat(stockQuantityService.checkForStockQuantityResponse(1001L)).isFalse();
    }

    @DisplayName("Задача отменена")
    @DatabaseSetup("/repository/register/register_axapta_request_sent_cancelled_task.xml")
    @ExpectedDatabase(
        value = "/repository/register/register_axapta_request_sent_cancelled_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void checkForStockQuantityResponseTaskCancelled() {
        Mockito.verifyNoMoreInteractions(axaptaClient);
        softly.assertThat(stockQuantityService.checkForStockQuantityResponse(1001L)).isTrue();
    }

    private GetPhysicalAvailQtyResult.ResultUnit unit(
        String ssku,
        long merchantId,
        @Nullable String realMerchantId,
        int partnerId,
        CountType type,
        int requestedQty,
        int availPhysicalQty,
        Boolean isMercury,
        Integer availMercuryQty
    ) {
        return new GetPhysicalAvailQtyResult.ResultUnit()
            .setResultUnitKey(new GetPhysicalAvailQtyResult.ResultUnitKey()
                .setSsku(ssku)
                .setMerchantId(merchantId)
                .setRealMerchantId(realMerchantId)
                .setStock(new Stock(partnerId, type))
            )
            .setResultStatus(ok())
            .setAvailPhysicalQty(availPhysicalQty)
            .setRequestedQty(requestedQty)
            .setIsMercury(isMercury)
            .setAvailMercuryQty(availMercuryQty);
    }

    private ResultStatus ok() {
        return new ResultStatus().setOk(true);
    }

    private ResultStatus fail() {
        return new ResultStatus().setOk(false).setErrorMessage("Error message");
    }
}
