package ru.yandex.market.logistics.lom.billing;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;

@DisplayName("Биллинг транзакций 'Доставка'")
@DatabaseSetup("/billing/before/billing_service_products.xml")
class DeliveryBillingTransactionTest extends AbstractBillingTransactionTest {

    @ParameterizedTest
    @DisplayName("Транзакция создаётся по одному статусу (PROCESSING)")
    @MethodSource("deliveryTransactionSource")
    @DatabaseSetup("/billing/before/delivery_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/delivery_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryTransactionProcessing(OrderDeliveryCheckpointStatus status, long subRequestId) throws Exception {
        processAndCheckTransaction(status, subRequestId);
    }

    @ParameterizedTest
    @DisplayName("Транзакция создаётся по одному статусу (RETURNING)")
    @MethodSource("deliveryReturningTransactionSource")
    @DatabaseSetup("/billing/before/delivery_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/delivery_returning_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryReturningTransactionProcessing(
        OrderDeliveryCheckpointStatus status,
        long subRequestId
    ) throws Exception {
        processAndCheckTransaction(status, subRequestId);
    }

    @Test
    @DisplayName("Транзакция создаётся по одному статусу (DELIVERED)")
    @DatabaseSetup("/billing/before/delivery_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/delivery_tx_after_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryTransactionDelivered() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }

    @Nonnull
    private static Stream<Arguments> deliveryTransactionSource() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_AT_START, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT, 1)
        );
    }

    @Nonnull
    private static Stream<Arguments> deliveryReturningTransactionSource() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.RETURN_TRANSMITTED_FULFILMENT, 2),
            Arguments.of(OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY, 2)
        );
    }

    @Test
    @DisplayName("Транзакция не создаётся по 60 статусу (включена быстрая отмена)")
    @DatabaseSetup("/billing/before/delivery_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/delivery_tx_after_60.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noDeliveryTransactionFastCancellation() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.RETURN_PREPARING, 2);
    }

    @Test
    @DisplayName("Два статуса (оба подходящие)")
    @DatabaseSetup("/billing/before/delivery_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/delivery_returning_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryTransactionTwoStatuses() throws Exception {
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
    @DisplayName("Два статуса (один подходящий)")
    @DatabaseSetup("/billing/before/delivery_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/delivery_tx_after_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryTransactionTwoStatusesOneValid() throws Exception {
        notifyTrack(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.SENDER_SENT,
                    OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED
                )
            ).build()
        );

        processDeliveryTrack(defaultTrack(
            List.of(
                OrderDeliveryCheckpointStatus.SENDER_SENT,
                OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED
            )
        ).build());

        checkAsyncTask(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED,
                    OrderDeliveryCheckpointStatus.SENDER_SENT
                ),
                true
            ).build(),
            3,
            1,
            2
        );
    }

    @Test
    @DisplayName("Транзакция существует")
    @DatabaseSetup("/billing/before/delivery_tx_exists_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/delivery_tx_after_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryTransactionExists() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }

    @Test
    @DisplayName("Чарджа не существует")
    @DatabaseSetup("/billing/before/tx_no_charge_setup.xml")
    @ExpectedDatabase(
        value = "/billing/before/tx_no_charge_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deliveryTransactionNoCharge() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 2);
    }

    @ParameterizedTest
    @DisplayName("Транзакция создается даже если для магазина отключен биллинг")
    @MethodSource("deliveryTransactionSource")
    @DatabaseSetup("/billing/before/delivery_tx_setup.xml")
    @DatabaseSetup(
        value = "/billing/before/transactions_billing_disabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/billing/after/delivery_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void disabledBillingShopTransactionCreated(
        OrderDeliveryCheckpointStatus status,
        long subRequestId
    ) throws Exception {
        processAndCheckTransaction(status, subRequestId);
    }

}
