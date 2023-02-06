package ru.yandex.market.logistics.lom.billing;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;

@DisplayName("Биллинг транзакций 'Проверка при курьере'")
@DatabaseSetup("/billing/before/billing_service_products.xml")
class CheckBillingTransactionTest extends AbstractBillingTransactionTest {

    @Test
    @DisplayName("Создание транзакции 'Проверка при курьере'")
    @DatabaseSetup("/billing/before/check_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/check_tx_after_only_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void checkTransactionTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }

    @Test
    @DisplayName("Создание транзакции 'Проверка при курьере', два статуса")
    @DatabaseSetup("/billing/before/check_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/check_tx_after_returning_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void checkTransactionTwoStatusesTest() throws Exception {
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
    @DisplayName("Транзакция 'Проверка при курьере' уже существует")
    @DatabaseSetup("/billing/before/check_tx_exists_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/check_tx_after_only_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void checkTransactionExistsTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }

    @Test
    @DisplayName("Чарджа 'Проверка при курьере' не существует")
    @DatabaseSetup("/billing/before/check_tx_no_charge_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/check_tx_no_charge_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void checkTransactionNoChargeTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }

    @Test
    @DisplayName("Транзакция создается даже если для магазина отключен биллинг")
    @DatabaseSetup("/billing/before/check_tx_setup.xml")
    @DatabaseSetup(
        value = "/billing/before/transactions_billing_disabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/billing/after/check_tx_after_only_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void disabledBillingShopTransactionCreated() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }

}
