package ru.yandex.market.logistics.lom.billing;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;

@DisplayName("Биллинг транзакций 'Ожидание курьера'")
@DatabaseSetup("/billing/before/billing_service_products.xml")
class WaitBillingTransactionTest extends AbstractBillingTransactionTest {

    @Test
    @DisplayName("Создание транзакции 'Ожидание курьера'")
    @DatabaseSetup("/billing/before/wait20_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/wait20_tx_after_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void checkTransactionTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }

    @Test
    @DisplayName("Создание транзакции 'Ожидание курьера', два статуса")
    @DatabaseSetup("/billing/before/wait20_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/wait20_tx_after_returning.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void checkTransactionTwoStatusesTest() throws Exception {
        notifyTrack(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED,
                    OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY
                )
            ).build()
        );

        processDeliveryTrack(defaultTrack(
            List.of(
                OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED,
                OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY
            )
        ).build());

        checkAsyncTask(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED,
                    OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY
                )
            ).build(),
            4,
            1,
            3
        );
    }

    @Test
    @DisplayName("Транзакция 'Ожидание курьера' уже существует")
    @DatabaseSetup("/billing/before/wait20_tx_exists_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/wait20_tx_after_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void checkTransactionExistsTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }

    @Test
    @DisplayName("Чарджа 'Ожидание курьера' не существует")
    @DatabaseSetup("/billing/before/wait20_tx_no_charge_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/wait20_tx_no_charge_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void checkTransactionNoChargeTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }

    @Test
    @DisplayName("Транзакция создается даже если для магазина отключен биллинг")
    @DatabaseSetup("/billing/before/wait20_tx_setup.xml")
    @DatabaseSetup(
        value = "/billing/before/transactions_billing_disabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/billing/after/wait20_tx_after_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void disabledBillingShopTransactionCreated() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }
}
