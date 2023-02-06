package ru.yandex.market.delivery.transport_manager.service.axapta;

import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.dto.axapta.ResultStatus;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.mockito.Mockito.when;

@DisplayName("Создать запросы реального кол-ва для отправик в axapta")
@DatabaseSetup("/repository/transportation_task/additional_transportation_tasks.xml")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class AxaptaStockQuantityCreateRequestsServiceTest extends AbstractContextualTest {
    @Autowired
    private AxaptaStockQuantityCreateRequestsService stockQuantityService;
    @Autowired
    private LMSClient lmsClient;

    @DisplayName("Задача на перемещение отсутствует")
    @Test
    void createStockQuantityRequestsMissing() {
        softly.assertThatThrownBy(() -> stockQuantityService.createStockQuantityRequests(100L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Transportation task with id 100 not found");
    }


    @DisplayName("Не подходящий статус задачи на перемещение")
    @ExpectedDatabase(
        value = "/repository/transportation_task/additional_transportation_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createStockQuantityRequestsWrongStatus() {
        softly.assertThatThrownBy(() -> stockQuantityService.createStockQuantityRequests(2L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("TransportationTask with id 2 has unsupported status VALIDATING");
    }

    @DisplayName("Задача на перемещение отменена")
    @ExpectedDatabase(
        value = "/repository/transportation_task/additional_transportation_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createStockQuantityRequestsTaskCancelled() {
        softly.assertThat(stockQuantityService.createStockQuantityRequests(6L)).isEmpty();
        Mockito.verifyNoMoreInteractions(lmsClient);
    }

    @DisplayName("Успешное создание")
    @DatabaseSetup("/repository/register_unit/register_unit_for_axapta_requests.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/transportation_tasks_after_axapta_request_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/after/register_axapta_request_new.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createStockQuantityRequests() {
        final LogisticsPointResponse logisticsPointResponse =
            LogisticsPointResponse.newBuilder().id(9L).partnerId(1L).build();
        Mockito.when(lmsClient.getLogisticsPoint(9L)).thenReturn(Optional.of(logisticsPointResponse));

        softly.assertThat(stockQuantityService.createStockQuantityRequests(4L)).isEqualTo(List.of(1L));
    }

    @DisplayName("Успешное создание и проверка дефектного стока с включенной фичей")
    @DatabaseSetup("/repository/register_unit/register_unit_for_axapta_requests_with_defect.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/transportation_tasks_after_axapta_request_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/after/register_axapta_request_new_with_defect.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createStockQuantityRequestsIfDefectStockCheckEnabled() {
        when(propertyService.getBoolean(TmPropertyKey.ENABLE_AXAPTA_DEFECT_CHECK_FOR_INTERWAREHOUSE))
            .thenReturn(true);
        final LogisticsPointResponse logisticsPointResponse =
            LogisticsPointResponse.newBuilder().id(9L).partnerId(1L).build();
        Mockito.when(lmsClient.getLogisticsPoint(9L)).thenReturn(Optional.of(logisticsPointResponse));

        softly.assertThat(stockQuantityService.createStockQuantityRequests(4L)).isEqualTo(List.of(1L));
    }

    @DisplayName("Успешное несоздание и проверка дефектного стока с выключенной фичей")
    @DatabaseSetup("/repository/register_unit/register_unit_for_axapta_requests_with_defect.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/transportation_tasks_after_axapta_request_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createStockQuantityRequestsIfDefectStockCheckDisabled() {

        final LogisticsPointResponse logisticsPointResponse =
            LogisticsPointResponse.newBuilder().id(9L).partnerId(1L).build();
        Mockito.when(lmsClient.getLogisticsPoint(9L)).thenReturn(Optional.of(logisticsPointResponse));

        softly.assertThat(stockQuantityService.createStockQuantityRequests(4L)).isEqualTo(List.of());
    }

    private ResultStatus ok() {
        return new ResultStatus().setOk(true);
    }

    private ResultStatus fail() {
        return new ResultStatus().setOk(false).setErrorMessage("Error message");
    }
}
