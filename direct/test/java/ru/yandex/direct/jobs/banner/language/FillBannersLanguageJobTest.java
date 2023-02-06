package ru.yandex.direct.jobs.banner.language;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.text.BannerTextExtractor;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.queryrec.QueryrecService;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.direct.common.db.PpcPropertyNames.FILL_BANNERS_LANGUAGE_JOB_ENABLED;
import static ru.yandex.direct.common.db.PpcPropertyNames.FILL_BANNERS_LANGUAGE_JOB_SLEEP_COEFFICIENT;
import static ru.yandex.direct.common.db.PpcPropertyNames.minBidWithProcessedLanguage;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@JobsTest
@ExtendWith(SpringExtension.class)
class FillBannersLanguageJobTest {

    @Autowired
    private Steps steps;

    @Autowired
    private QueryrecService queryrecService;

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
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private ClientRepository clientRepository;

    private int shard;
    private FillBannersLanguageJob job;

    @BeforeEach
    void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        ppcPropertiesSupport.set(FILL_BANNERS_LANGUAGE_JOB_ENABLED.getName(), String.valueOf(true));
        ppcPropertiesSupport.set(FILL_BANNERS_LANGUAGE_JOB_SLEEP_COEFFICIENT.getName(), String.valueOf(0.5));
        ppcPropertiesSupport.set(minBidWithProcessedLanguage(shard).getName(), String.valueOf(Long.MAX_VALUE));

        job = new FillBannersLanguageJob(shard, bannerTypedRepository, bannerRelationsRepository,
                bannerCommonRepository, bannerTextExtractor, ppcPropertiesSupport, queryrecService,
                dslContextProvider, clientRepository);
    }

    @Test
    void execute_OneBannerWithRussianText_RuLanguageSet() {
        Long bid = steps.bannerSteps().createBanner(activeTextBanner()
                .withTitle("продам гараж")
                .withBody("дешево")
                .withLanguage(Language.NO)).getBannerId();
        checkLanguage(bid, Language.NO);

        job.execute();

        checkLanguage(bid, Language.RU_);
    }

    @Test
    void execute_JobIsOff_LanguageNotSet() {
        ppcPropertiesSupport.set(FILL_BANNERS_LANGUAGE_JOB_ENABLED.getName(), String.valueOf(false));

        Long bid = steps.bannerSteps().createBanner(activeTextBanner().withLanguage(Language.NO)).getBannerId();
        checkLanguage(bid, Language.NO);

        job.execute();

        checkLanguage(bid, Language.NO);
    }

    @Test
    void execute_JobPropertyIsNull_LanguageNotSet() {
        ppcPropertiesSupport.set(FILL_BANNERS_LANGUAGE_JOB_ENABLED.getName(), null);

        Long bid = steps.bannerSteps().createBanner(activeTextBanner().withLanguage(Language.NO)).getBannerId();
        checkLanguage(bid, Language.NO);

        job.execute();

        checkLanguage(bid, Language.NO);
    }

    @Test
    void execute_SleepCoefficientNotSet_LanguageNotSet() {
        ppcPropertiesSupport.set(FILL_BANNERS_LANGUAGE_JOB_SLEEP_COEFFICIENT.getName(), null);

        Long bid = steps.bannerSteps().createBanner(activeTextBanner().withLanguage(Language.NO)).getBannerId();
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

        job.execute();

        checkLanguage(bid, Language.EN);
    }

    @Test
    void execute_OneBannerWithEnglishText_BannerLanguageWasAlreadySet_BannerLanguageNotChanged() {
        Long bid = steps.bannerSteps().createBanner(activeTextBanner()
                .withLanguage(Language.KK)
                .withTitle("buy fridge")
                .withBody("cheap")).getBannerId();
        checkLanguage(bid, Language.KK);

        job.execute();

        checkLanguage(bid, Language.KK);
    }

    @Test
    void execute_TwoBanners_OneIsAlreadyProcessed_LanguageSetOnlyForOneBanner() {
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

        ppcPropertiesSupport.set(minBidWithProcessedLanguage(shard).getName(), String.valueOf(anotherBid));

        job.execute();

        checkLanguage(bid, Language.EN);
        checkLanguage(anotherBid, Language.NO);
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

        ppcPropertiesSupport.set(minBidWithProcessedLanguage(shard).getName(), String.valueOf(anotherBid + 1));

        job.execute();

        checkLanguage(bid, Language.EN);
        checkLanguage(anotherBid, Language.EN);
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

        job.execute();

        checkLanguage(bid, Language.RU_);
        checkLanguage(anotherBid, Language.EN);
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

        job.execute();

        banners = bannerTypedRepository.getStrictlyFullyFilled(shard, singletonList(bid),
                BannerWithSystemFields.class);
        var bannerAfter = banners.get(0);
        assumeThat(bannerBefore.getLanguage(), not(is(bannerAfter.getLanguage())));

        assertThat(bannerBefore.getLastChange()).isEqualTo(bannerAfter.getLastChange());
    }

    private void checkLanguage(Long bid, Language language) {
        var banners = bannerTypedRepository.getStrictlyFullyFilled(shard, singletonList(bid),
                BannerWithSystemFields.class);
        assumeThat(banners, hasSize(1));
        var banner = banners.get(0);

        assertThat(banner.getLanguage()).isEqualTo(language);
    }
}
