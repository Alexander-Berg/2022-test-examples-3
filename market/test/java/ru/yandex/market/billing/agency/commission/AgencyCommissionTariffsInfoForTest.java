package ru.yandex.market.billing.agency.commission;

import java.util.Set;

import ru.yandex.market.billing.agency.model.AgencyCommissionTariff;
import ru.yandex.market.billing.core.factoring.PayoutFrequency;
import ru.yandex.market.billing.core.order.model.ValueType;

@SuppressWarnings("checkstyle:all")
public class AgencyCommissionTariffsInfoForTest {

    public static final AgencyCommissionTariff ZERO =
            AgencyCommissionTariff.builder()
                    .setValue(0L)
                    .setValueType(ValueType.RELATIVE)
                    .build();
    /**
     * Тариф АВ в процентах при частоте выплат {@link PayoutFrequency#MONTHLY}
     */
    public static final long MONTHLY_AGENCY_COMMISSION_TARIFF = 100L;

    /**
     * Тариф АВ в процентах при частоте выплат {@link PayoutFrequency#BI_WEEKLY}
     */
    public static final long BI_WEEKLY_AGENCY_COMMISSION_TARIFF = 130L;

    /**
     * Тариф АВ в процентах при частоте выплат {@link PayoutFrequency#WEEKLY}
     */
    public static final long WEEKLY_AGENCY_COMMISSION_TARIFF = 180L;

    /**
     * Тариф АВ в процентах при частоте выплат {@link PayoutFrequency#DAILY}
     */
    public static final long DAILY_AGENCY_COMMISSION_TARIFF = 220L;

    /**
     * Список контрактов с умными партнерами, которые зафиксировали в ДС ставку в 1%
     */
    public static final Set<Long> PROMO_TARIFF_CONTRACTS = Set.of(
            516462L,
            912706L,
            1312965L,
            2022078L,
            2109538L,
            2759383L,
            2827478L,
            3384739L,
            3189329L,
            2954211L,
            3484178L,
            3261203L,
            3403495L,
            3451083L,
            3432045L,
            3589881L,
            3584356L,
            3587225L,
            3651720L,
            3671748L,
            3696175L,
            3767147L,
            3651621L,
            3850257L,
            3915176L,
            391296L);

    /**
     * Особые контракты партнеров, для которых __не__ нужно биллить АВ совсем.
     */
    public static final long ZERO_AGENCY_COMMISSION_TARIFF = 0L;

    public static final Set<Long> ZERO_TARIFF_CONTRACTS = Set.of(
            4625488L
    );
}
