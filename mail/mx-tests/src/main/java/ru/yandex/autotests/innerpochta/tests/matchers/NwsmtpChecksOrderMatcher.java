package ru.yandex.autotests.innerpochta.tests.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;


import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

/**
 * User: alex89
 * Date: 25.08.17
 */
public class NwsmtpChecksOrderMatcher extends TypeSafeMatcher<String> {
    private static final Pattern NWSMTP_CHECKS_IN_LOG_PATTERN =
            Pattern.compile("nwsmtp: [A-Za-z0-9]+-[A-Za-z0-9]+-([A-Za-z0-9-]+)");
    private String errorMessage;
    private static String expectedMessage;

    private Matcher<List<String>> checksListMatcher;


    public NwsmtpChecksOrderMatcher(Matcher checksListMatcher) {
        this.checksListMatcher = checksListMatcher;
    }

    @Override
    protected boolean matchesSafely(String sessionLog) {
        List<String> nwsmtpChecks = new LinkedList<>();
        java.util.regex.Matcher serverFormat = NWSMTP_CHECKS_IN_LOG_PATTERN.matcher(sessionLog);
        while (serverFormat.find()) {
            nwsmtpChecks.add(serverFormat.group(1));
        }
        List<String>  nwsmtpChecksWithoutRepeated = new LinkedList<>();
        nwsmtpChecksWithoutRepeated.add(nwsmtpChecks.get(0));
        for (int i=1; i<nwsmtpChecks.size(); i++){
            if (!nwsmtpChecks.get(i).equals(nwsmtpChecks.get(i-1))){
                nwsmtpChecksWithoutRepeated.add(nwsmtpChecks.get(i));
            }
        }
        errorMessage = nwsmtpChecksWithoutRepeated.toString();
        return checksListMatcher.matches(nwsmtpChecksWithoutRepeated);
    }

    @Override
    protected void describeMismatchSafely(String sessionLog, Description description) {
        description.appendText(errorMessage);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(expectedMessage);
    }

    public static NwsmtpChecksOrderMatcher hasOrderOfChecksInLog(List<String> expectedChecks) {
        expectedMessage = expectedChecks.toString();
        return new NwsmtpChecksOrderMatcher(hasSameItemsAsList(expectedChecks).sameSorted());
    }
}
