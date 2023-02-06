package ru.yandex.market.core.moderation.passed;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

import ru.yandex.market.core.moderation.ShopIdConsumer;
import ru.yandex.market.core.moderation.sandbox.SandboxRepository;
import ru.yandex.market.core.moderation.sandbox.SandboxState;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.ShopProgram;

/**
 * @author zoom
 */
public class MockSandboxRepositoryAdapter implements SandboxRepository {

    @Override
    public void getExpiredIds(ShopIdConsumer shopIdHandler) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public SandboxState load(long shopId, ShopProgram shopProgram) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Nonnull
    @Override
    public Collection<SandboxState> load(long shopId) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Nonnull
    @Override
    public Map<Long, SandboxState> loadMany(Collection<Long> shopIds, ShopProgram shopProgram) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Nonnull
    @Override
    public Map<Long, Collection<SandboxState>> loadMany(Collection<Long> shopIds) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public SandboxState loadById(long id) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void store(ShopActionContext ctx, SandboxState status) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void delete(ShopActionContext ctx, SandboxState state) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
