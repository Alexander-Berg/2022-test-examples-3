package ru.yandex.market.delivery.mdbapp.util;

import java.util.Objects;
import java.util.function.Supplier;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public abstract class BaseMatcher<T> extends TypeSafeDiagnosingMatcher<T> {

    protected final <V1, V2> boolean areEqual(Description mismatchDescription,
                                              Supplier<V1> firstSupplier,
                                              Supplier<V2> secondSupplier,
                                              String message) {
        V1 firstValue = firstSupplier.get();
        V2 secondValue = secondSupplier.get();

        if (!Objects.equals(firstValue, secondValue)) {
            mismatchDescription
                    .appendText(message)
                    .appendText(".\t")
                    .appendText("Values [")
                    .appendValue(firstValue)
                    .appendText("] AND [")
                    .appendValue(secondValue)
                    .appendText("] are not equal.")
                    .appendText("\n");

            return false;
        }

        return true;
    }

    protected final <V> boolean isNull(Description mismatchDescription,
                                       Supplier<V> supplier,
                                       String fieldName) {
        V value = supplier.get();
        if (value != null) {
            mismatchDescription
                    .appendText("Field [")
                    .appendText(fieldName)
                    .appendText("] should be null.")
                    .appendText(" Instead got: [")
                    .appendValue(supplier)
                    .appendText("]");

            return false;
        }

        return true;
    }
}
