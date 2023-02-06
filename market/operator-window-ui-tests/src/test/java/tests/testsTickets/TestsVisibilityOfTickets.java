package ui_tests.src.test.java.tests.testsTickets;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;
import rules.BeforeClassRules;
import rules.CustomRuleChain;

public class TestsVisibilityOfTickets {
    private static WebDriver webDriver;

    @Rule
    public TestRule chain = new CustomRuleChain(webDriver).extendedLogging();
    @ClassRule
    public static ExternalResource externalResource = new BeforeClassRules(TestsVisibilityOfTickets.class);
}
