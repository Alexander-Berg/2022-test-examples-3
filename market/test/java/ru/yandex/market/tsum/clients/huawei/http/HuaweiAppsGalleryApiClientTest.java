package ru.yandex.market.tsum.clients.huawei.http;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonElement;
import org.apache.http.HttpException;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.StringBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.huawei.http.api.GetFileUploadingLinkRequest;
import ru.yandex.market.tsum.clients.huawei.http.api.SubmitApiRequest;
import ru.yandex.market.tsum.clients.huawei.http.api.UploadFileRequest;
import ru.yandex.market.tsum.clients.huawei.http.content.ContentBodyProducer;
import ru.yandex.market.tsum.clients.huawei.http.model.FileInfo;
import ru.yandex.market.tsum.clients.huawei.http.model.Suffix;
import ru.yandex.market.tsum.clients.huawei.http.model.UploadFileRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HuaweiAppsGalleryApiClientTest {

    @Mock
    private HttpClient httpClient;
    @InjectMocks
    private HuaweiAppsGalleryApiClient appsGalleryApiClient;

    @Test(expected = HttpException.class)
    public void serverErrorResponse() throws IOException, HttpException {
        when(httpClient.execute(any())).thenReturn(FakeResponseBuilder.create().status(500).emptyJsonContent().build());
        appsGalleryApiClient.executeRequest(new SubmitApiRequest("1", "1"));
    }

    @Test
    public void getUploadUrl() throws IOException, HttpException {
        when(httpClient.execute(any())).thenReturn(FakeResponseBuilder.create().status(200).content(
            "{\"ret\":{\"code\":0,\"msg\":\"success\"},\"uploadUrl\":\"https://developerfile7.hicloud" +
                ".com:443/FileServer/uploadFile\",\"chunkUploadUrl\":\"https://developerfile7.hicloud" +
                ".com:443/FileServer/chunkUploadFile\",\"authCode\":\"12345678\"}"
        ).build());
        UploadFileRef uploadFileRef = appsGalleryApiClient.executeRequest(new GetFileUploadingLinkRequest("1",
            Suffix.APK));
        assertEquals("12345678", uploadFileRef.getAuthCode());
        assertEquals("https://developerfile7.hicloud.com:443/FileServer/uploadFile", uploadFileRef.getUploadUrl());
    }

    @Test
    public void uploadFile() throws IOException, HttpException {
        final String uploadedFileLink = "https://developerfile7.hicloud" +
            ".com/FileServer/getFile/7/appAttachtemp/20200617/appAttach/000/000/024/0890041000000000024" +
            ".20200617180635.94925804169138074926621096448727:20200617180638:2500" +
            ":06318D7F5CF786D38A420BECC79DF50E89E10AC9B21D59F665A01FAC633C8F88.jpeg";

        UploadFileRef uploadFileResp = new UploadFileRef();
        uploadFileResp.setAuthCode("1234");
        uploadFileResp.setUploadUrl("https://wqe.asd/qwe");
        HuaweiAppsGalleryApiClient apiClientMock = mock(HuaweiAppsGalleryApiClient.class);
        when(apiClientMock.executeRequest(any(GetFileUploadingLinkRequest.class))).thenReturn(uploadFileResp);

        ContentBodyProducer producer = () -> new StringBody("fake", ContentType.TEXT_PLAIN);
        when(httpClient.execute(any())).thenReturn(FakeResponseBuilder.create()
            .status(200)
            .content("{\"result\":{\"UploadFileRsp\":{\"fileInfoList\":[{\"fileDestUlr\":\"" + uploadedFileLink +
                "\",\"imageResolution\":\"1200*900\"," +
                "\"imageResolutionSingature\":\"u/BNwX/ID+IvYecIxN8LMqrNizjiNystBwASSSA0VG/P3xnwpIqAuGa6" +
                "+uEa4awsXQFdxRpZ2rL1CtOy/j6EU7tiV4MRAULj/smJXtJ6MgecCLPe9vcj2Zd4II7hows3eezHI" +
                "+V8vsfKJjXlVWYWHpWg8UzuS3ZjFP91nwi3HjVhwat9Ur1IGjUsWii/3vg6ovvm9yAEFFkrt3xxZQ/82tmDDqOA8Bxmv" +
                "+gx6DwyfT5YcmmAprLQMNpNH/P8BZtuCkQlX8PE3anoxmhRxv3TWICQ9IKKscMPncQwLJPC5teFT/ybOpsUM0jrtJ" +
                "/fcZt9CiL48U6iH6k8u8Y623P+dg==\",\"size\":388857}],\"ifSuccess\":1},\"resultCode\":\"0\"}}")
            .build());

        List<FileInfo> fileInfos = appsGalleryApiClient.executeRequest(new UploadFileRequest("1", producer,
            Suffix.APK, apiClientMock));
        assertEquals(1, fileInfos.size());
        FileInfo fileInfo = fileInfos.get(0);
        assertEquals(uploadedFileLink, fileInfo.getFileDestUlr());
        assertEquals(388857, fileInfo.getSize());
        assertEquals("1200*900", fileInfo.getImageResolution());
    }

    @Test
    public void submit() throws IOException, HttpException {
        when(httpClient.execute(any())).thenReturn(FakeResponseBuilder.create().status(200).emptyJsonContent().build());
        JsonElement resp = appsGalleryApiClient.executeRequest(new SubmitApiRequest("1", "1"));
        assertNotNull(resp);
    }
}
