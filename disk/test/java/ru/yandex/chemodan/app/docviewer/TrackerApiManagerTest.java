package ru.yandex.chemodan.app.docviewer;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.chemodan.app.docviewer.copy.DocumentSourceInfo;
import ru.yandex.chemodan.app.docviewer.copy.provider.TrackerUrlProvider;
import ru.yandex.chemodan.app.docviewer.copy.resourcemanagers.TrackerApiManager;
import ru.yandex.chemodan.app.docviewer.copy.resourcemanagers.TrackerApiResponseHandler;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.inside.passport.tvm2.UserTicketHolder;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class TrackerApiManagerTest {
    @Test
    public void requestTest() throws IOException {
        String originalUrl = "tracker://0/100";
        PassportUidOrZero uid = TestUser.YA_TEAM_AKIRAKOZOV.uid;
        String userTvmTicket = "pretendsToExist";

        String storageUrl = "s83vla.storage.yandex.net/get-tools_st/35400/prod_attach_100_...";

        TrackerUrlProvider provider = new TrackerUrlProvider("st-api.test.yandex-team.ru");
        URI rewrittenInnerUri = URI.create(provider.rewriteUrl(
                DocumentSourceInfo.builder().originalUrl(originalUrl).uid(uid).build()));

        HttpClient httpClient = Mockito.mock(HttpClient.class);

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);

        Mockito.when(httpClient.execute(requestCaptor.capture(), Mockito.any(), Mockito.any()))
                .thenReturn(URI.create(storageUrl));

        TrackerApiManager manager = new TrackerApiManager(httpClient);

        // without tvm ticket
        Assert.isFalse(manager.isUriAccessible(rewrittenInnerUri));

        UserTicketHolder.withUserTicket(userTvmTicket, () -> manager.isUriAccessible(rewrittenInnerUri));

        HttpUriRequest request = requestCaptor.getValue();

        Assert.equals("0", request.getFirstHeader("X-Org-Id").getValue());
        Assert.equals("https://st-api.test.yandex-team.ru/v2/attachments/100/storageLink", request.getURI().toString());
    }

    @Test
    public void handleResponse() throws IOException {
        TrackerApiResponseHandler handler = new TrackerApiResponseHandler();
        HttpResponse response = Mockito.mock(HttpResponse.class);
        HttpEntity entity = Mockito.mock(HttpEntity.class);
        String responseContent = "\"//s83vla.storage.yandex.net/get-tools_st/35400/prod_attach_100_...\"";

        Mockito.when(entity.getContent()).thenReturn(IOUtils.toInputStream(responseContent, "utf-8"));

        Mockito.when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("", 0, 0), 200, ""));
        Mockito.when(response.getEntity()).thenReturn(entity);

        URI resolverUri = handler.handleResponse(response);

        Assert.equals("s83vla.storage.yandex.net/get-tools_st/35400/prod_attach_100_...", resolverUri.toString());
    }
}
