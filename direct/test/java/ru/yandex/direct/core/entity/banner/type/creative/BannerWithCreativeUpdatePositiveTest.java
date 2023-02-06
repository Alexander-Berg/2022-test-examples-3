package ru.yandex.direct.core.entity.banner.type.creative;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithCreative;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestCreativeRepository;
import ru.yandex.direct.core.testing.stub.CanvasClientStub;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultVideoAddition;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithCreativeUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithCreative> {

    private static final LocalDateTime DEFAULT_LAST_CHANGE = LocalDateTime.now().minusDays(1);

    @Autowired
    private TestCreativeRepository testCreativeRepository;

    @Autowired
    protected CanvasClientStub canvasClientStub;

    @Test
    public void validCreativeIdForTextBanner() {
        bannerInfo = createTextBanner();
        Long bannerId = bannerInfo.getBannerId();

        Long expectedCreativeId = createVideoAdditionCreative(bannerInfo.getClientInfo());

        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerId, TextBanner.class)
                .process(expectedCreativeId, TextBanner.CREATIVE_ID);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeId(), equalTo(expectedCreativeId));
        assertThat(actualBanner.getLastChange(), approximatelyNow());
    }

    @Test
    public void updateTextBannerWithCreativeId_VideoAdditionSync_DataIsSaved() {
        bannerInfo = createTextBanner();
        Long bannerId = bannerInfo.getBannerId();
        long creativeId = testCreativeRepository.getNextCreativeId();
        canvasClientStub.addCreatives(Collections.singletonList(creativeId));

        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerId, TextBanner.class)
                .process(creativeId, TextBanner.CREATIVE_ID);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeId(), equalTo(creativeId));
        assertThat(actualBanner.getLastChange(), approximatelyNow());
    }

    @Test
    public void updateTextBannerWithCreativeId_ShowTitleAndBody_IsUpdatedTrue() {
        bannerInfo = createTextBanner();
        Long bannerId = bannerInfo.getBannerId();
        long creativeId = testCreativeRepository.getNextCreativeId();
        canvasClientStub.addCreatives(Collections.singletonList(creativeId));
        prepareAndApplyValid(
                new ModelChanges<>(bannerId, TextBanner.class).process(creativeId, TextBanner.CREATIVE_ID)
        );

        steps.featureSteps().addClientFeature(bannerInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, true);
        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerId, TextBanner.class)
                .process(true, TextBanner.SHOW_TITLE_AND_BODY);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeId(), equalTo(creativeId));
        assertThat(actualBanner.getShowTitleAndBody(), equalTo(true));
    }

    @Test
    public void updateTextBannerWithCreativeId_ShowTitleAndBody_IsUpdatedFalse() {
        bannerInfo = createTextBanner();
        Long bannerId = bannerInfo.getBannerId();
        long creativeId = testCreativeRepository.getNextCreativeId();
        canvasClientStub.addCreatives(Collections.singletonList(creativeId));
        prepareAndApplyValid(
                new ModelChanges<>(bannerId, TextBanner.class).process(creativeId, TextBanner.CREATIVE_ID)
        );

        steps.featureSteps().addClientFeature(bannerInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, true);
        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerId, TextBanner.class)
                .process(false, TextBanner.SHOW_TITLE_AND_BODY);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeId(), equalTo(creativeId));
        assertThat(actualBanner.getShowTitleAndBody(), equalTo(false));
    }

    @Test
    public void updateTextBannerWithCreativeId_ShowTitleAndBody_IsUpdatedWithCreativeId() {
        bannerInfo = createTextBanner();
        Long bannerId = bannerInfo.getBannerId();
        long creativeId = testCreativeRepository.getNextCreativeId();
        canvasClientStub.addCreatives(Collections.singletonList(creativeId));

        steps.featureSteps().addClientFeature(bannerInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, true);
        prepareAndApplyValid(
                new ModelChanges<>(bannerId, TextBanner.class)
                        .process(creativeId, TextBanner.CREATIVE_ID)
                        .process(true, TextBanner.SHOW_TITLE_AND_BODY)
        );


        long anotherCreativeId = testCreativeRepository.getNextCreativeId();
        canvasClientStub.addCreatives(Collections.singletonList(anotherCreativeId));
        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerId, TextBanner.class)
                .process(anotherCreativeId, TextBanner.CREATIVE_ID)
                .process(false, TextBanner.SHOW_TITLE_AND_BODY);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeId(), equalTo(anotherCreativeId));
        assertThat(actualBanner.getShowTitleAndBody(), equalTo(false));
    }

    @Test
    public void updateTextBannerWithCreativeId_ShowTitleAndBody_IsInserted() {
        bannerInfo = createTextBanner();
        Long bannerId = bannerInfo.getBannerId();
        long creativeId = testCreativeRepository.getNextCreativeId();
        canvasClientStub.addCreatives(Collections.singletonList(creativeId));

        steps.featureSteps().addClientFeature(bannerInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, true);
        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerId, TextBanner.class)
                .process(creativeId, TextBanner.CREATIVE_ID)
                .process(true, TextBanner.SHOW_TITLE_AND_BODY);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeId(), equalTo(creativeId));
        assertThat(actualBanner.getShowTitleAndBody(), equalTo(true));
    }

    @Test
    public void updateTextBannerWithCreativeId_ShowTitleAndBody_IsDeleted() {
        bannerInfo = createTextBanner();
        Long bannerId = bannerInfo.getBannerId();
        long creativeId = testCreativeRepository.getNextCreativeId();
        canvasClientStub.addCreatives(Collections.singletonList(creativeId));

        steps.featureSteps().addClientFeature(bannerInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, true);
        prepareAndApplyValid(
                new ModelChanges<>(bannerId, TextBanner.class)
                        .process(creativeId, TextBanner.CREATIVE_ID)
                        .process(true, TextBanner.SHOW_TITLE_AND_BODY)
        );

        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerId, TextBanner.class)
                .process(null, TextBanner.CREATIVE_ID);
        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeId(), nullValue());
        assertThat(actualBanner.getShowTitleAndBody(), nullValue());
    }


    private Long createVideoAdditionCreative(ClientInfo clientInfo) {
        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        return creativeInfo.getCreativeId();
    }

    private TextBannerInfo createTextBanner() {
        OldTextBanner textBanner = activeTextBanner().withLastChange(DEFAULT_LAST_CHANGE);
        return steps.bannerSteps().createBanner(textBanner);
    }
}
