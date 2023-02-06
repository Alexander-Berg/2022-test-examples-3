package ru.yandex.market.ocrm.module.yadelivery;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.ocrm.module.yadelivery.domain.ApiType;
import ru.yandex.market.ocrm.module.yadelivery.domain.CargoStatus;
import ru.yandex.market.ocrm.module.yadelivery.domain.CrmPartnerType;
import ru.yandex.market.ocrm.module.yadelivery.domain.CrmSegmentStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class CargoStatusTest {
    @ParameterizedTest
    @EnumSource(SegmentStatus.class)
    public void checkThatAllLomSegmentStatusesMapToCargoStatuses(SegmentStatus lomSegmentStatus) {
        CrmSegmentStatus crmSegmentStatus = CrmSegmentStatus.valueOf(lomSegmentStatus.name());
        Set<ApiType> apiTypes = crmSegmentStatus.getApiTypes();

        Set<String> partnerTypes = Arrays.stream(CrmPartnerType.values())
                .filter(crmPartnerType -> apiTypes.containsAll(crmPartnerType.getApiTypes()))
                .filter(crmPartnerType -> crmPartnerType != CrmPartnerType.UNKNOWN)
                .map(CrmPartnerType::getLomPartnerType)
                .map(PartnerType::name)
                .collect(Collectors.toSet());

        partnerTypes.forEach(type -> assertThat(CargoStatus.createBasedOn(crmSegmentStatus.name(), type)).isNotNull());
    }
}
