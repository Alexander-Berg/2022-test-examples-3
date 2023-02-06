package ru.yandex.market.ff.model.converter;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.dto.RegistryUnitCountDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitIdDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitPartialIdDTO;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.dto.registry.RegistryUnit;
import ru.yandex.market.ff.model.dto.registry.RelatedUnitIds;
import ru.yandex.market.ff.model.dto.registry.UnitCount;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.dto.registry.UnitInfo;
import ru.yandex.market.ff.model.dto.registry.UnitMeta;

import static ru.yandex.market.ff.model.dto.registry.RegistryUnitId.of;

class RegistryUnitDTOConverterTest {

    private final RegistryUnitDTOConverter registryUnitDTOConverter = new RegistryUnitDTOConverter();
    private final SoftAssertions assertions = new SoftAssertions();
    @Test
    void registryEntityToDtoMinimalTest() {
        var registryId = 1L;

        var palletId = of(RegistryUnitIdType.PALLET_ID, "PL0001");
        RegistryUnit registryUnit = RegistryUnit.palletBuilder()
                .registryId(registryId)
                .unitInfo(UnitInfo.builder()
                        .unitId(palletId)
                        .unitCountsInfo(UnitCountsInfo.of(
                                UnitCount.of(
                                        UnitCountType.FIT,
                                        1,
                                        RelatedUnitIds.asSingleParenting(
                                                of(RegistryUnitIdType.CIS, "CIS0002"),
                                                of(RegistryUnitIdType.PALLET_ID, "PALLET_ID_0002")
                                        ),
                                        null
                                )))
                        .build()
                )
                .unitMeta(UnitMeta.builder().description("Some pallet").build())
                .build();
        RegistryUnitDTO dto = registryUnitDTOConverter.registryEntityToDTO(registryUnit);

        assertions.assertThat(dto.getType()).isEqualTo(RegistryUnitType.PALLET);
        assertions.assertThat(dto.getRegistryId()).isEqualTo(registryId);
        assertions.assertThat(dto.getUnitInfo().getUnitId().getParts())
                .containsExactly(new RegistryUnitPartialIdDTO(RegistryUnitIdType.PALLET_ID, "PL0001"));
        assertions.assertThat(dto.getUnitInfo().getUnitCountsInfo().getUnitCounts())
                .containsExactly(new RegistryUnitCountDTO(
                        UnitCountType.FIT,
                        1,
                        List.of(RegistryUnitIdDTO.of(RegistryUnitIdType.CIS, "CIS0002")),
                        List.of())
                );
        assertions.assertThat(dto.getMeta().getComment()).isEqualTo("Some pallet");
        assertions.assertAll();
    }
}
