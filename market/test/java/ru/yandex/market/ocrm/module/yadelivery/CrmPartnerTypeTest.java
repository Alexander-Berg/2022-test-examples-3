package ru.yandex.market.ocrm.module.yadelivery;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.ocrm.module.yadelivery.domain.CrmPartnerType;

import static org.assertj.core.api.Assertions.assertThatCode;

public class CrmPartnerTypeTest {
    @ParameterizedTest
    @EnumSource(PartnerType.class)
    public void checkThatAllLomPartnerTypesMapToInnerPartnerType(PartnerType lomPartnerType) {
        assertThatCode(() -> CrmPartnerType.createBasedOn(lomPartnerType)).doesNotThrowAnyException();
    }
}
