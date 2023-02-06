package ru.yandex.autotests.innerpochta.rules;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.junit4.AllureJunit4;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.util.ResultsUtils;
import net.thucydides.core.webdriver.WebdriverAssertionError;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;

import java.net.ConnectException;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.cthul.matchers.CthulMatchers.either;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsString;


/**
 * Usage:
 *
 * @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule public RetryRule retry = RetryRule.retry().ifException(WebdriverAssertionError.class)
 * .every(20, TimeUnit.SECONDS).times(2);
 */
@SuppressWarnings("unused")
public class RetryRule implements TestRule {

    private final AllureLifecycle lifecycle;
    private Matcher<Object> matcher;
    private int attempts = 3;
    private int delay = 10;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private int count = 0;
    private Matcher<Object> assertionMatcher;
    private int assertionCount = 0;
    private int assertionAttempts = 1;

    public RetryRule() {
        this(Allure.getLifecycle());
    }

    public RetryRule(AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public static RetryRule retry() {
        return new RetryRule();
    }

    public static RetryRule baseRetry() {
        return RetryRule.retry()
            .ifException(WebDriverException.class)
            .ifException(SocketException.class)
            .ifException(ConnectException.class)
            .ifException(StaleElementReferenceException.class)
            .ifException(WebdriverAssertionError.class)
            .ifException(ElementClickInterceptedException.class)
            .withAssertionAttempts(1)
            .ifMessage(containsString("was terminated due to SO_TIMEOUT"))
            .ifMessage(containsString("Session timed out or not found"))
            .ifMessage(containsString("unable to set cookie"))
            .ifMessage(containsString("element is not attached"))
            .ifMessage(containsString("retry"))
            .ifMessage(containsString("failed to respond"))
            .every(3, TimeUnit.SECONDS).times(3);
    }

    public RetryRule every(int delay, TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        this.delay = delay;
        return this;
    }

    public RetryRule times(int attempts) {
        this.attempts = attempts;
        return this;
    }

    @SuppressWarnings("unchecked")
    public RetryRule ifException(Matcher<?> newMatcher) {
        if (matcher == null)
            matcher = (Matcher<Object>) newMatcher;
        else
            matcher = either(matcher).or((Matcher<Object>) newMatcher);
        return this;
    }

    /**
     * Adds to the list of requirements for any thrown exception that it
     * should be an instance of {@code type}
     */
    public RetryRule ifException(Class<? extends Throwable> type) {
        return ifException(instanceOf(type));
    }

    /**
     * Adds to the list of requirements for any thrown exception that it
     * should <em>contain</em> string {@code substring}
     */
    public RetryRule ifMessage(String substring) {
        return ifMessage(containsString(substring));
    }

    /**
     * Adds {@code matcher} to the list of requirements for the message
     * returned from any thrown exception.
     */
    public RetryRule ifMessage(Matcher<String> matcher) {
        return ifException(hasMessage(matcher));
    }

    /**
     * Adds retries for FluentWait Timeout and Assertions
     *
     * @param assertionAttempts is a number of retries caused buy timeout
     */
    public RetryRule withAssertionAttempts(int assertionAttempts) {
        Matcher<Object> matcher =
            either(instanceOf(TimeoutException.class))
            .or(hasMessage(containsString("TimeoutException")))
            .or(instanceOf(AssertionError.class));
        if (assertionMatcher == null)
            assertionMatcher = matcher;
        else
            assertionMatcher = either(assertionMatcher)
                .or(matcher);
        this.assertionAttempts = assertionAttempts;
        return this;
    }

    public RetryRule or() {
        return this;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable lastException = null;
                for (count = 0; count <= attempts; count++) {
                    try {
                        if (count > 0) {
                            (new AllureJunit4()).testStarted(description);
                        }
                        base.evaluate();
                        return;
                    } catch (Throwable t) {
                        lastException = t;
                        RetryRule.this.getLifecycle().getCurrentTestCase().ifPresent((uuid) -> {
                            RetryRule.this.getLifecycle().updateTestCase(uuid, (testResult) -> {
                                testResult.withStatus((Status) ResultsUtils.getStatus(t).orElse((
                                    Status) null))
                                    .withStatusDetails(
                                        (StatusDetails) ResultsUtils.getStatusDetails(t).orElse((StatusDetails) null));
                            });
                        });
                        if (t instanceof AssumptionViolatedException) {
                            RetryRule.this.getLifecycle().getCurrentTestCase().ifPresent((uuid) -> {
                                RetryRule.this.getLifecycle().updateTestCase(uuid, (testResult) -> {
                                    testResult.setStatus(Status.SKIPPED);
                                });
                            });
                            break;
                        }
                        if (((matcher != null && matcher.matches(lastException)) ||
                            (assertionMatcher != null && assertionMatcher.matches(lastException))) &&
                            (assertionCount < assertionAttempts)) {
                            if (count < attempts) {
                                RetryRule.this.getLifecycle().getCurrentTestCase().ifPresent((uuid) -> {
                                    RetryRule.this.getLifecycle().updateTestCase(uuid, (testResult) -> {
                                        testResult.setLabels(new Label().setName("intermediate").setValue("true"));
                                    });
                                });
                            }
                            if (assertionMatcher != null && assertionMatcher.matches(lastException))
                                assertionCount++;
                            System.out.println(
                                String.format(
                                    "Attempt #%s failed, sleeping for %s %s to retry...",
                                    count + 1,
                                    delay,
                                    timeUnit.name()
                                )
                            );
                            Thread.sleep(timeUnit.toMillis(delay));
                        } else {
                            if (assertionAttempts == assertionCount)
                                System.out.println(
                                    "All TimeoutException [" + assertionAttempts + "] retries failed, forgiving...");
                            throw lastException;
                        }
                    } finally {
                        RetryRule.this.getLifecycle().getCurrentTestCase().ifPresent((uuid) -> {
                            RetryRule.this.getLifecycle().updateTestCase(uuid, (testResult) -> {
                                if (Objects.isNull(testResult.getStatus())) {
                                    testResult.setStatus(Status.PASSED);
                                }

                            });
                            RetryRule.this.getLifecycle().stopTestCase(uuid);
                            RetryRule.this.getLifecycle().writeTestCase(uuid);
                        });
                    }
                }
                System.out.println("All [" + attempts + "] attempts failed, forgiving...");
                throw lastException;
            }
        };
    }

    private Matcher<Throwable> hasMessage(final Matcher<String> matcher) {
        return new TypeSafeMatcher<Throwable>() {
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("exception with message ");
                description.appendDescriptionOf(matcher);
            }

            @Override
            public boolean matchesSafely(Throwable item) {
                return matcher.matches(item.getMessage());
            }
        };
    }

    private AllureLifecycle getLifecycle() {
        return this.lifecycle;
    }

    public int getCurrentCount() {
        return count;
    }
}

