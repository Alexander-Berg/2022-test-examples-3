package ru.yandex.direct.web.entity.feedback.service;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.crm.mail.model.MailProtobuf.MailRequest;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.web.core.model.WebSuccessResponse;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class CommanderFeedbackServiceTest {

    @Mock
    private LogbrokerClientFactory logbrokerClientFactory;
    @Mock
    private TvmIntegration tvmIntegration;
    @Mock
    private AmazonS3 amazonS3;
    @Mock
    private AsyncProducer producer;

    private final Map<String, byte[]> mds = new HashMap<>();
    private byte[] logbrokerTransfer;

    private CommanderFeedbackService service;

    @Before
    public void setUp() throws InterruptedException {
        openMocks(this);

        var config = new CrmTransportConfig(
                "host",
                "topic",
                "logbroker-production",
                "source_id",
                "bucket"
        );

        when(tvmIntegration.getTicket(any())).thenReturn("tvm_ticket");

        when(logbrokerClientFactory.asyncProducer(any())).thenReturn(producer);

        when(producer.init()).thenReturn(CompletableFuture.completedFuture(
                new ProducerInitResponse(0, config.getLogbrokerTopic(), 1, "session_id")));

        when(producer.write(any())).then(
                invocation -> {
                    var bytes = invocation.getArgument(0, byte[].class);
                    logbrokerTransfer = bytes.clone();
                    return CompletableFuture.completedFuture(new ProducerWriteResponse(0, 0, true));
                }
        );

        when(amazonS3.putObject(anyString(), anyString(), any(), any())).then(
                invocation -> {
                    var name = invocation.getArgument(1, String.class);
                    var is = invocation.getArgument(2, ByteArrayInputStream.class);
                    mds.put(name, is.readAllBytes());
                    return new PutObjectResult();
                }
        );
        when(amazonS3.getUrl(anyString(), anyString())).then(
                invocation -> new URL(getUrlPath(invocation.getArgument(1, String.class)))
        );

        service = new CommanderFeedbackService(tvmIntegration, config, amazonS3, logbrokerClientFactory);
    }

    @Test
    public void testSendMessage() throws InvalidProtocolBufferException {
        var testLogin = "test_login";
        var testEmail = "test_email";
        var testText = "simple text";
        var testScreenshotData = new byte[]{1, 2, 3, 4};
        var testLogData = new byte[]{4, 3, 2, 1};

        var response = service.sendMessage(testLogin, testEmail, testText,
                testScreenshotData, testLogData);
        assertThat(response, instanceOf(WebSuccessResponse.class));

        MailRequest request = MailRequest.parseFrom(logbrokerTransfer);
        assertThat(request.getUser().getLogin(), equalTo(testLogin));
        assertThat(request.getMail().getFrom().getEmail(), equalTo(testEmail));
        assertThat(request.getMail().getText(), equalTo(testText));

        assertThat(mds.values(), containsInAnyOrder(testScreenshotData, testLogData));
    }

    private String getUrlPath(String name) {
        return "http://path/to/" + name;
    }
}
