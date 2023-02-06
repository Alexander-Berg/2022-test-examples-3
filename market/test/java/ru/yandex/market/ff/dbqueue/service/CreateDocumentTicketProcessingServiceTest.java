package ru.yandex.market.ff.dbqueue.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.CreateDocumentTicketPayload;
import ru.yandex.market.ff.model.dto.CreateDocumentTicketDTO;
import ru.yandex.market.ff.service.StartrekIssueService;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.StatusRef;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class
CreateDocumentTicketProcessingServiceTest extends IntegrationTest {

    private static final String QUEUE_NAME = "TEST1p";

    private static final String TASK_TYPE = "serviceRequest";

    @Autowired
    private CreateDocumentTicketProcessingService service;

    @Autowired
    private StartrekIssueService startrekIssueService;

    @BeforeEach
    void init() {
        Mockito.reset(startrekIssueService);
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/create-document-ticket/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/create-document-ticket/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createWhenIssueNotFound() {

        StatusRef statusRef = mock(StatusRef.class);
        when(statusRef.getKey()).thenReturn("open");
        Issue issueMock = mock(Issue.class);
        when(issueMock.getKey()).thenReturn("TEST-252063");
        when(issueMock.getStatus()).thenReturn(statusRef);
        when(startrekIssueService.findIssueUrlByTagsAndComponent(any(), anyLong(), eq(QUEUE_NAME))).thenReturn(null);
        when(startrekIssueService.createDocumentTicket(any(), eq(QUEUE_NAME), eq(TASK_TYPE))).thenReturn(issueMock);

        service.processPayload(new CreateDocumentTicketPayload(1L));

        ArgumentCaptor<CreateDocumentTicketDTO> captor = ArgumentCaptor.forClass(CreateDocumentTicketDTO.class);
        verify(startrekIssueService, times(1)).createDocumentTicket(captor.capture(), eq(QUEUE_NAME), eq(TASK_TYPE));
        CreateDocumentTicketDTO value = captor.getValue();

        assertions.assertThat(value.getSupplierName()).isEqualTo("Real Supplier");
        assertions.assertThat(value.getServiceRequestId()).isEqualTo("TST0001234-4");
        assertions.assertThat(value.getServiceName()).isEqualTo("warehouse test");
        assertions.assertThat(value.getExternalRequestId()).isEqualTo("Зп0123456");
        assertions.assertThat(value.getComponentId()).isEqualTo(333);
        assertions.assertThat(value.getTags()).doesNotContain("X-Dock");
        assertions.assertThat(value.getTags()).contains("Импорт");

    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:db-queue/service/create-document-ticket/disable-component-mapping.xml"),
            @DatabaseSetup("classpath:db-queue/service/create-document-ticket/before.xml")
    })
    @ExpectedDatabase(value = "classpath:db-queue/service/create-document-ticket/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createWhenIssueNotFoundAndComponentMappingDisabled() {

        StatusRef statusRef = mock(StatusRef.class);
        when(statusRef.getKey()).thenReturn("open");
        Issue issueMock = mock(Issue.class);
        when(issueMock.getKey()).thenReturn("TEST-252063");
        when(issueMock.getStatus()).thenReturn(statusRef);
        when(startrekIssueService.findIssueUrlByTagsAndComponent(any(), anyLong(), eq(QUEUE_NAME))).thenReturn(null);
        when(startrekIssueService.createDocumentTicket(any(), eq(QUEUE_NAME), eq(TASK_TYPE))).thenReturn(issueMock);

        service.processPayload(new CreateDocumentTicketPayload(1L));

        ArgumentCaptor<CreateDocumentTicketDTO> captor = ArgumentCaptor.forClass(CreateDocumentTicketDTO.class);
        verify(startrekIssueService, times(1)).createDocumentTicket(captor.capture(), eq(QUEUE_NAME), eq(TASK_TYPE));
        CreateDocumentTicketDTO value = captor.getValue();

        assertions.assertThat(value.getSupplierName()).isEqualTo("Real Supplier");
        assertions.assertThat(value.getServiceRequestId()).isEqualTo("TST0001234-4");
        assertions.assertThat(value.getServiceName()).isEqualTo("warehouse test");
        assertions.assertThat(value.getExternalRequestId()).isEqualTo("Зп0123456");
        assertions.assertThat(value.getComponentId()).isEqualTo(null);
        assertions.assertThat(value.getTags()).contains("Импорт");
        assertions.assertThat(value.getTags()).contains("warehouse_test");
        assertions.assertThat(value.getTags()).contains("Поставка");
        assertions.assertThat(value.getTags()).contains("1P");
        assertions.assertThat(value.getTags()).doesNotContain("X-Dock");
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/create-document-ticket/when-no-real-supplier/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/create-document-ticket/when-no-real-supplier/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createWhenRealSupplierIsNull() {

        StatusRef statusRef = mock(StatusRef.class);
        when(statusRef.getKey()).thenReturn("open");
        Issue issueMock = mock(Issue.class);
        when(issueMock.getKey()).thenReturn("TEST-252063");
        when(issueMock.getStatus()).thenReturn(statusRef);
        when(startrekIssueService.findIssueUrlByTagsAndComponent(any(), anyLong(), eq(QUEUE_NAME))).thenReturn(null);
        when(startrekIssueService.createDocumentTicket(any(), eq(QUEUE_NAME), eq(TASK_TYPE))).thenReturn(issueMock);

        service.processPayload(new CreateDocumentTicketPayload(1L));

        ArgumentCaptor<CreateDocumentTicketDTO> captor = ArgumentCaptor.forClass(CreateDocumentTicketDTO.class);
        verify(startrekIssueService, times(1)).createDocumentTicket(captor.capture(), eq(QUEUE_NAME), eq(TASK_TYPE));
        CreateDocumentTicketDTO value = captor.getValue();

        assertions.assertThat(value.getSupplierName()).isEqualTo("Market");
        assertions.assertThat(value.getServiceRequestId()).isEqualTo("TST0001234-4");
        assertions.assertThat(value.getServiceName()).isEqualTo("warehouse test");
        assertions.assertThat(value.getExternalRequestId()).isEqualTo("Зп0123456");
        assertions.assertThat(value.getComponentId()).isEqualTo(333);
        assertions.assertThat(value.getTags()).doesNotContain("X-Dock");

    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/create-document-ticket/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/create-document-ticket/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void updateKeyWhenIssueWasFound() {
        StatusRef statusRef = mock(StatusRef.class);
        when(statusRef.getKey()).thenReturn("open");
        Issue issueMock = mock(Issue.class);
        when(issueMock.getKey()).thenReturn("TEST-252063");
        when(issueMock.getStatus()).thenReturn(statusRef);
        when(startrekIssueService.findIssueUrlByTagsAndComponent(any(), anyLong(), eq(QUEUE_NAME)))
                .thenReturn(issueMock);
        verify(startrekIssueService, never()).createDocumentTicket(any(), eq(QUEUE_NAME), eq(TASK_TYPE));

        service.processPayload(new CreateDocumentTicketPayload(1L));

    }


}
