package ru.yandex.direct.core.entity.image.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.model.ImageType;
import ru.yandex.direct.core.entity.image.model.AvatarHost;
import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.core.entity.image.model.BannerImageFormatNamespace;
import ru.yandex.direct.core.entity.image.model.ImageFormat;
import ru.yandex.direct.core.entity.image.model.ImageSmartCenter;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestBanners;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImageFormat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerImageFormatRepositoryTest {
    private static final int HASH_SIZE = 22;
    @Autowired
    public BannerImageFormatRepository bannerImageFormatRepository;
    @Autowired
    public ClientSteps clientSteps;
    @Autowired
    private TestBannerImageRepository testBannerImageRepository;
    @Autowired
    protected BannerSteps bannerSteps;

    private ClientInfo defaultClient;
    private int shard;

    private String imageHash;

    @Before
    public void before() {
        defaultClient = clientSteps.createDefaultClient();
        shard = defaultClient.getShard();

        imageHash = random(22, true, true);
        testBannerImageRepository.addBannerImageFormats(shard, List.of(TestBanners.imageAdImageFormat(imageHash)));
    }

    @Test
    public void addBannerImageFormat() {
        BannerImageFormat bannerImageFormat = getBannerImageFormat();
        bannerImageFormatRepository
                .addBannerImageFormat(defaultClient.getShard(), Collections.singletonList(bannerImageFormat));
        String imageHash = bannerImageFormat.getImageHash();
        Map<String, BannerImageFormat> actualBannerImageFormats =
                bannerImageFormatRepository.getBannerImageFormats(defaultClient.getShard(),
                        Collections.singletonList(imageHash));

        assertThat(actualBannerImageFormats.get(imageHash)).is(matchedBy(beanDiffer(bannerImageFormat)));
    }

    @Test
    public void updateBannerImageFormat() {
        BannerImageFormat bannerImageFormat = getBannerImageFormat();
        bannerImageFormatRepository
                .addBannerImageFormat(defaultClient.getShard(), Collections.singletonList(bannerImageFormat));
        var originalSize = bannerImageFormat.getSize();
        bannerImageFormat.getSize()
                .withHeight(originalSize.getHeight() - 1)
                .withWidth(originalSize.getWidth() - 1);
        bannerImageFormatRepository.updateBannerImageFormatSize(defaultClient.getShard(), bannerImageFormat);
        String imageHash = bannerImageFormat.getImageHash();
        Map<String, BannerImageFormat> actualBannerImageFormats =
                bannerImageFormatRepository.getBannerImageFormats(defaultClient.getShard(),
                        Collections.singletonList(imageHash));

        assertThat(actualBannerImageFormats.get(imageHash)).is(matchedBy(beanDiffer(bannerImageFormat)));
    }

    @Test(expected = IllegalStateException.class)
    public void addBannerImageFormatWithEmptyFormat() {
        var format = getBannerImageFormat().withFormats(Map.of());
        bannerImageFormatRepository.addBannerImageFormat(defaultClient.getShard(), List.of(format));
    }

    /**
     * Если в таблице BANNER_IMAGES_FORMATS есть запись с заданным image_hash, а в таблицах BANNER_IMAGES_POOL и
     * BANNER_IMAGES нет записей с данным image_hash -> при вызове метода
     * {@link BannerImageFormatRepository#deleteByHashesWithoutRelations} запись в таблице
     * BANNER_IMAGES_FORMATS будет удалена
     */
    @Test
    public void deleteWhenHasNoRelations() {
        bannerImageFormatRepository.deleteByHashesWithoutRelations(shard, List.of(imageHash));
        assertThat(bannerImageFormatRepository.getBannerImageFormats(shard, List.of(imageHash))).isEmpty();
    }

    /**
     * Если в таблицах BANNER_IMAGES_FORMATS и BANNER_IMAGES_POOL есть записи с заданным image_hash, а в таблице
     * BANNER_IMAGES нет записей с данным image_hash -> при вызове метода
     * {@link BannerImageFormatRepository#deleteByHashesWithoutRelations} запись в таблице
     * BANNER_IMAGES_FORMATS не будет удалена
     */
    @Test
    public void deleteWhenHasPoolRalation() {
        bannerSteps.addImageToImagePool(shard, defaultClient.getClientId(), imageHash);
        bannerImageFormatRepository.deleteByHashesWithoutRelations(shard, List.of(imageHash));
        assertThat(bannerImageFormatRepository.getBannerImageFormats(shard, List.of(imageHash))).isNotEmpty();
    }

    /**
     * Если в таблицах BANNER_IMAGES_FORMATS и BANNER_IMAGES есть записи с заданным image_hash, а в таблице
     * BANNER_IMAGES_POOL нет записей с данным image_hash -> при вызове метода
     * {@link BannerImageFormatRepository#deleteByHashesWithoutRelations} запись в таблице
     * BANNER_IMAGES_FORMATS не будет удалена
     */
    @Test
    public void deleteByHashesWithoutRelations() {
        TextBannerInfo textBanner = bannerSteps.createBanner(activeTextBanner(), defaultClient);
        bannerSteps.createBannerImage(textBanner, defaultBannerImageFormat(imageHash),
                defaultBannerImage(textBanner.getBannerId(), imageHash));
        bannerImageFormatRepository.deleteByHashesWithoutRelations(shard, List.of(imageHash));
        assertThat(bannerImageFormatRepository.getBannerImageFormats(shard, List.of(imageHash))).isNotEmpty();
    }

    private BannerImageFormat getBannerImageFormat() {
        ImageSmartCenter smartCenter = new ImageSmartCenter()
                .withHeight(139)
                .withWidth(139)
                .withX(0)
                .withHeight(0);
        int height = 607;
        int width = 1080;
        ImageFormat imageFormat = new ImageFormat()
                .withHeight(height)
                .withWidth(width)
                .withSmartCenter(smartCenter);
        Map<String, ImageFormat> imageFormats = ImmutableMap.<String, ImageFormat>builder()
                .put("x150", imageFormat)
                .build();
        return new BannerImageFormat()
                .withImageHash(RandomStringUtils.randomAlphabetic(HASH_SIZE))
                .withImageType(ImageType.REGULAR)
                .withAvatarsHost(AvatarHost.AVATARS_MDST_YANDEX_NET)
                .withMdsGroupId(42386)
                .withNamespace(BannerImageFormatNamespace.DIRECT)
                .withFormats(imageFormats)
                .withSize(new ImageSize()
                        .withWidth(width)
                        .withHeight(height));
    }
}
