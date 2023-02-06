package ru.yandex.market.logistics.lom.billing;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;

@DisplayName("Биллинг транзакций 'Вознаграждение за перечисление денежных средств'")
@DatabaseSetup("/billing/before/billing_service_products.xml")
class CashServiceBillingTransactionTest extends AbstractBillingTransactionTest {

    @Test
    @DisplayName("Создание транзакции 'Вознаграждение за перечисление денежных средств'")
    @DatabaseSetup("/billing/before/cash_service_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/cash_service_tx_after_only_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cashServiceTransactionTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }

    @Test
    @DisplayName("Создание транзакции 'Вознаграждение за перечисление денежных средств', два статуса")
    @DatabaseSetup("/billing/before/cash_service_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/cash_service_tx_after_returning_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cashServiceTransactionTwoStatusesTest() throws Exception {
        notifyTrack(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY,
                    OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED
                )
            ).build()
        );

        processDeliveryTrack(defaultTrack(
            List.of(
                OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY,
                OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED
            )
        ).build());

        checkAsyncTask(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED,
                    OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY
                ),
                true
            ).build(),
            4,
            1,
            3
        );
    }

    @Test
    @DisplayName("Транзакция 'Вознаграждение за перечисление средств' уже существует")
    @DatabaseSetup("/billing/before/cash_service_tx_exists_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/cash_service_tx_after_only_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cashServiceTransactionExistsTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }

    @Test
    @DisplayName("Не существует чарджа 'Вознаграждение за перечисление средств'")
    @DatabaseSetup("/billing/before/cash_service_tx_no_charge_setup.xml")
    @ExpectedDatabase(
        value = "/billing/before/cash_service_tx_no_charge_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cashServiceTransactionNoChargeTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 1);
    }
}
