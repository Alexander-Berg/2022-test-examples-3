package ru.yandex.direct.logviewercore;

import java.lang.reflect.Field;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Simple matcher checking all fields by equals().
 * Id DOES NOT go recursively.
 *
 * @param <T>
 */
public class FieldsMatcher<T> extends TypeSafeMatcher<T> {

    private T expected;
    private Field[] fields;

    private FieldsMatcher(T expected) {
        super();
        this.expected = expected;
        this.fields = expected.getClass().getFields();
    }

    public static <T> FieldsMatcher<T> fieldsEquals(T expected) {
        return new FieldsMatcher<>(expected);
    }

    @Override
    protected boolean matchesSafely(Object item) {
        for (Field field : fields) {
            Object expectedValue = getValueSafely(field, expected);
            Object actualValue = getValueSafely(field, item);

            if (expectedValue != null) {
                if (!expectedValue.equals(actualValue)) {
                    return false;
                }
            } else if (actualValue != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("object fields are equal");
    }

    private Object getValueSafely(Field field, Object object) {
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
