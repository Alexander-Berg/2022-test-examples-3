package ru.yandex.autotests.direct.httpclient;

import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;

/**
 * Created by alexey-n on 18.08.14.
 */
public class JsonResponse extends TypeSafeDiagnosingMatcher<DirectResponse> {

    private String jsonPath;
    private Filter[] filters;
    private Matcher elementMatcher;

    public JsonResponse(String jsonPath, Matcher elementMatcher, Filter... filters) {
        this.jsonPath = jsonPath;
        this.filters = filters;
        this.elementMatcher = elementMatcher;
    }

    @Override
    protected boolean matchesSafely(DirectResponse item, Description mismatchDescription) {
        Object element;
        try {
            element = JsonPath.read(item.getResponseContent().asString(), jsonPath, filters);
        } catch (IllegalArgumentException e) {
            throw new BackEndClientException("Не удалось прочитать свойство '" + jsonPath + "' в ответе контроллера", e);
        } catch (PathNotFoundException e){
            element = null;
        }
        if(!elementMatcher.matches(element)) {
            elementMatcher.describeMismatch(element, mismatchDescription);
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        String filters = StringUtils.join(this.filters, ", ");
        description.appendText("json element (path = '" + jsonPath + "') is ");
        elementMatcher.describeTo(description);
    }

    @Factory
    public static JsonResponse hasJsonProperty(String jsonPath, Matcher elementMatcher, Filter... filters) {
        return new JsonResponse(jsonPath, elementMatcher, filters);
    }
}
