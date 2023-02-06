package ru.yandex.market.partner.mvc.controller.delivery.region.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.delivery.tariff.model.TariffType;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.partner.delivery.region.dto.ParamCheckStatusDTO;
import ru.yandex.market.partner.delivery.region.dto.TariffTypeDTO;

/**
 * Проверка DTO, используемых в
 * {@link ru.yandex.market.partner.mvc.controller.delivery.region.LegacyDeliveryRegionController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DTOTest {
    @Test
    @DisplayName("Все сущности ParamCheckStatus мапаются на dto")
    void testParamCheckStatusDTO() {
        for (final ParamCheckStatus status : ParamCheckStatus.values()) {
            ParamCheckStatusDTO.map(status);
        }
    }

    @Test
    @DisplayName("Все сущности TariffType мапаются на dto")
    void testTariffTypeDTO() {
        for (final TariffType type : TariffType.values()) {
            TariffTypeDTO.map(type);
        }
    }
}
