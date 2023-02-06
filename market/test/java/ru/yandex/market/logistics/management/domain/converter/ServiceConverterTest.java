package ru.yandex.market.logistics.management.domain.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.entity.ServiceCode;
import ru.yandex.market.logistics.management.entity.response.point.Service;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;

class ServiceConverterTest extends AbstractTest {

    private static final Service SERVICE_DTO = new Service(
        ServiceCodeName.CHECK,
        true,
        "service",
        "Проверка заказа перед оплатой"
    );
    private static final ServiceCode SERVICE_CODE_ENTITY =
        new ServiceCode()
            .setCode(ru.yandex.market.logistics.management.domain.entity.type.ServiceCodeName.CHECK)
            .setOptional(true)
            .setName("service")
            .setDescription("Проверка заказа перед оплатой");

    private static final ServiceConverter CONVERTER = new ServiceConverter();

    @Test
    void convertToDto() {
        softly.assertThat(SERVICE_DTO).isEqualTo(CONVERTER.toDto(SERVICE_CODE_ENTITY));
    }
}
