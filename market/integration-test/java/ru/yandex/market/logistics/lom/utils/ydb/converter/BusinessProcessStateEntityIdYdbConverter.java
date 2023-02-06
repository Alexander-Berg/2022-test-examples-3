package ru.yandex.market.logistics.lom.utils.ydb.converter;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateEntityIdYdb;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateEntityIdTableDescription;
import ru.yandex.market.logistics.lom.utils.HashUtils;
import ru.yandex.market.ydb.integration.YdbTableDescription;
import ru.yandex.market.ydb.integration.utils.MapBuilder;

@Component
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public class BusinessProcessStateEntityIdYdbConverter {

    @Nonnull
    public Map<String, Object> mapToItem(
        YdbTableDescription tableDescription,
        Object o,
        Long businessProcessId
    ) {
        BusinessProcessStateEntityIdTableDescription table =
            (BusinessProcessStateEntityIdTableDescription) tableDescription;
        BusinessProcessStateEntityIdYdb entityIdYdb = (BusinessProcessStateEntityIdYdb) o;

        return MapBuilder.<String, Object>create()
            .entry(table.getBusinessProcessStateIdHash().name(), HashUtils.hashLong(businessProcessId))
            .entry(table.getBusinessProcessStateId().name(), businessProcessId)
            .entry(table.getEntityId().name(), entityIdYdb.getEntityId())
            .entry(table.getEntityType().name(), entityIdYdb.getEntityType().name())
            .entry(table.getEntityIdHash().name(), HashUtils.hashLong(entityIdYdb.getEntityId()))
            .build();
    }

}
