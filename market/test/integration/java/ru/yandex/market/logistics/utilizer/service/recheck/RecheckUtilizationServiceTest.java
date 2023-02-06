package ru.yandex.market.logistics.utilizer.service.recheck;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.util.ExcelAssertion;
import ru.yandex.startrek.client.model.Attachment;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

public class RecheckUtilizationServiceTest extends AbstractContextualTest {
    private static final String RECHECK_GENERATION_PATH = "fixtures/service/recheck-utilization/";

    private final ExcelAssertion excelAssertion = new ExcelAssertion(softly);

    @Autowired
    private RecheckUtilizationService recheckUtilizationService;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/recheck-utilization/1/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/recheck-utilization/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void recheckFinalizedCycle() throws IOException {
        Attachment attachment = mock(Attachment.class);
        Issue issueMockWH171 = mock(Issue.class);
        Issue issueMockWH172 = mock(Issue.class);
        when(issueMockWH171.getKey()).thenReturn("ticketWH171");
        when(issueMockWH172.getKey()).thenReturn("ticketWH172");
        when(startrekService.createIssue(argThat((ic) ->
                ic != null && ((List<String>)ic.getValues().getTs("tags")).contains("Томилино"))
        )).thenReturn(issueMockWH171);
        when(startrekService.createIssue(argThat((ic) ->
                ic != null && ((List<String>)ic.getValues().getTs("tags")).contains("Софьино"))
        )).thenReturn(issueMockWH172);
        when(startrekService.uploadAttachment(anyString(), any())).thenReturn(attachment);

        runInExternalTransaction(() -> recheckUtilizationService.recheckUtilizationCycles(List.of(1L, 3L)), false);

        ArgumentCaptor<InputStream> captorData = ArgumentCaptor.forClass(InputStream.class);
        ArgumentCaptor<String> captorFileName = ArgumentCaptor.forClass(String.class);
        Mockito.verify(startrekService, times(2)).uploadAttachment(captorFileName.capture(), captorData.capture());

        List<String> fileNames = captorFileName.getAllValues();
        List<InputStream> allValues = captorData.getAllValues();
        for (int i = 0; i < fileNames.size(); i++) {
            String fileName = fileNames.get(i);
            InputStream fileData = allValues.get(i);
            excelAssertion.assertXlsx(fileData, RECHECK_GENERATION_PATH + "1/" + fileName);
        }

    }


    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/recheck-utilization/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/recheck-utilization/2/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkNoRetry() {
        runInExternalTransaction(() -> recheckUtilizationService.recheckUtilizationCycles(List.of(1L)), false);
        Mockito.verifyNoInteractions(startrekService);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/recheck-utilization/3/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/recheck-utilization/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void noCrossLinkage() throws IOException {
        Issue issueMockWH171 = mock(Issue.class);
        Issue issueMockWH172 = mock(Issue.class);
        when(issueMockWH171.getKey()).thenReturn("ticketWH171");
        when(issueMockWH172.getKey()).thenReturn("ticketWH172");
        when(startrekService.createIssue(argThat((ic) ->
                ic != null && ((List<String>)ic.getValues().getTs("tags")).contains("Томилино"))
        )).thenReturn(issueMockWH171);
        when(startrekService.createIssue(argThat((ic) ->
                ic != null && ((List<String>)ic.getValues().getTs("tags")).contains("Софьино"))
        )).thenReturn(issueMockWH172);
        when(startrekService.uploadAttachment(anyString(), any())).thenReturn(mock(Attachment.class));

        runInExternalTransaction(() -> recheckUtilizationService.recheckUtilizationCycles(List.of(1L, 2L)), false);
    }
}
