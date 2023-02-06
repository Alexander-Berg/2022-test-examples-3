package ru.yandex.travel.elliptics;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Retrofit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author kurau (Yuri Kalinin)
 */
public class Elliptics {

    private final static String UPLOAD_AQUA_URL = "http://qa-storage.yandex-team.ru";
    private final static String DOWNLOAD_AQUA_URL = "http://aqua.yandex-team.ru/storage/get/";

    private final static String DOWNLOAD_S3_URL = "http://s3.mds.yandex.net";

    private EllipticsService elliptics;

    private String path;

    private Elliptics(String url) {
        OkHttpClient client = new OkHttpClient.Builder().build();
        elliptics = new Retrofit.Builder().baseUrl(url).client(client).build()
                .create(EllipticsService.class);
    }

    public static Elliptics upload(Class<?> path) {
        return new Elliptics(UPLOAD_AQUA_URL).onPath(path);
    }

    public static Elliptics downloadFromS3() {
        return new Elliptics(DOWNLOAD_S3_URL);
    }

    private Elliptics onPath(Class<?> path) {
        this.path = path.getCanonicalName().replace(".", "/") + "/";
        return this;
    }

    public String image(File file) throws IOException {
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        elliptics.upload(path + file.getName(), requestFile).execute();

        return DOWNLOAD_AQUA_URL + path + file.getName();
    }

    public InputStream fileS3(String fileName) throws IOException {
        return elliptics.getFromS3(fileName).execute().body().byteStream();
    }
}
