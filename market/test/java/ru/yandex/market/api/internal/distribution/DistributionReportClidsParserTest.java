package ru.yandex.market.api.internal.distribution;

import org.junit.Test;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DistributionReportClidsParserTest {
    @Test
    public void test() {
        byte[] toParse = ResourceHelpers.getResource("distribution_response.json");
        Map<Long, String> result = new DistributionReportClidsParser().parse(toParse);
        assertEquals(3, result.size());
        String forClid11 = result.get(11L);
        assertEquals("user_login1", forClid11);
        String forClid12 = result.get(12L);
        assertEquals("user_login1", forClid12);
        String forClid13 = result.get(13L);
        assertEquals("user_login2", forClid13);
    }

    @Test(expected = Exception.class)
    public void testError() {
        byte[] toParse = ResourceHelpers.getResource("distribution_response_error.json");
        new DistributionReportClidsParser().parse(toParse);
    }
}
