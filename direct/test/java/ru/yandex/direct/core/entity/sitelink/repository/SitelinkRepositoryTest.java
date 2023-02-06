package ru.yandex.direct.core.entity.sitelink.repository;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.sitelink.turbolanding.model.SitelinkTurboLanding;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestSitelinkSets.sitelinkSet;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink2;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SitelinkRepositoryTest {

    @Autowired
    private Steps steps;

    @Autowired
    private SitelinkRepository repoUnderTest;

    private int shard;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
    }

    @Test
    public void getSitelinkId() {
        Sitelink sitelink = defaultSitelink();
        repoUnderTest.add(shard, singletonList(sitelink));
        assumeThat("после добавления в модели есть id", sitelink.getId(), greaterThan(0L));

        Long slId = repoUnderTest.getSitelinkId(shard, sitelink);
        assertThat("полученный id сайтлинка совпадает с добавленным", slId, equalTo(sitelink.getId()));
    }

    @Test
    public void getSitelinkIds_OneSitelink() {
        Sitelink sitelink = defaultSitelink();
        repoUnderTest.add(shard, singletonList(sitelink));
        assumeThat("после добавления в модели есть id", sitelink.getId(), greaterThan(0L));

        Map<BigInteger, Long> sitelinkIds =
                repoUnderTest.getSitelinkIdsByHashes(shard, singletonList(sitelink));
        assertThat("полученный id сайтлинка совпадает с добавленным",
                sitelinkIds.values(), contains(sitelink.getId()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getSitelinkIds_TwoSitelinks() {
        Sitelink sitelink1 = defaultSitelink();
        Sitelink sitelink2 = defaultSitelink();
        List<Sitelink> sitelinks = asList(sitelink1, sitelink2);
        repoUnderTest.add(shard, sitelinks);
        List<Long> ids = mapList(sitelinks, Sitelink::getId);
        assumeThat("метод add вернул целые положительные числа", ids, contains(greaterThan(0L), greaterThan(0L)));

        Map<BigInteger, Long> sitelinkIds =
                repoUnderTest.getSitelinkIdsByHashes(shard, asList(sitelink1, sitelink2));
        assertThat("полученный id сайтлинка совпадает с добавленным",
                sitelinkIds.values(),
                containsInAnyOrder(equalTo(sitelink1.getId()), equalTo(sitelink2.getId())));
    }

    @Test
    public void getSitelinkIdsEmptyList() {
        Map<BigInteger, Long> sitelinkIds = repoUnderTest.getSitelinkIdsByHashes(shard, emptyList());
        assertThat("полученный id сайтлинка совпадает с добавленным",
                sitelinkIds.values(), empty());
    }

    @Test
    public void getSitelinks_OneSitelink() {
        Sitelink sitelink = defaultSitelink();
        repoUnderTest.add(shard, singletonList(sitelink));
        assumeThat("после добавления в модели есть id", sitelink.getId(), greaterThan(0L));

        Collection<Sitelink> actualSitelinks = repoUnderTest.get(shard, singletonList(sitelink.getId()));
        assertThat("полученный сайтлинк соответстует ожиданию", actualSitelinks, contains(beanDiffer(sitelink)));
    }

    @Test
    public void getSitelinks_emptySitelinkIds() {
        Collection<Sitelink> actualSitelinks = repoUnderTest.get(shard, emptyList());
        assertThat("должна быть пустая коллекция сайтлинков", actualSitelinks, empty());
    }

    @Test
    public void getSitelinks_SitelinkWithTurboLanding_TurboLandingFetched() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        SitelinkTurboLanding turboLanding =
                steps.turboLandingSteps().createDefaultSitelinkTurboLanding(client.getClientId());

        Sitelink sitelink = defaultSitelink().withTurboLandingId(turboLanding.getId());
        SitelinkSet sitelinkSet = sitelinkSet(client.getClientId(), singletonList(sitelink));
        steps.sitelinkSetSteps().createSitelinkSet(sitelinkSet, client);

        Sitelink actualSitelink = repoUnderTest.get(shard, singletonList(sitelink.getId())).get(0);
        assertThat(actualSitelink.getTurboLandingId(), equalTo(turboLanding.getId()));
    }

    //delete

    @Test
    public void deleteByIds_OneSitelink() {
        Sitelink sitelink = defaultSitelink();
        repoUnderTest.add(shard, singletonList(sitelink));
        assumeThat("после добавления в модели есть id", sitelink.getId(), greaterThan(0L));

        repoUnderTest.delete(shard, singletonList(sitelink.getId()));
        Collection<Sitelink> actualSitelinks = repoUnderTest.get(shard, singletonList(sitelink.getId()));
        assertThat("сайтлинк удален", actualSitelinks, empty());
    }

    @Test
    public void deleteByIds_emptySitelinkIds() {
        Sitelink sitelink = defaultSitelink();
        repoUnderTest.add(shard, singletonList(sitelink));
        assumeThat("после добавления в модели есть id", sitelink.getId(), greaterThan(0L));

        repoUnderTest.delete(shard, emptyList());
        Collection<Sitelink> actualSitelinks = repoUnderTest.get(shard, singletonList(sitelink.getId()));
        assertThat("сайтлинк не был удален", actualSitelinks, contains(beanDiffer(sitelink)));
    }

    // add

    @Test
    public void add_OneSitelink() {
        Sitelink sitelink = defaultSitelink();
        List<Sitelink> sitelinks = singletonList(sitelink);
        repoUnderTest.add(shard, sitelinks);
        List<Long> ids = mapList(sitelinks, Sitelink::getId);
        assertThat("метод add вернул целое положительное число", ids, contains(greaterThan(0L)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void add_TwoSitelinks() {
        Sitelink sitelink1 = defaultSitelink();
        Sitelink sitelink2 = defaultSitelink2();
        List<Sitelink> sitelinks = asList(sitelink1, sitelink2);
        repoUnderTest.add(shard, sitelinks);
        List<Long> ids = mapList(sitelinks, Sitelink::getId);
        assertThat("метод add вернул целые положительные числа", ids, contains(greaterThan(0L), greaterThan(0L)));
    }

    @Test
    public void add_Sitelink_SetModelId() {
        Sitelink sitelink = defaultSitelink();
        List<Sitelink> sitelinks = singletonList(sitelink);
        repoUnderTest.add(shard, sitelinks);
        List<Long> ids = mapList(sitelinks, Sitelink::getId);
        assumeThat("метод add вернул целое положительное число", ids, contains(greaterThan(0L)));

        assertThat("после добавления в модели есть id", sitelink.getId(), greaterThan(0L));
    }

    @Test
    public void add_Sitelink_SaveDataCorrectly() {
        Sitelink sitelink = defaultSitelink();
        repoUnderTest.add(shard, singletonList(sitelink));

        Collection<Sitelink> savedSitelink =
                repoUnderTest.get(shard, Collections.singletonList(sitelink.getId()));
        assertThat("данные извлеченного сайтлинка не соответствуют данным ранее сохраненной",
                savedSitelink, contains(beanDiffer(sitelink)));
    }
}
