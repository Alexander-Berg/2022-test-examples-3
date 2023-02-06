package ru.yandex.direct.core.entity.sitelink;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkRepository;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.entity.sitelink.service.SitelinkSetService;
import ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkSetValidationService;
import ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkValidationService;
import ru.yandex.direct.core.entity.turbolanding.service.TurboLandingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.sitelink.service.validation.SitelinkSetDefects.sitelinkSetInUse;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithItems;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SitelinkSetServiceTest {

    private SitelinkSetService sitelinkSetService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private SitelinkSetRepository sitelinkSetRepository;

    @Autowired
    private SitelinkRepository sitelinkRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    private TurboLandingService turboLandingService;

    @Autowired
    private FeatureService featureService;


    private ClientInfo clientInfo;

    private int shard;

    @Before
    public void before() {
        SitelinkSetValidationService sitelinkSetValidationService = new SitelinkSetValidationService(
                new SitelinkValidationService(),
                turboLandingService,
                sitelinkSetRepository,
                featureService
        );
        sitelinkSetService = new SitelinkSetService(shardHelper,
                sitelinkSetRepository,
                sitelinkRepository,
                sitelinkSetValidationService);

        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
    }

    //delete

    @Test
    public void delete_TwoSitelinkSets() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet(1, 2);
        SitelinkSet sitelinkSet2 = defaultSitelinkSet(3, 4);
        MassResult<Long> result = sitelinkSetService.addSitelinkSetsFull(clientInfo.getClientId(),
                asList(sitelinkSet1, sitelinkSet2));
        List<Long> ids = result.getResult().stream()
                .map(Result::getResult)
                .filter(Objects::nonNull)
                .collect(toList());
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        MassResult<Long> massResult = sitelinkSetService
                .deleteSiteLinkSets(clientInfo.getClientId(), asList(sitelinkSet1.getId(), sitelinkSet2.getId()));
        assertThat(massResult, isSuccessfulWithItems(sitelinkSet1.getId(), sitelinkSet2.getId()));
    }

    @Test
    public void delete_TwoSitelinkSets_OneUsedInBannerValidationError() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet(11, 12);
        SitelinkSet sitelinkSet2 = defaultSitelinkSet(13, 14);
        MassResult<Long> result = sitelinkSetService.addSitelinkSetsFull(clientInfo.getClientId(),
                asList(sitelinkSet1, sitelinkSet2));
        List<Long> ids = mapList(result.getResult(), Result::getResult);
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(sitelinkSet2.getId()), clientInfo);

        MassResult<Long> massResult = sitelinkSetService
                .deleteSiteLinkSets(clientInfo.getClientId(), asList(sitelinkSet1.getId(), sitelinkSet2.getId()));
        assertThat(massResult, isSuccessfulWithMatchers(equalTo(sitelinkSet1.getId()), null));

        assertThat(massResult.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1)), sitelinkSetInUse())));
    }

    @Test
    public void delete_TwoSitelinkSets_OneUsedInBannerNotDeleted() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet(21, 22);
        SitelinkSet sitelinkSet2 = defaultSitelinkSet(23, 24);
        MassResult<Long> result = sitelinkSetService.addSitelinkSetsFull(clientInfo.getClientId(),
                asList(sitelinkSet1, sitelinkSet2));
        List<Long> ids = result.getResult().stream()
                .map(Result::getResult)
                .filter(Objects::nonNull)
                .collect(toList());
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(sitelinkSet1.getId()), clientInfo);

        MassResult<Long> massResult = sitelinkSetService
                .deleteSiteLinkSets(clientInfo.getClientId(), asList(sitelinkSet1.getId(), sitelinkSet2.getId()));
        assertThat(massResult, isSuccessfulWithMatchers(null, equalTo(sitelinkSet2.getId())));

        assertThat(massResult.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), sitelinkSetInUse())));
    }

    @Test
    public void delete_TwoSitelinkSets_OneUsedInDeletedCamp() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet(31, 32);
        SitelinkSet sitelinkSet2 = defaultSitelinkSet(33, 34);
        MassResult<Long> result = sitelinkSetService.addSitelinkSetsFull(clientInfo.getClientId(),
                asList(sitelinkSet1, sitelinkSet2));
        List<Long> ids = result.getResult().stream()
                .map(Result::getResult)
                .filter(Objects::nonNull)
                .collect(toList());
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        TextBannerInfo bannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(sitelinkSet2.getId()), clientInfo);
        testCampaignRepository.setStatusEmpty(shard, bannerInfo.getCampaignId(), Boolean.TRUE);

        MassResult<Long> massResult = sitelinkSetService
                .deleteSiteLinkSets(clientInfo.getClientId(), asList(sitelinkSet1.getId(), sitelinkSet2.getId()));
        assertThat(massResult, isSuccessfulWithMatchers(equalTo(sitelinkSet1.getId()), equalTo(sitelinkSet2.getId())));
    }

    @Test
    public void add_duplicateSitelinkSet_areInserted() {
        MassResult<Long> addNonExistent = sitelinkSetService.addSitelinkSetsFull(clientInfo.getClientId(),
                singletonList(defaultSitelinkSet(1, 2)));
        checkState(addNonExistent.getErrors().isEmpty(), "сайтлинки успешно добавлены в первый раз");

        MassResult<Long> addExistent = sitelinkSetService.addSitelinkSetsFull(clientInfo.getClientId(),
                singletonList(defaultSitelinkSet(1, 2)));
        assertThat(addExistent.getValidationResult().getWarnings(), empty());
    }

    @Test
    public void add_OneSameSitelink_SitelinkIdIsSetToExisting() {
        Sitelink sitelink = defaultSitelink();
        Sitelink sitelink2 = new Sitelink()
                .withTitle(sitelink.getTitle())
                .withHref(sitelink.getHref())
                .withDescription(sitelink.getDescription());
        MassResult<Long> addNewResult = sitelinkSetService.addSitelinkSetsPartial(clientInfo.getClientId(),
                singletonList(new SitelinkSet().withSitelinks(singletonList(sitelink))
                        .withClientId(clientInfo.getClientId().asLong())));

        assumeThat("сайтлинк успешно добавлен в первый раз", addNewResult, isFullySuccessful());
        assumeThat("id в сайтлинке проставлен", sitelink.getId(), notNullValue());

        MassResult<Long> addExisting = sitelinkSetService.addSitelinkSetsPartial(clientInfo.getClientId(),
                singletonList(new SitelinkSet().withSitelinks(singletonList(sitelink2))
                        .withClientId(clientInfo.getClientId().asLong())));

        checkState(addExisting.getValidationResult().hasAnyWarnings(), "повторный add завершился с предупреждением");
        assertThat("одинаковые сайтлинки получили один id", sitelink.getId(), equalTo(sitelink2.getId()));
    }

    @Test
    public void add_OneSameSitelinkSet_SitelinkSetIdIsSetToExisting() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet(101, 102);
        SitelinkSet sitelinkSet2 = new SitelinkSet()
                .withClientId(clientInfo.getClientId().asLong())
                .withSitelinks(sitelinkSet1.getSitelinks());
        MassResult<Long> addNewResult = sitelinkSetService.addSitelinkSetsPartial(clientInfo.getClientId(),
                singletonList(sitelinkSet1));
        assumeThat("сет сайтлинков успешно добавлен в первый раз", addNewResult, isFullySuccessful());
        assumeThat("id в сайтлинке проставлен", sitelinkSet1.getId(), notNullValue());

        MassResult<Long> addExisting = sitelinkSetService
                .addSitelinkSetsPartial(clientInfo.getClientId(), singletonList(sitelinkSet2));
        checkState(addExisting.getValidationResult().hasAnyWarnings(), "повторный add завершился с предупреждением");

        assertThat("одинаковые сайтлинк сеты получили один id", sitelinkSet1.getId(), equalTo(sitelinkSet2.getId()));
    }

    @Test
    public void add_TwoSameSitelinkSets_WarningIsGenerated() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet(201, 202);
        SitelinkSet sitelinkSet2 = defaultSitelinkSet(201, 202);

        MassResult<Long> addDuplicate = sitelinkSetService.addSitelinkSetsPartial(clientInfo.getClientId(),
                asList(sitelinkSet1, sitelinkSet2));

        assertThat(addDuplicate.get(1).getWarnings(), empty());
    }

    @Test
    public void add_TwoSameSitelinkSets_BothHaveTheSameId() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet(201, 202);
        SitelinkSet sitelinkSet2 = defaultSitelinkSet(201, 202);

        MassResult<Long> addDuplicate = sitelinkSetService.addSitelinkSetsPartial(clientInfo.getClientId(),
                asList(sitelinkSet1, sitelinkSet2));

        assertThat(sitelinkSet1.getId(), equalTo(sitelinkSet2.getId()));
    }

    private SitelinkSet defaultSitelinkSet(int... ids) {
        List<Sitelink> sitelinks = Arrays.stream(ids)
                .mapToObj(id -> new Sitelink().withTitle("Sitelink " + id).withHref("http://sitelink.ru/" + id))
                .collect(toList());
        return new SitelinkSet()
                .withClientId(clientInfo.getClientId().asLong())
                .withSitelinks(sitelinks);
    }
}
