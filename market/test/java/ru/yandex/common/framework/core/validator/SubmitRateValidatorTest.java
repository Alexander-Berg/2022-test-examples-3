package ru.yandex.common.framework.core.validator;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test for submitRateValidator.
 *
 * @author maxkar
 */
public class SubmitRateValidatorTest {
    /**
     * Tests, that it "just works".
     */
    @Test
    public void testItJustWorks() {
        final SubmitRateValidator v = new SubmitRateValidator();
        v.setPerIpRate(1000);
        v.setTotalRate(1000);
        v.setAccountPeriod(1000);

        assertNull(v.validateIp("1.2.3.4"));
    }

    @Test
    public void testBlocksIfIpRateExceeded() {
        SubmitRateValidator v = new SubmitRateValidator();
        v.setAccountPeriod(1000);
        v.setPerIpRate(2);
        v.setTotalRate(100);

        assertNull(v.validateIp("1.2.3.4"));
        assertNull(v.validateIp("1.2.3.4"));
        assertNotNull(v.validateIp("1.2.3.4"));
        assertNotNull(v.validateIp("1.2.3.4"));
    }


    @Test
    public void testBlocksCorrectIp() {
        SubmitRateValidator v = new SubmitRateValidator();
        v.setAccountPeriod(1000);
        v.setPerIpRate(2);
        v.setTotalRate(100);

        assertNull(v.validateIp("1.2.3.4"));
        assertNull(v.validateIp("2.3.4.5"));
        assertNull(v.validateIp("2.3.4.5"));
        assertNotNull(v.validateIp("2.3.4.5"));
        assertNull(v.validateIp("3.4.5.6"));
    }

    @Test
    public void testAccountPeriod() throws Exception {
        SubmitRateValidator v = new SubmitRateValidator();
        v.setAccountPeriod(1);
        v.setPerIpRate(2);
        v.setTotalRate(100);

        assertNull(v.validateIp("1.2.3.4"));
        assertNull(v.validateIp("2.3.4.5"));
        Thread.sleep(600);
        assertNull(v.validateIp("2.3.4.5"));
        Thread.sleep(600);
        assertNull(v.validateIp("2.3.4.5"));
        assertNull(v.validateIp("3.4.5.6"));
    }

    @Test
    public void testBlocksIfTotalRateExceeded() {
        SubmitRateValidator v = new SubmitRateValidator();
        v.setAccountPeriod(1000);
        v.setPerIpRate(10);
        v.setTotalRate(2);

        assertNull(v.validateIp("1.2.3.4"));
        assertNull(v.validateIp("2.3.4.5"));
        assertNotNull(v.validateIp("3.4.5.6"));
    }
}
