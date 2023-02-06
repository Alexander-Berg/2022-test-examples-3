package ru.yandex.market.logistics.lom.utils.ydb.converter;

import java.util.Map;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Component;

import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderCombinedRouteHistoryTableDescription;
import ru.yandex.market.ydb.integration.YdbTableDescription;

@Component
public class OrderCombinedRouteHistoryYdbConverter {

    @Nonnull
    public Map<String, Object> mapToItem(YdbTableDescription tableDescription, Object o) {
        OrderCombinedRouteHistoryTableDescription table = (OrderCombinedRouteHistoryTableDescription) tableDescription;
        CombinedRoute combinedRoute = (CombinedRoute) o;
        return Map.of(
            table.getRoute().name(), combinedRoute.getSourceRoute(),
            table.getRouteUuid().name(), combinedRoute.getRouteUuid().toString()
        );
    }
}
