package ru.yandex.market.clab.tms.service.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import ru.yandex.market.clab.tms.service.ocr.data.RecognizeResponse;

import java.io.IOException;
import java.io.InputStream;

public class OcrClient {

    private static final String RECOGNIZE = "recognize";
    private final String recognizeUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OcrClient(String orcHost, String apiKey, HttpClient httpClient, ObjectMapper objectMapper) {
        this.recognizeUrl = orcHost + "/" + RECOGNIZE;
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public RecognizeResponse recognize(InputStream image) {

        HttpPost post = new HttpPost(recognizeUrl);

        HttpEntity entity = MultipartEntityBuilder.create()
            .addPart("file", new InputStreamBody(image, "file.jpg"))
            .addPart("apikey", new StringBody(apiKey, ContentType.TEXT_PLAIN))
            .addPart("langName", new StringBody("eng,rus", ContentType.TEXT_PLAIN))
            .addPart("meta", new StringBody("{\"StrategyName\":\"FullOcrMultihead\"}", ContentType.TEXT_PLAIN))
            .build();
        post.setEntity(entity);

        try {
            HttpResponse response = httpClient.execute(post);
            return objectMapper.readValue(response.getEntity().getContent(), RecognizeResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to recognize image", e);
        }
    }
}
