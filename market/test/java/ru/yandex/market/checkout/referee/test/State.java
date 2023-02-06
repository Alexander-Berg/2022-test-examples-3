package ru.yandex.market.checkout.referee.test;

import ru.yandex.market.checkout.entity.ConversationStatus;

/**
 * @author kukabara
 */
public class State {
    public static final State AFTER_START = new State.StateBuilder()
            .withStatus(ConversationStatus.OPEN)
            .build();

    public static final State AFTER_CLOSE = new State.StateBuilder()
            .withStatus(ConversationStatus.CLOSED)
            .build();

    public final boolean canRaiseIssue;
    public final boolean canEscalate;
    public final boolean canEscalateAfterNotNull;
    public final ConversationStatus status;
    public final int resolutionCount;

    State(boolean canRaiseIssue, boolean canEscalate, boolean canEscalateAfterNotNull,
          ConversationStatus status, int resolutionCount) {
        this.canRaiseIssue = canRaiseIssue;
        this.canEscalate = canEscalate;
        this.canEscalateAfterNotNull = canEscalateAfterNotNull;
        this.status = status;
        this.resolutionCount = resolutionCount;
    }

    public static final class StateBuilder {
        boolean canRaiseIssue;
        boolean canEscalate;
        boolean canEscalateAfterNotNull;
        ConversationStatus status;
        int resolutionCount;

        public StateBuilder() {
        }

        public StateBuilder withCanRaiseIssue(boolean canRaiseIssue) {
            this.canRaiseIssue = canRaiseIssue;
            return this;
        }

        public StateBuilder withCanEscalate(boolean canEscalate) {
            this.canEscalate = canEscalate;
            return this;
        }

        public StateBuilder withCanEscalateAfterNotNull(boolean canEscalateAfterNotNull) {
            this.canEscalateAfterNotNull = canEscalateAfterNotNull;
            return this;
        }

        public StateBuilder withStatus(ConversationStatus status) {
            this.status = status;
            return this;
        }

        public StateBuilder withResolutionCount(int resolutionCount) {
            this.resolutionCount = resolutionCount;
            return this;
        }

        public State build() {
            return new State(
                    this.canRaiseIssue,
                    this.canEscalate,
                    this.canEscalateAfterNotNull,
                    this.status,
                    this.resolutionCount
            );
        }
    }
}
