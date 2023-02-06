package ru.yandex.market.core.matchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

import ru.yandex.market.common.test.util.JsonTestUtil;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author vbudnev
 * @author fbokovikov
 */
public final class HttpClientErrorMatcher {

    @Factory
    public static <T extends HttpStatusCodeException> Matcher<T> hasErrorCode(final HttpStatus comparedVal) {
        return new FeatureMatcher<>(
                equalTo(comparedVal),
                "errorCode",
                "errorCode"
        ) {
            @Override
            protected HttpStatus featureValueOf(final T actual) {
                return actual.getStatusCode();
            }
        };
    }

    @Factory
    public static <T extends HttpStatusCodeException> Matcher<T> hasErrorMessage(final String expectedError) {
        return new FeatureMatcher<>(
                equalTo(expectedError),
                "firstErrorMessage",
                "firstErrorMessage"
        ) {
            @Override
            protected String featureValueOf(final T exception) {
                return extractErrorMessage(exception);
            }

            private String extractErrorMessage(T expectedException) {
                return JsonTestUtil.parseJson(expectedException.getResponseBodyAsString())
                        .getAsJsonObject()
                        .getAsJsonArray("errors").get(0)
                        .getAsJsonObject().get("message")
                        .getAsString();
            }

        };
    }

    /**
     * Статическая фабрика матчеров для проверки ответов с ошибками в ПИ.
     * <p>
     * Можно исспользовать для валидации формата ответа в кейсах возникновения ошибок, описанных здесь
     * {@link https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/#oshibkinovyjjformat}
     */
    @Factory
    public static Matcher<JsonObject> errorMatches(String errorCode, String field, String subCode) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(JsonObject item) {
                String itemErrorCode = item.get("code").getAsString();
                JsonObject itemDetails = item.getAsJsonObject("details");
                //'code' - единственный обязательный элемент в ответе
                if (itemDetails != null) {
                    //достаем field, если он есть
                    String itemField = Optional.ofNullable(itemDetails.get("field"))
                            .map(JsonElement::getAsString)
                            .orElse(null);

                    //достаем subcode, если он есть
                    String itemSubCode = Optional.ofNullable(itemDetails.get("subcode"))
                            .map(JsonElement::getAsString)
                            .orElse(null);

                    return Objects.equals(errorCode, itemErrorCode)
                            && Objects.equals(field, itemField)
                            && Objects.equals(subCode, itemSubCode);
                } else {
                    return Objects.equals(errorCode, itemErrorCode);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("{\"code\":").appendValue(errorCode).appendText(",")
                        .appendText("\"details\":{\"field\":").appendValue(field).appendText(",")
                        .appendText("\"subCode\":").appendValue(subCode).appendText("}");
            }
        };
    }

    /**
     * Статическая фабрика матчеров для сравнения списка ошибок из ошибочного ответа ПИ без учета порядка.
     */
    @Factory
    @SafeVarargs
    public static <T extends HttpStatusCodeException> Matcher<T> errorListMatchesInAnyOrder(Matcher<JsonObject>... errorMatchers) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(T e) {
                JsonArray errors = JsonTestUtil.parseJson(e.getResponseBodyAsString())
                        .getAsJsonObject()
                        .getAsJsonArray("errors");
                List<JsonObject> errorsList = new ArrayList<>(errors.size());
                for (JsonElement o : errors) {
                    errorsList.add(o.getAsJsonObject());
                }

                return containsInAnyOrder(errorMatchers).matches(errorsList);
            }

            @Override
            public void describeTo(Description description) {
                description.appendList("[", ", ", "]", Arrays.asList(errorMatchers))
                        .appendText(" in any order");
            }

            @Override
            protected void describeMismatchSafely(T item, Description mismatchDescription) {
                JsonObject jsonObject = JsonTestUtil.parseJson(item.getResponseBodyAsString()).getAsJsonObject();
                JsonArray errors = jsonObject.getAsJsonArray("errors");

                mismatchDescription.appendText("was ").appendValue(errors);
            }
        };
    }

    @Factory
    public static <T extends HttpStatusCodeException> Matcher<T> bodyMatches(Matcher<String> bodyMatcher) {
        return new TypeSafeMatcher<>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Body matches");
                bodyMatcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(T item) {
                return bodyMatcher.matches(item.getResponseBodyAsString());
            }

            @Override
            protected void describeMismatchSafely(T item, Description mismatchDescription) {
                mismatchDescription.appendText("Body doesn't match: ");
                bodyMatcher.describeMismatch(item.getResponseBodyAsString(), mismatchDescription);
            }
        };
    }
}
