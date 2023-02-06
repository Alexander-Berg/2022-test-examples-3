package ru.yandex.market.billing.sortingcenter.tariff;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.sortingcenter.model.SCServiceType;
import ru.yandex.market.billing.sortingcenter.model.SCTariff;
import ru.yandex.market.billing.sortingcenter.model.SCTariffDTO;
import ru.yandex.market.billing.sortingcenter.model.SCTariffMin;
import ru.yandex.market.billing.sortingcenter.utils.SCDateUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SCTariffHolderTest extends FunctionalTest {
    private static final long SC_ID = 1L;
    private static final SCServiceType SERVICE_TYPE = SCServiceType.ORDER_SHIPPED_TO_SO_FF;
    private static final Instant TARIFF_START_TIME = Instant.parse("2021-12-20T00:00:00.00Z");

    private static final BigDecimal TARIFF_1_VALUE = BigDecimal.valueOf(1.0d);
    private static final BigDecimal TARIFF_2_VALUE = BigDecimal.valueOf(2.0d);

    private SCTariffHolder tariffHolder = null;

    private static SCTariff buildTariff(long from, long to, BigDecimal value) {
        return preBuildSCTariff()
                .fromCount(from)
                .toCount(to)
                .value(value)
                .build();
    }

    private static SCTariffDTO.SCTariffDTOBuilder preBuildSCTariff() {
        return SCTariffDTO.builder()
                .scId(SC_ID)
                .serviceType(SERVICE_TYPE)
                .startTime(SCDateUtils.toMoscowDate(TARIFF_START_TIME));
    }

    private static SCTariffMin buildTariffMinimal(int min, BigDecimal kgt) {
        return prebuildSCTariffMinimal()
                .min(min)
                .kgt(kgt)
                .build();
    }

    private static SCTariffMin.SCTariffMinBuilder prebuildSCTariffMinimal() {
        return SCTariffMin.builder()
                .scId(SC_ID)
                .serviceType(SERVICE_TYPE)
                .startTime(SCDateUtils.toMoscowDate(TARIFF_START_TIME));
    }

    @BeforeEach
    void setUp() {
        var tariff1 = buildTariff(100L, 200L, TARIFF_1_VALUE);
        var tariff2 = buildTariff(201L, Long.MAX_VALUE, TARIFF_2_VALUE);
        var tariffList = Lists.newArrayList(tariff1, tariff2);
        var tariffMin = buildTariffMinimal(100, BigDecimal.valueOf(3.0d));
        var tariffMinimals = Lists.newArrayList(tariffMin);
        tariffHolder = new SCTariffHolder();
        tariffHolder.addTariffs(tariffList);
        tariffMinimals.forEach(scTariffMin -> {
            Long scId = scTariffMin.getScId();
            SCServiceType serviceType = scTariffMin.getServiceType();
            tariffHolder.addMinimalCount(scId, serviceType, scTariffMin.getMin());
            tariffHolder.addKGTTariff(scId, serviceType, scTariffMin.getKgt());
        });
    }

    @Test
    void getFor() {
        SCTariffData sCTariffData = tariffHolder.getTariffDataBy(SC_ID, SERVICE_TYPE).orElseThrow();

        Optional<SCTariff> tariffFor50 = sCTariffData.getTariffByCount(50L);
        assertFalse(tariffFor50.isPresent());

        SCTariff tariffFor150 = sCTariffData.getTariffByCount(150L).orElseThrow();
        assertEquals(TARIFF_1_VALUE, tariffFor150.getValue());

        SCTariff tariffFor250 = sCTariffData.getTariffByCount(250L).orElseThrow();
        assertEquals(TARIFF_2_VALUE, tariffFor250.getValue());
    }
}
