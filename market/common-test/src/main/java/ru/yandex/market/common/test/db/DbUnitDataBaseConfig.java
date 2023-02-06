package ru.yandex.market.common.test.db;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ParametersAreNonnullByDefault
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface DbUnitDataBaseConfig {
    @Nonnull Entry[] value() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @interface Entry {
        @Nonnull String name();
        @Nonnull String value();
    }
}
