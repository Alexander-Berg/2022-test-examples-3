package ru.yandex.direct.core.entity.banner.endtype;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusbssynced;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatuspostmoderate;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;

@CoreTest
@RunWith(Parameterized.class)
public class EndTypesStatusesIgnoresEmptyModelsChangesTest extends BannerNewBannerInfoUpdateOperationTestBase {

    @Autowired
    private DslContextProvider dslContextProvider;

    @Parameterized.Parameter
    public Class<BannerWithSystemFields> bannerEndType;

    @Parameterized.Parameter(1)
    public Function<Steps, NewBannerInfo> createBannerFunction;

    private LocalDateTime lastChangeBeforeUpdate;
    private int shard;
    private Long bannerId;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        ContentPromotionBanner.class,
                        bannerCreatingFunction(
                                steps -> steps.oldContentPromotionBannerSteps()
                                        .createContentPromotionBanner(ContentPromotionContentType.VIDEO))
                },
                {
                        CpcVideoBanner.class,
                        bannerCreatingFunction(
                                steps -> steps.cpcVideoBannerSteps()
                                        .createDefaultCpcVideoBanner())
                },
                {
                        CpmOutdoorBanner.class,
                        bannerCreatingFunction(
                                steps -> steps.cpmOutdoorBannerSteps()
                                        .createDefaultCpmOutdoorBanner())
                }
        });
    }

    private static Function<Steps, NewBannerInfo> bannerCreatingFunction(Function<Steps, NewBannerInfo> fn) {
        return fn;
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        bannerInfo = createBannerFunction.apply(steps);
        shard = bannerInfo.getShard();
        bannerId = bannerInfo.getBannerId();
        setInitialStatuses();
        lastChangeBeforeUpdate = getLastChange();
    }

    @Test
    public void statusesAndLastChangeAreNotDroppedOnEmptyModelChanges() {
        ModelChanges<BannerWithSystemFields> modelChanges =
                new ModelChanges<>(bannerId, bannerEndType);
        prepareAndApplyValid(modelChanges);

        BannerWithSystemFields actualBanner = getBanner(bannerId);
        assertSoftly(assertions -> {
            assertions.assertThat(actualBanner.getStatusBsSynced())
                    .describedAs("statusBsSynced не должен сбрасываться на пустых ModelChanges")
                    .isEqualTo(StatusBsSynced.YES);
            assertions.assertThat(actualBanner.getStatusModerate())
                    .describedAs("statusModerate не должен сбрасываться на пустых ModelChanges")
                    .isEqualTo(BannerStatusModerate.YES);
            assertions.assertThat(actualBanner.getStatusPostModerate())
                    .describedAs("statusPostModerate не должен сбрасываться на пустых ModelChanges")
                    .isEqualTo(BannerStatusPostModerate.YES);
            assertions.assertThat(actualBanner.getLastChange())
                    .describedAs("lastChange не должен сбрасываться на пустых ModelChanges")
                    .isEqualTo(lastChangeBeforeUpdate);
        });
    }

    private void setInitialStatuses() {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.STATUS_BS_SYNCED, BannersStatusbssynced.Yes)
                .set(BANNERS.STATUS_MODERATE, BannersStatusmoderate.Yes)
                .set(BANNERS.STATUS_POST_MODERATE, BannersStatuspostmoderate.Yes)
                .where(BANNERS.BID.eq(bannerId))
                .execute();
    }

    private LocalDateTime getLastChange() {
        return dslContextProvider.ppc(shard)
                .select(BANNERS.LAST_CHANGE)
                .from(BANNERS)
                .where(BANNERS.BID.eq(bannerId))
                .fetchOne()
                .value1();
    }
}
