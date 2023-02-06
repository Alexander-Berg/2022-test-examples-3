package ru.yandex.direct.core.entity.banner.type.pixels;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerWithPixels;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adriverPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.tnsPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithPixelsUpdateBsSyncedTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBanner> {
    @Autowired
    private DslContextProvider dslContextProvider;

    CreativeInfo creativeInfo;
    private LocalDateTime someTime;
    private ClientInfo defaultClientInfo;
    private AdGroupInfo adGroupInfo;

    @Before
    public void setUp() throws Exception {
        List<String> pixels = List.of(adriverPixelUrl(), yaAudiencePixelUrl());
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        defaultClientInfo = adGroupInfo.getClientInfo();
        steps.clientPixelProviderSteps().addCpmBannerPixelsPermissions(defaultClientInfo);
        creativeInfo = steps.creativeSteps()
                .addDefaultHtml5Creative(defaultClientInfo, steps.creativeSteps().getNextCreativeId());
        bannerInfo = steps.bannerSteps().createBanner(
                activeCpmBanner(null, null, creativeInfo.getCreativeId())
                        .withPixels(pixels), adGroupInfo);
        someTime = LocalDateTime.now().minusMinutes(7).withNano(0);
        setLastChange();
    }

    @Test
    public void updateBanner_NoChange_StatusBsSyncedYes() {
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class);

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);
        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.YES));
        assertThat(actualBanner.getLastChange(), equalTo(someTime));
    }

    @Test
    public void updateBanner_ChangePixels_StatusBsSyncedNo() {
        var pixels = List.of(tnsPixelUrl());
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(pixels, BannerWithPixels.PIXELS);

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat(actualBanner.getLastChange(), not(equalTo(someTime)));
    }

    @Test
    public void updateBanner_ChangePixelsOrder_StatusBsSyncedYes() {
        List<String> pixels = List.of(yaAudiencePixelUrl(), adriverPixelUrl());
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(pixels, BannerWithPixels.PIXELS);

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.YES));
        assertThat(actualBanner.getLastChange(), equalTo(someTime));
    }

    @Test
    public void updateBanner_ChangePixelsSameList_StatusBsSyncedYes() {
        List<String> pixels = List.of(adriverPixelUrl(), yaAudiencePixelUrl());
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(pixels, BannerWithPixels.PIXELS);

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.YES));
        assertThat(actualBanner.getLastChange(), equalTo(someTime));
    }

    @Test
    public void updateBanner_DeletePixels_StatusBsSyncedNo() {
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(emptyList(), BannerWithPixels.PIXELS);

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat(actualBanner.getLastChange(), not(equalTo(someTime)));
    }

    @Test
    public void updateBanner_AddPixels_StatusBsSyncedNo() {
        var pixels = List.of(tnsPixelUrl());
        bannerInfo = steps.bannerSteps().createBanner(
                activeCpmBanner(null, null, creativeInfo.getCreativeId()), adGroupInfo);
        ModelChanges<CpmBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(pixels, BannerWithPixels.PIXELS);
        setLastChange();

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat(actualBanner.getLastChange(), not(equalTo(someTime)));
    }

    @Test
    public void updateBanner_ChangeEmptyPixelsToNull_StatusBsSyncedYes() {
        bannerInfo = steps.bannerSteps().createBanner(activeCpmBanner(null, null, creativeInfo.getCreativeId())
                .withPixels(emptyList()), adGroupInfo);
        ModelChanges<CpmBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(null, BannerWithPixels.PIXELS);
        setLastChange();

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.YES));
        assertThat(actualBanner.getLastChange(), equalTo(someTime));
    }

    @Test
    public void updateBanner_ChangeEmptyPixelsToEmptyList_StatusBsSyncedYes() {
        bannerInfo = steps.bannerSteps().createBanner(activeCpmBanner(null, null, creativeInfo.getCreativeId())
                .withPixels(emptyList()), adGroupInfo);
        ModelChanges<CpmBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(emptyList(), BannerWithPixels.PIXELS);
        setLastChange();

        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);

        assertThat(actualBanner.getStatusBsSynced(), equalTo(StatusBsSynced.YES));
        assertThat(actualBanner.getLastChange(), equalTo(someTime));
    }

    private void setLastChange() {
        dslContextProvider.ppc(bannerInfo.getShard())
                .update(BANNERS)
                .set(BANNERS.LAST_CHANGE, someTime)
                .where(BANNERS.BID.equal(bannerInfo.getBannerId()))
                .execute();
    }
}
