package ru.yandex.market.logistics.logistics4shops.utils;

import java.math.BigDecimal;
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.lom.model.dto.AuthorDto;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

@UtilityClass
@ParametersAreNonnullByDefault
public class LomEventFactory {
    public static final Instant CREATED_TIMESTAMP = Instant.parse("2022-12-16T11:29:00.00Z");
    public static final Instant ENTITY_CREATED_TIMESTAMP = Instant.parse("2021-12-16T11:29:00.00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nonnull
    @SneakyThrows
    public EventDto eventDto(String diffFilepath, String snapshotFilePath) {
        return new EventDto()
            .setId(1L)
            .setLogbrokerId(2L)
            .setEntityType(EntityType.ORDER)
            .setEntityId(3L)
            .setCreated(CREATED_TIMESTAMP)
            .setEntityCreated(ENTITY_CREATED_TIMESTAMP)
            .setAuthor(AuthorDto.builder().abcServiceId(123L).yandexUid(BigDecimal.TEN).build())
            .setDiff(readJsonNode(diffFilepath))
            .setSnapshot(readJsonNode(snapshotFilePath))
            .setOrderIdHash(4L)
            .setEventIdHash(5L);
    }

    @Nonnull
    @SneakyThrows
    private JsonNode readJsonNode(String filepath) {
        return objectMapper.readValue(IntegrationTestUtils.extractFileContent(filepath), JsonNode.class);
    }
}
