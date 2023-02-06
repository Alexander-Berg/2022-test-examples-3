package ru.yandex.market.mbo.gwt.models.rules;

import org.assertj.core.api.Assertions;

import javax.annotation.Nonnull;

/**
 * @author s-ermakov
 */
public class ModelRuleFailTester {
    private final Throwable throwable;

    public ModelRuleFailTester(@Nonnull Throwable throwable) {
        this.throwable = throwable;
    }

    public ModelRuleFailTester withException(Class<? extends Throwable> exceptionClass) {
        try {
            Assertions.assertThat(throwable).isInstanceOf(exceptionClass);
        } catch (Throwable exception) {
            exception.initCause(throwable);
            throw exception;
        }
        return this;
    }

    public ModelRuleFailTester withMessage(String message) {
        try {
            Assertions.assertThat(throwable.getMessage()).isEqualTo(message);
        } catch (Throwable exception) {
            exception.initCause(throwable);
            throw exception;
        }
        return this;
    }
}
