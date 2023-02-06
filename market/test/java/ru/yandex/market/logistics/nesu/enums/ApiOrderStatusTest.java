package ru.yandex.market.logistics.nesu.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.nesu.api.converter.EnumConverter;
import ru.yandex.market.logistics.nesu.api.converter.orderstatus.ApiOrderStatusConverter;
import ru.yandex.market.logistics.nesu.api.model.order.ApiOrderStatus;
import ru.yandex.market.logistics.nesu.dto.enums.DaasOrderStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiOrderStatusTest {

    private final EnumConverter enumConverter = new EnumConverter();
    private final ApiOrderStatusConverter converter = new ApiOrderStatusConverter(enumConverter);

    @ParameterizedTest
    @EnumSource(DaasOrderStatus.class)
    void convertStatus(DaasOrderStatus status) {
        ApiOrderStatus apiStatus = converter.convertToApiOrderStatus(status);
        assertThat(apiStatus).isNotNull();
        assertThat(apiStatus.getDescription()).endsWith(".");
    }
}
