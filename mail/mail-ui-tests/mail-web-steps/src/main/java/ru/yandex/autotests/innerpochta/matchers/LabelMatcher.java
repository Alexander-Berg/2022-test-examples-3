package ru.yandex.autotests.innerpochta.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.innerpochta.ns.pages.folderblocks.CustomLabelBlock;
import ru.yandex.autotests.innerpochta.ns.pages.messages.MessagePage;

import java.util.HashMap;
import java.util.List;

import static org.apache.commons.lang.StringUtils.capitalize;
import static org.apache.commons.lang.StringUtils.trim;
import static ru.yandex.autotests.innerpochta.data.MailEnums.ElementAttributes.STYLE;

/**
 * User: alex89
 * Date: 10.10.12
 */
public class LabelMatcher extends TypeSafeMatcher<MessagePage> {

    private HashMap<String, String> expectedMap;
    private HashMap<String, String> actualMap = new HashMap<String, String>();

    public boolean matchesSafely(MessagePage messagePage) {
        populateMapOfActualLabels(messagePage);
        if (actualMap.size() != expectedMap.size()) {
            return false;
        }
        for (String key : expectedMap.keySet()) {
            if (!expectedMap.get(key).equals(actualMap.get(capitalize(key)))) {
                return false;
            }
        }
        return true;
    }

    private void populateMapOfActualLabels(MessagePage messagePage) {
        List<CustomLabelBlock> actualListOfMarks = messagePage.labelsNavigation().userLabels();
        for (int i = 0; i < actualListOfMarks.size(); i++) {
            actualMap.put(trim(actualListOfMarks.get(i).getText()),
                    messagePage.labelsNavigation().userLabelsColors().get(i).getAttribute(STYLE.getValue()));
        }
    }

    public LabelMatcher(HashMap<String, String> expectedMap) {
        this.expectedMap = expectedMap;
    }

    @Factory
    public static LabelMatcher hasCorrectLabelsList(HashMap<String, String> expectedMap) {
        return new LabelMatcher(expectedMap);
    }

    @Override
    public void describeMismatchSafely(MessagePage messagePage, Description description) {
        description.appendText("Метки на главной странице: ").appendValue(actualMap);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Метки на главной странице соотвествуют ожидаемым: ").appendValue(expectedMap);
    }

}
