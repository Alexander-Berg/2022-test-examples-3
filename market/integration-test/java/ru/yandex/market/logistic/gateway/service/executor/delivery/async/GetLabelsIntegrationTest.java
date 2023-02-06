package ru.yandex.market.logistic.gateway.service.executor.delivery.async;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskMessage;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.dto.ExecutorTaskWrapper;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.repository.ClientTaskRepository;
import ru.yandex.market.logistic.gateway.service.converter.HtmlToPdfConverter;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_LABELS_DS;
import static ru.yandex.market.logistic.gateway.service.util.RequestFactoryWrapper.wrapInBufferedRequestFactory;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetLabelsIntegrationTest extends AbstractIntegrationTest {

    private final static long TEST_ORDER_ID = 100L;

    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    private final static String PDF_URL = "http://pdf.url/label/url.pdf";

    @Value("${lgw.aws.s3BucketName}")
    private String s3BucketName;

    @MockBean
    private ClientTaskRepository repository;

    @MockBean
    private AmazonS3 amazonS3Client;

    @SpyBean
    HtmlToPdfConverter htmlToPdfConverter;

    @Autowired
    private GetLabelsExecutor getLabelsExecutor;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() throws Exception {
        mockServer = createMockServerByRequest(GET_LABELS_DS);

        wrapInBufferedRequestFactory(lgwRestTemplatesMapByMethods.get(GET_LABELS_DS));

        when(uniqService.generate()).thenReturn(UNIQ);

        when(amazonS3Client.getUrl(eq(s3BucketName), anyString())).thenReturn(new URL(PDF_URL));

        doReturn(new byte[0]).when(htmlToPdfConverter).convertWithUnescape(any(), any());
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccessHtmlWithParcelId() throws Exception {
        ClientTask task = getClientTaskWithParcelId();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_labels/delivery_get_labels_with_parcel_id.xml",
            "fixtures/response/delivery/get_labels/delivery_get_labels_html.xml");
        assertExecutionSuccess("fixtures/executors/get_labels/get_labels_task_response_with_parcel_id.json");
    }

    @Test
    public void executeSuccessHtmlWithoutParcelId() throws Exception {
        ClientTask task = getClientTaskWithoutParcelId();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_labels/delivery_get_labels_without_parcel_id.xml",
            "fixtures/response/delivery/get_labels/delivery_get_labels_html.xml");
        assertExecutionSuccess("fixtures/executors/get_labels/get_labels_task_response_without_parcel_id.json");
    }

    @Test
    public void executeSuccessPdfWithParcelId() throws Exception {
        ClientTask task = getClientTaskWithParcelId();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_labels/delivery_get_labels_with_parcel_id.xml",
            "fixtures/response/delivery/get_labels/delivery_get_labels_pdf.xml");
        assertExecutionSuccess("fixtures/executors/get_labels/get_labels_task_response_with_parcel_id.json");
    }

    @Test
    public void executeSuccessPdfWithoutParcelId() throws Exception {
        ClientTask task = getClientTaskWithoutParcelId();
        when(repository.findTask(eq(TEST_ORDER_ID))).thenReturn(task);

        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_labels/delivery_get_labels_without_parcel_id.xml",
            "fixtures/response/delivery/get_labels/delivery_get_labels_pdf.xml");
        assertExecutionSuccess("fixtures/executors/get_labels/get_labels_task_response_without_parcel_id.json");
    }

    private void assertExecutionSuccess(String successMessageBodyFileName) throws Exception {
        TaskMessage message = getLabelsExecutor.execute(new ExecutorTaskWrapper(TEST_ORDER_ID, 0));

        verify(amazonS3Client).putObject(eq(s3BucketName), anyString(),
            any(ByteArrayInputStream.class), refEq(new ObjectMetadata()));
        verify(amazonS3Client).getUrl(eq(s3BucketName), anyString());

        softAssert.assertThat(message.getMessageBody())
            .isEqualTo(getFileContent(successMessageBodyFileName));
    }

    private ClientTask getClientTaskWithParcelId() throws IOException {
        return getClientTask("fixtures/executors/get_labels/get_labels_task_message_with_parcel_id.json");
    }

    private ClientTask getClientTaskWithoutParcelId() throws IOException {
        return getClientTask("fixtures/executors/get_labels/get_labels_task_message_without_parcel_id.json");
    }

    private ClientTask getClientTask(String filename) throws IOException {
        ClientTask task = new ClientTask();
        task.setId(TEST_ORDER_ID);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setFlow(RequestFlow.DS_GET_LABELS);
        task.setMessage(getFileContent(filename));
        return task;
    }
}
