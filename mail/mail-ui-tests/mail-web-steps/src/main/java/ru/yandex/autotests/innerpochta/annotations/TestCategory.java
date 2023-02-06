package ru.yandex.autotests.innerpochta.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация к тестовым методам, которая принимает строку категорий теста, разделённых символом «_».
 * Пример: @TestCategoty("Smoke_Chrome")
 *
 * @author a-zoshchuk
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TestCategory {
    String value();
}
