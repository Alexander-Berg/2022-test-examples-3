package ru.yandex.direct.core.entity.banner.type.creative.moderation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestCreativeRepository;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultBannerstorageCreative;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerstorageBannerWithCreativeModerationAddTest extends BannerAdGroupInfoAddOperationTestBase {
    private static final List<Long> EXPECTED_CREATIVE_GEO = asList(RUSSIA_REGION_ID, KAZAKHSTAN_REGION_ID);

    @Autowired
    private CreativeRepository creativeRepository;
    @Autowired
    private TestCreativeRepository testCreativeRepository;

    private CpmBanner banner;
    private Long creativeId;

    @Before
    public void before() throws Exception {
        List<Long> adGroupGeo = asList(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID, KAZAKHSTAN_REGION_ID);
        adGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroupWithGeo(adGroupGeo);
        creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultBannerstorageCreative(adGroupInfo.getClientInfo(), creativeId);
        banner = clientCpmBanner(creativeId)
                .withAdGroupId(adGroupInfo.getAdGroupId());
    }

    @Test
    //проверить что статус модерации креатива стал ready
    public void creativeStatusModerate_Ready() {
        prepareAndApplyValid(banner, false);

        assertThat(getCreativeFromDB().getStatusModerate(), equalTo(StatusModerate.READY));
    }

    @Test
    //если черновик, то статус модерации креатива New
    public void creativeStatusModerate_New_whenSaveDraft() {
        prepareAndApplyValid(banner, true);

        assertThat(getCreativeFromDB().getStatusModerate(), equalTo(StatusModerate.NEW));
    }

    @Test
    //проверить что установилось sum_geo таблицы perf_creatives
    public void addBanners_updateCreativesGeo() {
        prepareAndApplyValid(banner, false);

        assertThat(getCreativeGeo(), containsInAnyOrder(EXPECTED_CREATIVE_GEO.toArray()));
    }

    @Test
    //проверить что установилось sum_geo таблицы perf_creatives с черновиком
    public void addBanners_updateCreativesGeo_whenSaveDraft() {
        prepareAndApplyValid(banner, true);

        assertThat(getCreativeGeo(), containsInAnyOrder(EXPECTED_CREATIVE_GEO.toArray()));
    }

    @Test
    //если креатив уже одобрен, то гео не должно меняться
    public void addBanners_dontUpdateCreativesGeo_whenCreativeStatusModerateIsYes() {
        List<Long> geo = List.of(1L, 5L);
        creativeId = steps.creativeSteps().getNextCreativeId();
        Creative creative = defaultBannerstorageCreative(adGroupInfo.getClientInfo().getClientId(), creativeId)
                .withStatusModerate(StatusModerate.YES);
        creativeRepository.add(adGroupInfo.getShard(), singletonList(creative));
        testCreativeRepository.updateCreativesGeo(adGroupInfo.getShard(), creativeId, geo);
        banner = clientCpmBanner(creativeId).withAdGroupId(adGroupInfo.getAdGroupId());

        prepareAndApplyValid(banner, true);

        assertThat(getCreativeGeo(), containsInAnyOrder(geo.toArray()));
    }

    @Test
    //создаём креатив и баннер.
    public void addBanners_updateBannerStatuses_whenNoDraft() {
        Long id = prepareAndApplyValid(banner, false);

        CpmBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getStatusModerate(), equalTo(BannerStatusModerate.READY));
        assertThat(actualBanner.getStatusPostModerate(), equalTo(BannerStatusPostModerate.NO));
        assertThat(actualBanner.getCreativeStatusModerate(), equalTo(BannerCreativeStatusModerate.READY));
    }

    @Test
    //создаём креатив и баннер. Черновик.
    // Проверяем что статус модерации, постмодерации и banners_performance.statusModerate проставились в New, No, New
    public void addBanners_updateBannerStatuses_whenSaveDraft() {
        Long id = prepareAndApplyValid(banner, true);

        CpmBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getStatusModerate(), equalTo(BannerStatusModerate.NEW));
        assertThat(actualBanner.getStatusPostModerate(), equalTo(BannerStatusPostModerate.NO));
        assertThat(actualBanner.getCreativeStatusModerate(), equalTo(BannerCreativeStatusModerate.NEW));
    }

    private List<Long> getCreativeGeo() {
        return getCreativeFromDB().getSumGeo();
    }

    private Creative getCreativeFromDB() {
        return creativeRepository.getCreatives(adGroupInfo.getShard(),
                adGroupInfo.getClientId(),
                singletonList(creativeId))
                    .get(0);
    }
}
