package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.openqa.selenium.WebDriver;
import ru.lanwen.diff.uri.UriDiffer;
import ru.lanwen.diff.uri.core.UriDiff;
import ru.lanwen.diff.uri.core.filters.UriDiffFilter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.yandex.autotests.innerpochta.matchers.CurrentUrlMatcher.currentUrl;

/**
 * User: lanwen
 * Date: 01.10.13
 * Time: 13:21
 */
public class UriDiffMatcher extends TypeSafeDiagnosingMatcher<String> {

    private URI expectedUri;
    private List<UriDiffFilter> filters = new ArrayList<>();

    public UriDiffMatcher(URI expectedUri) {
        this.expectedUri = expectedUri;
    }

    @Override
    protected boolean matchesSafely(String s, Description description) {
        UriDiff changes;
        try {
            changes = UriDiffer.diff().expected(expectedUri).actual(s).filter(filters).changes();
            description.appendText("was: ").appendValue(s).appendText("\nwith diff: ")
                    .appendValue(changes.report()).appendText("\n");
            description.appendValue(changes.getChanges());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Wrong URI! " + s, e);
        }
        return !changes.hasChanges();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("same as: ").appendValue(expectedUri);
    }

    private UriDiffMatcher filtered(UriDiffFilter... filters) {
        this.filters.addAll(Arrays.asList(filters));
        return this;
    }

    private UriDiffMatcher filtered(List<UriDiffFilter> filters) {
        this.filters.addAll(filters);
        return this;
    }

    @Factory
    public static UriDiffMatcher uriSameAs(String expectedUri) throws IllegalArgumentException {
        try {
            return new UriDiffMatcher(new URI(expectedUri));
        } catch (URISyntaxException e){
            throw new IllegalArgumentException(e);
        }
    }

    @Factory
    public static Matcher<WebDriver> currentUriSameAs(String expectedUri, UriDiffFilter... filters) throws IllegalArgumentException {
        return currentUrl(uriSameAs(expectedUri).filtered(filters));
    }

    @Factory
    public static Matcher<WebDriver> currentUriSameAs(String expectedUri, List<UriDiffFilter> filters) throws IllegalArgumentException {
        return currentUrl(uriSameAs(expectedUri).filtered(filters));
    }
}
