package ru.yandex.market.api.run;

import com.google.common.collect.Sets;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import ru.yandex.market.api.util.ApiStrings;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;

/**
 * @author dimkarp93
 */
public class TestStatusFormatter {
    private static final String SUCCESS = "PASSED";
    private static final String IGNORED = "SKIPPED";
    private static final String FAILURE = "FAILURE";

    private static final String TEMPLATE = "%s > %s %s%n%n";

    private StringBuilder outStringBuilder = new StringBuilder(4 * 1024);

    private Set<Description> failured = Sets.newHashSet();

    public void formatTestFinished(Description description) {
        if (!failured.contains(description)) {
            outStringBuilder.append(String.format(TEMPLATE, description.getClassName(), description.getMethodName(), SUCCESS));
        }
    }

    public void formatTestIgnored(Description description) {
        outStringBuilder.append(String.format(TEMPLATE, description.getClassName(), description.getMethodName(), IGNORED));
    }

    public void formatTestFailure(Failure failure) {
        Description description = failure.getDescription();
        failured.add(description);
        outStringBuilder.append(String.format(TEMPLATE, description.getClassName(), description.getMethodName(), FAILURE));
        ByteArrayOutputStream stream = new ByteArrayOutputStream(4 * 1024);
        try {
            failure.getException().printStackTrace(new PrintStream(stream, true, ApiStrings.UTF8_CHARSET_NAME));
            outStringBuilder.append(stream.toString(ApiStrings.UTF8_CHARSET_NAME)).append('\n');
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public String getOutput() {
        return outStringBuilder.toString();
    }
}
