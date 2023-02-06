package ru.yandex.direct.jobs.banner.language;

import java.util.List;
import java.util.Set;

import one.util.streamex.LongStreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.text.BannerTextExtractor;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannersToFillLanguageQueueRepository;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.queryrec.LanguageRecognizer;
import ru.yandex.direct.queryrec.QueryrecJni;
import ru.yandex.direct.queryrec.QueryrecService;
import ru.yandex.direct.queryrec.UzbekLanguageThresholds;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.regions.Region.UZBEKISTAN_REGION_ID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@JobsTest
@ExtendWith(SpringExtension.class)
class FillClearedBannersLanguageJobTest {

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private BannerRelationsRepository bannerRelationsRepository;

    @Autowired
    private BannerCommonRepository bannerCommonRepository;

    @Autowired
    private BannerTextExtractor bannerTextExtractor;

    @Autowired
    private TestBannersToFillLanguageQueueRepository testBannersToFillLanguageQueueRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    private QueryrecJni queryrecJni;

    private int shard;
    private FillClearedBannersLanguageJob job;
    private PpcProperty<Set<Long>> uzbekLanguageProperty;

    @BeforeEach
    void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        uzbekLanguageProperty = mock(PpcProperty.class);
        doReturn(emptySet()).when(uzbekLanguageProperty).getOrDefault(emptySet());
        PpcProperty<Set<Long>> enableVieLanguageProperty = mock(PpcProperty.class);
        doReturn(emptySet()).when(enableVieLanguageProperty).getOrDefault(emptySet());
        PpcProperty<Set<String>> recognizedLanguagesProperty = mock(PpcProperty.class);
        doReturn(Set.of()).when(recognizedLanguagesProperty).getOrDefault(Set.of());

        UzbekLanguageThresholds thresholds = new UzbekLanguageThresholds(mock(PpcProperty.class),
                mock(PpcProperty.class), mock(PpcProperty.class), mock(PpcProperty.class));

        job = new FillClearedBannersLanguageJob(shard, bannerTypedRepository, bannerRelationsRepository,
                bannerCommonRepository, bannerTextExtractor,
                new QueryrecService(new LanguageRecognizer(), uzbekLanguageProperty, enableVieLanguageProperty,
                        recognizedLanguagesProperty, thresholds, queryrecJni),
                dslContextProvider, testBannersToFillLanguageQueueRepository, clientRepository);
    }

    @Test
    void execute_OneBannerWithRussianText_RuLanguageSet() {
        Long bid = steps.bannerSteps().createBanner(activeTextBanner()
                .withTitle("продам гараж")
                .withBody("дешево")
                .withLanguage(Language.NO)).getBannerId();
        checkLanguage(bid, Language.NO);

        testBannersToFillLanguageQueueRepository.addItems(shard, singleton(bid));
        job.execute();

        checkLanguage(bid, Language.RU_);
        assertThat(testBannersToFillLanguageQueueRepository.peekItems(shard, 1)).isEmpty();
    }

    @Test
    void execute_OneBannerWithRussianText_WithUnknownLanguageInDb_RuLanguageSet() {
        Long bid = steps.bannerSteps().createBanner(activeTextBanner()
                .withTitle("продам гараж")
                .withBody("дешево")
                .withLanguage(Language.UNKNOWN)).getBannerId();
        checkLanguage(bid, Language.UNKNOWN);

        testBannersToFillLanguageQueueRepository.addItems(shard, singleton(bid));
        job.execute();

        checkLanguage(bid, Language.RU_);
        assertThat(testBannersToFillLanguageQueueRepository.peekItems(shard, 1)).isEmpty();
    }

    @Test
    void execute_OneBannerWithRussianText_NoItemsInQueue_LanguageNotSet() {
        Long bid = steps.bannerSteps().createBanner(activeTextBanner()
                .withTitle("продам гараж")
                .withBody("дешево")
                .withLanguage(Language.NO)).getBannerId();
        checkLanguage(bid, Language.NO);

        job.execute();

        checkLanguage(bid, Language.NO);
    }

    @Test
    void execute_OneBannerWithEnglishText_EnLanguageSet() {
        Long bid = steps.bannerSteps().createBanner(activeTextBanner()
                .withLanguage(Language.NO)
                .withTitle("buy fridge")
                .withBody("cheap")).getBannerId();
        checkLanguage(bid, Language.NO);

        testBannersToFillLanguageQueueRepository.addItems(shard, singleton(bid));
        job.execute();

        checkLanguage(bid, Language.EN);
        assertThat(testBannersToFillLanguageQueueRepository.peekItems(shard, 1)).isEmpty();
    }

    @Test
    void execute_OneBannerWithEnglishText_BannerLanguageWasAlreadySet_BannerLanguageNotChanged() {
        Long bid = steps.bannerSteps().createBanner(activeTextBanner()
                .withLanguage(Language.KK)
                .withTitle("buy fridge")
                .withBody("cheap")).getBannerId();
        checkLanguage(bid, Language.KK);

        testBannersToFillLanguageQueueRepository.addItems(shard, singleton(bid));
        job.execute();

        checkLanguage(bid, Language.KK);
        assertThat(testBannersToFillLanguageQueueRepository.peekItems(shard, 1)).isEmpty();
    }

    @Test
    void execute_TwoBanners_LanguageSetForBothBanners() {
        Long bid = steps.bannerSteps().createBanner(activeTextBanner()
                .withLanguage(Language.NO)
                .withTitle("buy fridge")
                .withBody("cheap")).getBannerId();
        Long anotherBid = steps.bannerSteps().createBanner(activeTextBanner()
                .withLanguage(Language.NO)
                .withTitle("buy fridge")
                .withBody("cheap")).getBannerId();

        checkLanguage(bid, Language.NO);
        checkLanguage(anotherBid, Language.NO);

        testBannersToFillLanguageQueueRepository.addItems(shard, asList(bid, anotherBid));
        job.execute();

        checkLanguage(bid, Language.EN);
        checkLanguage(anotherBid, Language.EN);
        assertThat(testBannersToFillLanguageQueueRepository.peekItems(shard, 1)).isEmpty();
    }

    @Test
    void execute_TwoBanners_OnlyOneItemInQueue_LanguageSetForOneBanner() {
        Long bid = steps.bannerSteps().createBanner(activeTextBanner()
                .withLanguage(Language.NO)
                .withTitle("buy fridge")
                .withBody("cheap")).getBannerId();
        Long anotherBid = steps.bannerSteps().createBanner(activeTextBanner()
                .withLanguage(Language.NO)
                .withTitle("buy fridge")
                .withBody("cheap")).getBannerId();

        checkLanguage(bid, Language.NO);
        checkLanguage(anotherBid, Language.NO);

        testBannersToFillLanguageQueueRepository.addItems(shard, singleton(bid));
        job.execute();

        checkLanguage(bid, Language.EN);
        checkLanguage(anotherBid, Language.NO);
        assertThat(testBannersToFillLanguageQueueRepository.peekItems(shard, 1)).isEmpty();
    }

    @Test
    void execute_TwoBannersWithDifferentLanguages_DifferentLanguagesSetForBothBanners() {
        Long bid = steps.bannerSteps().createBanner(activeTextBanner()
                .withTitle("продам гараж")
                .withBody("дешево")
                .withLanguage(Language.NO)).getBannerId();
        Long anotherBid = steps.bannerSteps().createBanner(activeTextBanner()
                .withLanguage(Language.NO)
                .withTitle("buy fridge")
                .withBody("cheap")).getBannerId();

        checkLanguage(bid, Language.NO);
        checkLanguage(anotherBid, Language.NO);

        testBannersToFillLanguageQueueRepository.addItems(shard, asList(bid, anotherBid));
        job.execute();

        checkLanguage(bid, Language.RU_);
        checkLanguage(anotherBid, Language.EN);
        assertThat(testBannersToFillLanguageQueueRepository.peekItems(shard, 1)).isEmpty();
    }

    @Test
    void execute_BannerLastChangeNotChanged() {
        Long bid = steps.bannerSteps().createBanner(activeTextBanner()
                .withTitle("продам гараж")
                .withBody("дешево")
                .withLanguage(Language.NO)).getBannerId();

        var banners = bannerTypedRepository.getStrictlyFullyFilled(shard, singletonList(bid),
                BannerWithSystemFields.class);
        var bannerBefore = banners.get(0);

        testBannersToFillLanguageQueueRepository.addItems(shard, singleton(bid));
        job.execute();

        banners = bannerTypedRepository.getStrictlyFullyFilled(shard, singletonList(bid),
                BannerWithSystemFields.class);
        var bannerAfter = banners.get(0);
        assumeThat(bannerBefore.getLanguage(), not(is(bannerAfter.getLanguage())));

        assertThat(bannerBefore.getLastChange()).isEqualTo(bannerAfter.getLastChange());
    }

    @Test
    void execute_OneBannerWithUzbekText_UzLanguageSet() {
        ClientInfo uzbekClientInfo =
                steps.clientSteps().createClient(defaultClient().withCountryRegionId(UZBEKISTAN_REGION_ID));

        doReturn(Set.of(-1L)).when(uzbekLanguageProperty).getOrDefault(emptySet());

        Long bid = steps.bannerSteps().createBanner(activeTextBanner()
                .withTitle("Менинг ишим эрталаб соат")
                .withBody("тўққиздан бошланади")
                .withLanguage(Language.NO), uzbekClientInfo).getBannerId();
        checkLanguage(bid, Language.NO);

        testBannersToFillLanguageQueueRepository.addItems(shard, singleton(bid));
        job.execute();

        checkLanguage(bid, Language.UZ);
        assertThat(testBannersToFillLanguageQueueRepository.peekItems(shard, 1)).isEmpty();
    }

    @Test
    void execute_BannerWithoutCampaign_NoException() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner()
                .withTitle("продам гараж")
                .withBody("дешево")
                .withLanguage(Language.NO));
        Long bid = bannerInfo.getBannerId();
        checkLanguage(bid, Language.NO);

        testCampaignRepository.deleteCampaign(shard, bannerInfo.getCampaignId());

        testBannersToFillLanguageQueueRepository.addItems(shard, singleton(bid));
        job.execute();

        checkLanguage(bid, Language.RU_);
        assertThat(testBannersToFillLanguageQueueRepository.peekItems(shard, 1)).isEmpty();
    }

    @Test
    void execute_QueueContainsNotTooManyBanners_AllItemsProcessedInOneExecute() {
        List<Long> bids = LongStreamEx.range(0L, 45000L).boxed().toList();

        testBannersToFillLanguageQueueRepository.addItems(shard, bids);
        job.execute();

        assertThat(testBannersToFillLanguageQueueRepository.countItems(shard)).isEqualTo(0);
    }

    @Test
    void execute_QueueContainsTooManyBanners_AlmostAllItemsProcessedInOneExecute() {
        List<Long> bids = LongStreamEx.range(0L, 55000L).boxed().toList();

        testBannersToFillLanguageQueueRepository.addItems(shard, bids);
        job.execute();

        assertThat(testBannersToFillLanguageQueueRepository.countItems(shard)).isEqualTo(5000);
    }

    private void checkLanguage(Long bid, Language language) {
        var banners = bannerTypedRepository.getStrictlyFullyFilled(shard, singletonList(bid),
                BannerWithSystemFields.class);
        assumeThat(banners, hasSize(1));
        var banner = banners.get(0);

        assertThat(banner.getLanguage()).isEqualTo(language);
    }
}
