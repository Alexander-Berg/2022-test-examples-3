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
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;

@DisplayName("Биллинг транзакций 'Страховка'")
@DatabaseSetup("/billing/before/billing_service_products.xml")
class InsuranceBillingTransactionTest extends AbstractBillingTransactionTest {

    @Test
    @DisplayName("Транзакция создаётся по одному статусу (DELIVERED)")
    @DatabaseSetup("/billing/before/insurance_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/insurance_tx_after_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insuranceTransactionDelivered() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 101, 2);
    }

    @ParameterizedTest
    @DisplayName("Транзакция создаётся по одному статусу (PROCESSING)")
    @MethodSource("insuranceTransactionSource")
    @DatabaseSetup("/billing/before/insurance_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/insurance_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insuranceTransactionProcessing(
        OrderDeliveryCheckpointStatus status,
        long trackId, long subRequestId
    ) throws Exception {
        processAndCheckTransaction(status, trackId, subRequestId);
    }

    @Nonnull
    private static Stream<Arguments> insuranceTransactionSource() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_AT_START, 101, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_AT_START_SORT, 101, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION, 101, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED, 101, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT, 101, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT, 101, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT, 101, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START, 100, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED, 100, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED, 100, 1)
        );
    }

    @ParameterizedTest
    @DisplayName("Транзакция создаётся по одному статусу (PROCESSING) + изменение даты доставки")
    @MethodSource("insuranceTransactionSourceWithDelivaryDate")
    @DatabaseSetup("/billing/before/insurance_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/insurance_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insuranceTransactionProcessingWithDelivaryDate(
        OrderDeliveryCheckpointStatus status,
        long trackId, long subRequestId
    ) throws Exception {
        mockLmsClientGetPartner(List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", null, "1")));
        processAndCheckTransaction(status, trackId, subRequestId + 1);
    }

    @Nonnull
    private static Stream<Arguments> insuranceTransactionSourceWithDelivaryDate() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_RECIPIENT, 101, 1),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY, 101, 1)
        );
    }

    @ParameterizedTest
    @DisplayName("Транзакция создаётся по одному статусу (RETURNING)")
    @MethodSource("insuranceReturnTransactionSource")
    @DatabaseSetup("/billing/before/insurance_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/insurance_returning_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insuranceReturnTransactionProcessing(
        OrderDeliveryCheckpointStatus status,
        long trackId, long subRequestId
    ) throws Exception {
        processAndCheckTransaction(status, trackId, subRequestId);
    }

    @Nonnull
    private static Stream<Arguments> insuranceReturnTransactionSource() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.RETURN_TRANSMITTED_FULFILMENT, 101, 2),
            Arguments.of(OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY, 101, 2)
        );
    }

    @Test
    @DisplayName("Транзакция не создаётся по 60 статусу (включена быстрая отмена)")
    @DatabaseSetup("/billing/before/insurance_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/insurance_tx_after_60.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noInsuranceTransactionFastCancellation() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.RETURN_PREPARING, 101, 2);
    }

    @Test
    @DisplayName("Два статуса (подходят оба)")
    @DatabaseSetup("/billing/before/insurance_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/insurance_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insuranceTransactionTwoStatuses() throws Exception {
        notifyTrack(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START,
                    OrderDeliveryCheckpointStatus.SENDER_SENT
                )
            ).build()
        );

        processDeliveryTrack(defaultTrack(
            List.of(
                OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START,
                OrderDeliveryCheckpointStatus.SENDER_SENT
            )
        ).build());

        checkAsyncTask(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.SENDER_SENT,
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START
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
    @DatabaseSetup("/billing/before/insurance_tx_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/insurance_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insuranceTransactionTwoStatusOneValid() throws Exception {
        notifyTrack(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.SENDER_SENT,
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START
                )
            ).build()
        );

        processDeliveryTrack(defaultTrack(
            List.of(
                OrderDeliveryCheckpointStatus.SENDER_SENT,
                OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START
            )
        ).build());

        checkAsyncTask(
            defaultTracks(
                List.of(
                    OrderDeliveryCheckpointStatus.SENDER_SENT,
                    OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START
                )
            ).build(),
            2,
            1,
            1
        );
    }

    @Test
    @DisplayName("Транзакция существует")
    @DatabaseSetup("/billing/before/insurance_tx_exists_setup.xml")
    @ExpectedDatabase(
        value = "/billing/after/insurance_tx_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insuranceTransactionExists() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 1);
    }

    @Test
    @DisplayName("Чарджа не существует")
    @DatabaseSetup("/billing/before/insurance_tx_no_charge_setup.xml")
    @ExpectedDatabase(
        value = "/billing/before/insurance_tx_no_charge_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insuranceTransactionNoCharge() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 1);
    }

    @Test
    @DisplayName("Транзакция создается даже если для магазина отключен биллинг")
    @DatabaseSetup("/billing/before/insurance_tx_setup.xml")
    @DatabaseSetup(
        value = "/billing/before/transactions_billing_disabled.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/billing/after/insurance_tx_after_delivered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void disabledBillingShopTransactionCreated() throws Exception {
        processAndCheckTransaction(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, 101, 2);
    }
}
