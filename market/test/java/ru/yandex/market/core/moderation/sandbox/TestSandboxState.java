package ru.yandex.market.core.moderation.sandbox;

import ru.yandex.market.core.testing.TestingState;

/**
 * @author Vadim Lyalin
 */
public class TestSandboxState extends SandboxState {
    public TestSandboxState(TestingState state, ModerationClock clock) {
        super(state, clock);
    }

    @Override
    public TestingState getState() {
        return super.getState();
    }
}
