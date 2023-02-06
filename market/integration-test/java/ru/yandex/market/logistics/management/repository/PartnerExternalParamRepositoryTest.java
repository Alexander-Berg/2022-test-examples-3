package ru.yandex.market.logistics.management.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamValue;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

class PartnerExternalParamRepositoryTest extends AbstractContextualTest {

    @Autowired
    private PartnerExternalParamRepository partnerExternalParamRepository;

    @Autowired
    private PartnerExternalParamTypeRepository typeRepository;

    @Test
    @DisplayName("У партнера должна быть включена синхронизация ВГХ")
    @DatabaseSetup("/data/repository/partner-external-param/partner_with_param.xml")
    @SuppressWarnings("unchecked")
    void testFindByPartnerIdAndType() {
        PartnerExternalParamValue value = partnerExternalParamRepository.findByPartnerIdAndParamType(
            1L,
            typeRepository.findByKey(PartnerExternalParamType.KOROBYTE_SYNC_ENABLED.name()).orElse(null)
        ).orElse(null);

        softly.assertThat(value)
            .extracting(
                pepv -> pepv.getParamType().getKey(),
                PartnerExternalParamValue::getValue,
                pepv -> pepv.getPartner().getId()
            )
            .contains(
                PartnerExternalParamType.KOROBYTE_SYNC_ENABLED.name(),
                "1",
                1L
            );
    }
}
