package ru.yandex.market.delivery.mdbapp.integration.converter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.pvz.client.logistics.model.ReturnStatus;

import static org.assertj.core.api.Java6Assertions.assertThat;

class PvzReturnStatusConverterTest {

    private final PvzReturnStatusConverter converter = new PvzReturnStatusConverter();

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(ReturnStatus.class)
    void convert(ReturnStatus returnStatus) {
        assertThat(converter.convert(returnStatus)).isNotNull();
    }
}
