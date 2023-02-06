package ru.yandex.market.common.test.junit;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Method argument provider for JUnit 5 {@link org.junit.jupiter.params.ParameterizedTest}s.
 *
 * You should use {@link CartesianSource} annotation to use it.
 *
 * @see CartesianSource for documentation and example
 */
public class CartesianProvider implements ArgumentsProvider, AnnotationConsumer<CartesianSource> {

    private ValueSource[] sources;

    @Override
    public void accept(CartesianSource cartesianSource) {
        sources = cartesianSource.value();
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return provideArguments(context, Arrays.asList(sources));
    }

    private static Stream<? extends Arguments> provideArguments(
            ExtensionContext context,
            List<ValueSource> valueSets
    ) throws Exception {
        if (valueSets.isEmpty()) {
            return Stream.of(Arguments.of());
        } else {
            ValueSource first = valueSets.get(0);
            List<ValueSource> rest = valueSets.subList(1, valueSets.size());
            List<Object> values = extractValues(context, first);
            return values.stream()
                    .flatMap(value -> providePrefixedArguments(context, value, rest));
        }
    }

    private static Stream<? extends Arguments> providePrefixedArguments(
            ExtensionContext context,
            Object value,
            List<ValueSource> valueSets
    ) {
        try {
            return provideArguments(context, valueSets).map(arguments -> concat(Arguments.of(value), arguments));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> extractValues(ExtensionContext context, ValueSource valueSet) throws Exception {
        Constructor<? extends ArgumentsProvider> argumentProviderConstructor =
                ValueSource.class.getAnnotation(ArgumentsSource.class).value().getDeclaredConstructor();
        argumentProviderConstructor.setAccessible(true);
        ArgumentsProvider argumentsProvider = argumentProviderConstructor.newInstance();
        if (argumentsProvider instanceof AnnotationConsumer) {
            AnnotationConsumer<ValueSource> annotationConsumer = (AnnotationConsumer<ValueSource>) argumentsProvider;
            annotationConsumer.accept(valueSet);
        }
        return argumentsProvider.provideArguments(context)
                .map(arguments -> arguments.get()[0])
                .collect(Collectors.toList());
    }

    public static Arguments concat(Arguments leftArguments, Arguments rightArguments) {
        Object[] left = leftArguments.get();
        Object[] right = rightArguments.get();
        Object[] result = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return Arguments.of(result);
    }
}
