package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.bidmodifier;

import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideo;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexUpdateBidModifierDataTest extends ComplexUpdateBidModifierTestBase {

    private static final CompareStrategy MOBILE_STRATEGY =
            onlyFields(newPath("type"), newPath("mobileAdjustment", "osType"), newPath("mobileAdjustment", "percent"));

    private static final CompareStrategy VIDEO_STRATEGY =
            onlyFields(newPath("type"), newPath("videoAdjustment", "percent"));

    private static final CompareStrategy DEMOGRAPHICS_STRATEGY =
            onlyFields(newPath("type"), newPath("demographicsAdjustments", "0", "percent"));

    // добавление

    @Test
    public void adGroupWithAddedMobileModifier() {
        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobile();
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, hasSize(1));
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                findMobileBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getMobileModifier()).useCompareStrategy(MOBILE_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(1));
    }

    @Test
    public void adGroupWithAddedMobileModifierAndDemographicsModifier() {
        createSecondAdGroup();

        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobileAndDemographics();
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, hasSize(2));
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                findMobileBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getMobileModifier()).useCompareStrategy(MOBILE_STRATEGY));
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                findDemographicsBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getDemographyModifier()).useCompareStrategy(DEMOGRAPHICS_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(2));
    }

    // обновление

    @Test
    public void adGroupWithUpdatedMobileModifier() {
        createBidModifierMobile(adGroupInfo1);

        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobile();
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, hasSize(1));
        assertThat("данные обновленной корректировки не соответствуют ожидаемым",
                findMobileBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getMobileModifier()).useCompareStrategy(MOBILE_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(1));
    }

    @Test
    public void adGroupWithUpdatedMobileModifierToTheSameValue() {
        AdGroupBidModifierInfo bidModifierInfo = createBidModifierMobile(adGroupInfo1);

        ComplexBidModifier complexBidModifier = new ComplexBidModifier()
                .withMobileModifier((BidModifierMobile) bidModifierInfo.getBidModifier());
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, hasSize(1));
        assertThat("данные обновленной корректировки не соответствуют ожидаемым",
                findMobileBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getMobileModifier()).useCompareStrategy(MOBILE_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(1));
    }

    @Test
    public void adGroupWithUpdatedMobileAndDemographyModifier() {
        createBidModifierMobile(adGroupInfo1);
        createBidModifierDemographics(adGroupInfo1);

        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobileAndDemographics();
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, hasSize(2));
        assertThat("данные обновленной корректировки не соответствуют ожидаемым",
                findMobileBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getMobileModifier()).useCompareStrategy(MOBILE_STRATEGY));
        assertThat("данные обновленной корректировки не соответствуют ожидаемым",
                findDemographicsBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getDemographyModifier()).useCompareStrategy(DEMOGRAPHICS_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(2));
    }

    // удаление

    @Test
    public void adGroupWithDeletedMobileModifier() {
        createBidModifierMobile(adGroupInfo1);

        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, emptyIterable());

        assertThat(getClientBidModifiersCount(), equalTo(0));
    }

    @Test
    public void adGroupWithDeletedMobileAndDemographyModifier() {
        createBidModifierMobile(adGroupInfo1);
        createBidModifierDemographics(adGroupInfo1);

        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, emptyIterable());

        assertThat(getClientBidModifiersCount(), equalTo(0));
    }

    @Test
    public void adGroupWithDeletedMobileModifierAndUntouchedAdGroupWithMobileModifier() {
        createSecondAdGroup();
        createBidModifierMobile(adGroupInfo1);
        AdGroupBidModifierInfo untouched = createBidModifierMobile(adGroupInfo2);

        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers1 = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers1, emptyIterable());

        List<BidModifier> bidModifiers2 = findBidModifiersInAdGroup(adGroupInfo2);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers2, hasSize(1));
        assertThat("данные незатронутой корректировки не соответствуют ожидаемым",
                findMobileBidModifier(bidModifiers2),
                beanDiffer((BidModifierMobile) untouched.getBidModifier()).useCompareStrategy(MOBILE_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(1));
    }

    // добавление + обновление

    @Test
    public void adGroupWithAddedMobileModifierAndUpdatedDemographicsModifier() {
        createBidModifierDemographics(adGroupInfo1);

        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobileAndDemographics();
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, hasSize(2));
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                findMobileBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getMobileModifier()).useCompareStrategy(MOBILE_STRATEGY));
        assertThat("данные обновленной корректировки не соответствуют ожидаемым",
                findDemographicsBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getDemographyModifier()).useCompareStrategy(DEMOGRAPHICS_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(2));
    }

    @Test
    public void adGroupWithAddedMobileModifierAndUpdatedDemographicsModifierToTheSameValue() {
        AdGroupBidModifierInfo bidModifierInfo = createBidModifierDemographics(adGroupInfo1);

        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobile()
                .withDemographyModifier((BidModifierDemographics) bidModifierInfo.getBidModifier());
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, hasSize(2));
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                findMobileBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getMobileModifier()).useCompareStrategy(MOBILE_STRATEGY));
        assertThat("данные существующей корректировки не соответствуют ожидаемым",
                findDemographicsBidModifier(bidModifiers),
                beanDiffer(bidModifierInfo.getBidModifier()).useCompareStrategy(DEMOGRAPHICS_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(2));
    }

    // добавление + удаление

    @Test
    public void adGroupWithAddedMobileModifierAndDeletedDemographicsModifier() {
        createBidModifierDemographics(adGroupInfo1);

        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobile();
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, hasSize(1));
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                findMobileBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getMobileModifier()).useCompareStrategy(MOBILE_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(1));
    }

    // обновление + удаление

    @Test
    public void adGroupWithUpdatedDemographicsModifierAndDeletedMobileModifier() {
        createBidModifierDemographics(adGroupInfo1);
        createBidModifierMobile(adGroupInfo1);

        ComplexBidModifier complexBidModifier = randomComplexBidModifierDemographics();
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, hasSize(1));
        assertThat("данные обновленной корректировки не соответствуют ожидаемым",
                findDemographicsBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getDemographyModifier()).useCompareStrategy(DEMOGRAPHICS_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(1));
    }

    // добавление + обновление + удаление

    @Test
    public void adGroupWithAddedVideoModifierAndUpdatedDemographicsModifierAndDeletedMobileModifier() {
        createBidModifierDemographics(adGroupInfo1);
        createBidModifierMobile(adGroupInfo1);

        ComplexBidModifier complexBidModifier = randomComplexBidModifierDemographics()
                .withVideoModifier(randomBidModifierVideo());
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        updateAndCheckResultIsEntirelySuccessful(complexAdGroup);

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, hasSize(2));
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                findVideoBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getVideoModifier()).useCompareStrategy(VIDEO_STRATEGY));
        assertThat("данные обновленной корректировки не соответствуют ожидаемым",
                findDemographicsBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getDemographyModifier()).useCompareStrategy(DEMOGRAPHICS_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(2));
    }

    // работа с несколькими группами

    @Test
    public void emptyAdGroupAndAdGroupWithAddedMobileModifier() {
        createSecondAdGroup();

        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobile();
        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBidModifier(complexBidModifier);

        updateAndCheckResultIsEntirelySuccessful(asList(complexAdGroup1, complexAdGroup2));

        List<BidModifier> bidModifiers = findBidModifiersInAdGroup(adGroupInfo2);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers, hasSize(1));
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                findMobileBidModifier(bidModifiers),
                beanDiffer(complexBidModifier.getMobileModifier()).useCompareStrategy(MOBILE_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(1));
    }

    @Test
    public void adGroupWithDeletedDemographicsModifierAndAdGroupWithAddedMobileModifier() {
        createSecondAdGroup();
        createBidModifierDemographics(adGroupInfo1);

        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobile();
        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBidModifier(complexBidModifier);

        updateAndCheckResultIsEntirelySuccessful(asList(complexAdGroup1, complexAdGroup2));

        List<BidModifier> bidModifiers1 = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers1, emptyIterable());

        List<BidModifier> bidModifiers2 = findBidModifiersInAdGroup(adGroupInfo2);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers2, hasSize(1));
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                findMobileBidModifier(bidModifiers2),
                beanDiffer(complexBidModifier.getMobileModifier()).useCompareStrategy(MOBILE_STRATEGY));

        assertThat(getClientBidModifiersCount(), equalTo(1));
    }

    @Test
    public void adGroupWithAddedVideoModifierAndAdGroupWithUpdatedDemographicsModifierAndAdGroupWithDeletedMobileModifier() {
        createSecondAdGroup();
        createThirdAdGroup();

        createBidModifierDemographics(adGroupInfo2);
        createBidModifierMobile(adGroupInfo3);

        ComplexBidModifier complexBidModifier1 = randomComplexBidModifierVideo();
        ComplexBidModifier complexBidModifier2 = randomComplexBidModifierDemographics();
        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier1);
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBidModifier(complexBidModifier2);
        ComplexTextAdGroup complexAdGroup3 = createValidAdGroupForUpdate(adGroupInfo3);

        updateAndCheckResultIsEntirelySuccessful(asList(complexAdGroup1, complexAdGroup2, complexAdGroup3));

        List<BidModifier> bidModifiers1 = findBidModifiersInAdGroup(adGroupInfo1);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers1, hasSize(1));
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                findVideoBidModifier(bidModifiers1),
                beanDiffer(complexBidModifier1.getVideoModifier()).useCompareStrategy(VIDEO_STRATEGY));

        List<BidModifier> bidModifiers2 = findBidModifiersInAdGroup(adGroupInfo2);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers2, hasSize(1));
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                findDemographicsBidModifier(bidModifiers2),
                beanDiffer(complexBidModifier2.getDemographyModifier()).useCompareStrategy(DEMOGRAPHICS_STRATEGY));

        List<BidModifier> bidModifiers3 = findBidModifiersInAdGroup(adGroupInfo3);
        assertThat("количество корректировок в группе не соответствует ожидаемому", bidModifiers3, emptyIterable());

        assertThat(getClientBidModifiersCount(), equalTo(2));
    }

    private BidModifierMobile findMobileBidModifier(List<BidModifier> bidModifiers) {
        List<BidModifierMobile> mobileBidModifiers = StreamEx.of(bidModifiers)
                .filter(bm -> bm instanceof BidModifierMobile)
                .map(bm -> (BidModifierMobile) bm)
                .toList();
        assertThat("не найдена мобильная корректировка", mobileBidModifiers, not(emptyIterable()));
        assertThat("найдено более одной мобильной корректировки", mobileBidModifiers, hasSize(1));
        return mobileBidModifiers.get(0);
    }

    private BidModifierVideo findVideoBidModifier(List<BidModifier> bidModifiers) {
        List<BidModifierVideo> mobileBidModifiers = StreamEx.of(bidModifiers)
                .filter(bm -> bm instanceof BidModifierVideo)
                .map(bm -> (BidModifierVideo) bm)
                .toList();
        assertThat("не найдена видео-корректировка", mobileBidModifiers, not(emptyIterable()));
        assertThat("найдено более одной видео-корректировки", mobileBidModifiers, hasSize(1));
        return mobileBidModifiers.get(0);
    }

    private BidModifierDemographics findDemographicsBidModifier(List<BidModifier> bidModifiers) {
        List<BidModifierDemographics> demographicsBidModifiers = StreamEx.of(bidModifiers)
                .filter(bm -> bm instanceof BidModifierDemographics)
                .map(bm -> (BidModifierDemographics) bm)
                .toList();
        assertThat("не найдена демографическая корректировка", demographicsBidModifiers, not(emptyIterable()));
        assertThat("найдено более одной демографической корректировки", demographicsBidModifiers, hasSize(1));
        return demographicsBidModifiers.get(0);
    }
}
