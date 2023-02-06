package ru.yandex.market.global.index.mapper;

import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.mj.generated.client.pim.model.AttributesResponseList;

public class EntityMapperTest {

    @SneakyThrows
    @ParameterizedTest(name = "type = {0}")
    @EnumSource(AttributesResponseList.TypeEnum.class)
    public void testAllPimTypesMapped(AttributesResponseList.TypeEnum type) {
        AttributesResponseList pimAttr = new AttributesResponseList().type(type);
        Assertions.assertThatCode(() -> EntityMapper.MAPPER.toAttributeType(pimAttr))
                .doesNotThrowAnyException();
    }
}
