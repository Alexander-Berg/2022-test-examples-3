package ru.yandex.chemodan.uploader.office;

import java.io.ByteArrayInputStream;

import com.xebialabs.restito.server.StubServer;
import org.glassfish.grizzly.http.Method;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.mpfs.MpfsClient;
import ru.yandex.chemodan.uploader.config.UploaderCoreContextConfigurationForTests;
import ru.yandex.chemodan.uploader.registry.ApiVersion;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.chemodan.util.test.StubServerUtils;
import ru.yandex.commune.uploader.local.queue.LocalQueueProcessor;
import ru.yandex.commune.uploader.local.queue.LocalRequestQueue;
import ru.yandex.commune.uploader.registry.UploadRegistry;
import ru.yandex.commune.uploader.registry.UploadRequestId;
import ru.yandex.commune.uploader.registry.UploadRequestStatus;
import ru.yandex.commune.uploader.util.http.HttpContentRange;
import ru.yandex.commune.uploader.web.data.HttpPutRequestContext;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.io.InputStreamX;
import ru.yandex.misc.io.http.HttpStatus;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadUtils;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.bytesContent;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.ok;
import static com.xebialabs.restito.semantics.Condition.method;

/**
 * @author akirakozov
 */
@ContextConfiguration(classes = {
        ChemodanInitContextConfiguration.class,
        UploaderCoreContextConfigurationForTests.class})
public class OfficeUploadManagerTest extends AbstractTest {

    @Autowired
    private MpfsClient mpfsClient;
    @Autowired
    private OfficeUploadManager officeUploadManager;
    @Autowired
    protected LocalQueueProcessor localQueueProcessor;
    @Autowired
    protected LocalRequestQueue localRequestQueue;
    @Autowired
    private UploadRegistry<MpfsRequestRecord> uploadRegistry;
    private static final int PORT = 54321;

    @Before
    public void before() {
        localRequestQueue.setEnabled(true);
        localRequestQueue.start();
        localQueueProcessor.setEnabled(true);
        localQueueProcessor.start();
    }

    @After
    public void after() {
        localQueueProcessor.stop();
        localRequestQueue.stop();
    }

    @Test
    public void startUpload() throws Exception {
        mpfsClient.setMpfsHost("http://localhost:" + PORT);
        StubServerUtils.withStubServer(PORT, stubServer -> uploadDocFromOffice(stubServer, PORT));
    }

    @Test
    public void startUploadHardLinkedFile() throws Exception {
        mpfsClient.setMpfsHost("http://localhost:" + PORT);
        StubServerUtils.withStubServer(PORT, stubServer -> uploadLockedFile(stubServer, PORT));
    }

    private void uploadDocFromOffice(StubServer stubServer, int port) {
        MpfsInitialStoreInfo info = new MpfsInitialStoreInfo(
                123, "fileid:123", "123:/disk/test.txt", "https://localhost:" + port,
                DataSize.fromGigaBytes(1).toBytes(), "disk", ApiVersion.V_0_2.toSerializedString()
            );
        MpfsOfficeStoreResponseInfo response = new MpfsOfficeStoreResponseInfo(
                Option.of(info), "", HttpStatus.SC_200_OK, Cf.map("X-WOPI-Test", "val1"));

        OfficeUploadResult result = mockServerAndStartUpload(stubServer, response);
        Assert.equals(HttpStatus.SC_200_OK, result.statusCode);
        Assert.equals("val1", result.headers.get("X-WOPI-Test"));

        checkSuccess(result.uploadRequestId.get());
    }

    private void uploadLockedFile(StubServer stubServer, int port) {
        MpfsOfficeStoreResponseInfo response = new MpfsOfficeStoreResponseInfo(
                Option.empty(), "", HttpStatus.SC_409_CONFLICT, Cf.map("X-WOPI-Test", "LOCK_ID"));

        OfficeUploadResult result = mockServerAndStartUpload(stubServer, response);
        Assert.equals(HttpStatus.SC_409_CONFLICT, result.statusCode);
        Assert.equals(Cf.map("X-WOPI-Test", "LOCK_ID"), result.headers);
    }

    private OfficeUploadResult mockServerAndStartUpload(StubServer stubServer, MpfsOfficeStoreResponseInfo response) {
        byte[] serializedInfo = MpfsOfficeStoreResponseInfo.PS.getSerializer().serializeJson(response);
        whenHttp(stubServer)
                .match(method(Method.POST))
                .then(ok(), bytesContent(serializedInfo), contentType("application/json"));

        whenHttp(stubServer).match(method(Method.GET)).then(ok());

        return officeUploadManager.startUpload(
                "id", "token", "ttl", Cf.map(), Option.empty(), createPutRequestContext(), Option.empty());
    }

    private void checkSuccess(UploadRequestId id) {
        while (true) {
            MpfsRequestRecord r = uploadRegistry.findRecord(id).get();
            if (r.getStatus().isFinishedWithFinalStages()) {
                Assert.equals(UploadRequestStatus.Result.COMPLETED, r.getStatus().resultWithFinalStages());
                break;
            }
            ThreadUtils.sleep(Duration.millis(50));
        }
    }

    private HttpPutRequestContext createPutRequestContext() {
        return new HttpPutRequestContext() {
            private final byte[] content = "some text".getBytes();

            @Override
            public Option<Long> getContentLength() {
                return Option.of((long) content.length);
            }

            @Override
            public Option<String> getContentType() {
                return Option.of("text/plain");
            }

            @Override
            public Option<String> getContentEncoding() {
                return Option.empty();
            }

            @Override
            public Option<HttpContentRange> getContentRange() {
                return Option.empty();
            }

            @Override
            public InputStreamX getInputStream() {
                return new InputStreamX(new ByteArrayInputStream(content));
            }
        };
    }

}
