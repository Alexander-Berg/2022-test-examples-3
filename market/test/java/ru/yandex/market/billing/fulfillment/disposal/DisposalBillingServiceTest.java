package ru.yandex.market.billing.fulfillment.disposal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsIterator;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.DisposalJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Тесты для {@link DisposalBillingService}.
 */
public class DisposalBillingServiceTest extends FunctionalTest {

    private static final BigDecimal RUB_39 = BigDecimal.valueOf(39);
    private static final BigDecimal RUB_390 = BigDecimal.valueOf(390);

    @Autowired
    private DisposalBillingService disposalBillingService;

    @Autowired
    private TariffsService tariffsService;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");
            Assertions.assertNotNull(findQuery.getTargetDate());
            Assertions.assertEquals(findQuery.getServiceType(), ServiceTypeEnum.SELF_REQUESTED_DISPOSAL);

            return new TariffsIterator((pageNumber, batchSize) -> List.of(
                    createTariff(1L, LocalDate.of(2021, 3, 1),
                            List.of(
                                    createMeta(RUB_39, 150),
                                    createMeta(RUB_390, null)))
            ));
        }).when(tariffsService).findTariffs(any(TariffFindQuery.class));
    }

    @DisplayName("Проверка обилливания утилизации")
    @DbUnitDataSet(
            before = "db/DisposalBillingServiceTest.before.csv",
            after = "db/DisposalBillingServiceTest.after.csv"
    )
    @Test
    void test_calculateDisposalSupplies() {
        disposalBillingService.process(LocalDate.of(2019, 10, 5));
    }

    @DisplayName("Обилливание только для указанных поставщиков")
    @DbUnitDataSet(
            before = "db/DisposalBillingServiceTest.billSelectedSuppliers.before.csv",
            after = "db/DisposalBillingServiceTest.billSelectedSuppliers.after.csv"
    )
    @Test
    void billSelectedSuppliers() {
        disposalBillingService.billForSuppliers(LocalDate.of(2019, 10, 5), Set.of(21L, 23L));
    }

    private TariffDTO createTariff(long id, LocalDate from, List<Object> meta) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setIsActive(true);
        tariff.setServiceType(ServiceTypeEnum.SELF_REQUESTED_DISPOSAL);
        tariff.setDateFrom(from);
        tariff.setDateTo(null);
        tariff.setMeta(meta);
        return tariff;
    }

    private CommonJsonSchema createMeta(BigDecimal amount, Integer dimensionTo) {
        return new DisposalJsonSchema()
                .dimensionsTo(dimensionTo)
                .amount(amount)
                .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                .amount(amount)
                .currency("RUB")
                .billingUnit(BillingUnitEnum.ITEM);
    }
}
