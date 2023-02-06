package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.sitelink;

import java.util.List;
import java.util.Objects;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.model.ComplexTextBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexUpdateSitelinkDataTest extends ComplexUpdateSitelinkTestBase {

    // в контексте добавления баннеров

    @Test
    public void adGroupWithAddedBannerWithSitelinkSet() {
        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks();
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(1));
        assertThat("добавленные сайтлинки не соответствуют ожидаемым",
                extractDescriptions(actualSitelinkSets.get(0)),
                contains(extractDescriptions(complexBanner.getSitelinkSet()).toArray()));

        assertThat("баннер должен быть прилинкован к соответствующему набору сайтлинков",
                ((TextBanner) actualBanners.get(0)).getSitelinksSetId(),
                equalTo(actualSitelinkSets.get(0).getId()));
    }

    @Test
    public void adGroupWithAddedBannerWithRarefiedSitelinkSet() {
        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks();
        complexBanner.getSitelinkSet().getSitelinks().set(0, null);
        complexBanner.getSitelinkSet().getSitelinks().set(2, null);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(1));
        assertThat("добавленные сайтлинки не соответствуют ожидаемым",
                extractDescriptions(actualSitelinkSets.get(0)),
                contains(extractDescriptions(complexBanner.getSitelinkSet()).toArray()));

        assertThat("баннер должен быть прилинкован к соответствующему набору сайтлинков",
                ((TextBanner) actualBanners.get(0)).getSitelinksSetId(),
                equalTo(actualSitelinkSets.get(0).getId()));
    }

    @Test
    public void adGroupWithTwoAddedBannersWithSitelinkSets() {
        ComplexTextBanner complexBanner1 = bannerWithRandomDescriptionSitelinks();
        ComplexTextBanner complexBanner2 = bannerWithRandomDescriptionSitelinks();
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(complexBanner1, complexBanner2));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(2));
        var actualBanner1 = findBannerByTitleWithAssumption(actualBanners, complexBanner1);
        var actualBanner2 = findBannerByTitleWithAssumption(actualBanners, complexBanner2);

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(2));

        SitelinkSet actualSitelinkSet1 = findSitelinkSetByDesc(actualSitelinkSets, complexBanner1.getSitelinkSet());
        SitelinkSet actualSitelinkSet2 = findSitelinkSetByDesc(actualSitelinkSets, complexBanner2.getSitelinkSet());
        assertThat("добавленные сайтлинки одного из наборов не соответствуют ожидаемым",
                extractDescriptions(actualSitelinkSet1),
                contains(extractDescriptions(complexBanner1.getSitelinkSet()).toArray()));
        assertThat("добавленные сайтлинки одного из наборов не соответствуют ожидаемым",
                extractDescriptions(actualSitelinkSet2),
                contains(extractDescriptions(complexBanner2.getSitelinkSet()).toArray()));

        assertThat("первый баннер должен быть прилинкован к соответствующему набору сайтлинков",
                actualBanner1.getSitelinksSetId(), equalTo(actualSitelinkSet1.getId()));

        assertThat("второй баннер должен быть прилинкован к соответствующему набору сайтлинков",
                actualBanner2.getSitelinksSetId(), equalTo(actualSitelinkSet2.getId()));
    }

    @Test
    public void emptyAdGroupAndAdGroupWithEmptyAddedBannerAndBannerWithSitelinkSet() {
        createSecondAdGroup();

        ComplexTextBanner complexBanner1 = randomTitleTextComplexBanner();
        ComplexTextBanner complexBanner2 = bannerWithRandomDescriptionSitelinks();
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(asList(complexBanner1, complexBanner2));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate1, adGroupForUpdate2);

        var actualBanners = findBanners(adGroupInfo2);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(2));
        var actualBanner1 = findBannerByTitleWithAssumption(actualBanners, complexBanner1);
        var actualBanner2 = findBannerByTitleWithAssumption(actualBanners, complexBanner2);

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(1));
        assertThat("добавленные сайтлинки не соответствуют ожидаемым",
                extractDescriptions(actualSitelinkSets.get(0)),
                contains(extractDescriptions(complexBanner2.getSitelinkSet()).toArray()));

        assertThat("первый баннер не должен быть прилинкован к набору сайтлинков",
                actualBanner1.getSitelinksSetId(), nullValue());
        assertThat("второй баннер должен быть прилинкован к соответствующему набору сайтлинков",
                actualBanner2.getSitelinksSetId(), equalTo(actualSitelinkSets.get(0).getId()));
    }

    // проверка отсутствия влияния sitelinkSetId в контексте добавления баннера

    @Test
    public void adGroupWithAddedBannerWithSitelinkSetId() {
        SitelinkSetInfo existingSitelinkSetInfo = createRandomDescriptionSitelinkSet();
        ComplexTextBanner complexBanner = randomTitleTextComplexBanner();
        complexBanner.getBanner().withSitelinksSetId(existingSitelinkSetInfo.getSitelinkSetId());
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        assertThat("баннер не должен быть прилинкован к набору сайтлинков",
                ((TextBanner) actualBanners.get(0)).getSitelinksSetId(),
                nullValue());
    }

    @Test
    public void adGroupWithAddedBannerWithSitelinkSetAndSitelinkSetId() {
        SitelinkSetInfo existingSitelinkSetInfo = createRandomDescriptionSitelinkSet();
        SitelinkSet addedSitelinkSet = randomDescriptionSitelinkSet();
        ComplexTextBanner complexBanner = randomTitleTextComplexBanner()
                .withSitelinkSet(addedSitelinkSet);
        complexBanner.getBanner().withSitelinksSetId(existingSitelinkSetInfo.getSitelinkSetId());
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(2));
        SitelinkSet actualAddedSitelinkSet = findSitelinkSetByDesc(actualSitelinkSets, addedSitelinkSet);
        assertThat("добавленные сайтлинки не соответствуют ожидаемым",
                extractDescriptions(actualAddedSitelinkSet),
                contains(extractDescriptions(addedSitelinkSet).toArray()));

        assertThat("баннер должен быть прилинкован к вложенному в него набору сайтлинков",
                ((TextBanner) actualBanners.get(0)).getSitelinksSetId(),
                equalTo(actualAddedSitelinkSet.getId()));
    }

    // в контексте обновления баннеров

    @Test
    public void adGroupWithUpdatingBannerSitelinkSetFromNull() {
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks(bannerInfo);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(1));
        assertThat("добавленные сайтлинки не соответствуют ожидаемым",
                extractDescriptions(actualSitelinkSets.get(0)),
                contains(extractDescriptions(complexBanner.getSitelinkSet()).toArray()));

        assertThat("баннер должен быть прилинкован к соответствующему набору сайтлинков",
                ((TextBanner) actualBanners.get(0)).getSitelinksSetId(),
                equalTo(actualSitelinkSets.get(0).getId()));
    }

    @Test
    public void adGroupWithUpdatingBannerSitelinkSetToNull() {
        SitelinkSet sitelinkSet = randomDescriptionSitelinkSet();
        TextBannerInfo bannerInfo = createRandomTitleBanner(adGroupInfo1, sitelinkSet);

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(1));

        assertThat("баннер должен быть отлинкован от набора сайтлинков",
                ((TextBanner) actualBanners.get(0)).getSitelinksSetId(),
                nullValue());
    }

    @Test
    public void adGroupWithUpdatingBannerSitelinkSetFromOneToAnother() {
        SitelinkSet oldSitelinkSet = randomDescriptionSitelinkSet();
        TextBannerInfo bannerInfo = createRandomTitleBanner(adGroupInfo1, oldSitelinkSet);

        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks(bannerInfo);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(2));
        SitelinkSet actualNewSitelinkSet = findSitelinkSetByDesc(actualSitelinkSets, complexBanner.getSitelinkSet());
        assertThat("добавленные сайтлинки не соответствуют ожидаемым",
                extractDescriptions(actualNewSitelinkSet),
                contains(extractDescriptions(complexBanner.getSitelinkSet()).toArray()));

        assertThat("баннер должен быть прилинкован к новому набору сайтлинков",
                ((TextBanner) actualBanners.get(0)).getSitelinksSetId(),
                equalTo(actualNewSitelinkSet.getId()));
    }

    @Test
    public void adGroupWithUpdatingBannerSitelinkSetFromOneToRarefied() {
        SitelinkSet oldSitelinkSet = randomDescriptionSitelinkSet();
        TextBannerInfo bannerInfo = createRandomTitleBanner(adGroupInfo1, oldSitelinkSet);

        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks(bannerInfo);
        complexBanner.getSitelinkSet().getSitelinks().set(0, null);
        complexBanner.getSitelinkSet().getSitelinks().set(2, null);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(2));
        SitelinkSet actualNewSitelinkSet = findSitelinkSetByDesc(actualSitelinkSets, complexBanner.getSitelinkSet());
        assertThat("добавленные сайтлинки не соответствуют ожидаемым",
                extractDescriptions(actualNewSitelinkSet),
                contains(extractDescriptions(complexBanner.getSitelinkSet()).toArray()));

        assertThat("баннер должен быть прилинкован к новому набору сайтлинков",
                ((TextBanner) actualBanners.get(0)).getSitelinksSetId(),
                equalTo(actualNewSitelinkSet.getId()));
    }

    @Test
    public void adGroupWithUpdatingBannerWithUnchangedSitelinkSet() {
        SitelinkSet sitelinkSet = randomDescriptionSitelinkSet();
        TextBannerInfo bannerInfo = createRandomTitleBanner(adGroupInfo1, sitelinkSet);

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo)
                .withSitelinkSet(sitelinkSet);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(1));

        assertThat("баннер должен быть прилинкован к старому набору сайтлинков",
                ((TextBanner) actualBanners.get(0)).getSitelinksSetId(),
                equalTo(actualSitelinkSets.get(0).getId()));
    }

    @Test
    public void adGroupWithUpdatingTwoBannersSitelinkSetsFromOneToAnother() {
        SitelinkSet oldSitelinkSet1 = randomDescriptionSitelinkSet();
        SitelinkSet oldSitelinkSet2 = randomDescriptionSitelinkSet();
        TextBannerInfo bannerInfo1 = createRandomTitleBanner(adGroupInfo1, oldSitelinkSet1);
        TextBannerInfo bannerInfo2 = createRandomTitleBanner(adGroupInfo1, oldSitelinkSet2);

        ComplexTextBanner complexBanner1 = bannerWithRandomDescriptionSitelinks(bannerInfo1);
        ComplexTextBanner complexBanner2 = randomTitleTextComplexBanner(bannerInfo2)
                .withSitelinkSet(oldSitelinkSet1);
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(asList(complexBanner1, complexBanner2));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(2));
        var actualBanner1 = findBannerByTitleWithAssumption(actualBanners, complexBanner1);
        var actualBanner2 = findBannerByTitleWithAssumption(actualBanners, complexBanner2);

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(3));
        SitelinkSet actualNewSitelinkSet = findSitelinkSetByDesc(actualSitelinkSets, complexBanner1.getSitelinkSet());
        SitelinkSet actualOldSitelinkSet1 = findSitelinkSetByDesc(actualSitelinkSets, oldSitelinkSet1);
        assertThat("добавленные сайтлинки не соответствуют ожидаемым",
                extractDescriptions(actualNewSitelinkSet),
                contains(extractDescriptions(complexBanner1.getSitelinkSet()).toArray()));

        assertThat("первый баннер должен быть прилинкован к новому набору сайтлинков",
                actualBanner1.getSitelinksSetId(),
                equalTo(actualNewSitelinkSet.getId()));
        assertThat("второй баннер должен быть прилинкован к новому набору сайтлинков",
                actualBanner2.getSitelinksSetId(),
                equalTo(actualOldSitelinkSet1.getId()));
    }

    @Test
    public void emptyAdGroupAndAdGroupWithEmptyBannerAndBannerWithUpdatedSitelinkSet() {
        createSecondAdGroup();
        TextBannerInfo bannerInfo1 = createRandomTitleTextBanner(adGroupInfo2);
        TextBannerInfo bannerInfo2 = createRandomTitleTextBanner(adGroupInfo2);

        ComplexTextBanner complexBanner1 = randomTitleTextComplexBanner(bannerInfo1);
        ComplexTextBanner complexBanner2 = bannerWithRandomDescriptionSitelinks(bannerInfo2);
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(asList(complexBanner1, complexBanner2));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate1, adGroupForUpdate2);

        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                findBanners(adGroupInfo1), emptyIterable());

        var actualBanners = findBanners(adGroupInfo2);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(2));
        var actualBanner1 = findBannerByTitleWithAssumption(actualBanners, complexBanner1);
        var actualBanner2 = findBannerByTitleWithAssumption(actualBanners, complexBanner2);

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(1));
        assertThat("добавленные сайтлинки не соответствуют ожидаемым",
                extractDescriptions(actualSitelinkSets.get(0)),
                contains(extractDescriptions(complexBanner2.getSitelinkSet()).toArray()));

        assertThat("первый баннер не должен быть прилинкован к набору сайтлинков",
                actualBanner1.getSitelinksSetId(),
                nullValue());
        assertThat("второй баннер должен быть прилинкован к набору сайтлинков",
                actualBanner2.getSitelinksSetId(),
                equalTo(actualSitelinkSets.get(0).getId()));
    }

    // проверка отсутствия влияния sitelinkSetId в контексте обновления баннера

    @Test
    public void adGroupWithUpdatedBannerWithSitelinkSetId() {
        SitelinkSetInfo existingSitelinkSetInfo = createRandomDescriptionSitelinkSet();
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        ComplexTextBanner complexBanner = randomTitleTextComplexBanner(bannerInfo);
        complexBanner.getBanner().withSitelinksSetId(existingSitelinkSetInfo.getSitelinkSetId());
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        assertThat("баннер не должен быть прилинкован к набору сайтлинков",
                ((TextBanner) actualBanners.get(0)).getSitelinksSetId(),
                nullValue());
    }

    @Test
    public void adGroupWithUpdatingBannerSitelinkSetFromNullAndSitelinkSetId() {
        SitelinkSetInfo existingSitelinkSetInfo = createRandomDescriptionSitelinkSet();
        TextBannerInfo bannerInfo = createRandomTitleTextBanner(adGroupInfo1);

        ComplexTextBanner complexBanner = bannerWithRandomDescriptionSitelinks(bannerInfo);
        complexBanner.getBanner().withSitelinksSetId(existingSitelinkSetInfo.getSitelinkSetId());
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(complexBanner));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate);

        var actualBanners = findBanners(adGroupInfo1);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners, hasSize(1));

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(2));
        SitelinkSet actualAddedSitelinkSet = findSitelinkSetByDesc(actualSitelinkSets, complexBanner.getSitelinkSet());
        assertThat("добавленные сайтлинки не соответствуют ожидаемым",
                extractDescriptions(actualAddedSitelinkSet),
                contains(extractDescriptions(complexBanner.getSitelinkSet()).toArray()));

        assertThat("баннер должен быть прилинкован к вложенному в него набору сайтлинков",
                ((TextBanner) actualBanners.get(0)).getSitelinksSetId(),
                equalTo(actualAddedSitelinkSet.getId()));
    }

    // в контексте добавления и обновления баннеров вместе

    @Test
    public void adGroupWithUpdatedBannerSitelinkSetAndAdGroupWithAddedAndUpdatedBannersWithSitelinkSets() {
        createSecondAdGroup();
        TextBannerInfo bannerInfo1 = createRandomTitleTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = createRandomTitleTextBanner(adGroupInfo2);

        ComplexTextBanner updatedComplexBanner1 = bannerWithRandomDescriptionSitelinks(bannerInfo1);
        ComplexTextBanner updatedComplexBanner2 = bannerWithRandomDescriptionSitelinks(bannerInfo2);
        ComplexTextBanner addedComplexBanner = bannerWithRandomDescriptionSitelinks();
        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBanners(singletonList(updatedComplexBanner1));
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBanners(asList(addedComplexBanner, updatedComplexBanner2));

        updateAndCheckResultIsEntirelySuccessful(adGroupForUpdate1, adGroupForUpdate2);

        var actualBanners1 = findBanners(adGroupInfo1);
        var actualBanners2 = findBanners(adGroupInfo2);
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners1, hasSize(1));
        assumeThat("количество баннеров в группе не соответствует ожидаемому",
                actualBanners2, hasSize(2));
        var actualUpdatedBanner1 = findBannerByTitleWithAssumption(actualBanners1, updatedComplexBanner1);
        var actualUpdatedBanner2 = findBannerByTitleWithAssumption(actualBanners2, updatedComplexBanner2);
        var actualAddedBanner = findBannerByTitleWithAssumption(actualBanners2, addedComplexBanner);

        List<SitelinkSet> actualSitelinkSets = findClientSitelinkSets();
        assertThat("количество наборов сайтлинков у клиента не соответствует ожидаемому",
                actualSitelinkSets, hasSize(3));
        SitelinkSet actualSitelinkSet1 =
                findSitelinkSetByDesc(actualSitelinkSets, updatedComplexBanner1.getSitelinkSet());
        SitelinkSet actualSitelinkSet2 =
                findSitelinkSetByDesc(actualSitelinkSets, updatedComplexBanner2.getSitelinkSet());
        SitelinkSet actualSitelinkSet3 = findSitelinkSetByDesc(actualSitelinkSets, addedComplexBanner.getSitelinkSet());

        assertThat("первый обновляемый баннер должен быть прилинкован к соответствующему набору сайтлинков",
                actualUpdatedBanner1.getSitelinksSetId(),
                equalTo(actualSitelinkSet1.getId()));
        assertThat("второй обновляемый баннер должен быть прилинкован к соответствующему набору сайтлинков",
                actualUpdatedBanner2.getSitelinksSetId(),
                equalTo(actualSitelinkSet2.getId()));
        assertThat("добавляемый баннер должен быть прилинкован к соответствующему набору сайтлинков",
                actualAddedBanner.getSitelinksSetId(),
                equalTo(actualSitelinkSet3.getId()));
    }

    private SitelinkSet findSitelinkSetByDesc(List<SitelinkSet> sitelinkSets, SitelinkSet sitelinkSetToFind) {
        String descToFind = filterList(sitelinkSetToFind.getSitelinks(), Objects::nonNull).get(0).getDescription();
        List<SitelinkSet> found = filterList(sitelinkSets,
                sitelinkSet -> sitelinkSet.getSitelinks().get(0).getDescription().equals(descToFind));
        assumeThat("не найден набор сайтлинков с description = " + descToFind,
                found, not(emptyIterable()));
        assumeThat("найдено более одного набора сайтлинков с description = " + descToFind,
                found, hasSize(1));
        return found.get(0);
    }
}
