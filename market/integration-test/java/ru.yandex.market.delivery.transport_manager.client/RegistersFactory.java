package ru.yandex.market.delivery.transport_manager.client;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.delivery.transport_manager.model.dto.RegisterDto;
import ru.yandex.market.delivery.transport_manager.model.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.RegisterType;

@UtilityClass
public class RegistersFactory {

    @Nonnull
    public RegisterDto newRegister() {
        return RegisterDto.builder()
            .id(2L)
            .type(RegisterType.PLAN)
            .status(RegisterStatus.PREPARING)
            .externalId("register1")
            .documentId("abc123")
            .partnerId(2L)
            .comment("Очень важный комментарий")
            .build();
    }
}
