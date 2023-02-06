package ru.yandex.market.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.SubstringMatcher;

import ru.yandex.market.api.util.ApiStrings;
import ru.yandex.market.api.util.CommonCollections;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

/**
 * @author dimkarp93
 */
public class ApiMatchers {

    public static <T, S> TypeSafeMatcher<T> map(Function<T, S> mapper,
                                                String matcherDescription,
                                                Matcher<S> matcher) {
        return map(
            mapper,
            matcherDescription,
            matcher,
            Object::toString
        );
    }

    public static <T, S> TypeSafeMatcher<T> map(Function<T, S> mapper,
                                                String matcherDescription,
                                                Matcher<S> matcher,
                                                Function<T, String> origToStr) {
        return map(
            mapper,
            matcherDescription,
            matcher,
            origToStr,
            x -> x
        );
    }

    public static <T, S> TypeSafeMatcher<T> map(Function<T, S> mapper,
                                                String matcherDescription,
                                                Matcher<S> matcher,
                                                Function<T, String> origToStr,
                                                Function<S, Object> mappedToDescription) {
        return new TypeSafeMatcher<T>() {
            @Override
            protected boolean matchesSafely(T item) {
                return matcher.matches(mapper.apply(item));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(matcherDescription).appendText(": ");
                matcher.describeTo(description);
            }

            @Override
            protected void describeMismatchSafely(T item, Description mismatchDescription) {
                S mapped = mapper.apply(item);
                matcher.describeMismatch(
                    null == mapped ? "null" : mappedToDescription.apply(mapped),
                    mismatchDescription
                );
                mismatchDescription.appendText(" in ").appendValue(origToStr.apply(item));
            }
        };
    }

    public static <K, V> Matcher<Map.Entry<K, V>> entry(K id, V value) {
        return allOf(
            map(
                Map.Entry::getKey,
                "'id'",
                is(id),
                e -> entryToStr(e, Object::toString, Object::toString)
            ),
            map(
                Map.Entry::getValue,
                "'value'",
                is(value),
                e -> entryToStr(e, Object::toString, Object::toString)
            )
        );
    }

    public static <K, V> Matcher<Map.Entry<K, V>> entry(K id, Matcher<V> value) {
        return allOf(
            map(
                Map.Entry::getKey,
                "'id'",
                is(id),
                e -> entryToStr(e, Object::toString, Object::toString)
            ),
            map(
                Map.Entry::getValue,
                "'value'",
                value,
                e -> entryToStr(e, Object::toString, Object::toString)
            )
        );
    }

    private static <K, V> String entryToStr(Map.Entry<K, V> entry,
                                            Function<K, String> kStrFunc,
                                            Function<V, String> vStrFunc) {
        return "Entry{" +
            "key='" + kStrFunc.apply(entry.getKey()) +
            "',value='" + vStrFunc.apply(entry.getValue()) +
            "'}";
    }

    public static <T> String collectionToStr(Collection<? extends T> collection,
                                             Function<T, String> elemToStr) {
        if (CommonCollections.isEmpty(collection)) {
            return "Collection[]";
        } else {
            return "Collection[" +
                collection.stream()
                    .map(elemToStr)
                    .collect(Collectors.joining(","))
                + "]";
        }
    }

    public static <T> Matcher<Optional<T>> emptyOptional(Function<T, String> toStr) {
        return map(
            Optional::isPresent,
            "'isPresent'",
            is(false),
            o -> optionalToStr(o, toStr)
        );
    }

    public static <T> Matcher<Optional<T>> emptyOptional() {
        return emptyOptional(Object::toString);
    }

    public static <T> Matcher<Optional<T>> optionalHasValue(Matcher<T> value,
                                                            Function<T, String> toStr) {
        return allOf(
            map(
                Optional::isPresent,
                "'isPresent'",
                is(true),
                o -> optionalToStr(o, toStr)
            ),
            map(
                Optional::get,
                "'value'",
                value,
                o -> optionalToStr(o, toStr)
            )
        );
    }

    public static <T> Matcher<Optional<T>> optionalHasValue(Matcher<T> value) {
        return optionalHasValue(value, Object::toString);
    }

    public static <T> Matcher<Optional<T>> optionalHasValue(T value) {
        return optionalHasValue(Matchers.is(value), Object::toString);
    }

    private static <T> String optionalToStr(Optional<T> optional,
                                           Function<T, String> valueToStr) {
        if (!optional.isPresent()) {
            return "Optional{has no value}";
        }
        return "Optional{value='" + valueToStr.apply(optional.get()) + "'}";
    }

    public static <T> TypeSafeMatcher<Future<T>> returns(Matcher<T> matcher) {
        return new TypeSafeMatcher<Future<T>>() {
            @Override
            protected boolean matchesSafely(Future<T> item) {
                try {
                    item.get(60, TimeUnit.SECONDS);
                    return item.isDone() && !item.isCancelled() && matcher.matches(item.get());
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("promise with result: ");
                matcher.describeTo(description);
            }

            @Override
            protected void describeMismatchSafely(Future<T> item, Description mismatchDescription) {
                if (!item.isDone()) {
                    mismatchDescription.appendText("unfinished promise");
                    return;
                }
                try {
                    T value = item.get();
                    mismatchDescription.appendText("promise with result: ");
                    matcher.describeMismatch(value, mismatchDescription);
                } catch (InterruptedException | CancellationException e) {
                    mismatchDescription.appendText("cancelled promise");
                } catch (ExecutionException e) {
                    mismatchDescription.appendText("promise failed with ").appendValue(e);
                }
            }
        };
    }

    public static TypeSafeMatcher<String> containsTimes(String substring, int times) {
        return new SubstringMatcher(substring) {
            private final String error = String.format("contains %d times", times);

            @Override
            protected boolean evalSubstringOf(String string) {
                return ApiStrings.containsStringNTimes(string, substring, times);
            }


            @Override
            protected String relationship() {
                return error;
            }
        };
    }
}
