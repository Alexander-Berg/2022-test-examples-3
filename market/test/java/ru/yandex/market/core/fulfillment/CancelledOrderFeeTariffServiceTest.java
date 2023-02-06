package ru.yandex.market.core.fulfillment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.model.PriceAndTariffValue;
import ru.yandex.market.core.fulfillment.tariff.TariffsException;
import ru.yandex.market.core.fulfillment.tariff.TariffsIterator;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.core.order.model.OrderItemForBilling;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CancelledOrderFeeJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Тесты для {@link CancelledOrderFeeTariffService}
 */
class CancelledOrderFeeTariffServiceTest extends FunctionalTest {
    private final LocalDate LOCAL_DATE_BEFORE_START_BILLING = LocalDate.of(2020, 6, 7);
    private final LocalDate LOCAL_DATE_AFTER_START_BILLING = LocalDate.of(2020, 6, 8);
    private final LocalDate LOCAL_DATE_END_OF_TARIFF = LocalDate.of(2020, 12, 31);

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private TariffsService tariffsService;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");

            LocalDate targetDate = Objects.requireNonNull(findQuery.getTargetDate());
            return new TariffsIterator((pageNumber, batchSize) -> {
                if (pageNumber != 0) {
                    return List.of();
                }

                return getCancelledOrderTariffs()
                        .stream()
                        .filter(tariff -> {
                            LocalDate to = tariff.getDateTo();
                            LocalDate from = tariff.getDateFrom();
                            return targetDate.compareTo(from) >= 0
                                    && (to == null || targetDate.compareTo(to) < 0);
                        })
                        .collect(Collectors.toList());
            });
        }).when(tariffsService).findTariffs(any(TariffFindQuery.class));
    }

    @Test
    void testFreeTariffBeforeStartBilling() {
        PriceAndTariffValue priceAndTariff = new CancelledOrderFeeTariffService(
                LOCAL_DATE_BEFORE_START_BILLING, environmentService, tariffsService
        ).getForCancelledOrderFee(createOrderItem(1L, true));

        assertEquals(priceAndTariff.getPrice(), BigDecimal.ZERO);
    }

    @Test
    void testFreeTariffForDSBSProgramIsFree() {
        environmentService.setValue("mbi.dsbs.program.cancelled.order.fee.is.free", "true");
        PriceAndTariffValue priceAndTariff = new CancelledOrderFeeTariffService(
                LOCAL_DATE_AFTER_START_BILLING, environmentService, tariffsService
        ).getForCancelledOrderFee(createOrderItem(1L, true));
        assertEquals(priceAndTariff.getPrice(), BigDecimal.ZERO);
    }

    @Test
    void testFreeTariffForDSBSSupplier() {
        environmentService.setValue("mbi.dsbs.program.cancelled.order.fee.is.free", "false");
        environmentService.setValues("mbi.dsbs.suppliers.free.cancelled.order.fee", List.of("1", "2"));

        PriceAndTariffValue priceAndTariff = new CancelledOrderFeeTariffService(
                LOCAL_DATE_AFTER_START_BILLING, environmentService, tariffsService
        ).getForCancelledOrderFee(createOrderItem(1L, true));
        assertEquals(priceAndTariff.getPrice(), BigDecimal.ZERO);
    }


    @Test
    void testNotFreeExternalTariffForNotDSBS() {
        environmentService.setValue("mbi.billing.use.external.tariffs.cancelled_order_fee", "true");
        environmentService.setValue("mbi.dsbs.program.cancelled.order.fee.is.free", "false");
        environmentService.setValues("mbi.dsbs.suppliers.free.cancelled.order.fee", List.of("1", "2"));

        PriceAndTariffValue priceAndTariff = new CancelledOrderFeeTariffService(
                LOCAL_DATE_AFTER_START_BILLING, environmentService, tariffsService
        ).getForCancelledOrderFee(createOrderItem(1L, false)); // айдишник в енв есть, но это не DSBS заказ

        assertEquals(priceAndTariff.getPrice(), new BigDecimal(22200));
    }

    @Test
    void testNotFreeExternalCommonTariffForNotDSBS() {
        environmentService.setValue("mbi.billing.use.external.tariffs.cancelled_order_fee", "true");
        environmentService.setValue("mbi.dsbs.program.cancelled.order.fee.is.free", "false");
        environmentService.setValues("mbi.dsbs.suppliers.free.cancelled.order.fee", List.of("1", "2"));

        PriceAndTariffValue priceAndTariff = new CancelledOrderFeeTariffService(
                LOCAL_DATE_AFTER_START_BILLING, environmentService, tariffsService
        ).getForCancelledOrderFee(createOrderItem(3L, false));

        assertEquals(priceAndTariff.getPrice(), new BigDecimal(11100));
    }

    @Test
    void testTariffNotFoundInTariffsService() {
        environmentService.setValue("mbi.billing.use.external.tariffs.cancelled_order_fee", "true");
        environmentService.setValue("mbi.dsbs.program.cancelled.order.fee.is.free", "false");
        environmentService.setValues("mbi.dsbs.suppliers.free.cancelled.order.fee", List.of("1", "2"));


        Exception exception = Assertions.assertThrows(TariffsException.class, () -> {
            new CancelledOrderFeeTariffService(
                    LOCAL_DATE_END_OF_TARIFF, environmentService, tariffsService
            ).getForCancelledOrderFee(createOrderItem(1L, false));
        });

        String expectedMsg = "Tariff from tariff service is not found for supplierId:";
        assertTrue(exception.getMessage().startsWith(expectedMsg));
    }


    private static OrderItemForBilling createOrderItem(long supplierId, boolean isDSBS) {
        return OrderItemForBilling.builder()
                .withDimensionsSum(3L)
                .withDepth(1L)
                .withWidth(1L)
                .withHeight(1L)
                .withWeight(1L)
                .withRealLength(100L)
                .withRealHeight(100L)
                .withRealWidth(100L)
                .withRealWeight(BigDecimal.valueOf(0.1D))
                .withBillingPrice(BigDecimal.valueOf(310))
                .withSupplierId(supplierId)
                .withCategoryId(1)
                .withItemId(0L)
                .withOrderId(0L)
                .withOrderCreatedAt(LocalDate.of(2020, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant())
                .withFeedId(0L)
                .withOfferId("")
                .withPrice(BigDecimal.ZERO)
                .withCount(0)
                .withDropshipItem(false)
                .withCrossdock(false)
                .withDSBSItem(isDSBS)
                .withClickAndCollectItem(false)
                .withExpressOrder(false)
                .withCashOnlyOrder(false)
                .build();
    }

    private List<TariffDTO> getCancelledOrderTariffs() {
        return List.of(
                createTariff(1L, LOCAL_DATE_BEFORE_START_BILLING, LOCAL_DATE_END_OF_TARIFF, null, List.of(
                        createMeta(new BigDecimal(111))
                )),
                createTariff(2L, LOCAL_DATE_BEFORE_START_BILLING, LOCAL_DATE_END_OF_TARIFF, new Partner().id(1L),
                        List.of(createMeta(new BigDecimal(222)))
                )
        );
    }

    private TariffDTO createTariff(long id, LocalDate from, LocalDate to, Partner partner, List<Object> meta) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setMeta(meta);
        tariff.setDateFrom(from);
        tariff.setDateTo(to);
        tariff.setPartner(partner);
        return tariff;
    }

    private CommonJsonSchema createMeta(BigDecimal amount) {
        return new CancelledOrderFeeJsonSchema()
                .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                .amount(amount)
                .currency("RUB")
                .billingUnit(BillingUnitEnum.ITEM);
    }
}
