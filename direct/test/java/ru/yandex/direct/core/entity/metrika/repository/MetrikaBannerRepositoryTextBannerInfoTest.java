package ru.yandex.direct.core.entity.metrika.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerImageOpts;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.metrika.model.objectinfo.BannerInfoForMetrika;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerImageRepository;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES;
import static ru.yandex.direct.multitype.entity.LimitOffset.limited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class MetrikaBannerRepositoryTextBannerInfoTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private MetrikaBannerRepository repoUnderTest;

    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private TestBannerImageRepository testBannerImageRepository;

    @Autowired
    private OldBannerRepository bannerRepository;

    private LocalDateTime someTime;

    private int shard;
    private long bannerId1;
    private long bannerId2;
    private long bannerId3;
    private long bannerId4;
    private List<Long> allBannerIds;

    @Before
    public void before() {
        TextBannerInfo bannerInfo1 = bannerSteps.createActiveTextBanner();
        TextBannerInfo bannerInfo2 = bannerSteps.createActiveTextBanner();
        TextBannerInfo bannerInfo3 = bannerSteps.createActiveTextBanner();
        TextBannerInfo bannerInfo4 = bannerSteps.createActiveTextBanner();

        bannerId1 = bannerInfo1.getBannerId();
        bannerId2 = bannerInfo2.getBannerId();
        bannerId3 = bannerInfo3.getBannerId();
        bannerId4 = bannerInfo4.getBannerId();

        shard = bannerInfo1.getShard();

        someTime = LocalDateTime.now().minusMinutes(2).withNano(0);

        /*
            Выставляем баннерам BannerID и время последнего изменения
         */
        updateBannerLastChange(shard, bannerInfo1.getBannerId(), someTime.plusSeconds(1));
        updateBannerLastChange(shard, bannerInfo2.getBannerId(), someTime);
        updateBannerLastChange(shard, bannerInfo3.getBannerId(), someTime);
        updateBannerLastChange(shard, bannerInfo4.getBannerId(), someTime.minusSeconds(1));

        allBannerIds = asList(bannerId1, bannerId2, bannerId3, bannerId4);
    }

    @Test
    public void getBannersInfo_Short_LimitIsNull_LimitWorksFine() {
        List<BannerInfoForMetrika> bannerInfoList =
                repoUnderTest.getBannersInfo(shard, null);
        List<Long> fetchedIds = mapList(bannerInfoList, BannerInfoForMetrika::getBannerId);

        checkState(bannerId4 <= ObjectInfoConstants.DEFAULT_LIMIT,
                "невозможно провести тест, так как созданные баннеры не попадают в лимит");

        boolean allBannersExists = fetchedIds.containsAll(allBannerIds);
        assertThat("в ответе метода должны присутствовать все созданные в тесте баннеры",
                allBannersExists, is(true));
    }

    @Test
    public void getBannersInfo_Short_LimitIsDefined_LimitWorksFine() {
        List<BannerInfoForMetrika> bannerInfoList =
                repoUnderTest.getBannersInfo(shard, limited(3));

        assertThat("в ответе метода должно присутствовать равное лимиту количество баннеров",
                bannerInfoList, hasSize(3));
    }

    @Test
    public void getBannersInfo_LimitIsNull_LimitWorksFine() {
        List<BannerInfoForMetrika> bannerInfoList =
                repoUnderTest.getBannersInfo(shard, someTime.minusSeconds(1), 0L, null);
        List<Long> fetchedIds = mapList(bannerInfoList, BannerInfoForMetrika::getBannerId);

        boolean allBannersExists = fetchedIds.containsAll(allBannerIds);
        assertThat("в ответе метода должны присутствовать все созданные в тесте баннеры",
                allBannersExists, is(true));
    }

    @Test
    public void getBannersInfo_LimitIsDefined_LimitWorksFine() {
        List<BannerInfoForMetrika> bannerInfoList =
                repoUnderTest.getBannersInfo(shard, someTime.minusSeconds(1), 0L, limited(3));

        assertThat("в ответе метода должно присутствовать равное лимиту количество баннеров",
                bannerInfoList, hasSize(3));
    }

    @Test
    public void getBannersInfo_ResponseContainSuitableBanners() {
        List<BannerInfoForMetrika> bannerInfoList =
                repoUnderTest.getBannersInfo(shard, someTime, bannerId3, null);
        List<Long> fetchedIds = mapList(bannerInfoList, BannerInfoForMetrika::getBannerId);

        boolean suitableBannersExists = fetchedIds
                .containsAll(asList(bannerId1, bannerId3));
        assertThat("в ответе метода должны присутствовать баннеры, "
                        + "измененные после указанного времени или измененные в то же время, "
                        + "но имеющие больший bannerId, чем переданный",
                suitableBannersExists, is(true));
    }

    @Test
    public void getBannersInfo_ResponseDoesNotContainBannersWithLessLastChange() {
        List<BannerInfoForMetrika> bannerInfoList =
                repoUnderTest.getBannersInfo(shard, someTime, bannerId3, null);
        List<Long> fetchedIds = mapList(bannerInfoList, BannerInfoForMetrika::getBannerId);

        boolean unsuitableBannersExists = fetchedIds.contains(bannerId4);
        assertThat("в ответе метода не должно присутствовать баннеров, "
                        + "измененных до указанного времени",
                unsuitableBannersExists, is(false));
    }

    @Test
    public void getBannersInfo_ResponseDoesNotContainBannersWithEqualLastChangeAndLessId() {
        List<BannerInfoForMetrika> bannerInfoList =
                repoUnderTest.getBannersInfo(shard, someTime, bannerId3, null);
        List<Long> fetchedIds = mapList(bannerInfoList, BannerInfoForMetrika::getBannerId);

        boolean unsuitableBannersExists = fetchedIds.contains(bannerId2);
        assertThat("в ответе метода не должно присутствовать баннеров, "
                        + "измененных в указанное время, но имеющих id меньше указанного",
                unsuitableBannersExists, is(false));
    }

    @Test
    public void getBannersInfo_ResponseDoesNotContainBannersWithTooLateLastChange() {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.LAST_CHANGE, LocalDateTime.now().minusSeconds(27))
                .where(BANNERS.BID.equal(bannerId3))
                .execute();

        List<BannerInfoForMetrika> bannerInfoList =
                repoUnderTest.getBannersInfo(shard, someTime, bannerId2, null);
        List<Long> fetchedIds = mapList(bannerInfoList, BannerInfoForMetrika::getBsBannerId);

        boolean unsuitableBannersExists = fetchedIds.contains(bannerId3);
        assertThat("в ответе метода не должно присутствовать баннеров, измененных за последние 30 секунд",
                unsuitableBannersExists, is(false));
    }

    @Test
    public void getBannersInfo_ResponseDoesNotContainBannersWithoutBannerId() {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.BANNER_ID, 0L)
                .where(BANNERS.BID.equal(bannerId3))
                .execute();

        List<BannerInfoForMetrika> bannerInfoList =
                repoUnderTest.getBannersInfo(shard, null);
        List<Long> fetchedIds = mapList(bannerInfoList, BannerInfoForMetrika::getBannerId);

        boolean unsuitableBannersExists = fetchedIds.contains(bannerId3);
        assertThat("в ответе метода не должно присутствовать баннеров без BannerID",
                unsuitableBannersExists, is(false));
    }

    public Object testDataForImageBanner() {
        return new Object[]{
                new Object[]{Set.of(BannerImageOpts.SINGLE_AD_TO_BS), false},
                new Object[]{emptySet(), true},
                new Object[]{null, true},
        };
    }

    @Test
    @TestCaseName("{0} -> contains image banner: {1}")
    @Parameters(method = "testDataForImageBanner")
    public void getBannersInfo_WithImageBanner(
            Set<BannerImageOpts> bannerImageOpts,
            Boolean expectImageBanner
    ) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        var adGroupId = adGroupInfo.getAdGroupId();
        var campaignId = adGroupInfo.getCampaignId();
        var shard = adGroupInfo.getShard();

        var textBanner = activeTextBanner(campaignId, adGroupId)
                .withBannerImage(defaultBannerImage());
        bannerRepository.addBanners(shard, singletonList(textBanner));
        if (bannerImageOpts != null) {
            testBannerImageRepository.updateImageOpts(shard, textBanner.getId(), bannerImageOpts);
        }
        updateBannerLastChange(shard, textBanner.getId(), someTime);
        dslContextProvider.ppc(shard)
                .update(BANNER_IMAGES)
                .set(BANNER_IMAGES.DATE_ADDED, someTime)
                .where(BANNER_IMAGES.BID.equal(textBanner.getId()))
                .execute();

        var imageBanner = testBannerImageRepository.getBannerImage(shard, textBanner.getId());
        assumeThat(imageBanner, notNullValue());

        List<BannerInfoForMetrika> bannerInfoList = repoUnderTest.getBannersInfo(shard, null);
        var sortIdToBid = StreamEx.of(bannerInfoList)
                .mapToEntry(BannerInfoForMetrika::getSortId, BannerInfoForMetrika::getBannerId)
                .toMap();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(sortIdToBid)
                .as("присутствие текстового баннера в ответе метода")
                .containsEntry(imageBanner.getBannerId(), imageBanner.getBannerId());
        if (expectImageBanner) {
            soft.assertThat(sortIdToBid)
                    .as("присутствие image баннера в ответе метода")
                    .containsEntry(imageBanner.getId(), imageBanner.getBannerId());
        } else {
            soft.assertThat(sortIdToBid)
                    .as("отсутствие image баннера в ответе метода")
                    .doesNotContainEntry(imageBanner.getId(), imageBanner.getBannerId());
        }
        soft.assertAll();
    }

    private void updateBannerLastChange(int shard, Long bid, LocalDateTime dateTime) {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.LAST_CHANGE, dateTime)
                .where(BANNERS.BID.equal(bid))
                .execute();
    }

    private OldBannerImage defaultBannerImage() {
        return new OldBannerImage()
                .withImageHash(random(5, true, true))
                .withBsBannerId(Long.valueOf(randomNumeric(5)))
                .withStatusModerate(OldStatusBannerImageModerate.YES)
                .withStatusShow(true)
                .withDateAdded(LocalDateTime.now());
    }
}
