package ru.yandex.market.logistics.utilizer.service.autoclose;

import java.io.IOException;
import java.util.List;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.service.cycle.autoclose.AutoCloseService;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class AutoCloseServiceTest extends AbstractContextualTest {

    @Autowired
    private AutoCloseService autoCloseService;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/autoclose/1/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/autoclose/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void oneEmptyOneNot() {
        Issue callIssue = mock(Issue.class);
        Issue recheckIssue = mock(Issue.class);
        when(callIssue.getKey()).thenReturn("CALL-1");
        when(recheckIssue.getKey()).thenReturn("RECHECK-1");
        when(startrekService.findIssuesByKeys(any())).thenReturn(List.of(recheckIssue));
        when(recheckIssue.getComments()).thenReturn(Cf.emptyIterator());


        runInExternalTransaction(() -> autoCloseService.closeEmptyUtilizationCycle(3), false);

        Mockito.verify(recheckIssue, never()).executeTransition(anyString(), any());
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/autoclose/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/autoclose/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void closeEmpty() {
        Issue callIssue = mock(Issue.class);
        Issue recheckIssue = mock(Issue.class);
        when(callIssue.getKey()).thenReturn("CALL-1");
        when(recheckIssue.getKey()).thenReturn("RECHECK-2");
        when(startrekService.findIssuesByKeys(any())).thenReturn(List.of(recheckIssue));
        when(recheckIssue.getComments()).thenReturn(Cf.emptyIterator());


        runInExternalTransaction(() -> autoCloseService.closeEmptyUtilizationCycle(3), false);

        Mockito.verify(recheckIssue, times(1)).executeTransition(eq("close"), any());
    }

}
