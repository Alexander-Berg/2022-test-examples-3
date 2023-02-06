package ru.yandex.market.tpl.core.domain.clientreturn.mapper;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.clientreturn.item.ClientReturnItem;
import ru.yandex.market.tpl.core.domain.clientreturn.item.ClientReturnItemReturnReason;

import static org.assertj.core.api.Assertions.assertThat;

class ClientReturnItemDtoMapperTest {

    private final ClientReturnItemDtoMapper clientReturnItemDtoMapper = new ClientReturnItemDtoMapper();

    private static final List<String> PHOTO_URLS = List.of(
            "url1.com",
            "url2.com",
            "url3.com"
    );

    @Test
    void map() {
        var clientReturnItem = ClientReturnItem.builder()
                .id(123L)
                .clientPhotoUrls(PHOTO_URLS)
                .returnReason(ClientReturnItemReturnReason.BAD_QUALITY)
                .build();

        var dto = clientReturnItemDtoMapper.mapReturnItemDto(clientReturnItem);
        var mappedReason = dto.getReturnReason();

        assertThat(dto.getId()).isEqualTo(123L);
        assertThat(dto.getClientPhotoUrls()).containsExactlyInAnyOrderElementsOf(PHOTO_URLS);
        assertThat(mappedReason).isEqualTo(ClientReturnItemReturnReason.BAD_QUALITY.getDescription());
    }

}
