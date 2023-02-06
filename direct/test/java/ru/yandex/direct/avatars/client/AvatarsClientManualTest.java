package ru.yandex.direct.avatars.client;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.HashedWheelTimer;
import org.apache.commons.io.IOUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.avatars.client.model.AvatarId;
import ru.yandex.direct.avatars.client.model.AvatarInfo;
import ru.yandex.direct.avatars.config.AvatarsConfig;
import ru.yandex.direct.avatars.config.ServerConfig;

import static ru.yandex.direct.utils.HashingUtils.getMd5HashAsBase64YaStringWithoutPadding;

/**
 * Тест реальной загрузки картинки из ресурсов в аватарницу и удаления загруженной картинки.
 * Только для локального запуска (ну или из среды в которой просверлены дырки до аватарницы).
 */
@Ignore("For manual run")
public class AvatarsClientManualTest {
    private static final int WRITE_SERVER_PORT = 13000;
    private static final String WRITE_AVATARS_TESTING_HOST = "avatars-int.mdst.yandex.net";
    private static final String READ_AVATARS_TESTING_HOST = "avatars.mdst.yandex.net";
    private static final String SERVER_SCHEMA = "http";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final String NAMESPACE = "direct-avatars";
    private static final Duration COMMON_TIMEOUT = Duration.ofSeconds(6);
    private static final int IO_THREADS_COUNT = 4;
    private static final String IMAGE_PATH = "avatars/YandexDirect.png";
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(AvatarsClientManualTest.class);
    private static final String DELETE_OPERATION = "delete";
    private final AvatarsConfig conf = new AvatarsConfig("test config",
            new ServerConfig(READ_AVATARS_TESTING_HOST, 443, "https"),
            new ServerConfig(WRITE_AVATARS_TESTING_HOST, WRITE_SERVER_PORT, "http"),
            TIMEOUT,
            NAMESPACE,
            false);
    private byte[] imageBody;
    private AvatarsClient avatarsClient;

    private byte[] getImageBody() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = classloader.getResourceAsStream(IMAGE_PATH)) {
            return IOUtils.toByteArray(is);
        }
    }

    @Before
    public void setup() throws IOException {
        imageBody = getImageBody();
        FetcherSettings fetcherSettings =
                new FetcherSettings().withRequestTimeout(conf.getWriteTimeout());
        ParallelFetcherFactory fetcherFactory = new ParallelFetcherFactory(getAsyncHttpClient(), fetcherSettings);
        avatarsClient = new AvatarsClient(conf, fetcherFactory, null, null);
    }

    private AsyncHttpClient getAsyncHttpClient() {
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
        builder.setRequestTimeout((int) COMMON_TIMEOUT.toMillis());
        builder.setReadTimeout((int) COMMON_TIMEOUT.toMillis());
        builder.setConnectTimeout((int) COMMON_TIMEOUT.toMillis());
        builder.setConnectionTtl((int) COMMON_TIMEOUT.toMillis());
        builder.setPooledConnectionIdleTimeout((int) COMMON_TIMEOUT.toMillis());
        builder.setIoThreadsCount(IO_THREADS_COUNT);
        builder.setNettyTimer(new HashedWheelTimer(
                new ThreadFactoryBuilder().setNameFormat("ahc-timer-%02d").setDaemon(true).build()));
        return new DefaultAsyncHttpClient(builder.build());
    }

    @Test
    public void uploadAndDelete() {
        //Загружаем картинку в аватарницу.
        AvatarId avatarId = avatarsClient.upload(imageBody);
        //Удаляем картинку из аватарницы
        delete(avatarId);
    }

    @Test
    public void uploadWithKeyAndDelete() {
        //Загружаем картинку c ключом в аватарницу.
        String md5HashAsBase64YaStringWithoutPadding = getMd5HashAsBase64YaStringWithoutPadding(imageBody);
        AvatarInfo avatarInfo = avatarsClient.upload(md5HashAsBase64YaStringWithoutPadding, imageBody);
        AvatarId avatarId = new AvatarId(avatarInfo.getNamespace(), avatarInfo.getGroupId(), avatarInfo.getKey());
        //Удаляем картинку из аватарницы
        delete(avatarId);
    }

    private void delete(AvatarId avatarId) {
        int attemptsLeft = 10;
        while (!avatarsClient.delete(avatarId)) {
            attemptsLeft--;
            if (attemptsLeft <= 0) {
                //каким-то образом только-что созданная картинка разъехалась по шардам и теперь не может удалиться
                // сразу на всех.  Это особенность работы аватарницы.
                logger.warn(String.format(
                        "Не удалось удалить загруженную картинку, сделайте, пожалуйста, это вручную, чтобы не " +
                                "занимать квоту. Для этого выполняйте команду 'curl -I %s://%s-%s' пока не получите " +
                                "404 код.",
                        SERVER_SCHEMA,
                        DELETE_OPERATION,
                        avatarId.toAvatarIdString()
                ));
                break;
            }
        }
    }
}
