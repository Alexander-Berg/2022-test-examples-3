package ru.yandex.market.personal.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.personal.AbstractTest;
import ru.yandex.market.personal.client.model.CommonTypeEnum;

class PersonalDataTypeTest extends AbstractTest {

    @DisplayName("PersonalDataTypeTest не содержит дублирующихся значений CommonTypeEnum")
    @Test
    void noDuplicateCommonEnumType() {
        Set<CommonTypeEnum> commonTypeEnumsSet = Arrays.stream(PersonalDataType.values())
            .map(PersonalDataType::getCommonTypeEnum)
            .collect(Collectors.toSet());
        softly.assertThat(commonTypeEnumsSet.size()).isEqualTo(PersonalDataType.values().length);
    }
}
