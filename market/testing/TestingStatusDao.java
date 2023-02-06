package ru.yandex.market.core.testing;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.yandex.market.core.shop.ShopActionContext;

/**
 * @author zoom
 */
public interface TestingStatusDao {

    /**
     * @return информацию о прохождении тестирования для магазина по указанной программе или <code>null</code>,
     * если не найдена
     */
    @Nullable
    TestingState load(long shopId, ShopProgram shopProgram);

    /**
     * @return состояние проверок по всем программам для магазина.
     */
    List<TestingState> load(long shopId);

    /**
     * @return состояние тестирования
     */
    @Nullable
    TestingState loadById(long id);

    /**
     * @return информацию о прохождении тестирования для множества магазинов
     */
    @Nonnull
    Map<Long, Collection<TestingState>> loadMany(Collection<Long> keys);

    /**
     * @return информацию о прохождении тестирования для множества магазинов по указанной программе
     */
    @Nonnull
    Map<Long, TestingState> loadMany(Collection<Long> keys, ShopProgram shopProgram);

    List<TestingState> loadAll();

    void loadAllToConvert(Consumer<TestingState> stateConsumer);

    void makeNeedTesting(ShopActionContext ctx, TestingState state);

    void removeFromTesting(long actionId, long datasourceId, long testingId);

    void update(ShopActionContext ctx, TestingState state);
}
