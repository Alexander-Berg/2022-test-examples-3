package ru.yandex.market.billing.fulfillment.orders;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.fulfillment.OrderType;
import ru.yandex.market.billing.fulfillment.tariffs.FulfillmentTariff;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsService;
import ru.yandex.market.billing.model.billing.BillingServiceType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import static ru.yandex.market.billing.fulfillment.tariffs.TestTariffCreationUtil.createFulfillmentTariff;


@SuppressWarnings("ParameterNumber")
class FulfillmentRecalculateBillingServiceTest extends FunctionalTest {

    private static final LocalDate DATE_FROM = LocalDate.of(2022, 5, 1);
    private static final LocalDate DATE_TO = LocalDate.of(2022, 5, 4);
    private static final List<FulfillmentTariff> DEFAULT_TARIFF = List.of(
            createFulfillmentTariff(
                    LocalDate.of(2022, 5, 1),
                    LocalDate.MAX,
                    BillingServiceType.DELIVERY_TO_CUSTOMER,
                    15000L,
                    25000L,
                    40000,
                    OrderType.FULFILLMENT)
    );

    @Autowired
    private FulfillmentOrderBillingService fulfillmentOrderBillingService;
    @Autowired
    private FulfillmentRecalculateBillingDao fulfillmentRecalculateBillingDao;
    @Autowired
    @Qualifier("clientTariffsService")
    private TariffsService fulfillmentTariffDao;


    private FulfillmentRecalculateBillingService fulfillmentRecalculateBillingService;

    @BeforeEach
    void init() {
        fulfillmentRecalculateBillingService = new FulfillmentRecalculateBillingService(
                fulfillmentOrderBillingService,
                fulfillmentRecalculateBillingDao
        );
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentRecalculateBillingServiceTest.process.before.csv",
            after = "FulfillmentRecalculateBillingServiceTest.process.after.csv"
    )
    @DisplayName("Переобиливаются услуги зависящие от ВГХ c изменившимся ВГХ сначала текущего месяца" +
            "(на текущий момент доставка)")
    void process() {
        Mockito.when(fulfillmentTariffDao.getOrderedTariffs(Mockito.any())).thenReturn(DEFAULT_TARIFF);
        fulfillmentRecalculateBillingService.process(DATE_FROM, DATE_TO);
    }
}
