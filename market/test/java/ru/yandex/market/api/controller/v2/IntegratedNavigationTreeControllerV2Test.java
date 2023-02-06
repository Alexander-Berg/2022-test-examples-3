package ru.yandex.market.api.controller.v2;

import javax.inject.Inject;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.util.ApiStrings;
import ru.yandex.market.api.util.httpclient.clients.CatalogerTestClient;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * @author dimkarp93
 */
public class IntegratedNavigationTreeControllerV2Test extends BaseTest {
    @Inject
    private CatalogerTestClient catalogerTestClient;

    @Test
    public void treeWithCache() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "-3");

        int categoryId = 67450;

        catalogerTestClient.getPathByNid(categoryId, 225, "cataloger_navgiation_path.xml");
        catalogerTestClient.getTree(categoryId, 0, 225, "cataloger_navgiation_tree.xml");

        String url = baseUrl + "/v2.1.6/navigation/tree?category_id=" + categoryId;

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        String body = response.getBody();
        String header = response.getHeaders().get("ETag").get(0);

        String treeRaw = ApiStrings.valueOf(ResourceHelpers.getResource("result_navigation_tree.json"));

        Assert.assertEquals(200, response.getStatusCodeValue());
        JSONAssert.assertEquals(treeRaw, new JSONObject(body).getJSONObject("category"), JSONCompareMode.NON_EXTENSIBLE);

        Assert.assertEquals("\"2c3f3e493bcb5b2374f501399ad5b6ac;429\"", header);
    }

    @Test
    public void cachedTree() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "-3");
        headers.add("If-None-Match", "\"2c3f3e493bcb5b2374f501399ad5b6ac;429\"");

        int categoryId = 67450;

        catalogerTestClient.getPathByNid(categoryId, 225, "cataloger_navgiation_path.xml");
        catalogerTestClient.getTree(categoryId, 0, 225, "cataloger_navgiation_tree.xml");

        String url = baseUrl + "/v2.1.6/navigation/tree?category_id=" + categoryId;

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        String body = response.getBody();
        String header = response.getHeaders().get("ETag").get(0);

        Assert.assertEquals(304, response.getStatusCodeValue());
        Assert.assertNull(body);

        Assert.assertEquals("\"2c3f3e493bcb5b2374f501399ad5b6ac;429\"", header);
    }
}
