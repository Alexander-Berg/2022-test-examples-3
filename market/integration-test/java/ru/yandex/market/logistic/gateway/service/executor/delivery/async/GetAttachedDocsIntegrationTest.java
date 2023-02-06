package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistics.test.integration.matchers.JsonMatcher;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_ATTACHED_DOCS_DS;
import static ru.yandex.market.logistic.gateway.service.executor.common.ClientTaskFactory.createClientTask;
import static ru.yandex.market.logistic.gateway.service.util.RequestFactoryWrapper.wrapInBufferedRequestFactory;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetAttachedDocsIntegrationTest extends AbstractIntegrationTest {
    private final static String PDF_URL = "http://pdf.url/label/url.pdf";
    private final static long TASK_ID = 100L;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private AmazonS3 amazonS3Client;

    private MockRestServiceServer mockServer;

    @Autowired
    private GetAttachedDocsExecutor getAttachedDocsExecutor;

    @Before
    public void setup() throws MalformedURLException {
        mockServer = createMockServerByRequest(GET_ATTACHED_DOCS_DS);
        wrapInBufferedRequestFactory(lgwRestTemplatesMapByMethods.get(GET_ATTACHED_DOCS_DS));
        when(amazonS3Client.getUrl(anyString(), anyString())).thenReturn(new URL(PDF_URL));
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccess() throws Exception {
        ClientTask task = createClientTask(
            TASK_ID,
            RequestFlow.DS_GET_ATTACHED_DOCS,
            "fixtures/executors/get_attached_docs/get_attached_docs_message.json"
        );

        when(repository.findTask(TASK_ID)).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_attached_docs/delivery_get_attached_docs.xml",
            "fixtures/response/delivery/get_attached_docs/delivery_get_attached_docs_pdf.xml"
            );

        TaskMessage message = getAttachedDocsExecutor.execute(new ExecutorTaskWrapper(TASK_ID, 0));

        verify(amazonS3Client).putObject(anyString(), anyString(),
            any(ByteArrayInputStream.class), refEq(new ObjectMetadata()));
        verify(amazonS3Client).getUrl(anyString(), anyString());

        softAssert.assertThat(
            new JsonMatcher(getFileContent("fixtures/executors/get_attached_docs/get_attached_docs_response.json"))
            .matches(message.getMessageBody())
        )
            .as("Asserting that JSON response is correct")
            .isTrue();
    }
}
