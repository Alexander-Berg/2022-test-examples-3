package ru.yandex.market.request.jetty.trace;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.request.trace.Module;

/**
 * Tests for {@link JettyTraceRequestRecordCreator#hideSensitiveInfo(String, StringBuilder)}
 */
@RunWith(Parameterized.class)
public class HideSensitiveInfoTest {

    private final String query;
    private final String expectedValue;

    public HideSensitiveInfoTest(String query, String expectedValue) {
        this.query = query;
        this.expectedValue = expectedValue;
    }

    private static final JettyTraceRequestRecordCreator recordCreator =
            new JettyTraceRequestRecordCreator(Module.TSUM_API);

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"key", "key"},
                {"key&", "key&"},
                {"user_ticket=ut:userTicket&", "user_ticket=*****&"},
                {"user_ticket=ut:userTicket", "user_ticket=*****"},
                {"user_ticket=1&service_ticket=2", "user_ticket=*****&service_ticket=*****"},
                {"user_ticket=1&service_ticket=2&sessionid=1&", "user_ticket=*****&service_ticket=*****&sessionid=*****&"},
                {"user_ticket=1&service_ticket=2&sessionid=3&sslsessionid=4", "user_ticket=*****&service_ticket=*****&sessionid=*****&sslsessionid=*****"},
                {"user_ticket=1&service_ticket=2&sessionid=3&sslsessionid=4&a=b", "user_ticket=*****&service_ticket=*****&sessionid=*****&sslsessionid=*****&a=b"},
                {"test=test", "test=test"},
                {"a=1&b=2&", "a=1&b=2&"},
                {"a=1&b=2", "a=1&b=2"}
        });
    }

    @Test
    public void hideSensitiveInfoTest() {
        StringBuilder sb = new StringBuilder();
        recordCreator.hideSensitiveInfo(query, sb);
        Assert.assertEquals(expectedValue, sb.toString());
    }
}
