package ru.yandex.market.cataloger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import org.apache.http.client.HttpClient;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.cataloger.model.CatalogerResponseWrapper;
import ru.yandex.market.cataloger.model.GetBrandResponseWrapper;
import ru.yandex.market.cataloger.model.GetTreeResponseWrapper;
import ru.yandex.market.cataloger.model.NavigationNode;
import ru.yandex.market.cataloger.model.RootNavnode;
import ru.yandex.market.cataloger.model.VersionInfoWrapper;
import ru.yandex.market.mock.HttpResponseMockFactory;

import static org.mockito.Mockito.mock;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.06.2019
 */
public class CatalogerClientTest {
    private final HttpClient httpClient = mock(HttpClient.class);
    private final CatalogerClient client = new CatalogerClient(
            "http://target:90",
            new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)));

    @Test
    public void getBrandTest() throws IOException {
        HttpResponseMockFactory.mockResponse(httpClient, getTestData("cataloger/cataloger_brand_info.json"), 200);

        GetBrandResponseWrapper brandResponse = client.getBrand(123).orElse(null);
        Assert.assertNotNull(brandResponse);
        Assert.assertEquals("Samsung", brandResponse.getResult().getName());
        Assert.assertEquals(153061, brandResponse.getResult().getId());
    }

    @Test
    public void getBrandNameTest() throws IOException {
        HttpResponseMockFactory.mockResponse(httpClient, getTestData("cataloger/cataloger_brand_info.json"), 200);

        String name = client.getBrandName(123).orElse(null);
        Assert.assertNotNull(name);
        Assert.assertEquals("Samsung", name);
    }

    @Test
    public void getCatalogerVersionTest() throws IOException {
        HttpResponseMockFactory.mockResponse(httpClient, getTestData("cataloger/cataloger_stat.json"), 200);

        VersionInfoWrapper versionInfoWrapper = client.getCatalogerVersion().orElse(null);
        Assert.assertNotNull(versionInfoWrapper);
        Assert.assertEquals("20171005_1213", versionInfoWrapper.getVersionInfo().getVersion());
    }

    @Test
    public void getNavigationTreeTest() throws IOException {
        HttpResponseMockFactory.mockResponse(httpClient, getTestData("cataloger/cataloger_navigation_tree.json"), 200);

        CatalogerResponseWrapper catalogerResponseWrapper = client.getNavigationTreeFromDepartment().orElse(null);
        Assert.assertNotNull(catalogerResponseWrapper);
        List<NavigationNode> nodes = catalogerResponseWrapper.getCatalogerResponse().getNavigationNodes();
        Assert.assertEquals(1, nodes.size());
        List<NavigationNode> navigationNodesForElectronics = nodes.get(0).getNavigationNodes();
        Assert.assertEquals(1, navigationNodesForElectronics.size());
        Assert.assertTrue(navigationNodesForElectronics.get(0).getNavigationNodes().stream()
                .map(NavigationNode::getRootNavnode)
                .flatMap(rootNavnode -> rootNavnode.map(Stream::of).orElseGet(Stream::empty))
                .map(RootNavnode::getNid)
                .allMatch(nid -> nid == 54440L));
    }

    @Test
    public void getTreeTest() throws IOException {
        HttpResponseMockFactory.mockResponse(httpClient, getTestData("cataloger/cataloger_get_tree.json"), 200);

        GetTreeResponseWrapper responseWrapper = client.getTree(12345, 2).orElse(null);

        Assert.assertNotNull(responseWrapper);
        Assert.assertNotNull(responseWrapper.getResult());
        Assert.assertEquals("Для малышей", responseWrapper.getResult().getName());
        Assert.assertEquals("Одежда для малышей", responseWrapper.getResult().getFullName());
        Assert.assertEquals(17, responseWrapper.getResult().getCategories().size());
        Assert.assertEquals("Брюки и шорты для малышей",
                responseWrapper.getResult().getCategories().get(2).getFullName());
    }

    private InputStream getTestData(String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }

}
