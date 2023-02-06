package ru.yandex.market.tsum.pipe.ui.common.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 02/07/2019
 */
public class ClassNameMatcher extends BaseMatcher<HtmlElement> {
    private final String className;

    public ClassNameMatcher(String className) {
        this.className = className;
    }


    @Override
    public void describeTo(Description description) {

    }

    @Override
    public boolean matches(Object item) {
        String classes = ((HtmlElement) item).getAttribute("class");
        return classes != null && classes.contains(className);
    }

    @Override
    public void describeMismatch(Object item, Description mismatchDescription) {
        mismatchDescription.appendText(
            String.format("Expected class %s not found on element", className)
        );
    }
}
