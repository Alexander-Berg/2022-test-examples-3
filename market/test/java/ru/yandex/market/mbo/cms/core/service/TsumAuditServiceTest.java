package ru.yandex.market.mbo.cms.core.service;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.models.PageInfo;
import ru.yandex.market.mbo.cms.core.service.user.UsersManager;

public class TsumAuditServiceTest {
    private static final String NAMESPACE = "namespace";
    private static final String PAGE_NAME = "name";
    private static final int PUBLISHED_REVISION_ID = 11;
    private static final int LATEST_REVISION_ID = 22;
    private static final int PUBLISH_TIMESTAMP_SECONDS = 12345;
    private static final int SAVE_TIMESTAMP_SECONDS = 54321;
    private static final String LOGIN = "login";
    private static final int STATUS_CODE_OK = 200;
    private JsonParser parser = new JsonParser();
    private TsumAuditService tsumAuditService;
    private HttpClient httpClient;

    @Before
    public void init() throws IOException {
        UsersManager usersManager = Mockito.mock(UsersManager.class);
        Mockito.when(usersManager.getUserStaffLogin(Mockito.anyLong())).thenReturn(LOGIN);
        TsumAuditService service = new TsumAuditService(usersManager);
        httpClient = Mockito.mock(HttpClient.class);
        tsumAuditService = Mockito.spy(service);
        Mockito.when(tsumAuditService.getHttpClient()).thenReturn(httpClient);
        Mockito.when(httpClient.execute(Mockito.any())).thenReturn(
                new BasicHttpResponse(
                        new BasicStatusLine(new ProtocolVersion("", 0, 0), STATUS_CODE_OK, "")));
    }

    @Test
    public void registerPublish() throws IOException {
        tsumAuditService.registerPublish(getSamplePage());
        awaitTaskComplete();
        ArgumentCaptor<HttpPost> requestCaptor = ArgumentCaptor.forClass(HttpPost.class);
        Mockito.verify(httpClient, Mockito.times(1)).execute(requestCaptor.capture());
        String json = IOUtils.toString(requestCaptor.getValue().getEntity().getContent(), "UTF-8");
        JsonObject requestJsonObject = parser.parse(json).getAsJsonObject();

        checkRequestJsonCommonFields(requestJsonObject);
        Assert.assertEquals(PUBLISH_TIMESTAMP_SECONDS, requestJsonObject.get("startTimeSeconds").getAsLong());
        Assert.assertEquals(PUBLISH_TIMESTAMP_SECONDS, requestJsonObject.get("endTimeSeconds").getAsLong());
        Assert.assertEquals("page_publish", requestJsonObject.get("type").getAsString());
        Assert.assertEquals("Публикация страницы 1 'name' (ревизия 11)", requestJsonObject.get("title").getAsString());
    }

    @Test
    public void registerSave() throws IOException {
        tsumAuditService.registerSave(getSamplePage());
        awaitTaskComplete();
        ArgumentCaptor<HttpPost> requestCaptor = ArgumentCaptor.forClass(HttpPost.class);
        Mockito.verify(httpClient, Mockito.times(1)).execute(requestCaptor.capture());
        String json = IOUtils.toString(requestCaptor.getValue().getEntity().getContent(), "UTF-8");
        JsonObject requestJsonObject = parser.parse(json).getAsJsonObject();

        checkRequestJsonCommonFields(requestJsonObject);
        Assert.assertEquals(SAVE_TIMESTAMP_SECONDS, requestJsonObject.get("startTimeSeconds").getAsLong());
        Assert.assertEquals(SAVE_TIMESTAMP_SECONDS, requestJsonObject.get("endTimeSeconds").getAsLong());
        Assert.assertEquals("page_save", requestJsonObject.get("type").getAsString());
        Assert.assertEquals("Сохранение страницы 1 'name' (ревизия 22)", requestJsonObject.get("title").getAsString());
    }

    private void awaitTaskComplete() {
        tsumAuditService.getAsyncExecutor().shutdown();
        try {
            tsumAuditService.getAsyncExecutor().awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private PageInfo getSamplePage() {
        return PageInfo.PageInfoBuilder.aPageInfo()
                .withSchemaId(0)
                .withSchemaRevisionId(1)
                .withNamespace(NAMESPACE)
                .withId(1)
                .withName(PAGE_NAME)
                .withPublishedRevisionId(PUBLISHED_REVISION_ID)
                .withLatestRevisionId(LATEST_REVISION_ID)
                .withRevisionId(LATEST_REVISION_ID)
                .withPublished(new Date(PUBLISH_TIMESTAMP_SECONDS * TimeUnit.SECONDS.toMillis(1)))
                .withUpdatedTime(new Date(SAVE_TIMESTAMP_SECONDS * TimeUnit.SECONDS.toMillis(1)))
                .build();
    }

    private void checkRequestJsonCommonFields(JsonObject object) {
        Assert.assertEquals(object.get("project").getAsString(), "market_cms");
        Assert.assertEquals(object.get("status").getAsString(), "INFO");
        Assert.assertEquals(object.get("tags").getAsJsonArray().size(), 1);
        Assert.assertEquals(object.get("tags").getAsJsonArray().get(0).getAsString(), NAMESPACE);
        Assert.assertEquals(object.get("author").getAsString(), LOGIN);
    }
}
