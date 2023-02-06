package ru.yandex.market.billing.express;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.order.model.ValueType;
import ru.yandex.market.billing.fulfillment.OrderType;
import ru.yandex.market.billing.fulfillment.orders.FulfillmentOrderBillingService;
import ru.yandex.market.billing.fulfillment.tariffs.FulfillmentTariff;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsService;
import ru.yandex.market.billing.model.billing.BillingServiceType;
import ru.yandex.market.billing.order.model.BillingUnit;
import ru.yandex.market.billing.service.environment.EnvironmentAwareDateValidationService;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.fulfillment.entities.base.DateTimeInterval;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


class ExpressDeliveryItemsBillingServiceTest extends FunctionalTest {
    private static final LocalDate DATE_2022_01_01 = LocalDate.of(2022, 1, 1);
    private static final LocalDate DATE_2021_12_01 = LocalDate.of(2021, 12, 1);
    private static final Set<Long> PARTNERS_FOR_BILL = Set.of(555L, 556L);

    private ExpressDeliveryItemsBillingService expressDeliveryItemsBillingService;

    @Autowired
    @Qualifier("testFulfillmentOrderBillingService")
    private FulfillmentOrderBillingService fulfillmentOrderBillingService;

    @Autowired
    @Qualifier("clientTariffsService")
    private TariffsService fulfillmentTariffDao;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private EnvironmentAwareDateValidationService environmentAwareDateValidationService;


    private ExceptionCollector exceptionCollector;

    @BeforeEach
    void setup() {
        expressDeliveryItemsBillingService = new ExpressDeliveryItemsBillingService(
                environmentAwareDateValidationService,
                fulfillmentOrderBillingService,
                environmentService);
        this.exceptionCollector = new ExceptionCollector();
    }

    @AfterEach
    void endTest() {
        assertDoesNotThrow(
                () -> this.exceptionCollector.close()
        );
    }

    @Test
    @DisplayName("Поайтемное обилливание для всех услуг экспресс-доставки.")
    @DbUnitDataSet(
            before = "ExpressDeliveryItemsBillingServiceTest.before.csv",
            after = "ExpressDeliveryItemsBillingServiceTest.after.csv"
    )
    void test() {
        mock(ListUtils.union(createExpressDeliveredTariffs(), createExpressCanceledByPartnerTariffs()));
        expressDeliveryItemsBillingService.process(DATE_2022_01_01);
    }

    @Test
    @DisplayName("Поайтемное обилливание экспрессов. Проверяем, что не биллим заигноренные товары заказа.")
    @DbUnitDataSet(
            before = {
                    "ExpressDeliveryItemsBillingServiceTest.before.csv",
                    "ExpressDeliveryItemsBillingServiceTest.testSkipIgnoredOrderItems.before.csv"
            },
            after = "ExpressDeliveryItemsBillingServiceTest.testSkipIgnoredOrderItems.after.csv"
    )
    void testSkipIgnoredOrderItems() {
        mock(ListUtils.union(createExpressDeliveredTariffs(), createExpressCanceledByPartnerTariffs()));
        expressDeliveryItemsBillingService.process(DATE_2022_01_01);
    }

    @Test
    @DisplayName("Поайтемное обилливание экспрессов. Проверяем, что получаем ошибку, " +
            "если дата для обилливания раньше 1-го января 2022 года")
    void testWhenDateEarlierThanExpressItemBillingStartDate() {
        final IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                () -> expressDeliveryItemsBillingService.process(DATE_2021_12_01)
        );

        Assertions.assertEquals("Current date 2021-12-01 is earlier than express items delivery billing date",
                exception.getMessage());
    }

    @Test
    @DisplayName("Поайтемное обилливание экспрессов. Проверяем, что биллим только указанных партнеров.")
    @DbUnitDataSet(
            before = "ExpressDeliveryItemsBillingServiceTest.testWithSpecifiedPartners.before.csv",
            after = "ExpressDeliveryItemsBillingServiceTest.testWithSpecifiedPartners.after.csv"
    )
    void testWithSpecifiedPartners() {
        mock(ListUtils.union(createExpressDeliveredTariffs(), createExpressCanceledByPartnerTariffs()));
        expressDeliveryItemsBillingService.processSpecifiedPartners(DATE_2022_01_01, PARTNERS_FOR_BILL);
    }

    @Test
    @DisplayName("Поайтемное обилливание экспрессов. Обилливание услуги EXPRESS_DELIVERED.")
    @DbUnitDataSet(
            before = "ExpressDeliveryItemsBillingServiceTest.testExpressDelivered.before.csv",
            after = "ExpressDeliveryItemsBillingServiceTest.testExpressDelivered.after.csv"
    )
    void testExpressDelivered() {
        mock(createExpressDeliveredTariffs());
        fulfillmentOrderBillingService.billForPartners(
                BillingServiceType.EXPRESS_DELIVERED,
                DATE_2022_01_01,
                null,
                Collections.emptySet(),
                exceptionCollector);
    }

    @Test
    @DisplayName("Поайтемное обилливание экспрессов. Обилливание услуги EXPRESS_CANCELLED_BY_PARTNER.")
    @DbUnitDataSet(
            before = "ExpressDeliveryItemsBillingServiceTest.testExpressCanceledByPartner.before.csv",
            after = "ExpressDeliveryItemsBillingServiceTest.testExpressCanceledByPartner.after.csv"
    )
    void testExpressCanceledByPartner() {
        mock(createExpressCanceledByPartnerTariffs());
        fulfillmentOrderBillingService.billForPartners(
                BillingServiceType.EXPRESS_CANCELLED_BY_PARTNER,
                DATE_2022_01_01,
                null,
                Collections.emptySet(),
                exceptionCollector);
    }

    @Test
    @DisplayName("Поайтемное обилливание экспрессов. Обилливание услуги EXPRESS_DELIVERED_CANCELLATION.")
    @DbUnitDataSet(
            before = "ExpressDeliveryItemsBillingServiceTest.testExpressDeliveredCancellation.before.csv",
            after = "ExpressDeliveryItemsBillingServiceTest.testExpressDeliveredCancellation.after.csv"
    )
    void testExpressDeliveredCancellation() {
        fulfillmentOrderBillingService.billForPartners(
                BillingServiceType.EXPRESS_DELIVERED_CANCELLATION,
                LocalDate.of(2022, 1, 2),
                null,
                Collections.emptySet(),
                exceptionCollector);
    }

    @Test
    @DisplayName("Поайтемное обилливание экспрессов - EXPRESS_DELIVERED_CANCELLATION для квантовых товаров.")
    @DbUnitDataSet(
            before = "ExpressDeliveryItemsBillingServiceTest.testExpressDeliveredCancellation_quantum.before.csv",
            after = "ExpressDeliveryItemsBillingServiceTest.testExpressDeliveredCancellation_quantum.after.csv"
    )
    void testExpressDeliveredCancellationOfQuantumItems() {
        fulfillmentOrderBillingService.billForPartners(
                BillingServiceType.EXPRESS_DELIVERED_CANCELLATION,
                LocalDate.of(2022, 5, 3),
                null,
                Collections.emptySet(),
                exceptionCollector);
    }

    private static List<FulfillmentTariff> createExpressDeliveredTariffs() {
        return List.of(
                createTariff(
                        LocalDate.of(2022, 1, 1),
                        LocalDate.MAX,
                        BillingServiceType.EXPRESS_DELIVERED,
                        4000000L,
                        1000000L,
                        800,
                        OrderType.DROP_SHIP

                ),
                createTariff(
                        LocalDate.of(2022, 1, 1),
                        LocalDate.MAX,
                        BillingServiceType.EXPRESS_DELIVERED,
                        null,
                        null,
                        1000,
                        OrderType.DROP_SHIP
                )
        );
    }

    private static List<FulfillmentTariff> createExpressCanceledByPartnerTariffs() {
        return List.of(
                createTariff(
                        LocalDate.of(2022, 1, 1),
                        LocalDate.MAX,
                        BillingServiceType.EXPRESS_CANCELLED_BY_PARTNER,
                        null,
                        null,
                        1000,
                        OrderType.DROP_SHIP
                )
        );
    }

    private static FulfillmentTariff createTariff(
            LocalDate from,
            LocalDate to,
            BillingServiceType serviceType,
            Long dimensionsTo,
            Long weightTo,
            int value,
            OrderType orderType) {
        return new FulfillmentTariff(
                new DateTimeInterval(
                        createOffsetDateTime(from),
                        createOffsetDateTime(to)
                ),
                serviceType,
                null,
                dimensionsTo,
                weightTo,
                value,
                null,
                null,
                ValueType.ABSOLUTE,
                BillingUnit.ITEM,
                orderType,
                null,
                null,
                null);
    }

    private static OffsetDateTime createOffsetDateTime(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.systemDefault()).toOffsetDateTime();
    }

    private void mock(List<FulfillmentTariff> tariffList) {
        Mockito.when(fulfillmentTariffDao.getOrderedTariffs(Mockito.any())).thenReturn(tariffList);
    }
}
