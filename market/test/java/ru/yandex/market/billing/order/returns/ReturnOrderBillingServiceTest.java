package ru.yandex.market.billing.order.returns;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsIterator;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsService;
import ru.yandex.market.billing.order.returns.billing.ReturnOrderBillingService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.PartnerType;
import ru.yandex.market.mbi.tariffs.client.model.ReturnTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.ReturnedOrderStorageJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public class ReturnOrderBillingServiceTest extends FunctionalTest {

    private static final LocalDate DATE_2022_03_02 = LocalDate.of(2022, 3, 2);
    private static final LocalDate DATE_2022_03_09 = LocalDate.of(2022, 3, 9);

    private static final Set<Long> SPECIFIED_PARTNERS = Set.of(12453L, 12455L);

    @Autowired
    private ReturnOrderBillingService returnOrderBillingService;

    @Autowired
    private TariffsService clientTariffsService;

    @Test
    @DisplayName("Повозвратное обилливание возвратов.")
    @DbUnitDataSet(
            before = "ReturnOrderBillingServiceTest.testReturnOrderBilling.before.csv",
            after = "ReturnOrderBillingServiceTest.testReturnOrderBilling.after.csv"
    )
    void testReturnOrderBilling() {
        mockTariffs(createTariffsForReturnOrder());
        returnOrderBillingService.process(DATE_2022_03_02);
    }

    @Test
    @DisplayName("Повозвратное обилливание возвратов для указанного списка партнеров.")
    @DbUnitDataSet(
            before = "ReturnOrderBillingServiceTest.testReturnOrderBillingForSpecifiedPartners.before.csv",
            after = "ReturnOrderBillingServiceTest.testReturnOrderBillingForSpecifiedPartners.after.csv"
    )
    void testReturnOrderBillingForSpecifiedPartners() {
        mockTariffs(createTariffsForReturnOrder());
        returnOrderBillingService.process(DATE_2022_03_09, SPECIFIED_PARTNERS);
    }

    @Test
    @DisplayName("Повозвратное обилливание возвратов. Проверяем, что не биллим заигноренные возвраты.")
    @DbUnitDataSet(
            before = "ReturnOrderBillingServiceTest.testSkipIgnoredLogisticReturnIds.before.csv",
            after = "ReturnOrderBillingServiceTest.testSkipIgnoredLogisticReturnIds.after.csv"
    )
    void testSkipIgnoredLogisticReturnIds() {
        mockTariffs(createTariffsForReturnOrder());
        returnOrderBillingService.process(DATE_2022_03_02);
    }

    @Test
    @DisplayName("Повозвратное обилливание возвратов. Проверяем, что не биллим на статусе IN_TRANSIT и продолжаем " +
            "биллинг после второго перехода в READY_FOR_PICKUP из статуса IN_TRANSIT.")
    @DbUnitDataSet(
            before = "ReturnOrderBillingServiceTest.testBillingAfterSecondInTransitStatus.before.csv",
            after = "ReturnOrderBillingServiceTest.testBillingAfterSecondInTransitStatus.after.csv"
    )
    void testBillingAfterSecondInTransitStatus() {
        mockTariffs(createTariffsForReturnOrder());
        Stream.iterate(DATE_2022_03_09, date -> date.plusDays(1))
                .limit(3)
                .forEach(date -> returnOrderBillingService.process(date));
    }

    @Test
    @DisplayName("Обилливание невыкупов.")
    @DbUnitDataSet(
            before = "ReturnOrderBillingServiceTest.testUnredeemedOrderBilling.before.csv",
            after = "ReturnOrderBillingServiceTest.testUnredeemedOrderBilling.after.csv"
    )
    void testUnredeemedOrderBilling() {
        mockTariffs(createTariffsForReturnOrder());
        returnOrderBillingService.process(DATE_2022_03_02);
    }

    private List<TariffDTO> createTariffsForReturnOrder() {
        return List.of(
                createTariff(1L, null, LocalDate.parse("2022-01-01"), LocalDate.MAX, ModelType.FULFILLMENT_BY_SELLER,
                        List.of(
                                createMeta(1L, 8L, ReturnTypeEnum.RETURN, new BigDecimal("0")),
                                createMeta(8L, 15L, ReturnTypeEnum.RETURN, new BigDecimal("15"))
                        )),
                createTariff(2L, 12453L, LocalDate.parse("2022-01-01"), LocalDate.MAX, ModelType.FULFILLMENT_BY_SELLER,
                        List.of(
                                createMeta(1L, 8L, ReturnTypeEnum.RETURN, new BigDecimal("0")),
                                createMeta(8L, 15L, ReturnTypeEnum.RETURN, new BigDecimal("10"))
                        )),
                createTariff(3L, 1245L, LocalDate.parse("2022-01-01"), LocalDate.MAX, ModelType.FULFILLMENT_BY_SELLER,
                        List.of(
                                createMeta(1L, 8L, ReturnTypeEnum.UNREDEEMED, new BigDecimal("0")),
                                createMeta(8L, 15L, ReturnTypeEnum.UNREDEEMED, new BigDecimal("15"))
                        )),
                createTariff(2L, 12423L, LocalDate.parse("2022-01-01"), LocalDate.MAX, ModelType.FULFILLMENT_BY_SELLER,
                        List.of(
                                createMeta(1L, 8L, ReturnTypeEnum.RETURN, new BigDecimal("0")),
                                createMeta(8L, 15L, ReturnTypeEnum.RETURN, new BigDecimal("20"))
                        ))
        );
    }

    private TariffDTO createTariff(long id, Long partnerId, LocalDate from, LocalDate to, ModelType modelType,
                                   List<Object> meta) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setPartner(new Partner().id(partnerId).type(PartnerType.SUPPLIER));
        tariff.setIsActive(true);
        tariff.setDateFrom(from);
        tariff.setModelType(modelType);
        tariff.setServiceType(ServiceTypeEnum.RETURNED_ORDERS_STORAGE);
        tariff.setDateTo(to);
        tariff.setMeta(meta);
        return tariff;
    }

    private CommonJsonSchema createMeta(
            Long daysOnStockFrom,
            Long daysOnStockTo,
            ReturnTypeEnum returnType,
            BigDecimal amount
    ) {
        return new ReturnedOrderStorageJsonSchema()
                .daysOnStockFrom(daysOnStockFrom)
                .daysOnStockTo(daysOnStockTo)
                .returnType(returnType)
                .billingUnit(BillingUnitEnum.ORDER)
                .amount(amount)
                .currency("RUB")
                .type(CommonJsonSchema.TypeEnum.ABSOLUTE);
    }

    private void mockTariffs(List<TariffDTO> tariffDTOList) {
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");

            LocalDate targetDate = Objects.requireNonNull(findQuery.getTargetDate());
            return new TariffsIterator((pageNumber, batchSize) -> {
                if (pageNumber != 0) {
                    return List.of();
                }
                return tariffDTOList
                        .stream()
                        .filter(tariff -> tariff.getServiceType() == ServiceTypeEnum.RETURNED_ORDERS_STORAGE)
                        .filter(tariff -> {
                            LocalDate to = tariff.getDateTo();
                            LocalDate from = tariff.getDateFrom();
                            return targetDate.compareTo(from) >= 0
                                    && (to == null || targetDate.compareTo(to) < 0);
                        })
                        .collect(Collectors.toList());
            });
        }).when(clientTariffsService).findTariffs(any(TariffFindQuery.class));
    }
}
