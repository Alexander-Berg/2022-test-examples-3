package ru.yandex.market.sc.core.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.params.provider.ValueSource;

@ValueSource(booleans = {true, false})
@Retention(RetentionPolicy.RUNTIME)
public @interface BooleanSource {

}