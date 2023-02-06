package ru.yandex.market.sdk.userinfo.matcher;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.concurrent.NotThreadSafe;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.market.sdk.userinfo.matcher.dsl.FieldDsl;

/**
 * @authror dimkarp93
 */
@NotThreadSafe
public class CommonMatcher<T> extends TypeSafeMatcher<T> {
    private final String name;
    private final Collection<FieldDsl<? super T, ?>> matchers;

    private Collection<FieldDsl<? super T, ?>> failed;

    public CommonMatcher(Class<T> clazz, Collection<FieldDsl<? super T, ?>> matchers) {
        this(clazz.getSimpleName(), clazz, matchers);
    }

    public CommonMatcher(String name, Class<T> clazz, Collection<FieldDsl<? super T, ?>> matchers) {
        super(clazz);
        this.name = name;
        this.matchers = matchers;
    }

    @Override
    protected boolean matchesSafely(T item) {
        failed = new ArrayList<>();
        for (FieldDsl m: matchers) {
            if (!m.matches(item)) {
                failed.add(m);
            }

        }
        return failed.isEmpty();
    }

    @Override
    public void describeTo(Description description) {
        if (null == failed || failed.isEmpty()) {
            //Ни разу не звали матч или нету расхождений
            return;
        }

        description.appendText("\n").appendText(name).appendText("{\n");
        for (FieldDsl<? super T, ?> m: failed) {
            description
                    .appendText("\t")
                    .appendText(m.getName())
                    .appendText(" = ")
                    .appendDescriptionOf(m.getMatcher())
                    .appendText("\n");
        }
        description.appendText("}\n");
    }

    @Override
    protected void describeMismatchSafely(T item, Description description) {
        if (null == failed || failed.isEmpty()) {
            //Ни разу не звали матч или нету расхождений
            return;
        }

        description.appendText("\n").appendText(name).appendText("{\n");
        for (FieldDsl<? super T, ?> m: failed) {
            description
                    .appendText("\t")
                    .appendText(m.getName())
                    .appendText(" = ")
                    .appendValue(m.extract(item))
                    .appendText("\n");

        }
        description.appendText("}\n");
    }
}
