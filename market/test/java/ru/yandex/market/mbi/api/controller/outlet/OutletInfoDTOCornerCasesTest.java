package ru.yandex.market.mbi.api.controller.outlet;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.DeliveryRuleDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.OutletInfoDTO;

/**
 * Тесты для метода {@link OutletInfoDTO#toOutletInfo()}.
 */
class OutletInfoDTOCornerCasesTest {

    /**
     * Тест проверяет преобразование пустого класса.
     */
    @Test
    void testNullV2toV1() {
        OutletInfoDTO outletInfoDTO = new OutletInfoDTO(
                1,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        OutletInfo actual = outletInfoDTO.toOutletInfo();
        OutletInfo expected = new OutletInfo(1, 1, null, null, null, null);
        Assertions.assertEquals(expected, actual);
    }

    /**
     * Тест провверяет преобразование в DeliveryRule без shipperId.
     */
    @Test
    void testNullShipperIdV2toV1() {
        OutletInfoDTO outletInfoDTO = new OutletInfoDTO(
                1,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ImmutableList.of(new DeliveryRuleDTO(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )),
                null,
                null,
                null,
                null
        );
        OutletInfo outletInfo = outletInfoDTO.toOutletInfo();
        Assertions.assertEquals(1, outletInfo.getDeliveryRules().size());
    }
}
