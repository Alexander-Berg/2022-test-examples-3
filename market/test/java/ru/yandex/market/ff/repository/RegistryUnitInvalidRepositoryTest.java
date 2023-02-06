package ru.yandex.market.ff.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;


import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitInvalidEntity;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitInvalidReason;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitInvalidStatus;

public class RegistryUnitInvalidRepositoryTest extends IntegrationTest {
    @Autowired
    private RegistryUnitInvalidRepository registryUnitInvalidRepository;

    @Test
    @DatabaseSetup("classpath:repository/registry-unit-invalid/before.xml")
    @ExpectedDatabase(value = "classpath:repository/registry-unit-invalid/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void saveRegistryUnitInvalidMustSave() {
        RegistryUnitInvalidEntity registryUnitInvalid = RegistryUnitInvalidEntity.builder()
                .sourceRegistryId(1L)
                .type(RegistryUnitType.BAG)
                .status(RegistryUnitInvalidStatus.INVALID)
                .reasons(List.of(RegistryUnitInvalidReason.MISSING_RETURN_ID,
                        RegistryUnitInvalidReason.MISSING_ORDER_ID))
                .unitCountsInfo(new UnitCountsInfo())
                .identifiers(RegistryUnitId.of(
                        UnitPartialId.builder().type(RegistryUnitIdType.BAG_ID).value("someValue").build()))
                .build();

        registryUnitInvalidRepository.save(registryUnitInvalid);
    }

    @Test
    @DatabaseSetup("classpath:repository/registry-unit-invalid/after.xml")
    @ExpectedDatabase(value = "classpath:repository/registry-unit-invalid/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void tryUpdateReasonsColumnNothingChange() {
        RegistryUnitInvalidEntity invalidEntity = registryUnitInvalidRepository.findOne(1L);

        invalidEntity.setReasons(List.of(RegistryUnitInvalidReason.MISSING_RETURN_ID));

        registryUnitInvalidRepository.save(invalidEntity);
    }

    @Test
    @DatabaseSetup("classpath:repository/registry-unit-invalid/before-count.xml")
    void getCountBetweenDates() {
        LocalDateTime from = LocalDateTime.parse("2016-01-02T00:00:00");
        LocalDateTime to = LocalDateTime.parse("2016-01-02T23:59:59");
        Long count = registryUnitInvalidRepository.countBetweenCreatedDates(from,
                to);

        assertions.assertThat(count).isEqualTo(3);
    }
}
