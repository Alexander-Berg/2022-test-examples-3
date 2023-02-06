package ru.yandex.market.mbo.tms.billing;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.utils.ApplicationUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 * @date 19.2.2020
 */
@RunWith(MockitoJUnitRunner.class)
public class IncompleteBillingSessionFinisherTest {

    IncompleteBillingSessionFinisher incompleteBillingSessionFinisher;
    @Mock
    BillingSessionMarks billingSessionMarks;

    private static final String CURRENT_HOST = ApplicationUtils.getHostName();
    private static final String OTHER_HOST = "OTHER_HOSTNAME_000";

    @Before
    public void setUp() {
        incompleteBillingSessionFinisher = new IncompleteBillingSessionFinisher(billingSessionMarks);
    }

    @Test
    public void testNullHostname() {
        when(billingSessionMarks.loadLastIncompleteSessionHostname()).thenReturn(Optional.empty());
        incompleteBillingSessionFinisher.endUnsuccessfullyForCurrentHost();
        verify(billingSessionMarks, times(0)).storeSessionEnd(eq(false));
    }

    @Test
    public void testEmptyStrHostname() {
        when(billingSessionMarks.loadLastIncompleteSessionHostname()).thenReturn(Optional.of(""));
        incompleteBillingSessionFinisher.endUnsuccessfullyForCurrentHost();
        verify(billingSessionMarks, times(0)).storeSessionEnd(eq(false));
    }

    @Test
    public void testHostnameIsCurrent() {
        when(billingSessionMarks.loadLastIncompleteSessionHostname()).thenReturn(Optional.of(CURRENT_HOST));
        incompleteBillingSessionFinisher.endUnsuccessfullyForCurrentHost();
        verify(billingSessionMarks).storeSessionEnd(eq(false));
    }

    @Test
    public void testHostnameIsDifferent() {
        when(billingSessionMarks.loadLastIncompleteSessionHostname()).thenReturn(Optional.of(OTHER_HOST));
        incompleteBillingSessionFinisher.endUnsuccessfullyForCurrentHost();
        verify(billingSessionMarks, times(0)).storeSessionEnd(eq(false));
    }
}
