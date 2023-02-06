package ru.yandex.market.tsum.tms.tasks.duty.shuffle_escalation_logins;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.market.tsum.clients.juggler.JugglerApiClient;
import ru.yandex.market.tsum.tms.tasks.duty.switchduty.SwitchDutyTaskTestConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@TestPropertySource(properties = {
    "tsum.tms.duty-switch.department.logistics-delivery-nsk-shuffle-phone-escalation-logins.rule-id="
        + ShuffleDeliveryNskPhoneEscalationTest.RULE_ID,
    "tsum.tms.duty-switch.department.logistics-delivery-nsk-shuffle-phone-escalation-logins.duty-login="
        + ShuffleDeliveryNskPhoneEscalationTest.DUTY_LOGIN
})
@ContextConfiguration(classes = SwitchDutyTaskTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ShuffleDeliveryNskPhoneEscalationTest {
    protected static final String RULE_ID = "test_rule";
    protected static final String DUTY_LOGIN = "'@svc_delivery:duty_one'";

    @Autowired
    private JugglerApiClient jugglerApiClient;
    @Autowired
    private ShuffleDeliveryNskPhoneEscalation shuffleDeliveryNskPhoneEscalation;
    @Autowired
    private ExecutionContext executionContext;
    @Captor
    private ArgumentCaptor<List<String>> loginsCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testShuffling() throws Exception {
        List<String> logins = List.of(
            DUTY_LOGIN,
            "login1",
            "login2",
            "login3",
            "login4"
        );
        doReturn(logins).when(jugglerApiClient).getLoginsFromNotificationRule(RULE_ID);
        shuffleDeliveryNskPhoneEscalation.execute(executionContext);
        verify(jugglerApiClient).updateNotificationRule(eq(RULE_ID), loginsCaptor.capture());
        List<String> actualLogins = loginsCaptor.getValue();
        assertEquals(logins.size(), actualLogins.size());
        assertEquals(DUTY_LOGIN, actualLogins.get(0));
        assertTrue(actualLogins.containsAll(logins));
        assertNotEquals(logins, actualLogins);
        reset(jugglerApiClient);
    }

    @Test
    public void testShufflingLessThanTwoLogins() throws Exception {
        List<String> logins = List.of(DUTY_LOGIN);
        doReturn(logins).when(jugglerApiClient).getLoginsFromNotificationRule(RULE_ID);
        shuffleDeliveryNskPhoneEscalation.execute(executionContext);
        verify(jugglerApiClient).getLoginsFromNotificationRule(RULE_ID);
        verifyNoMoreInteractions(jugglerApiClient);
        reset(jugglerApiClient);
    }

    @Test
    public void testShufflingExactlyTwoLogins() throws Exception {
        List<String> logins = List.of(DUTY_LOGIN, "login1");
        doReturn(logins).when(jugglerApiClient).getLoginsFromNotificationRule(RULE_ID);
        shuffleDeliveryNskPhoneEscalation.execute(executionContext);
        verify(jugglerApiClient).getLoginsFromNotificationRule(RULE_ID);
        verifyNoMoreInteractions(jugglerApiClient);
        reset(jugglerApiClient);
    }
}
