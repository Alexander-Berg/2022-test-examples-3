package ru.yandex.market.ff.service.registry;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.dto.registry.RegistryUnit;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.RelatedUnitIds;
import ru.yandex.market.ff.model.dto.registry.UnitCount;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.dto.registry.UnitInfo;
import ru.yandex.market.ff.model.dto.registry.UnitMeta;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.ff.model.dto.registry.RegistryUnitId.of;

class RegistryServiceTest extends IntegrationTest {

    @Autowired
    private RegistryService registryUnitService;

    /**
     * Тест на создание планового реестра.
     */
    @Test
    @DatabaseSetup(value = "classpath:service/registry/1/before.xml")
    @ExpectedDatabase(value = "classpath:service/registry/1/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void onSuccessfulPlanSaving() {
        var requestId = 1L;
        registryUnitService.createPlanRegistry(requestId, List.of(createPallet()));
    }

    /**
     * Тест на создание фактического реестра.
     */
    @Test
    @DatabaseSetup(value = "classpath:service/registry/2/before.xml")
    @ExpectedDatabase(value = "classpath:service/registry/2/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void onSuccessfulFactSaving() {
        var requestId = 1L;
        OffsetDateTime date = OffsetDateTime.of(2020, 1, 1, 10, 10, 10, 0, ZoneOffset.UTC);
        registryUnitService.createFactRegistry(requestId, date, List.of(createPallet()));
    }

    private RegistryUnit createPallet() {
        var palletId = RegistryUnitId.of(RegistryUnitIdType.PALLET_ID, "PL0001");
        return RegistryUnit.palletBuilder()
                .unitInfo(UnitInfo.builder()
                        .unitId(palletId)
                        .unitCountsInfo(UnitCountsInfo.of(
                                UnitCount.of(UnitCountType.FIT, 1),
                                RelatedUnitIds.asOrphan(of(RegistryUnitIdType.CIS, "CIS0002"))
                        ))
                        .build()
                )
                .unitMeta(UnitMeta.empty())
                .build();
    }

}
