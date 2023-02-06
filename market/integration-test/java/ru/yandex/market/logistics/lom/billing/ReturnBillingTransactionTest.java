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

@DisplayName("Биллинг транзакций 'Возврат'")
@DatabaseSetup("/billing/before/billing_service_products.xml")
class ReturnBillingTransactionTest extends AbstractBillingTransactionTest {

    @ParameterizedTest
    @DisplayName("Транзакция создаётся")
    @MethodSource("returnTransactionSource")
    @DatabaseSetup("/billing/before/return_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnTransaction(OrderDeliveryCheckpointStatus status, Long trackId, Long subRequestId) throws Exception {
        processAndCheckTransaction(status, trackId, subRequestId);
    }

    @Nonnull
    private static Stream<Arguments> returnTransactionSource() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_PREPARING_SENDER, 100L, 1L),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED, 100L, 1L)
        );
    }

    @ParameterizedTest
    @DisplayName("Транзакция создаётся")
    @MethodSource("returnTransactionReturningSource")
    @DatabaseSetup("/billing/before/return_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_tx_after_returning.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnTransactionReturning(
        OrderDeliveryCheckpointStatus status,
        Long trackId,
        Long subRequestId
    ) throws Exception {
        processAndCheckTransaction(status, trackId, subRequestId);
    }

    @Nonnull
    private static Stream<Arguments> returnTransactionReturningSource() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_ARRIVED, 100L, 2L)
        );
    }

    @ParameterizedTest
    @DisplayName("Транзакция создаётся")
    @MethodSource("returnReturningTransactionSource")
    @DatabaseSetup("/billing/before/return_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_returning_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnReturningTransaction(
        OrderDeliveryCheckpointStatus status,
        Long trackId,
        Long subRequestId
    ) throws Exception {
        processAndCheckTransaction(status, trackId, subRequestId);
    }

    @Nonnull
    private static Stream<Arguments> returnReturningTransactionSource() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.RETURN_TRANSMITTED_FULFILMENT, 200L, 2L),
            Arguments.of(OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY, 200L, 2L),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_RETURNED, 100L, 2L)
        );
    }

    @Test
    @DisplayName("Транзакция не создаётся по 160 статусу")
    @DatabaseSetup("/billing/before/return_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_tx_after_without_transaction.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noReturnTransaction160() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_PREPARING, 100, 1);
    }

    @Test
    @DisplayName("Транзакция не создаётся по 60 статусу (включена быстрая отмена)")
    @DatabaseSetup("/billing/before/return_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_tx_after_without_transaction.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noReturnTransactionFastCancellation() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.RETURN_PREPARING, 200, 2);
    }

    @Test
    @DisplayName("Два статуса (подходят оба)")
    @DatabaseSetup("/billing/before/return_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnTransactionTwoStatusesTest() throws Exception {
        notifyTrack(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED,
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_PREPARING
                )
            ).build()
        );

        processDeliveryTrack(defaultTrack(
            List.of(
                OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED,
                OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_PREPARING
            )
        ).build());

        checkAsyncTask(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_PREPARING,
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED
                ),
                true
            ).build(),
            2,
            1,
            1
        );
    }

    @Test
    @DisplayName("Два статуса (один неподходящий)")
    @DatabaseSetup("/billing/before/return_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnTransactionTwoStatusOneValidTest() throws Exception {
        notifyTrack(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START,
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED
                )
            ).build()
        );

        processDeliveryTrack(defaultTrack(
            List.of(
                OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START,
                OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED
            )
        ).build());

        checkAsyncTask(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START,
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED
                )
            ).build(),
            2,
            1,
            1
        );
    }

    @Test
    @DisplayName("Транзакция уже существует")
    @DatabaseSetup("/billing/before/return_tx_exists_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnTransactionExistsTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED, 1);
    }

    @Test
    @DisplayName("Чарджа не существует")
    @DatabaseSetup("/billing/before/return_tx_no_charge_setup.xml")
    @ExpectedDatabase(
        value = "/billing/before/return_tx_no_charge_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnTransactionNoChargeTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED, 1);
    }

    @ParameterizedTest
    @DisplayName("Транзакция создается даже если для магазина отключен биллинг")
    @MethodSource("returnTransactionSource")
    @DatabaseSetup("/billing/before/return_tx_setup.xml")
    @DatabaseSetup(
        value = "/billing/before/transactions_billing_disabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/billing/after/return_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void disabledBillingShopTransactionCreated(
        OrderDeliveryCheckpointStatus status,
        Long trackId,
        Long subRequestId
    ) throws Exception {
        processAndCheckTransaction(status, trackId, subRequestId);
    }

}
