package ru.yandex.autotests.innerpochta.tests.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.*;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.innerpochta.tests.headers.ismixed.SOTypesData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

/**
 * User: alex89
 * Date: 25.08.17
 */
public class SoTypesInLogMatcher extends TypeSafeMatcher<String> {
    private static final Pattern SO_TYPES_IN_LOG_PATTERN =
            Pattern.compile("message types:(?<soLabel>[0-9A-Za-z-_, ]*)");
    private String errorMessage;
    private static String expectedMessage;

    private Matcher<List<String>> soTypesMatcher;


    public SoTypesInLogMatcher(Matcher soTypesMatcher) {
        this.soTypesMatcher = soTypesMatcher;
    }

    @Override
    protected boolean matchesSafely(String sessionLog) {
        List<String> soTypes = getSoLabels(sessionLog);
        errorMessage = soTypes.toString();
        return soTypesMatcher.matches(soTypes);
    }

    public static List<String> getSoLabels(String sessionLog) {
        List<String> labels = new ArrayList<String>();
        java.util.regex.Matcher soLabelMatcher = SO_TYPES_IN_LOG_PATTERN.matcher(sessionLog);
        if(soLabelMatcher.find())
        {
            labels.addAll(Arrays.asList(soLabelMatcher.group("soLabel").trim().split("\\, ")));
        }
        return labels;
    }

    @Override
    protected void describeMismatchSafely(String sessionLog, Description description) {
        description.appendText(errorMessage);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(expectedMessage);
    }

    public static SoTypesInLogMatcher hasSoTypes(List<SOTypesData.SoTypes>  listOfTypes) {
        expectedMessage = listOfTypes.toString();
        List<String> expectedListOfSoLabelsList = listOfTypes.stream()
                .map(SOTypesData.SoTypes::getName).collect(Collectors.toList());
        expectedListOfSoLabelsList =   expectedListOfSoLabelsList.stream().distinct().collect(Collectors.toList());
        return new SoTypesInLogMatcher(hasSameItemsAsList(expectedListOfSoLabelsList));
    }

    public static SoTypesInLogMatcher hasSoType(String typeName) {
        expectedMessage = "[" + typeName + "]";
        return new SoTypesInLogMatcher(hasItem(typeName));
    }

    public static SoTypesInLogMatcher hasNoSoType(String typeName) {
        expectedMessage = "has no [" + typeName + "]";
        return new SoTypesInLogMatcher(not(hasItem(typeName)));
    }
}
