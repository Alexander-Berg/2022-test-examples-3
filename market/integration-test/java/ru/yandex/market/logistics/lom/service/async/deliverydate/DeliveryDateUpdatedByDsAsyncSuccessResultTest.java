package ru.yandex.market.logistics.lom.service.async.deliverydate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.OrderDeliveryDate;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.exception.http.HttpConflictException;
import ru.yandex.market.logistics.lom.model.async.GetOrdersDeliveryDateSuccessDto;
import ru.yandex.market.logistics.lom.repository.BusinessProcessStateRepository;
import ru.yandex.market.logistics.lom.service.async.ProcessDeliveryDateUpdatedByDsAsyncResultService;

@DisplayName("Обработка успешного результата получения обновленной даты доставки заказа")
@DatabaseSetup("/service/async/deliverydates/before/prepare_update_delivery_date_process_success.xml")
class DeliveryDateUpdatedByDsAsyncSuccessResultTest extends AbstractContextualTest {

    @Autowired
    private ProcessDeliveryDateUpdatedByDsAsyncResultService asyncResultService;

    @Autowired
    private BusinessProcessStateRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-10-06T12:00:30.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Успех - последняя миля")
    @ExpectedDatabase(
        value = "/service/async/deliverydates/after/success_process_success_last_mile.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successLastMile() {
        processSuccess();
    }

    @Test
    @DisplayName("Успех - последняя миля (часовые слоты - дата/время обновились)")
    @DatabaseSetup(
        value = "/service/async/deliverydates/before/last_mile_deferred_courier.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/async/deliverydates/after/success_process_success_last_mile_deferred_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successLastMileDeferredCourier() {
        processSuccess();
    }

    @Test
    @DisplayName("Успех - средняя миля")
    @DatabaseSetup(value = "/service/async/deliverydates/before/not_last_mile.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/service/async/deliverydates/after/success_process_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() {
        processSuccess();
    }

    @Test
    @DisplayName("Успех - средняя миля (часовые слоты - дата/время не обновились)")
    @DatabaseSetup(
        value = "/service/async/deliverydates/before/not_last_mile_deferred_courier.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/async/deliverydates/after/success_process_success_not_last_mile_deferred_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNotLastMileDeferredCourier() {
        processSuccess();
    }

    @Test
    @DisplayName("Успех - средняя миля (доставка по клику - обновилась только дата)")
    @DatabaseSetup(
        value = "/service/async/deliverydates/before/not_last_mile_on_demand.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/async/deliverydates/after/success_process_success_not_last_mile_on_demand.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNotLastMileOnDemand() {
        processSuccess();
    }

    @Test
    @DisplayName(
        "Успех - средняя миля (доставка по клику, дата доставки изменилась в лучшую сторону - дата/время не обновились)"
    )
    @DatabaseSetup(
        value = "/service/async/deliverydates/before/not_last_mile_on_demand.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/async/deliverydates/after/success_process_success_not_last_mile_on_demand_date_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNotLastMileOnDemandDateBefore() {
        processSuccess(
            DateTime.fromLocalDateTime(LocalDateTime.of(2021, 10, 30, 11, 0, 0)),
            TimeInterval.of(LocalTime.of(11, 0), LocalTime.of(12, 0))
        );
    }

    @Test
    @DisplayName("Успех - средняя миля (дата доставки изменилась в лучшую сторону - дата/время не обновились)")
    @DatabaseSetup(value = "/service/async/deliverydates/before/not_last_mile.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/service/async/deliverydates/after/success_process_success_not_last_mile_date_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNotLastMileDateBefore() {
        processSuccess(
            DateTime.fromLocalDateTime(LocalDateTime.of(2021, 10, 30, 11, 0, 0)),
            TimeInterval.of(LocalTime.of(11, 0), LocalTime.of(12, 0))
        );
    }

    @Test
    @DisplayName("Успех - средняя миля (время доставки изменилось в лучшую сторону - дата/время не обновились)")
    @DatabaseSetup(value = "/service/async/deliverydates/before/not_last_mile.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/service/async/deliverydates/after/success_process_success_not_last_mile_time_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNotLastMileTimeBefore() {
        processSuccess(
            DateTime.fromLocalDateTime(LocalDateTime.of(2021, 11, 1, 9, 0, 0)),
            TimeInterval.of(LocalTime.of(9, 0), LocalTime.of(10, 0))
        );
    }

    @Test
    @DisplayName("Пэйлоад уже существует")
    @DatabaseSetup(value = "/service/async/deliverydates/before/payload.xml", type = DatabaseOperation.INSERT)
    void payloadExist() {
        softly.assertThatCode(this::processSuccess).isInstanceOf(HttpConflictException.class);
    }

    @Test
    @DisplayName("Заявка не в том статусе")
    @DatabaseSetup(
        value = "/service/async/deliverydates/before/change_order_request_processing.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/async/deliverydates/after/error_process_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void invalidRequestStatus() {
        softly.assertThatCode(this::processSuccess).doesNotThrowAnyException();
    }

    private void processSuccess() {
        processSuccess(
            DateTime.fromLocalDateTime(LocalDateTime.of(2021, 11, 5, 10, 0, 0)),
            TimeInterval.of(LocalTime.of(11, 0), LocalTime.of(12, 0))
        );
    }

    private void processSuccess(DateTime deliveryDate, TimeInterval deliveryInterval) {
        transactionTemplate.execute(ts -> {
            asyncResultService.processSuccess(repository.getOne(100L), dateSuccessDto(deliveryDate, deliveryInterval));
            return null;
        });
    }

    @Nonnull
    private GetOrdersDeliveryDateSuccessDto dateSuccessDto(DateTime deliveryDate, TimeInterval deliveryInterval) {
        return new GetOrdersDeliveryDateSuccessDto(
            1L,
            48L,
            List.of(new OrderDeliveryDate(ResourceId.builder().build(), deliveryDate, deliveryInterval, ""))
        );
    }
}
