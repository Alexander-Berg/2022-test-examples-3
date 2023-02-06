package ru.yandex.vendor.cataloger;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;
import ru.yandex.vendor.util.IRestClient;
import ru.yandex.vendor.util.RestTemplateRestClient;

import java.net.URI;
import java.util.List;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static ru.yandex.vendor.cataloger.CatalogerClient.*;
import static ru.yandex.vendor.util.IRestClient.request;

public class CatalogerClientTest {

    private static final String CATALOGER_URL = "http://www.cataloger.ru";

    private final IRestClient restClientMock = createRestClientMock();
    private final ICatalogerClient catalogerClient = createCatalogerClient(restClientMock);

    @Test
    public void get_brand_categories_method_exists() throws Exception {
        long brandId = 42;
        List<BrandCategoryEntry> brandCategories = catalogerClient.getBrandCategories(brandId);
        Assert.assertEquals(emptyList(), brandCategories);
    }

    @Test
    public void parse_categories() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<data version=\"2017.11.02\" servant=\"marketcataloger\" hostname=\"kgb02ht\">\n" +
                "  <vendor id=\"3732937\" name=\"LEGO\" picture=\"//avatars.mds.yandex.net/get-mpic/96484/img_id5835532366437487578/orig\" site=\"http://www.lego.com\">\n" +
                "    <category hid=\"90666\" name=\"Товары для дома и дачи\">\n" +
                "      <category hid=\"6858918\" name=\"Копилки\" nid=\"55739\" popularity=\"25\"/>\n" +
                "      <category hid=\"90712\" name=\"Ночники\" nid=\"55154\" popularity=\"14\"/>\n" +
                "    </category>\n" +
                "    <category hid=\"90764\" name=\"Детские товары\">\n" +
                "      <category hid=\"966823\" name=\"Аксессуары и запчасти для машинок и техники\" nid=\"55192\" popularity=\"7\"/>\n" +
                "      <category hid=\"13858284\" name=\"Блокноты\" nid=\"67102\" popularity=\"11\"/>\n" +
                "    </category>\n" +
                "  </vendor>\n" +
                "</data>";
        List<BrandCategoryEntry> entries = CatalogerClient.parseBrandCategories(xml);
        assertEquals(
            asList(
                new BrandCategoryEntry(3732937, 6858918, 25),
                new BrandCategoryEntry(3732937, 90712, 14),
                new BrandCategoryEntry(3732937, 966823, 7),
                new BrandCategoryEntry(3732937, 13858284, 11)
            ),
            entries);
    }

    @Test
    public void get_categories_calls_cataloger() throws Exception {
        when(restClientMock.getForObject(any())).thenReturn(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<data>\n" +
                "  <vendor id=\"42\">\n" +
            "      <category hid=\"1\" name=\"Копилки\" nid=\"55739\" popularity=\"5\"/>\n" +
            "      <category hid=\"2\" name=\"Ночники\" nid=\"55154\" popularity=\"4\"/>\n" +
                "    <category hid=\"3\" name=\"Детские товары\">\n" +
                "      <category hid=\"4\" name=\"Аксессуары и запчасти для машинок и техники\" nid=\"55192\" popularity=\"2\"/>\n" +
                "      <category hid=\"5\" name=\"Блокноты\" nid=\"67102\" popularity=\"1\"/>\n" +
                "    </category>\n" +
                "  </vendor>\n" +
                "</data>"
        );
        List<BrandCategoryEntry> brandCategories = catalogerClient.getBrandCategories(42);
        verify(restClientMock).getForObject(
                request(CATALOGER_GET_VENDOR_CATEGORIES, String.class).addParameter("id", "42"));
        assertThat(brandCategories, hasSize(4));
        assertEquals(asList(
                new BrandCategoryEntry(42, 1, 5),
                new BrandCategoryEntry(42, 2, 4),
                new BrandCategoryEntry(42, 4, 2),
                new BrandCategoryEntry(42, 5, 1)
        ), brandCategories);
    }

    private static CatalogerClient createCatalogerClient(IRestClient restClient) {
        CatalogerClient catalogerClient = new CatalogerClient();
        catalogerClient.setRestClient(restClient);
        return catalogerClient;
    }

    private static IRestClient createRestClientMock() {
        IRestClient mock = mock(IRestClient.class);
        when(mock.getForObject(any())).thenReturn("<data/>");
        return mock;
    }
}