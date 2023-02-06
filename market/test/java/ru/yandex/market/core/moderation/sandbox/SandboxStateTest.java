package ru.yandex.market.core.moderation.sandbox;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Ignore;

import ru.yandex.market.core.moderation.sandbox.impl.DefaultSandboxStateFactory;

/**
 * @author zoom
 */
@Ignore
public abstract class SandboxStateTest extends Assert {

    protected static final long SHOP_ID = 321;

    protected final AtomicReference<Date> clock = new AtomicReference<>(new Date(0));

    protected final SandboxStateFactory factory = new DefaultSandboxStateFactory(clock::get);

    protected void tick() {
        clock.set(new Date(clock.get().getTime() + 1000));
    }

}