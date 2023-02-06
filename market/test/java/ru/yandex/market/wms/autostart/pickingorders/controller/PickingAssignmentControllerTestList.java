package ru.yandex.market.wms.autostart.pickingorders.controller;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.shared.libs.utils.JsonUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/pickingOrders/1/taskdetail.xml", connection = "wmwhseConnection"),
})
abstract class PickingAssignmentControllerTestList extends AutostartIntegrationTest
        implements PickingAssignmentControllerTestData {

    String endPoint() {
        return "/picking/assignments/list";
    }

    abstract String uriTemplateL();


    URI limit(int limit) {
        return uri(uriTemplateL(), limit);
    }

    // C+P from Spring
    URI uri(String url, Object... vars) {
        Assert.notNull(url, "'url' must not be null");
        Assert.isTrue(url.startsWith("/") || url.startsWith("http://") || url.startsWith("https://"), "" +
                "'url' should start with a path or be a complete HTTP URL: " + url);
        return UriComponentsBuilder.fromUriString(url).buildAndExpand(vars).encode().toUri();
    }

    void expectFor(URI uri, String json) throws Exception {
        mockMvc.perform(get(uri))
                .andExpect(status().isOk())
                .andExpect(content().json(json, true));
    }


    String json(String cursor, List<String> itemJsons) {
        return "{"
                + "items:["
                + String.join(",", itemJsons)
                + "],"
                + "cursor:" + JsonUtil.writeValueAsString(cursor)
                + "}";
    }

    String json(List<String> itemJsons) {
        return "{"
                + "items:["
                + String.join(",", itemJsons)
                + "]"
                + "}";
    }

    List<String> i(String... itemJsons) {
        return Arrays.asList(itemJsons);
    }

    String cursor(String key, String value) {
        return "{\"" + key + "\":\"" + value + "\"}";
    }

    String cursor(String key1, String value1, String key2, String value2) {
        return "{\"" + key1 + "\":\"" + value1 + "\",\"" + key2 + "\":\"" + value2 + "\"}";
    }
}
