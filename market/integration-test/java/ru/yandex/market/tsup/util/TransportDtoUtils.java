package ru.yandex.market.tsup.util;

import java.util.List;

import lombok.experimental.UtilityClass;

import ru.yandex.mj.generated.client.carrier.model.PageOfTransportDto;
import ru.yandex.mj.generated.client.carrier.model.TransportDto;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class TransportDtoUtils {
    public static PageOfTransportDto page(TransportDto... users) {
        return page(List.of(users));
    }

    public static TransportDto transportDto(Long id, String number, Integer palletsCapacity) {
        return new TransportDto()
                .id(id)
                .number(number)
                .palletsCapacity(palletsCapacity);
    }

    public static PageOfTransportDto page(List<TransportDto> transports) {
        return new PageOfTransportDto()
                .totalPages(1)
                .totalElements(2L)
                .size(10)
                .number(0)
                .content(transports);
    }

}
