package ru.yandex.chemodan.uploader.services;

import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class OnlyOfficeRequestTest {
    private static final ServiceFileId OLD_SERVICE_FILE_ID =
            ServiceFileId.valueOf("12345:files/heehehehe/output.xlsx?AWSAccessKeyId=key&ooname=output.xlsx");

    private static final ServiceFileId ENDPOINT_SERVICE_FILE_ID =
            ServiceFileId.valueOf("12345:endpoint_host=jbvei7dnsaq7hnyo.man.yp-c.yandex.net:808;" +
                    "endpoint_sign=POPYHcOTVVgq5hrqKIJrcA;" +
                    "request=files/heehehehe/output.xlsx?AWSAccessKeyId=key&ooname=output.xlsx");

    private static final ServiceFileId SUBDOMAIN_SERVICE_FILE_ID =
            ServiceFileId.valueOf("12345:subdomain=me2cckwfj5jtovfe_man_809_718987b91c2acfa09a2beed24d08e5c6;" +
                    "request=files/heehehehe/output.xlsx?AWSAccessKeyId=key&ooname=output.xlsx");

    @Test
    public void parseOldStyleServiceId() {
        Assert.isEmpty(OnlyOfficeRequest.fromServiceFileId(OLD_SERVICE_FILE_ID));
    }

    @Test
    public void parseServiceIdWithEndpoint() {
        OnlyOfficeRequest expected = new OnlyOfficeRequest.RequestWithCookie(
                "jbvei7dnsaq7hnyo.man.yp-c.yandex.net:808",
                "POPYHcOTVVgq5hrqKIJrcA",
                "files/heehehehe/output.xlsx?AWSAccessKeyId=key&ooname=output.xlsx");

        Assert.some(expected, OnlyOfficeRequest.fromServiceFileId(ENDPOINT_SERVICE_FILE_ID));
    }

    @Test
    public void parseServiceIdWithSubdomain() {
        OnlyOfficeRequest expected = new OnlyOfficeRequest.RequestWithSubdomain(
                "me2cckwfj5jtovfe_man_809_718987b91c2acfa09a2beed24d08e5c6",
                "files/heehehehe/output.xlsx?AWSAccessKeyId=key&ooname=output.xlsx");

        Assert.some(expected, OnlyOfficeRequest.fromServiceFileId(SUBDOMAIN_SERVICE_FILE_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingParamServiceId() {
        OnlyOfficeRequest.fromServiceFileId(ServiceFileId.valueOf("12345:endpoint_host=a;request=b"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void malformedParamServiceId() {
        OnlyOfficeRequest.fromServiceFileId(ServiceFileId.valueOf("12345:endpoint_host=a;unexpected=b;request=c"));
    }

    @Test
    public void createEndpointRequest() {
        HttpUriRequest request = OnlyOfficeRequest.fromServiceFileId(ENDPOINT_SERVICE_FILE_ID).get()
                .toHttpRequest("https://onlyoffice.dst.yandex.net");

        String expectedCookie =
                "endpoint_host=jbvei7dnsaq7hnyo.man.yp-c.yandex.net:808; endpoint_sign=POPYHcOTVVgq5hrqKIJrcA";
        Assert.equals(expectedCookie, request.getFirstHeader("Cookie").getValue());

        String expectedUri =
                "https://onlyoffice.dst.yandex.net/files/heehehehe/output.xlsx?AWSAccessKeyId=key&ooname=output.xlsx";
        Assert.equals(expectedUri, request.getURI().toString());

        Assert.equals("GET", request.getMethod());
    }

    @Test
    public void createSubdomainRequest() {
        HttpUriRequest request = OnlyOfficeRequest.fromServiceFileId(SUBDOMAIN_SERVICE_FILE_ID).get()
                .toHttpRequest("https://onlyoffice.dst.yandex.net");

        String expectedUri =
                "https://me2cckwfj5jtovfe_man_809_718987b91c2acfa09a2beed24d08e5c6.onlyoffice.dst.yandex.net" +
                        "/files/heehehehe/output.xlsx?AWSAccessKeyId=key&ooname=output.xlsx";
        Assert.equals(expectedUri, request.getURI().toString());

        Assert.equals("GET", request.getMethod());
    }
}
