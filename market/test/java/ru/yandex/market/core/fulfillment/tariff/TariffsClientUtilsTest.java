package ru.yandex.market.core.fulfillment.tariff;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TariffsClientUtilsTest {

    private static final Set<ServiceTypeEnum> typesToIgnore = Set.of(
            ServiceTypeEnum.GLOBAL_FEE,
            ServiceTypeEnum.GLOBAL_DELIVERY,
            ServiceTypeEnum.GLOBAL_AGENCY_COMMISSION,
            ServiceTypeEnum.FIXED_TARIFFS,
            ServiceTypeEnum.DISTRIBUTION,
            ServiceTypeEnum.FF_PARTNER,
            ServiceTypeEnum.FF_STORAGE_BILLING_MULTIPLIER,
            ServiceTypeEnum.INTAKE,
            ServiceTypeEnum.MIN_DAILY,
            ServiceTypeEnum.RETURNED_ORDERS_STORAGE,
            ServiceTypeEnum.SELF_REQUESTED_DISPOSAL,
            ServiceTypeEnum.CANCELLED_EXPRESS_ORDER_FEE,
            ServiceTypeEnum.CANCELLED_ORDER_FEE,
            ServiceTypeEnum.FF_STORAGE_TURNOVER,
            // тарифы курьерских смен, для tpl-billing
            ServiceTypeEnum.COURIER_SHIFT,
            ServiceTypeEnum.COURIER_DROPOFF_RETURN,
            ServiceTypeEnum.COURIER_BULKY_CARGO,
            ServiceTypeEnum.COURIER_SMALL_GOODS,
            ServiceTypeEnum.COURIER_WITHDRAW,
            ServiceTypeEnum.COURIER_YANDEX_DRIVE,
            ServiceTypeEnum.COURIER_FINE,
            ServiceTypeEnum.COURIER_VELO_SHIFT,
            // пвз тарифы, они в обычном биллинге не нужны
            ServiceTypeEnum.PVZ_REWARD,
            ServiceTypeEnum.PVZ_CASH_COMPENSATION,
            ServiceTypeEnum.PVZ_CARD_COMPENSATION,
            ServiceTypeEnum.PVZ_DROPOFF,
            ServiceTypeEnum.PVZ_DROPOFF_RETURN,
            ServiceTypeEnum.PVZ_RETURN,
            ServiceTypeEnum.PVZ_BRANDED_DECORATION,
            ServiceTypeEnum.PVZ_REWARD_YADO,
            ServiceTypeEnum.PVZ_REWARD_DBS,
            ServiceTypeEnum.PVZ_DBS_INCOME,
            ServiceTypeEnum.PVZ_DBS_OUTCOME,
            // Тарифы сортировочных центров так как они не используются в обычном биллинге
            ServiceTypeEnum.SC_REWARD,
            ServiceTypeEnum.SC_KGT_REWARD,
            ServiceTypeEnum.SC_MINIMAL_REWARD
    );

    /**
     * Проверяет, что для всех тарифов из тариффницы есть соответствующая конвертация.
     * Если тарифы отсутствуют в конвертации, то либо их необходимо туда добавить, либо в случае отсутствия нужжды
     * конвертировать надо их добавить в {@param typesToIgnore}
     */
    @Test
    void testBillingServiceTypeExistsForAllTariffs() {
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            Arrays.stream(ServiceTypeEnum.values())
                    .filter(type -> !typesToIgnore.contains(type))
                    .forEach(exceptionCollector.wrap(type ->
                        TariffsClientUtils.convertToBillingServiceType(type)
                    ));
        }
    }

    @Test
    void convertToBillingServiceType() {
        BillingServiceType actual = TariffsClientUtils.convertToBillingServiceType(ServiceTypeEnum.CASH_ONLY_ORDER);
        assertEquals(actual, BillingServiceType.FEE);
    }
}
