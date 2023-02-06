package ru.yandex.market.logistics.lom.utils.ydb.converter;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateStatusHistoryYdb;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.utils.HashUtils;
import ru.yandex.market.ydb.integration.YdbTableDescription;

@Component
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public class BusinessProcessStateStatusHistoryYdbConverter {

    @Nonnull
    public Map<String, Object> mapToItem(YdbTableDescription tableDescription, Object o) {
        var table = (BusinessProcessStateStatusHistoryTableDescription) tableDescription;
        BusinessProcessStateStatusHistoryYdb businessProcessState = (BusinessProcessStateStatusHistoryYdb) o;
        Long sequenceId = businessProcessState.getSequenceId();
        Long id = businessProcessState.getId();
        return Map.of(
            table.getBusinessProcessStateSequenceId().name(), sequenceId,
            table.getBusinessProcessStateSequenceIdHash().name(), HashUtils.hashLong(sequenceId),
            table.getBusinessProcessStateId().name(), id,
            table.getBusinessProcessStateIdHash().name(), HashUtils.hashLong(id),
            table.getCreated().name(), businessProcessState.getCreated(),
            table.getStatus().name(), businessProcessState.getStatus().name(),
            table.getMessage().name(), businessProcessState.getMessage(),
            table.getRequestId().name(), businessProcessState.getRequestId()
        );
    }
}
