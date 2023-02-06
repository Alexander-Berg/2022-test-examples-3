package ru.yandex.market.logistics.lom.utils.ydb.converter;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateYdb;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateTableDescription;
import ru.yandex.market.logistics.lom.utils.HashUtils;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.utils.MapBuilder;

@Component
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public class BusinessProcessStateYdbConverter {

    private final ObjectMapper objectMapper;

    @Nonnull
    @SneakyThrows
    public Map<String, Object> mapToItem(YdbTableDescription tableDescription, Object o) {
        BusinessProcessStateTableDescription table =
            (BusinessProcessStateTableDescription) tableDescription;
        BusinessProcessStateYdb state = (BusinessProcessStateYdb) o;

        return MapBuilder.<String, Object>create()
            .entry(table.getIdHash().name(), HashUtils.hashLong(state.getId()))
            .entry(table.getId().name(), state.getId())
            .entry(table.getAuthor().name(), objectMapper.valueToTree(state.getAuthor()))
            .entry(table.getCreated().name(), state.getCreated())
            .entry(table.getCreatedHash().name(), HashUtils.hashInstant(state.getCreated()))
            .entry(table.getParentId().name(), state.getParentId())
            .entry(table.getParentIdHash().name(), HashUtils.hashLong(state.getParentId()))
            .entry(table.getPayload().name(), state.getPayload())
            .entry(table.getQueueType().name(), state.getQueueType().name())
            .entry(table.getSequenceId().name(), state.getSequenceId())
            .entry(table.getSequenceIdHash().name(), HashUtils.hashLong(state.getSequenceId()))
            .entry(table.getStatus().name(), state.getStatus().name())
            .entry(table.getUpdated().name(), state.getUpdated())
            .entry(table.getUpdatedHash().name(), HashUtils.hashInstant(state.getUpdated()))
            .entry(table.getMessage().name(), state.getMessage())
            .build();
    }
}
