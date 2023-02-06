package ru.yandex.market.core.testing;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import ru.yandex.market.core.history.HistoryItem;
import ru.yandex.market.core.shop.ShopActionContext;

/**
 * Сервис реализующий логику изменений карточки тестования магазина - {@link TestingState}.
 *
 * @author AShevenkov
 */
public interface TestingService {

    default TestingInfo getTestingInfo(long datasourceId) {
        return getTestingInfos(Collections.singletonList(datasourceId)).get(datasourceId);
    }

    Map<Long, TestingInfo> getTestingInfos(Collection<Long> datasourceIds);

    @Nullable
    TestingState getTestingStatus(long datasourceId, ShopProgram shopProgram);

    FullTestingState getFullTestingState(long datasourceId);

    Map<Long, TestingState> getTestingStatuses(Collection<Long> datasourceIds);

    int getCountOfShopsInTesting();

    List<HistoryItem> getModerationHistory(long datasourceId);

    void insertState(ShopActionContext ctx, TestingState state);

    void updateState(ShopActionContext ctx, TestingState state);

    void removeState(ShopActionContext ctx, TestingState state);

}
