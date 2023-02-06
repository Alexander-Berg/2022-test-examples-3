package ru.yandex.direct.core.testing.mock;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;

import ru.yandex.direct.avatars.client.AvatarsClient;
import ru.yandex.direct.avatars.client.model.AvatarInfo;
import ru.yandex.direct.avatars.client.model.answer.ImageSize;
import ru.yandex.direct.avatars.config.AvatarsConfig;
import ru.yandex.direct.avatars.config.ServerConfig;
import ru.yandex.direct.core.entity.freelancer.service.AvatarsClientPool;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.ORIG;

public class AvatarsClientMockUtils {
    public static final int GROUP_ID = 1;
    public static final String NAMESPACE = "direct";
    public static final int WRITE_SERVER_PORT = 13000;
    public static final String WRITE_AVATARS_TESTING_HOST = "avatars-int.mdst.yandex.net";
    public static final String READ_AVATARS_TESTING_HOST = "avatars.mdst.yandex.net";
    public static final Duration TIMEOUT = Duration.ofSeconds(20);
    public static final int DEFAULT_HEIGHT_TEXT_BANNER_IMAGE = 607;
    public static final int DEFAULT_WIDTH_TEXT_BANNER_IMAGE = 1080;
    public static final String CONFIG_NAME = "test config";


    public static AvatarsClientPool getMockAvatarsClientPool() {
        AvatarsClientPool avatarsClientPool = mock(AvatarsClientPool.class);
        AvatarsClient mockAvatarsClient = getMockAvatarsClientWithDefaultUploadResult();
        when(avatarsClientPool.getDefaultClient()).thenReturn(mockAvatarsClient);
        return avatarsClientPool;
    }

    public static void mockUpload(ImageSize defaultImageSizeOfImageAd, AvatarsClientPool avatarsClientPool) {
        AvatarsClient defaultClient = avatarsClientPool.getDefaultClient();
        when(defaultClient.upload(any(), any())).then(l ->
        {
            String key = UUID.randomUUID().toString();
            Map<String, ImageSize> sizes = ImmutableMap.<String, ImageSize>builder()
                    .put(ORIG, defaultImageSizeOfImageAd)
                    .put("x450", new ImageSize().withHeight(400).withWidth(450))
                    .build();
            return new AvatarInfo(NAMESPACE, GROUP_ID, key, null, sizes);
        });
    }

    /**
     * Мок AvatarsClient
     * в {@link AvatarsClient#upload(String, byte[])} возвращающий всегда {@link #getDefaultAvatarInfo}
     * в {@link AvatarsClient#getConf} возвращающися всегда {@link #getDefaultConfig}
     */
    private static AvatarsClient getMockAvatarsClientWithDefaultUploadResult() {
        AvatarsClient avatarsClient = mock(AvatarsClient.class);
        when(avatarsClient.upload(any(), any())).then(l ->
        {
            String key = UUID.randomUUID().toString();
            return getDefaultAvatarInfo(key);
        });
        when(avatarsClient.getConf()).thenReturn(getDefaultConfig());
        return avatarsClient;
    }

    private static AvatarInfo getDefaultAvatarInfo(String key) {
        return new AvatarInfo(NAMESPACE, GROUP_ID, key, null, getDefaultImageSizesForTextBanner());
    }

    private static AvatarsConfig getDefaultConfig() {
        return new AvatarsConfig(CONFIG_NAME,
                new ServerConfig(READ_AVATARS_TESTING_HOST, 443, "https"),
                new ServerConfig(WRITE_AVATARS_TESTING_HOST, WRITE_SERVER_PORT, "http"),
                TIMEOUT,
                NAMESPACE,
                false);
    }

    private static Map<String, ImageSize> getDefaultImageSizesForTextBanner() {
        ImageSize imageSize = new ImageSize()
                .withHeight(DEFAULT_HEIGHT_TEXT_BANNER_IMAGE)
                .withWidth(DEFAULT_WIDTH_TEXT_BANNER_IMAGE);
        return ImmutableMap.<String, ImageSize>builder()
                .put("x150", imageSize)
                .put("orig", imageSize)
                .build();
    }
}
