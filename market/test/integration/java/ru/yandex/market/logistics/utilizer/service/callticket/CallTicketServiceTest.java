package ru.yandex.market.logistics.utilizer.service.callticket;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.util.ExcelAssertion;
import ru.yandex.startrek.client.model.Attachment;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

public class CallTicketServiceTest extends AbstractContextualTest {
    private static final String CALLTICKET_GENERATION_PATH = "fixtures/service/callticket/";

    private final ExcelAssertion excelAssertion = new ExcelAssertion(softly);

    @Autowired
    private CallTicketService callTicketService;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/callticket/1/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/callticket/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void createForFinalized() throws IOException {
        Attachment attachment = mock(Attachment.class);
        Issue callIssue = mock(Issue.class);
        Issue recheckIssue = mock(Issue.class);
        when(callIssue.getKey()).thenReturn("callTicket1");
        when(recheckIssue.getKey()).thenReturn("recheckTicket1");
        when(startrekService.createIssue(any())).thenReturn(callIssue);
        when(startrekService.findIssuesByKeys(any())).thenReturn(List.of(recheckIssue));
        when(startrekService.uploadAttachment(anyString(), any())).thenReturn(attachment);

        runInExternalTransaction(() -> callTicketService.createCallTicket(1), false);

        ArgumentCaptor<InputStream> captorData = ArgumentCaptor.forClass(InputStream.class);
        ArgumentCaptor<String> captorFileName = ArgumentCaptor.forClass(String.class);
        Mockito.verify(startrekService, times(1)).uploadAttachment(captorFileName.capture(), captorData.capture());

        List<String> fileNames = captorFileName.getAllValues();
        List<InputStream> allValues = captorData.getAllValues();
        for (int i = 0; i < fileNames.size(); i++) {
            String fileName = fileNames.get(i);
            InputStream fileData = allValues.get(i);
            excelAssertion.assertXlsx(fileData, CALLTICKET_GENERATION_PATH + "1/" + fileName);
        }

    }


    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/callticket/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/callticket/2/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkNoRetry() {
        runInExternalTransaction(() -> callTicketService.createCallTicket(1), false);
        Mockito.verifyNoInteractions(startrekService);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/callticket/3/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/callticket/3/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void skipEmptyCycle() {
        runInExternalTransaction(() -> callTicketService.createCallTicket(1), false);
        Mockito.verifyNoInteractions(startrekService);
    }
}
