package ru.yandex.market.mbo.cms.api.servlets.bean;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.common.util.http.HttpClientFactoryImpl;
import ru.yandex.market.mbo.cms.core.utils.http.response.ApiResponse;

/**
 * @author gilmulla
 */
public class RequestHelper {
    private static final int HTTP_CLIENT_SOCKET_TIMEOUT_MS = 5 * 60 * 1000;

    private HttpClient httpClient = HttpClientFactoryImpl.getInstance()
            .createHttpClient(HTTP_CLIENT_SOCKET_TIMEOUT_MS);
    private RequestConfig config = RequestConfig.custom().build();

    private static final Gson GSON = new Gson();
    private JsonParser gsonParser = new JsonParser();

    @Value("${http.port}")
    private String port;

    @SuppressWarnings("unchecked")
    public <T> ApiResponse<T> request(String uri, Class<T> rowType) {
        String url = getUrl(uri);
        HttpGet get = new HttpGet(url);
        get.setConfig(config);

        try {
            HttpResponse resp = httpClient.execute(get);
            InputStreamReader reader = new InputStreamReader(resp.getEntity().getContent());

            JsonObject jsonObject = gsonParser.parse(reader).getAsJsonObject();
            JsonArray rowsElement = jsonObject.getAsJsonArray("rows");

            ApiResponse<T> apiResponse = GSON.fromJson(jsonObject, ApiResponse.class);

            if (rowsElement != null) {
                List<T> rows = new ArrayList<>();
                for (JsonElement element : rowsElement) {
                    rows.add(GSON.fromJson(element, rowType));
                }
                apiResponse.setRows(rows);
            }

            return apiResponse;
        } catch (IOException e) {
            throw new IllegalStateException("Request failed to " + url, e);
        }
    }

    private String getUrl(String uri) {
        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }
        return String.format("http://localhost:%s%s", port, uri);
    }
}
