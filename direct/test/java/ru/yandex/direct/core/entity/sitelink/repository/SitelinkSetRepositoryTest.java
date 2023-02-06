package ru.yandex.direct.core.entity.sitelink.repository;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink2;
import static ru.yandex.direct.multitype.entity.LimitOffset.limited;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SitelinkSetRepositoryTest {

    @Autowired
    private Steps steps;

    @Autowired
    private SitelinkSetRepository repoUnderTest;

    @Autowired
    private SitelinkRepository sitelinkRepository;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    private ClientInfo clientInfo;

    private int shard;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
    }

    @Test
    public void getSitelinkSetIds_TwoSitelinkSet() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet().withSitelinks(singletonList(defaultSitelink2()));
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet1, sitelinkSet2).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet1, sitelinkSet2);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        Collection<Long> setIds = repoUnderTest.getSitelinkSetIds(shard, asList(sitelinkSet1, sitelinkSet2));
        assertThat("полученные id сайтлинков соответствуют ожиданию", setIds,
                containsInAnyOrder(sitelinkSet1.getId(), sitelinkSet2.getId()));
    }

    @Test
    public void getSitelinkSetIdsMap_TwoSitelinkSet() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet().withSitelinks(singletonList(defaultSitelink2()));
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet1, sitelinkSet2).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet1, sitelinkSet2);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        Map<BigInteger, Long> sitelinkSetIds = repoUnderTest
                .getSitelinkSetIdsByHashes(shard, clientInfo.getClientId(), asList(sitelinkSet1, sitelinkSet2));

        assertThat("полученные id сайтлинков соответствуют ожиданию", sitelinkSetIds.values(),
                containsInAnyOrder(sitelinkSet1.getId(), sitelinkSet2.getId()));
    }

    @Test
    public void getSitelinksBySetIds_OneSitelinkSet() {
        SitelinkSet sitelinkSet = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = singletonList(sitelinkSet);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 1, "должен вернутся 1 идентификатор");

        Multimap<Long, Sitelink> slSetIdToSitelinks =
                repoUnderTest.getSitelinksBySetIds(shard, singletonList(sitelinkSet.getId()));
        assertThat("полученные сайтлинки соответствуют ожиданию", slSetIdToSitelinks,
                beanDiffer(getSitelinkMultiMap(singletonList(sitelinkSet))));
    }

    @Test
    public void getSitelinksBySetIds_TwoSitelinkSets() {
        SitelinkSet sitelinkSet = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet().withSitelinks(
                asList(defaultSitelink().withTitle("firstTitle").withOrderNum(0L),
                        defaultSitelink2().withTitle("secondTitle").withOrderNum(1L),
                        defaultSitelink().withTitle("thirdTitle").withOrderNum(2L),
                        defaultSitelink2().withTitle("forthTitle").withOrderNum(3L)));
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet, sitelinkSet2).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet, sitelinkSet2);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        Multimap<Long, Sitelink> slSetIdToSitelinks =
                repoUnderTest.getSitelinksBySetIds(shard, asList(sitelinkSet.getId(), sitelinkSet2.getId()));
        assertThat("полученные сайтлинки соответствуют ожиданию", slSetIdToSitelinks,
                beanDiffer(getSitelinkMultiMap(asList(sitelinkSet, sitelinkSet2))));
    }

    @Test
    public void get_OneSitelinkSet() {
        SitelinkSet sitelinkSet = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = singletonList(sitelinkSet);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 1, "должен вернутся 1 идентификатор");

        Collection<SitelinkSet> resultSitelinkSets = repoUnderTest.get(shard,
                clientInfo.getClientId(),
                singletonList(sitelinkSet.getId()));
        assertThat("полученные сайтлинк сет соответствуют ожиданию", resultSitelinkSets,
                contains(beanDiffer(sitelinkSet)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void get_TwoSitelinkSets() {
        SitelinkSet sitelinkSet = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet().withSitelinks(
                asList(defaultSitelink().withTitle("firstTitleSl").withOrderNum(0L),
                        defaultSitelink2().withTitle("secondTitleSl").withOrderNum(1L),
                        defaultSitelink().withTitle("thirdTitleSl").withOrderNum(2L),
                        defaultSitelink2().withTitle("forthTitleSl").withOrderNum(3L)));
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet, sitelinkSet2).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet, sitelinkSet2);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        Collection<SitelinkSet> resultSitelinkSets = repoUnderTest.get(shard,
                clientInfo.getClientId(),
                asList(sitelinkSet.getId(), sitelinkSet2.getId()));
        assertThat("полученные сайтлинк сеты соответствуют ожиданию", resultSitelinkSets,
                containsInAnyOrder(beanDiffer(sitelinkSet), beanDiffer(sitelinkSet2)));
    }

    @Test
    public void get_idNotExists() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet1, sitelinkSet2).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet1, sitelinkSet2);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);

        List<SitelinkSet> result = repoUnderTest.get(shard, clientInfo.getClientId(),
                asList(ids.get(0), ids.get(1) + 1));

        assertThat(result, contains(beanDiffer(sitelinkSet1)));
    }

    @Test
    public void getIdsByClientId_success() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet();

        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet1).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = singletonList(sitelinkSet1);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);

        List<Long> received = repoUnderTest.getIdsByClientId(shard, clientInfo.getClientId(), limited(1000));
        assertThat(received, contains(ids.get(0)));
    }

    @Test
    public void getIdsByClientId_withOffset() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet();
        SitelinkSet sitelinkSet3 = defaultSitelinkSet();

        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet1, sitelinkSet2, sitelinkSet3)
                .toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet1, sitelinkSet2, sitelinkSet3);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);

        List<Long> received = repoUnderTest.getIdsByClientId(shard, clientInfo.getClientId(), limited(1000, 1));
        assertThat(received, contains(ids.get(1), ids.get(2)));
    }

    @Test
    public void getSitelinkSetIdsMapUsed_TwoSitelinkSets_OneUsedInBanner() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet1, sitelinkSet2).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet1, sitelinkSet2);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(sitelinkSet2.getId()), clientInfo);

        Map<Long, Boolean> expectedMap = new HashMap<>();
        expectedMap.put(sitelinkSet1.getId(), Boolean.FALSE);
        expectedMap.put(sitelinkSet2.getId(), Boolean.TRUE);
        Map<Long, Boolean> slSetToUsed = repoUnderTest.getSitelinkSetIdsMapUsed(shard, clientInfo.getClientId(),
                asList(sitelinkSet1.getId(), sitelinkSet2.getId()));
        assertThat("полученный результат соответствует ожиданию", slSetToUsed, beanDiffer(expectedMap));
    }

    @Test
    public void getSitelinkSetIdsMapUsed_TwoSitelinkSets_OneUsedInDeletedCamp() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet1, sitelinkSet2).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet1, sitelinkSet2);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        TextBannerInfo bannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(sitelinkSet2.getId()), clientInfo);
        testCampaignRepository.setStatusEmpty(shard, bannerInfo.getCampaignId(), Boolean.TRUE);

        Map<Long, Boolean> expectedMap = new HashMap<>();
        expectedMap.put(sitelinkSet1.getId(), Boolean.FALSE);
        expectedMap.put(sitelinkSet2.getId(), Boolean.FALSE);
        Map<Long, Boolean> slSetToUsed = repoUnderTest.getSitelinkSetIdsMapUsed(shard, clientInfo.getClientId(),
                asList(sitelinkSet1.getId(), sitelinkSet2.getId()));
        assertThat("полученный результат соответствует ожиданию", slSetToUsed, beanDiffer(expectedMap));
    }

    // add

    @Test
    public void add_OneSitelinkSet() {
        SitelinkSet sitelinkSet = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = singletonList(sitelinkSet);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        assertThat("метод add вернул целое положительное число", ids, contains(greaterThan(0L)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void add_TwoSitelinksSets() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet().withSitelinks(
                singletonList(defaultSitelink().withTitle("firstTitleTwoSl")));

        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet1, sitelinkSet2).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet1, sitelinkSet2);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        assertThat("метод add вернул целые положительные числа", ids, contains(greaterThan(0L), greaterThan(0L)));
    }

    @Test
    public void add_SitelinkSet_SetModelId() {
        SitelinkSet sitelinkSet = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = singletonList(sitelinkSet);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 1, "должен вернутся 1 идентификатор");

        assertThat("репозиторий должен проставить в модели положительный id", sitelinkSet.getId(), greaterThan(0L));
    }

    @Test
    public void add_SitelinkSet_SaveDataCorrectly() {
        SitelinkSet sitelinkSet = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet).toFlatList(SitelinkSet::getSitelinks));
        repoUnderTest.add(shard, singletonList(sitelinkSet));

        List<SitelinkSet> savedSitelinkSet = repoUnderTest.get(shard,
                clientInfo.getClientId(),
                Collections.singletonList(sitelinkSet.getId()));
        assertThat("данные сайтлинк сета не соответствуют данным ранее сохраненному",
                savedSitelinkSet, contains(beanDiffer(sitelinkSet)));
    }

    //delete
    @Test
    public void delete_OneSitelinkSet() {
        SitelinkSet sitelinkSet = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = singletonList(sitelinkSet);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 1, "должен вернутся 1 идентификатор");

        Collection<Long> deletedSiteLinkSets =
                repoUnderTest.delete(shard, clientInfo.getClientId(), singletonList(sitelinkSet.getId()));
        assertThat("метод delete вернул ид удаленного сайтлинк сета", deletedSiteLinkSets,
                contains(equalTo(sitelinkSet.getId())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void delete_TwoSitelinkSets() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet1, sitelinkSet2).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet1, sitelinkSet2);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        Collection<Long> deletedSiteLinkSets =
                repoUnderTest
                        .delete(shard, clientInfo.getClientId(), asList(sitelinkSet1.getId(), sitelinkSet2.getId()));
        assertThat("метод delete вернул ид удаленных сайтлинк сетов", deletedSiteLinkSets,
                contains(equalTo(sitelinkSet1.getId()), equalTo(sitelinkSet2.getId())));
    }

    @Test
    public void delete_TwoSitelinkSets_OneUsedInBanner() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet1, sitelinkSet2).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet1, sitelinkSet2);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(sitelinkSet2.getId()), clientInfo);

        Collection<Long> deletedSiteLinkSets = repoUnderTest.delete(
                shard,
                clientInfo.getClientId(),
                asList(sitelinkSet1.getId(), sitelinkSet2.getId()));
        assertThat("метод delete вернул ид удаленных сайтлинк сетов", deletedSiteLinkSets,
                contains(equalTo(sitelinkSet1.getId())));
    }

    @Test
    public void delete_TwoSitelinkSets_OneUsedInBanner_DeletedCorrectly() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet1, sitelinkSet2).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet1, sitelinkSet2);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(sitelinkSet2.getId()), clientInfo);

        Collection<Long> deletedSiteLinkSets =
                repoUnderTest
                        .delete(shard, clientInfo.getClientId(), asList(sitelinkSet1.getId(), sitelinkSet2.getId()));
        checkState(deletedSiteLinkSets.size() == 1 && deletedSiteLinkSets.contains(sitelinkSet1.getId()),
                "метод delete должен вернуть ид удаленного сета");

        List<SitelinkSet> sitelinkSetsInDB =
                repoUnderTest.get(shard, clientInfo.getClientId(), asList(sitelinkSet1.getId(), sitelinkSet2.getId()));
        assertThat("в базе остался один сайтлик сет", sitelinkSetsInDB, contains(beanDiffer(sitelinkSet2)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void delete_TwoSitelinkSets_OneUsedInDeletedCamp() {
        SitelinkSet sitelinkSet1 = defaultSitelinkSet();
        SitelinkSet sitelinkSet2 = defaultSitelinkSet();
        sitelinkRepository.add(shard, StreamEx.of(sitelinkSet1, sitelinkSet2).toFlatList(SitelinkSet::getSitelinks));
        List<SitelinkSet> sitelinkSets = asList(sitelinkSet1, sitelinkSet2);
        repoUnderTest.add(shard, sitelinkSets);
        List<Long> ids = mapList(sitelinkSets, SitelinkSet::getId);
        checkState(ids.size() == 2, "должно вернутся 2 идентификатора");

        TextBannerInfo bannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(sitelinkSet2.getId()), clientInfo);
        testCampaignRepository.setStatusEmpty(shard, bannerInfo.getCampaignId(), Boolean.TRUE);

        Collection<Long> deletedSiteLinkSets =
                repoUnderTest
                        .delete(shard, clientInfo.getClientId(), asList(sitelinkSet1.getId(), sitelinkSet2.getId()));
        assertThat("метод delete вернул ид удаленных сайтлинк сетов", deletedSiteLinkSets,
                contains(equalTo(sitelinkSet1.getId()), equalTo(sitelinkSet2.getId())));
    }

    private Multimap<Long, Sitelink> getSitelinkMultiMap(List<SitelinkSet> sitelinkSets) {
        Multimap<Long, Sitelink> slSetIdToSitelinks = MultimapBuilder.hashKeys().arrayListValues().build();
        sitelinkSets.forEach(sitelinkSet -> slSetIdToSitelinks.putAll(sitelinkSet.getId(), sitelinkSet.getSitelinks()));
        return slSetIdToSitelinks;
    }

    private SitelinkSet defaultSitelinkSet() {
        return new SitelinkSet()
                .withClientId(clientInfo.getClientId().asLong())
                .withSitelinks(asList(defaultSitelink().withOrderNum(0L), defaultSitelink().withOrderNum(1L)));
    }
}
