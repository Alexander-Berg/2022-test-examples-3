package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import ru.yandex.qatools.htmlelements.element.TextInput;

import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created by kurau on 11.03.14.
 */
public class InputFieldHasTextMatcher {

    public static Matcher<TextInput> hasTextInField(String text) {
        return new FeatureMatcher<TextInput, String>(equalTo(text), "text should be", "actual"
        ) {
            @Override
            protected String featureValueOf(TextInput element) {
                return element.getText();
            }
        };
    }
}
