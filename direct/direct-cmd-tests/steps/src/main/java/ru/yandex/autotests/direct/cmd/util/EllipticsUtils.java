package ru.yandex.autotests.direct.cmd.util;

import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;

public class EllipticsUtils {

    private static final String BASE_URL_UPLOAD = "http://qa-storage.yandex-team.ru/upload";
    private static final String BASE_URL_DOWNLOAD = "http://aqua.yandex-team.ru/storage/get";

    private static final String HOME_PATH = "/screenshooter";

    public static String save(String path, byte[] bytes) {
        String uploadUrl = buildUrl(BASE_URL_UPLOAD, HOME_PATH + path);
        String downloadUrl = buildUrl(BASE_URL_DOWNLOAD, HOME_PATH + path);
        EntityBuilder entityBuilder = EntityBuilder.create();
        entityBuilder.setBinary(bytes);

        HttpPost post = new HttpPost(uploadUrl);
        post.setEntity(entityBuilder.build());
        post.setHeader("Accept", "*/*");
        try {
            HttpClient client = HttpClients.createDefault();
            client.execute(post);
            return downloadUrl;
        } catch (Exception e) {
            throw new IllegalStateException("ошибка загрузки файла в эллиптикс по урлу: " + uploadUrl, e);
        }
    }

    private static String buildUrl(String base, String path) {
        return base + path;
    }
}
