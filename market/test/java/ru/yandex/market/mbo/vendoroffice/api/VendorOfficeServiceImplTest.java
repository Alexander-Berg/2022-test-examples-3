package ru.yandex.market.mbo.vendoroffice.api;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.utils.http.SimpleHttpClientStub;

/**
 * @author ayratgdl
 * @since 12/12/2018
 */
public class VendorOfficeServiceImplTest {
    private static final String TEST_HOST = "http://example.org:80";
    private static final long UID = 1;
    private static final long VENDOR_ID = 101;

    private VendorOfficeServiceImpl vendorOfficeService;
    private SimpleHttpClientStub httpClient;

    @Before
    public void setUp() throws Exception {
        vendorOfficeService = new VendorOfficeServiceImpl();
        vendorOfficeService.setHost(TEST_HOST);
        vendorOfficeService.setUser(new AutoUser(UID));

        httpClient = new SimpleHttpClientStub();
        vendorOfficeService.setHttpClient(httpClient);
    }

    @Test(expected = RuntimeException.class)
    public void getVendorThrowExceptionIfHttpClientThrowException() {
        vendorOfficeService.getVendor(VENDOR_ID);
    }

    @Test
    public void getVendorReturnNullIfVendorOfficeDoesNotKnowVendor() {
        String emptyVendorsResponse = "{\"errors\":[],\"meta\":{},\"result\":{\"item\":{\"pager\":{},\"items\":[]}}}";
        httpClient.setResponseOnGet(emptyVendorsResponse);

        Assert.assertNull(vendorOfficeService.getVendor(VENDOR_ID));
    }

    /**
     * ручка https://github.yandex-team.ru/market/vendors-api-spec/tree/master/vendors#get-vendors.
     * может вернуть результ не относящийся к запрашиваемому вендору, поэтому vendorOfficeService.getVendor
     * фильтрует ответ от ручки
     */
    @Test
    public void getVendorReturnNullIfVendorOfficeDoesNotKnowVendorButContainsSimilar() {
        String similarVendorResponse = "{\"errors\":[],\"meta\":{},\"result\":{\"item\":{\"pager\":{},\"items\":[" +
            "{\"id\": 1, \"brand\": {\"id\": 102, \"name\": \"brand\"}}" +
            "]}}}";
        httpClient.setResponseOnGet(similarVendorResponse);

        Assert.assertNull(vendorOfficeService.getVendor(VENDOR_ID));
    }

    @Test
    public void getVendorReturnVendorIfVendorOfficeKnowsVendor() {
        String vendorResponse = "{\"errors\":[],\"meta\":{},\"result\":{\"item\":{\"pager\":{},\"items\":[" +
            "{\"id\": 1, \"brand\": {\"id\": " + VENDOR_ID + ", \"name\": \"brand\"}}" +
            "]}}}";
        httpClient.setResponseOnGet(vendorResponse);

        Brand expectedBrand = new Brand().setId(VENDOR_ID).setName("brand");
        Assert.assertEquals(expectedBrand, vendorOfficeService.getVendor(VENDOR_ID));
    }

    @Test
    public void getVendorWithSuccessProcessResponseWithAbsentBrand() {
        String absentVendorResponse = "{\"errors\":[],\"meta\":{},\"result\":{\"item\":{\"pager\":{},\"items\":[" +
            "{\"id\": 1}" +
            "]}}}";
        httpClient.setResponseOnGet(absentVendorResponse);

        Assert.assertNull(vendorOfficeService.getVendor(VENDOR_ID));
    }
}
