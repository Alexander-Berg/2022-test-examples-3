package ru.yandex.market.checkout.helpers;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.actualization.flow.ContextualFlowRuntimeSession;
import ru.yandex.market.checkout.checkouter.actualization.flow.FetchingPromise;
import ru.yandex.market.checkout.checkouter.actualization.flow.FlowSessionAware;
import ru.yandex.market.reservation.feature.api.config.flow.FlowSessionStageKey;

public final class FlowSessionHelper {

    private FlowSessionHelper() {
    }

    @Nonnull
    public static <I, C extends FlowSessionAware<C, I>, T> C patchSession(
            @Nonnull C contextHolder,
            @Nonnull Function<C, I> contextMutator,
            @Nonnull BiConsumer<C, FlowSessionStageKey<T>> setter,
            T value) {
        if (!contextHolder.hasSession()) {
            contextHolder.setSession(ContextualFlowRuntimeSession.useSession(contextHolder, contextMutator));
        }
        var stage = FetchingPromise.fetch(contextHolder, value);
        contextHolder.getSession().traveled(stage);
        setter.accept(contextHolder, stage.sessionKey());
        return contextHolder;
    }
}
