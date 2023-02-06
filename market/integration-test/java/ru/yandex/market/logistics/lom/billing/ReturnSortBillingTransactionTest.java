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

@DisplayName("Биллинг транзакций 'Возвратная сортировка'")
@DatabaseSetup("/billing/before/billing_service_products.xml")
class ReturnSortBillingTransactionTest extends AbstractBillingTransactionTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("returnSortTransactionSource")
    @DatabaseSetup("/billing/before/return_sort_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_sort_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnSortTransactionTest(OrderDeliveryCheckpointStatus status, Long subRequestId) throws Exception {
        processAndCheckTransaction(status, subRequestId);
    }

    @Nonnull
    private static Stream<Arguments> returnSortTransactionSource() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_PREPARING_SENDER, 1L),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED, 1L)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("returnSortTransactionReturningSource")
    @DatabaseSetup("/billing/before/return_sort_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_sort_tx_after_returning.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnSortTransactionReturningTest(OrderDeliveryCheckpointStatus status, Long subRequestId) throws Exception {
        processAndCheckTransaction(status, subRequestId);
    }

    @Nonnull
    private static Stream<Arguments> returnSortTransactionReturningSource() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_ARRIVED, 2L)
        );
    }

    @Test
    @DisplayName("Транзакция 'Возвратная сортировка' для возвращенного отправителю заказа")
    @DatabaseSetup("/billing/before/return_sort_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_returned_sort_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnSortReturnedTransactionTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_RETURNED, 2L);
    }

    @Test
    @DisplayName("Транзакция не создаётся по 160 статусу")
    @DatabaseSetup("/billing/before/return_sort_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_sort_tx_after_without_transaction.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noReturnSortTransaction() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_PREPARING, 2L);
    }

    @Test
    @DisplayName("Транзакция 'Возвратная сортировка', два статуса (подходят оба)")
    @DatabaseSetup("/billing/before/return_sort_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_sort_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnSortTransactionTwoStatusesTest() throws Exception {
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
            3,
            1,
            2
        );
    }

    @Test
    @DisplayName("Транзакция 'Возвратная сортировка', два статуса (один неподходящий)")
    @DatabaseSetup("/billing/before/return_sort_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_sort_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnSortTransactionTwoStatusOneValidTest() throws Exception {
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
    @DisplayName("Транзакция 'Возвратная сортировка' уже существует")
    @DatabaseSetup("/billing/before/return_sort_tx_exists_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/return_sort_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnSortTransactionExistsTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED, 1);
    }

    @Test
    @DisplayName("Чарджа 'Возвратная сортировка' не существует")
    @DatabaseSetup("/billing/before/return_sort_tx_no_charge_setup.xml")
    @ExpectedDatabase(
        value = "/billing/before/return_sort_tx_no_charge_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnSortTransactionNoChargeTest() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED, 1);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("returnSortTransactionSource")
    @DisplayName("Транзакция создается даже если для магазина отключен биллинг")
    @DatabaseSetup("/billing/before/return_sort_tx_setup.xml")
    @DatabaseSetup(
        value = "/billing/before/transactions_billing_disabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/billing/after/return_sort_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void disabledBillingShopTransactionCreated(
        OrderDeliveryCheckpointStatus status,
        Long subRequestId
    ) throws Exception {
        processAndCheckTransaction(status, subRequestId);
    }
}
