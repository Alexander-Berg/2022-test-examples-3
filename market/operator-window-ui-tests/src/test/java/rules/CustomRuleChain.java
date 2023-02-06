package ui_tests.src.test.java.rules;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.openqa.selenium.WebDriver;

public class CustomRuleChain {
    private static WebDriver webDriver;

    public CustomRuleChain(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public static TestRule extendedLogging() {
        return RuleChain.outerRule(new RetryRule())
                .around(new BeforeRules(webDriver));
    }

}
