package ru.yandex.direct.grid.processing.service.client;

import java.time.LocalDateTime;
import java.util.Collections;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.client.GdClientSearchRequest;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImage;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImageFilter;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImageType;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImagesContainer;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImagesContext;

import static java.util.Arrays.asList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.grid.processing.service.client.converter.TestClientEntityConverter.toGdImage;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientEntityImagesTest {
    @Autowired
    ClientEntityDataService clientEntityDataService;

    @Autowired
    Steps steps;

    @Autowired
    TestBannerImageRepository testBannerImageRepository;

    private BannerImageFormat imageFormat;
    private BannerImageFormat imageAdImageFormat;
    private GdClientInfo clientInfo;
    private GdImage gdImage;
    private GdImage imageAdGdImage;
    private GdImagesContainer container;

    @Before
    public void init() {
        UserInfo user = steps.userSteps().createDefaultUser();
        clientInfo = new GdClientInfo().withShard(user.getShard())
                .withId(user.getClientInfo().getClientId().asLong());

        imageFormat = steps.bannerSteps().createBannerImageFormat(user.getClientInfo());
        LocalDateTime createTime = testBannerImageRepository
                .getImageCreateTime(user.getShard(), clientInfo.getId(), imageFormat.getImageHash());
        gdImage = toGdImage(imageFormat).withCreateTime(createTime);

        imageAdImageFormat = steps.bannerSteps().createImageAdImageFormat(user.getClientInfo());
        createTime = createTime.plusSeconds(1);
        testBannerImageRepository
                .setImageCreateTime(user.getShard(), clientInfo.getId(), imageAdImageFormat.getImageHash(), createTime);
        imageAdGdImage = toGdImage(imageAdImageFormat).withCreateTime(createTime);

        container = defaultGdImagesContainer();
    }

    @Test
    public void testClientImages_noFilter() {
        check(defaultGdImagesContext()
                .withImageHashes(ImmutableSet.of(imageAdImageFormat.getImageHash(), imageFormat.getImageHash()))
                .withTotalCount(2)
                .withRowset(asList(gdImage, imageAdGdImage)));
    }

    @Test
    public void testClientImages_filterUnexistingImageHash() {
        container.getFilter().setImageHashIn(ImmutableSet.of(RandomStringUtils.randomAlphanumeric(21)));
        check(defaultGdImagesContext());
    }

    @Test
    public void testClientImages_filterMaxCreateTimeLessThanActual() {
        container.getFilter().setMaxCreatedDateTime(gdImage.getCreateTime().minusSeconds(1));
        check(defaultGdImagesContext());
    }

    @Test
    public void testClientImages_filterMinCreateTimeMoreThanActual() {
        container.getFilter().setMinCreatedDateTime(imageAdGdImage.getCreateTime().plusSeconds(1));
        check(defaultGdImagesContext());
    }

    @Test
    public void testClientImages_filterCreateTime() {
        container.getFilter().setMaxCreatedDateTime(imageAdGdImage.getCreateTime().plusSeconds(1));
        container.getFilter().setMinCreatedDateTime(gdImage.getCreateTime().minusSeconds(1));
        check(defaultGdImagesContext()
                .withImageHashes(ImmutableSet.of(imageAdImageFormat.getImageHash(), imageFormat.getImageHash()))
                .withTotalCount(2)
                .withRowset(asList(gdImage, imageAdGdImage)));
    }

    @Test
    public void testClientImages_filterBannerTypeText() {
        container.getFilter().setImageType(GdImageType.BANNER_TEXT);
        check(defaultGdImagesContext()
                .withImageHashes(ImmutableSet.of(imageFormat.getImageHash()))
                .withTotalCount(1)
                .withRowset(Collections.singletonList(gdImage)));
    }

    @Test
    public void testClientImages_filterBannerTypeImageAd() {
        container.getFilter().setImageType(GdImageType.BANNER_IMAGE_AD);
        check(defaultGdImagesContext()
                .withImageHashes(ImmutableSet.of(imageAdImageFormat.getImageHash()))
                .withTotalCount(1)
                .withRowset(Collections.singletonList(imageAdGdImage)));
    }

    @Test
    public void testClientImages_filterBannerTypeMcbanner() {
        container.getFilter().setImageType(GdImageType.BANNER_MCBANNER);
        check(defaultGdImagesContext());
    }

    @Test
    public void testClientImages_filterExistingImageHash() {
        container.getFilter().setImageHashIn(ImmutableSet.of(gdImage.getImageHash()));

        check(defaultGdImagesContext()
                .withImageHashes(ImmutableSet.of(imageFormat.getImageHash()))
                .withTotalCount(1)
                .withRowset(Collections.singletonList(gdImage)));
    }

    @Test
    public void testClientImages_limitNoOffset() {
        container.getLimitOffset().withLimit(1).withOffset(0);

        GdImage expectedGdImage =
                ((gdImage.getCreateTime().compareTo(imageAdGdImage.getCreateTime()) > 0)) ? gdImage : imageAdGdImage;

        check(defaultGdImagesContext()
                .withImageHashes(ImmutableSet.of(imageAdImageFormat.getImageHash(), imageFormat.getImageHash()))
                .withTotalCount(2)
                .withRowset(Collections.singletonList(expectedGdImage)));
    }

    @Test
    public void testClientImages_limitWithOffset() {
        container.getLimitOffset().withLimit(1).withOffset(1);

        GdImage expectedGdImage =
                ((gdImage.getCreateTime().compareTo(imageAdGdImage.getCreateTime()) > 0)) ? imageAdGdImage : gdImage;

        check(defaultGdImagesContext()
                .withImageHashes(ImmutableSet.of(imageAdImageFormat.getImageHash(), imageFormat.getImageHash()))
                .withTotalCount(2)
                .withRowset(Collections.singletonList(expectedGdImage)));
    }

    @Test
    public void testClientImages_filterUnexistingAndExistingImageHashes() {
        container.getFilter()
                .setImageHashIn(ImmutableSet.of(imageFormat.getImageHash(), RandomStringUtils.randomAlphanumeric(21)));

        check(defaultGdImagesContext()
                .withImageHashes(ImmutableSet.of(imageFormat.getImageHash()))
                .withTotalCount(1)
                .withRowset(Collections.singletonList(gdImage)));
    }

    private void check(GdImagesContext expected) {
        GdImagesContext gdImagesContext = clientEntityDataService.getClientImages(clientInfo, container);

        assertSoftly(assertions -> {
            assertions.assertThat(gdImagesContext.getTotalCount()).isEqualTo(expected.getTotalCount());
            assertions.assertThat(gdImagesContext.getImageHashes()).hasSameElementsAs(expected.getImageHashes());
            assertions.assertThat(gdImagesContext.getRowset()).hasSameElementsAs(expected.getRowset());
        });
    }

    private GdImagesContainer defaultGdImagesContainer() {
        return new GdImagesContainer()
                .withSearchBy(new GdClientSearchRequest().withId(clientInfo.getId()))
                .withFilter(new GdImageFilter().withImageHashIn(Collections.emptySet()))
                .withLimitOffset(new GdLimitOffset());
    }

    private GdImagesContext defaultGdImagesContext() {
        return new GdImagesContext()
                .withRowset(Collections.emptyList())
                .withImageHashes(Collections.emptySet())
                .withTotalCount(0);
    }
}
