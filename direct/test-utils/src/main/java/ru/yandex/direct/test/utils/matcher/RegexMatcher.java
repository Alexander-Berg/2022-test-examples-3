package ru.yandex.direct.test.utils.matcher;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

@ParametersAreNonnullByDefault
public class RegexMatcher extends BaseMatcher<Object> {
    private final String regex;

    public RegexMatcher(String regex){
        this.regex = regex;
    }

    public boolean matches(Object o){
        return ((String)o).matches(regex);

    }

    public void describeTo(Description description){
        description.appendText("matches regex=");
    }

    public static RegexMatcher matches(String regex){
        return new RegexMatcher(regex);
    }
}
