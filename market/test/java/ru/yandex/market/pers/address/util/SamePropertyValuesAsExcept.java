package ru.yandex.market.pers.address.util;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.beans.PropertyUtil;
import org.hamcrest.beans.SamePropertyValuesAs;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import static org.hamcrest.beans.PropertyUtil.NO_ARGUMENTS;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * The same as {@link SamePropertyValuesAs} with capability to exclude properties by name.
 *
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class SamePropertyValuesAsExcept<T> extends TypeSafeDiagnosingMatcher<T> {
    private final T expectedBean;
    private final Set<String> propertyNames;
    private final Class<T> baseClass;
    private final Set<String> exceptPropertyNames;
    private final List<PropertyMatcher> propertyMatchers;

    public SamePropertyValuesAsExcept(Class<T> baseClass, T expectedBean, String... exceptPropertyNames) {
        this.baseClass = baseClass;
        this.exceptPropertyNames = Arrays.stream(exceptPropertyNames)
                .collect(Collectors.toSet());
        List<PropertyDescriptor> descriptors = propertyDescriptorsFor(expectedBean, false);
        this.expectedBean = expectedBean;
        this.propertyNames = propertyNamesFrom(descriptors);
        this.propertyMatchers = propertyMatchersFor(expectedBean, descriptors);
    }

    private List<PropertyDescriptor> propertyDescriptorsFor(Object fromObj, boolean excludeExpectedPropertyNames) {
        Stream<PropertyDescriptor> stream = Arrays.stream(PropertyUtil.propertyDescriptorsFor(fromObj, Object.class))
                .filter(propertyDescriptor -> !exceptPropertyNames.contains(propertyDescriptor.getName()));
        if (excludeExpectedPropertyNames) {
            stream = stream.filter(propertyDescriptor -> !propertyNames.contains(propertyDescriptor.getName()));
        }
        return stream.collect(Collectors.toList());
    }

    @Override
    public boolean matchesSafely(T bean, Description mismatch) {
        return isCompatibleType(bean, mismatch)
                && hasNoExtraProperties(bean, mismatch)
                && hasMatchingValues(bean, mismatch);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("same property values as " + expectedBean.getClass().getSimpleName())
                .appendList(" [", ", ", "]", propertyMatchers);
    }


    private boolean isCompatibleType(T item, Description mismatchDescription) {
        if (!baseClass.isAssignableFrom(item.getClass())) {
            mismatchDescription.appendText("is incompatible type: " + item.getClass().getSimpleName());
            return false;
        }
        return true;
    }

    private boolean hasNoExtraProperties(T item, Description mismatchDescription) {
        Set<String> actualPropertyNames = propertyNamesFrom(propertyDescriptorsFor(item, true));
        if (!actualPropertyNames.isEmpty()) {
            mismatchDescription.appendText("has extra properties called " + actualPropertyNames);
            return false;
        }
        return true;
    }

    private boolean hasMatchingValues(T item, Description mismatchDescription) {
        return propertyMatchers.stream()
                .filter(propertyMatcher -> !propertyMatcher.matches(item))
                .findAny()
                .map(propertyMatcher -> {
                    propertyMatcher.describeMismatch(item, mismatchDescription);
                    return false;
                }).orElse(true);
    }

    private static <T> List<PropertyMatcher> propertyMatchersFor(T bean, List<PropertyDescriptor> descriptors) {
        return descriptors.stream()
                .map(propertyDescriptor -> new PropertyMatcher(propertyDescriptor, bean))
                .collect(Collectors.toList());
    }

    private static Set<String> propertyNamesFrom(List<PropertyDescriptor> descriptors) {
        return descriptors.stream()
                .map(PropertyDescriptor::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Creates a matcher that matches when the examined object has values for all of
     * its JavaBean properties that are equal to the corresponding values of the
     * specified bean.
     * <p/>
     * For example:
     * <pre>assertThat(myBean, samePropertyValuesAsExcept(myExpectedBean, "mutableProperty"))</pre>
     *
     * @param expectedBean the bean against which examined beans are compared
     */
    @SuppressWarnings("unchecked")
    public static <T> Matcher<? super T> samePropertyValuesAsExcept(T expectedBean, String... exceptProperties) {
        return new SamePropertyValuesAsExcept<>((Class<T>) expectedBean.getClass(), expectedBean, exceptProperties);
    }

    public static <T> Matcher<? super T> samePropertyValuesAsExcept(Class<T> baseClass, T expectedBean, String... exceptProperties) {
        return new SamePropertyValuesAsExcept<>(baseClass, expectedBean, exceptProperties);
    }

    public static <T> List<Matcher<? super T>> sameCollectionByPropertyValuesAsExcept(Collection<T> expected, String... exceptProperties) {
        return expected.stream()
                .map(value -> (Matcher<? super T>) samePropertyValuesAsExcept(value, exceptProperties))
                .collect(Collectors.toList());
    }

    public static <T> List<Matcher<? super T>> sameCollectionByPropertyValuesAs(Collection<T> expected) {
        return expected.stream()
                .map(Matchers::samePropertyValuesAs)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("WeakerAccess")
    private static class PropertyMatcher extends DiagnosingMatcher<Object> {
        private final Method readMethod;
        private final Matcher<Object> matcher;
        private final String propertyName;

        public PropertyMatcher(PropertyDescriptor descriptor, Object expectedObject) {
            this.propertyName = descriptor.getDisplayName();
            this.readMethod = descriptor.getReadMethod();
            this.matcher = equalTo(readProperty(readMethod, expectedObject));
        }

        @Override
        public boolean matches(Object actual, Description mismatch) {
            final Object actualValue = readProperty(readMethod, actual);
            if (!matcher.matches(actualValue)) {
                mismatch.appendText(propertyName + " ");
                matcher.describeMismatch(actualValue, mismatch);
                return false;
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(propertyName + ": ").appendDescriptionOf(matcher);
        }
    }

    private static Object readProperty(Method method, Object target) {
        try {
            return method.invoke(target, NO_ARGUMENTS);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not invoke " + method + " on " + target, e);
        }
    }

}
