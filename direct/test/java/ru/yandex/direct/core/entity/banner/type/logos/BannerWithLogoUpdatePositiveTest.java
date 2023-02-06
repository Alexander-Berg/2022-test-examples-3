package ru.yandex.direct.core.entity.banner.type.logos;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.StatusBannerLogoModerate;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusbssynced;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithLogoUpdatePositiveTest extends BannerClientInfoUpdateOperationTestBase {
    @Autowired
    public BannersAddOperationFactory addOperationFactory;

    @Autowired
    private DslContextProvider dslContextProvider;

    private LocalDateTime someTime;
    private Map<String, String> imageHashes;
    private Long creativeId1;
    private Long creativeId2;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String oldImageHashKey;

    @Parameterized.Parameter(2)
    public String newImageHashKey;

    @Parameterized.Parameter(3)
    public StatusBannerLogoModerate oldBannerLogoStatusModerate;

    @Parameterized.Parameter(4)
    public BannerLogoStatusModerate newBannerLogoStatusModerate;

    //Изменение креатива для смены статуса модерации
    @Parameterized.Parameter(5)
    public Boolean changeCreative;

    @Parameterized.Parameter(6)
    public StatusBsSynced newStatusBsSynced;

    @Parameterized.Parameter(7)
    public Boolean sameLastChange;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "null -> null",
                        "null",
                        "null",
                        null,
                        null,
                        false,
                        StatusBsSynced.YES,
                        true
                },
                {
                        "null -> imageHash1",
                        "null",
                        "imageHash1",
                        null,
                        BannerLogoStatusModerate.READY,
                        false,
                        StatusBsSynced.NO,
                        false
                },
                {
                        "imageHash1 -> null",
                        "imageHash1",
                        "null",
                        StatusBannerLogoModerate.YES,
                        null,
                        false,
                        StatusBsSynced.NO,
                        false
                },
                {
                        "imageHash1 -> imageHash1",
                        "imageHash1",
                        "imageHash1",
                        StatusBannerLogoModerate.YES,
                        BannerLogoStatusModerate.YES,
                        false,
                        StatusBsSynced.YES,
                        true
                },
                {
                        "imageHash1 -> imageHash1, при перемодерации всего баннера, перемодерируем логотип",
                        "imageHash1",
                        "imageHash1",
                        StatusBannerLogoModerate.YES,
                        BannerLogoStatusModerate.READY,
                        true,
                        StatusBsSynced.NO,
                        false
                },
                {
                        "imageHash1 -> imageHash2",
                        "imageHash1",
                        "imageHash2",
                        StatusBannerLogoModerate.YES,
                        BannerLogoStatusModerate.READY,
                        false,
                        StatusBsSynced.NO,
                        false
                },
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        clientInfo = adGroupInfo.getClientInfo();
        creativeId1 = steps.creativeSteps().getNextCreativeId();
        creativeId2 = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultHtml5Creative(clientInfo, creativeId1);
        steps.creativeSteps().addDefaultHtml5Creative(clientInfo, creativeId2);

        String imageHash1 = steps.bannerSteps().createLogoImageFormat(clientInfo).getImageHash();
        String imageHash2 = steps.bannerSteps().createLogoImageFormat(clientInfo).getImageHash();
        imageHashes = new HashMap<>();
        imageHashes.put("imageHash1", imageHash1);
        imageHashes.put("imageHash2", imageHash2);
        imageHashes.put("null", null);

        someTime = LocalDateTime.now().minusMinutes(7).withNano(0);
    }

    @Test
    public void update() {
        String oldImageHash = imageHashes.get(oldImageHashKey);
        String newImageHash = imageHashes.get(newImageHashKey);

        BannerInfo bannerInfo = steps.bannerSteps().createBanner(
                activeCpmBanner(null, null, creativeId1)
                        .withLogoImageHash(oldImageHash)
                        .withLogoStatusModerate(oldBannerLogoStatusModerate)
                        .withStatusModerate(OldBannerStatusModerate.YES),
                clientInfo);

        Long bannerId = bannerInfo.getBannerId();
        setLastChangeAndBsSynced(bannerId);

        ModelChanges<CpmBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(newImageHash, CpmBanner.LOGO_IMAGE_HASH);

        BannerStatusModerate newBannerStatusModerate = BannerStatusModerate.YES;
        if (changeCreative) {
            modelChanges.process(creativeId2, CpmBanner.CREATIVE_ID);
            newBannerStatusModerate = BannerStatusModerate.READY;
        }

        prepareAndApplyValid(modelChanges);

        CpmBanner actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getLogoImageHash()).isEqualTo(newImageHash);
        assertThat(actualBanner.getLogoStatusModerate()).isEqualTo(newBannerLogoStatusModerate);
        assertThat(actualBanner.getStatusModerate()).isEqualTo(newBannerStatusModerate);
        assertThat(actualBanner.getStatusBsSynced()).isEqualTo(newStatusBsSynced);
        if (sameLastChange) {
            assertThat(actualBanner.getLastChange()).isEqualTo(someTime);
        } else {
            assertThat(actualBanner.getLastChange()).isNotEqualTo(someTime);
        }
    }

    private void setLastChangeAndBsSynced(Long bannerId) {
        dslContextProvider.ppc(1)
                .update(BANNERS)
                .set(BANNERS.LAST_CHANGE, someTime)
                .set(BANNERS.STATUS_BS_SYNCED, BannersStatusbssynced.Yes)
                .where(BANNERS.BID.equal(bannerId))
                .execute();
    }
}
