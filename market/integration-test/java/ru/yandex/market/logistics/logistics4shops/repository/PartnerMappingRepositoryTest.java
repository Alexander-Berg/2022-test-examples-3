package ru.yandex.market.logistics.logistics4shops.repository;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.model.entity.PartnerMapping;
import ru.yandex.market.logistics.logistics4shops.model.enums.PartnerType;

@DisplayName("Репозиторий соответствий lms и mbi партнёров")
class PartnerMappingRepositoryTest extends AbstractIntegrationTest {
    @Autowired
    private PartnerMappingRepository partnerMappingRepository;

    @Test
    @DisplayName("Получение сущности")
    @DatabaseSetup("/repository/partnermapping/before/prepare.xml")
    void getById() {
        PartnerMapping partnerMapping = partnerMappingRepository.findById(1L).orElse(null);
        Assertions.assertThat(partnerMapping)
            .isNotNull()
            .usingRecursiveComparison()
            .isEqualTo(new PartnerMapping()
                .setId(1L)
                .setMbiPartnerId(123L)
                .setLmsPartnerId(456L)
                .setPartnerType(PartnerType.DROPSHIP)
                .setCreated(Instant.ofEpochSecond(1640433600))
            );
    }
}
