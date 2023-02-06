package ru.yandex.market.logistics.management.service.client;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamValue;
import ru.yandex.market.logistics.management.repository.PartnerExternalParamTypeRepository;

class PartnerExternalParamServiceTest extends AbstractContextualTest {

    private static final String TYPE_1 = "TYPE1";
    private static final String TYPE_2 = "TYPE2";

    @Autowired
    private PartnerExternalParamService partnerExternalParamService;

    @Autowired
    PartnerExternalParamTypeRepository externalParamTypeRepository;

    @Test
    @DisplayName("Поиск параметров партнера по типам")
    @DatabaseSetup("/data/service/client/partner_external_params_prepare_data.xml")
    void findParamsValuesByTypes() {
        List<PartnerExternalParamValue> valuesByTypes = partnerExternalParamService.findValuesByTypes(
            ImmutableSet.of(TYPE_1, TYPE_2)
        );

        softly.assertThat(valuesByTypes)
            .extracting(PartnerExternalParamValue::getValue)
            .containsExactly("value1", "value2", "value3");

        softly.assertThat(valuesByTypes)
            .extracting(PartnerExternalParamValue::getPartner)
            .extracting(Partner::getId)
            .containsExactly(1L, 2L, 2L);

        softly.assertThat(valuesByTypes)
            .extracting(PartnerExternalParamValue::getParamType)
            .extracting(ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamType::getKey)
            .containsExactly(TYPE_1, TYPE_1, TYPE_2);
    }
}
