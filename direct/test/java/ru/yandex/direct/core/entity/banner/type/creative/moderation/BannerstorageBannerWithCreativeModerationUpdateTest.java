package ru.yandex.direct.core.entity.banner.type.creative.moderation;

import java.util.Collection;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;

@CoreTest
@RunWith(Parameterized.class)
public class BannerstorageBannerWithCreativeModerationUpdateTest extends BannerClientInfoUpdateOperationTestBase {
    private static final OldBannerStatusModerate BANNER_DRAFT =
            OldBannerStatusModerate.NEW;
    private static final OldBannerStatusModerate BANNER_ACTIVE =
            OldBannerStatusModerate.YES;
    private static final List<Long> EXPECTED_CREATIVE_GEO = asList(RUSSIA_REGION_ID, KAZAKHSTAN_REGION_ID);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CreativeRepository creativeRepository;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public ModerationMode moderationMode;

    @Parameterized.Parameter(2)
    public OldBannerStatusModerate statusModerate;

    @Parameterized.Parameter(3)
    public BannerStatusModerate bannerStatusModerate;

    @Parameterized.Parameter(4)
    public BannerStatusPostModerate bannerStatusPostModerate;

    @Parameterized.Parameter(5)
    public BannerCreativeStatusModerate bannerCreativeStatusModerate;

    @Parameterized.Parameter(6)
    public StatusModerate creativeStatusModerate;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "баннер черновик, дефолтный режим модерации",
                        // режим модерации
                        ModerationMode.DEFAULT,
                        // исходный баннер
                        BANNER_DRAFT,
                        // ожидаемые статусы модерации баннера, постмодерации, banners_performance, perf_creatives
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        BannerCreativeStatusModerate.NEW,
                        StatusModerate.READY
                },
                {
                        "баннер черновик, режим принудительной отправки в модерацию",
                        // режим модерации
                        ModerationMode.FORCE_MODERATE,
                        // исходный баннер
                        BANNER_DRAFT,
                        // ожидаемые статусы модерации баннера, постмодерации, banners_performance, perf_creatives
                        BannerStatusModerate.READY,
                        BannerStatusPostModerate.NO,
                        BannerCreativeStatusModerate.READY,
                        StatusModerate.READY
                },
                {
                        "баннер черновик, режим сохранения в черновики",
                        // режим модерации
                        ModerationMode.FORCE_SAVE_DRAFT,
                        // исходный баннер
                        BANNER_DRAFT,
                        // ожидаемые статусы модерации баннера, постмодерации, banners_performance, perf_creatives
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        BannerCreativeStatusModerate.NEW,
                        StatusModerate.NEW
                },
                {
                        "баннер не черновик, дефолтный режим модерации",
                        // режим модерации
                        ModerationMode.DEFAULT,
                        // исходный баннер
                        BANNER_ACTIVE,
                        // ожидаемые статусы модерации баннера, постмодерации, banners_performance, perf_creatives
                        BannerStatusModerate.READY,
                        BannerStatusPostModerate.NO,
                        BannerCreativeStatusModerate.READY,
                        StatusModerate.READY
                },
                {
                        "баннер не черновик, режим принудительной отправки в модерацию",
                        // режим модерации
                        ModerationMode.FORCE_MODERATE,
                        // исходный баннер
                        BANNER_ACTIVE,
                        // ожидаемые статусы модерации баннера, постмодерации, banners_performance, perf_creatives
                        BannerStatusModerate.READY,
                        BannerStatusPostModerate.NO,
                        BannerCreativeStatusModerate.READY,
                        StatusModerate.READY
                },
                {
                        "баннер не черновик, режим сохранения в черновики",
                        // режим модерации
                        ModerationMode.FORCE_SAVE_DRAFT,
                        // исходный баннер
                        BANNER_ACTIVE,
                        // ожидаемые статусы модерации баннера, постмодерации, banners_performance, perf_creatives
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        BannerCreativeStatusModerate.NEW,
                        StatusModerate.NEW
                },
        });
    }

    @Override
    protected ModerationMode getModerationMode() {
        return moderationMode;
    }

    @Test
    public void test() {
        var adGroupGeo = asList(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID, KAZAKHSTAN_REGION_ID);
        var adGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroupWithGeo(adGroupGeo);
        clientInfo = adGroupInfo.getClientInfo();

        Long oldCreativeId = steps.creativeSteps().getNextCreativeId();
        Long newCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultBannerstorageCreative(clientInfo, oldCreativeId);
        steps.creativeSteps().addDefaultBannerstorageCreative(clientInfo, newCreativeId);

        var bid = steps.bannerSteps().createBanner(
                activeCpmBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), oldCreativeId)
                        .withStatusModerate(statusModerate)
                        .withStatusPostModerate(OldBannerStatusPostModerate.NO)
                        .withPixels(null),
                adGroupInfo).getBannerId();

        var modelChanges = ModelChanges.build(bid, CpmBanner.class,
                CpmBanner.CREATIVE_ID, newCreativeId);
        prepareAndApplyValid(asList(modelChanges));

        CpmBanner actualBanner = getBanner(bid);
        assertThat(actualBanner.getStatusModerate(), equalTo(bannerStatusModerate));
        assertThat(actualBanner.getStatusPostModerate(), equalTo(bannerStatusPostModerate));
        assertThat(actualBanner.getCreativeStatusModerate(), equalTo(bannerCreativeStatusModerate));
        assertThat(getCreativeFromDB(newCreativeId).getStatusModerate(), equalTo(creativeStatusModerate));
        assertThat(getCreativeFromDB(newCreativeId).getSumGeo(), containsInAnyOrder(EXPECTED_CREATIVE_GEO.toArray()));
    }

    private Creative getCreativeFromDB(Long id) {
        List<Creative> list = creativeRepository.getCreatives(clientInfo.getShard(), clientInfo.getClientId(),
                singletonList(id));
        return list.get(0);
    }
}
