package ru.yandex.market.common.test.junit;

import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method argument source for JUnit 5 {@link org.junit.jupiter.params.ParameterizedTest}s.
 *
 * You can use {@link CartesianSource} to assign individual {@link ValueSource} for each method parameter.
 * <p>
 * Example:
 * <pre>{@code
 *     {@lit @}ParameterizedTest
 *     {@lit @}CartesianSource({
 *             {@lit @}ValueSource(ints = {1, 2, 3, 4}),
 *             {@lit @}ValueSource(chars = {'a', 'b', 'c'}),
 *             {@lit @}ValueSource(ints = {"alpha", "beta"})
 *     })
 *     void test1(int a, char b, string s) {
 *         // ...
 *     }
 * }</pre>
 * <p>
 * Example test will be called 24 times:
 * <ol>
 *     <li>{@code test1(1, 'a', "alpha")}
 *     <li>{@code test1(2, 'a', "alpha")}
 *     <li>{@code test1(3, 'a', "alpha")}
 *     <li>{@code test1(4, 'a', "alpha")}
 *     <li>{@code test1(1, 'b', "alpha")}
 *     <li>{@code test1(2, 'b', "alpha")}
 *     <li>{@code test1(3, 'b', "alpha")}
 *     <li>{@code test1(4, 'b', "alpha")}
 *     <li>{@code test1(1, 'c', "alpha")}
 *     <li>{@code test1(2, 'c', "alpha")}
 *     <li>{@code test1(3, 'c', "alpha")}
 *     <li>{@code test1(4, 'c', "alpha")}
 *     <li>{@code test1(1, 'a', "beta")}
 *     <li>{@code test1(2, 'a', "beta")}
 *     <li>{@code test1(3, 'a', "beta")}
 *     <li>{@code test1(4, 'a', "beta")}
 *     <li>{@code test1(1, 'b', "beta")}
 *     <li>{@code test1(2, 'b', "beta")}
 *     <li>{@code test1(3, 'b', "beta")}
 *     <li>{@code test1(4, 'b', "beta")}
 *     <li>{@code test1(1, 'c', "beta")}
 *     <li>{@code test1(2, 'c', "beta")}
 *     <li>{@code test1(3, 'c', "beta")}
 *     <li>{@code test1(4, 'c', "beta")}
 * </ol>
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(CartesianProvider.class)
public @interface CartesianSource {
    ValueSource[] value();
}
