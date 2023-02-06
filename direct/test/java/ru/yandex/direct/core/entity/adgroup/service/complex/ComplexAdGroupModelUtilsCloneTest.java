package ru.yandex.direct.core.entity.adgroup.service.complex;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.reflections.ReflectionUtils;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.data.TestBidModifiers;
import ru.yandex.direct.core.testing.data.TestCreatives;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.data.TestRetargetingConditions;
import ru.yandex.direct.core.testing.data.TestSitelinkSets;
import ru.yandex.direct.core.testing.data.TestUserSegments;
import ru.yandex.direct.core.testing.data.TestVcards;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;
import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withPrefix;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexAdGroup.AD_GROUP;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BANNERS;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BID_MODIFIER;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.KEYWORDS;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.OFFER_RETARGETINGS;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.RELEVANCE_MATCHES;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.TARGET_INTERESTS;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupModelUtils.cloneComplexAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupModelUtils.cloneComplexAdGroupInCampaignOnOversize;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupModelUtils.cloneComplexCpmAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultClientKeyword;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestOfferRetargetingsKt.getDefaultOfferRetargeting;
import static ru.yandex.direct.core.testing.data.TestRelevanceMatches.defaultRelevanceMatch;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultTargetInterest;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class ComplexAdGroupModelUtilsCloneTest {

    private ComplexTextAdGroup initComplexAdGroup(int bannersCount) {
        Long campaignId = RandomNumberUtils.nextPositiveLong();

        ComplexBidModifier complexBidModifier = new ComplexBidModifier()
                .withDemographyModifier(TestBidModifiers.createEmptyDemographicsModifier().withCampaignId(campaignId))
                .withWeatherModifier(TestBidModifiers.createEmptyWeatherModifier().withCampaignId(campaignId))
                .withGeoModifier(TestBidModifiers.createEmptyGeoModifier().withCampaignId(campaignId))
                .withRetargetingModifier(TestBidModifiers.createEmptyRetargetingModifier().withCampaignId(campaignId))
                .withMobileModifier(TestBidModifiers.createEmptyMobileModifier().withCampaignId(campaignId))
                .withDesktopModifier(TestBidModifiers.createEmptyDesktopModifier().withCampaignId(campaignId))
                .withTabletModifier(TestBidModifiers.createEmptyTabletModifier().withCampaignId(campaignId))
                .withDesktopOnlyModifier(TestBidModifiers.createEmptyDesktopOnlyModifier().withCampaignId(campaignId))
                .withSmartTVModifier(TestBidModifiers.createEmptySmartTVModifier().withCampaignId(campaignId))
                .withVideoModifier(TestBidModifiers.createEmptyVideoModifier().withCampaignId(campaignId))
                .withPerformanceTgoModifier(TestBidModifiers.createEmptyPerformanceTgoModifier().withCampaignId(campaignId))
                .withAbSegmentModifier(TestBidModifiers.createEmptyABSegmentModifier().withCampaignId(campaignId))
                .withBannerTypeModifier(TestBidModifiers.createEmptyBannerTypeModifier().withCampaignId(campaignId))
                .withInventoryModifier(TestBidModifiers.createEmptyInventoryModifier().withCampaignId(campaignId))
                .withExpressionModifiers(singletonList(TestBidModifiers.createDefaultTrafficModifier().withCampaignId(campaignId)))
                .withTrafaretPositionModifier(TestBidModifiers.createEmptyTrafaretPositionModifier().withCampaignId(campaignId))
                .withRetargetingFilterModifier(TestBidModifiers.createEmptyRetargetingFilterModifier().withCampaignId(campaignId));

        return new ComplexTextAdGroup()
                .withAdGroup(activeTextAdGroup()
                        .withCampaignId(campaignId)
                        .withTags(singletonList(RandomNumberUtils.nextPositiveLong()))
                )
                .withKeywords(singletonList(defaultClientKeyword().withCampaignId(campaignId)))
                .withComplexBidModifier(complexBidModifier)
                .withRelevanceMatches(singletonList(defaultRelevanceMatch().withCampaignId(campaignId)))
                .withOfferRetargetings(singletonList(getDefaultOfferRetargeting().withCampaignId(campaignId)))
                .withTargetInterests(singletonList(defaultTargetInterest().withCampaignId(campaignId)))
                .withComplexBanners(getBanners(bannersCount, campaignId));
    }

    private ComplexTextAdGroup initComplexAdGroup() {
        return initComplexAdGroup(1);
    }

    private List<ComplexBanner> getBanners(int bannersCount, Long campaignId) {
        var banners = new ArrayList<ComplexBanner>();
        var bannerId = RandomUtils.nextLong(1, Long.MAX_VALUE - bannersCount);
        for (int i = 0; i < bannersCount; i++) {
            banners.add(new ComplexBanner()
                    .withBanner(fullTextBanner()
                            .withCampaignId(campaignId)
                            .withDomain("test domain")
                            .withId(bannerId + i))
                    .withVcard(TestVcards.fullVcard().withCampaignId(campaignId))
                    .withCreative(TestCreatives.defaultCanvas(null, null))
                    .withSitelinkSet(TestSitelinkSets.defaultSitelinkSet()));
        }
        return banners;
    }

    private ComplexCpmAdGroup initComplexCpmAdGroup() {
        Long campaignId = RandomNumberUtils.nextPositiveLong();

        ComplexBidModifier complexBidModifier = new ComplexBidModifier()
                .withDemographyModifier(TestBidModifiers.createEmptyDemographicsModifier().withCampaignId(campaignId))
                .withWeatherModifier(TestBidModifiers.createEmptyWeatherModifier().withCampaignId(campaignId))
                .withGeoModifier(TestBidModifiers.createEmptyGeoModifier().withCampaignId(campaignId))
                .withRetargetingModifier(TestBidModifiers.createEmptyRetargetingModifier().withCampaignId(campaignId))
                .withMobileModifier(TestBidModifiers.createEmptyMobileModifier().withCampaignId(campaignId))
                .withDesktopModifier(TestBidModifiers.createEmptyDesktopModifier().withCampaignId(campaignId))
                .withSmartTVModifier(TestBidModifiers.createEmptySmartTVModifier().withCampaignId(campaignId))
                .withVideoModifier(TestBidModifiers.createEmptyVideoModifier().withCampaignId(campaignId))
                .withPerformanceTgoModifier(TestBidModifiers.createEmptyPerformanceTgoModifier().withCampaignId(campaignId))
                .withAbSegmentModifier(TestBidModifiers.createEmptyABSegmentModifier().withCampaignId(campaignId))
                .withBannerTypeModifier(TestBidModifiers.createEmptyBannerTypeModifier().withCampaignId(campaignId))
                .withInventoryModifier(TestBidModifiers.createEmptyInventoryModifier().withCampaignId(campaignId))
                .withExpressionModifiers(singletonList(TestBidModifiers.createDefaultTrafficModifier().withCampaignId(campaignId)))
                .withTrafaretPositionModifier(TestBidModifiers.createEmptyTrafaretPositionModifier().withCampaignId(campaignId));

        var cpmBannerAdGroup = TestGroups.activeCpmBannerAdGroup(campaignId)
                .withId(2L)
                .withTags(singletonList(RandomNumberUtils.nextPositiveLong()));

        return new ComplexCpmAdGroup()
                .withAdGroup(cpmBannerAdGroup)
                .withKeywords(singletonList(defaultClientKeyword().withCampaignId(campaignId)))
                .withComplexBidModifier(complexBidModifier)
                .withTargetInterests(singletonList(defaultTargetInterest().withCampaignId(campaignId)))
                .withBanners(singletonList(fullCpmBanner(campaignId, cpmBannerAdGroup.getId(), 1L)))
                .withRetargetingConditions(singletonList(TestRetargetingConditions.defaultCpmRetCondition()))
                .withUsersSegments(singletonList(
                        TestUserSegments.defaultSegment(cpmBannerAdGroup.getId(), AdShowType.COMPLETE)));
    }

    /**
     * Убедиться, что вручную проинициализированный объект комплексной группы не имеет пустых полей.
     * Необходимо, чтобы в последующих тестах можно было использовать инициализацию для проверки клонирования бина.
     * <p>
     * Важно: если этот тест упал, значит нужно поправить {@link #clonedComplexAdGroupHasNewInstancesForProperties()}
     * на проверку новых инстансов для полей.
     */
    @Test
    public void checkInitComplexAdGroup_NotNullProperties() {
        ComplexTextAdGroup complexAdGroup = initComplexAdGroup();

        assertThat(complexAdGroup.getAdGroup().getTags(), notNullValue());

        assertNotNullFields(ComplexTextAdGroup.class, complexAdGroup);
        assertNotNullFields(ComplexBidModifier.class, complexAdGroup.getComplexBidModifier());

        assertCollectionNotEmptyAndNotNullElements(complexAdGroup.getKeywords());
        assertCollectionNotEmptyAndNotNullElements(complexAdGroup.getRelevanceMatches());
        assertCollectionNotEmptyAndNotNullElements(complexAdGroup.getOfferRetargetings());
        assertCollectionNotEmptyAndNotNullElements(complexAdGroup.getTargetInterests());

        // проверяем вложенные свойства, т.к. это тоже составной объект
        assertCollectionNotEmptyAndNotNullElements(complexAdGroup.getComplexBanners());
        for (ComplexBanner complexBanner : complexAdGroup.getComplexBanners()) {
            assertNotNullFields(ComplexBanner.class, complexBanner);
        }
    }

    @Test
    public void clonedComplexCpmAdGroupHasNewInstancesForProperties() {
        var complexCpmAdGroup = initComplexCpmAdGroup();
        var cloned = cloneComplexCpmAdGroup(complexCpmAdGroup, null);

        assertNewInstances(ComplexCpmAdGroup.class, complexCpmAdGroup, cloned);

        assertCollectionsHaveDifferentInstances(complexCpmAdGroup.getKeywords(), cloned.getKeywords());
        assertCollectionsHaveDifferentInstances(complexCpmAdGroup.getTargetInterests(), cloned.getTargetInterests());
        assertCollectionsHaveDifferentInstances(complexCpmAdGroup.getUsersSegments(), cloned.getUsersSegments());

        assertCollectionsHaveDifferentInstances(complexCpmAdGroup.getBanners(), cloned.getBanners());
        for (int i = 0; i < complexCpmAdGroup.getBanners().size(); i++) {
            var sourceBanner = (CpmBanner) complexCpmAdGroup.getBanners().get(i);
            var clonedBanner = (CpmBanner) cloned.getBanners().get(i);
            assertThat("объект баннера должен быть инстанциирован заново", clonedBanner != sourceBanner);
        }
    }

    // (!!) - тест зависим от {@link #checkInitComplexAdGroup_NotNullProperties()}
    @Test
    public void clonedComplexAdGroupHasNewInstancesForProperties() {
        ComplexTextAdGroup complexAdGroup = initComplexAdGroup();
        ComplexTextAdGroup cloned = cloneComplexAdGroupInCampaignOnOversize(complexAdGroup, false);

        assertNewInstances(ComplexTextAdGroup.class, complexAdGroup, cloned);

        assertCollectionsHaveDifferentInstances(complexAdGroup.getKeywords(), cloned.getKeywords());
        assertCollectionsHaveDifferentInstances(complexAdGroup.getRelevanceMatches(), cloned.getRelevanceMatches());
        assertCollectionsHaveDifferentInstances(complexAdGroup.getOfferRetargetings(), cloned.getOfferRetargetings());
        assertCollectionsHaveDifferentInstances(complexAdGroup.getTargetInterests(), cloned.getTargetInterests());

        assertCollectionsHaveDifferentInstances(complexAdGroup.getComplexBanners(), cloned.getComplexBanners());
        for (int i = 0; i < complexAdGroup.getComplexBanners().size(); i++) {
            ComplexBanner sourceBanner = complexAdGroup.getComplexBanners().get(i);
            ComplexBanner clonedBanner = cloned.getComplexBanners().get(i);

            assertThat("объект баннера должен быть инстанциирован заново",
                    clonedBanner.getBanner() != sourceBanner.getBanner());
            assertThat("объект креатива должен быть инстанциирован заново",
                    clonedBanner.getCreative() != sourceBanner.getCreative());
        }
    }

    private <T> void assertNewInstances(Class classObject, T objectSource, T objectClone) {
        Map<String, Object> gettersValuesSource = gettersValues(classObject, objectSource);
        Map<String, Object> gettersValuesCloned = gettersValues(classObject, objectClone);

        gettersValuesSource.remove("getType");
        gettersValuesSource.remove("getRetargetingCondition");

        gettersValuesSource.forEach((name, value) -> {
            assumeThat(gettersValuesCloned.get(name), notNullValue());
            assertThat("getter " + name + " must not return same instance", value != gettersValuesCloned.get(name));
        });
    }

    // проверяем копирование объекта и вложенных подобъектов для вызова менеджером.
    // В таком случае домен в баннерах копируется как есть.
    @Test
    public void clonedComplexAdGroupHasSameValuePropertiesButNewInstancesForProps() {
        ComplexTextAdGroup complexAdGroup = initComplexAdGroup();
        ComplexTextAdGroup cloned = cloneComplexAdGroupInCampaignOnOversize(complexAdGroup, true);

        assertThat(mapList(cloned.getComplexBanners(), ComplexBanner::getVcard), contains(nullValue()));
        assertThat(mapList(cloned.getComplexBanners(), ComplexBanner::getSitelinkSet), contains(nullValue()));

        assertThat(cloned, beanDiffer(complexAdGroup).useCompareStrategy(
                allFieldsExcept(
                        // гео не копируется, имеет смысл только на кампанию
                        newPath("complexBidModifier", "geoModifier"),
                        newPath("complexBanners", "\\d+", "sitelinkSet"),
                        newPath("complexBanners", "\\d+", "vcard"))));
    }

    // проверяем копирование объекта и вложенных подобъектов при копировании в другую кампанию
    @Test
    public void clonedComplexAdGroupHasSameValuePropertiesButNewInstancesForProps_WhenCallCopyToOtherCampaign() {
        ComplexTextAdGroup complexAdGroup = initComplexAdGroup();
        Long otherCampaignId = complexAdGroup.getAdGroup().getCampaignId() + 1;
        ComplexTextAdGroup cloned = cloneComplexAdGroup(complexAdGroup, true, otherCampaignId);

        CompareStrategy compareStrategy = allFieldsExcept(
                // гео не копируется, имеет смысл только на кампанию
                newPath(COMPLEX_BID_MODIFIER.name(), ComplexBidModifier.GEO_MODIFIER.name()),
                newPath(COMPLEX_BANNERS.name(), "\\d+", ComplexBanner.SITELINK_SET.name()),
                newPath(AD_GROUP.name(), AdGroup.TAGS.name()))
                .forFields(
                        newPath(AD_GROUP.name(), AdGroup.CAMPAIGN_ID.name()),

                        newPath(COMPLEX_BANNERS.name(), "\\d+", ComplexBanner.BANNER.name(), BannerWithSystemFields.CAMPAIGN_ID.name()),
                        newPath(COMPLEX_BANNERS.name(), "\\d+", ComplexBanner.VCARD.name(), Vcard.CAMPAIGN_ID.name()),

                        newPath(COMPLEX_BID_MODIFIER.name(), ".*", BidModifier.CAMPAIGN_ID.name()),

                        newPath(KEYWORDS.name(), "\\d+", Keyword.CAMPAIGN_ID.name()),
                        newPath(TARGET_INTERESTS.name(), "\\d+", TargetInterest.CAMPAIGN_ID.name()),
                        newPath(RELEVANCE_MATCHES.name(), "\\d+", RelevanceMatch.CAMPAIGN_ID.name()),
                        newPath(OFFER_RETARGETINGS.name(), "\\d+", OfferRetargeting.CAMPAIGN_ID.name()))
                .useMatcher(equalTo(otherCampaignId));

        assertThat(cloned, beanDiffer(complexAdGroup).useCompareStrategy(compareStrategy));
    }

    // отдельно проверяем что для обычного пользователя домен при копировании сбрасывается
    @Test
    public void resetDomainOnCopiedComplexAdGroupOnBannerWhenCallCopyByClient() {
        ComplexTextAdGroup complexAdGroup = initComplexAdGroup();
        ComplexTextAdGroup cloned = cloneComplexAdGroupInCampaignOnOversize(complexAdGroup, false);

        for (ComplexBanner complexBanner : cloned.getComplexBanners()) {
            assertThat(((BannerWithHref) complexBanner.getBanner()).getDomain(), nullValue());
        }
    }

    @Test
    public void clonedVcards_WhenCallCopyToOtherCampaign() {
        ComplexTextAdGroup complexAdGroup = initComplexAdGroup();
        Long otherCampaignId = complexAdGroup.getAdGroup().getCampaignId() + 1;

        ComplexTextAdGroup cloned = cloneComplexAdGroup(complexAdGroup, false, otherCampaignId);

        for (int i = 0; i < complexAdGroup.getComplexBanners().size(); i++) {
            ComplexBanner sourceBanner = complexAdGroup.getComplexBanners().get(i);
            ComplexBanner clonedBanner = cloned.getComplexBanners().get(i);

            assertThat(clonedBanner.getVcard(), notNullValue());
            assertThat("объект визитки должен быть инстанциирован заново",
                    clonedBanner.getVcard() != sourceBanner.getVcard());
        }
    }

    @Test
    public void resetTagsOnCopiedComplexAdGroup_WhenCallCopyToOtherCampaign() {
        ComplexTextAdGroup complexAdGroup = initComplexAdGroup();
        Long otherCampaignId = complexAdGroup.getAdGroup().getCampaignId() + 1;

        ComplexTextAdGroup cloned = cloneComplexAdGroup(complexAdGroup, false, otherCampaignId);
        assertThat(cloned.getAdGroup().getTags(), nullValue());
    }

    @Test
    public void TestCloneComplexAdGroup_BannersInOrder() {
        ComplexTextAdGroup complexAdGroup = initComplexAdGroup(99);

        Long otherCampaignId = complexAdGroup.getAdGroup().getCampaignId();
        Collections.shuffle(complexAdGroup.getComplexBanners());
        ComplexTextAdGroup cloned = cloneComplexAdGroup(complexAdGroup, false, otherCampaignId);

        List<ComplexBanner> banners = new ArrayList<>(complexAdGroup.getComplexBanners());
        banners.sort(Comparator.comparingLong(complexBanner -> complexBanner.getBanner().getId()));
        CompareStrategy compareStrategy = allFieldsExcept(
                newPath("\\d+", "sitelinkSet"),
                newPath("\\d+", "vcard"),
                newPath("\\d+", "banner", "domain"));
        assertThat(cloned.getComplexBanners(), beanDiffer(banners).useCompareStrategy(compareStrategy));
    }

    @Test
    public void resetBsRarelyLoadedOnCopiedComplexAdGroup_WhenCallCopyToOtherCampaign() {
        ComplexTextAdGroup complexAdGroup = initComplexAdGroup();
        complexAdGroup.getAdGroup().setBsRarelyLoaded(true);
        Long otherCampaignId = complexAdGroup.getAdGroup().getCampaignId() + 1;

        ComplexTextAdGroup cloned = cloneComplexAdGroup(complexAdGroup, false, otherCampaignId);
        assertThat("признак 'мало показов' не копируется",
                cloned.getAdGroup().getBsRarelyLoaded(), equalTo(false));
    }

    @Test
    public void resetBsRarelyLoadedOnCopiedComplexAdGroupOnBannerWhenCallCopyByClient() {
        ComplexTextAdGroup complexAdGroup = initComplexAdGroup();
        complexAdGroup.getAdGroup().setBsRarelyLoaded(true);
        ComplexTextAdGroup cloned = cloneComplexAdGroupInCampaignOnOversize(complexAdGroup, false);

        for (ComplexBanner complexBanner : cloned.getComplexBanners()) {
            assertThat(((BannerWithHref) complexBanner.getBanner()).getDomain(), nullValue());
        }
        assertThat("признак 'мало показов' не копируется",
                cloned.getAdGroup().getBsRarelyLoaded(), equalTo(false));
    }

    @Test
    public void resetBsRarelyLoadedOnCopiedComplexCpmAdGroup_SameCampaign() {
        var complexCpmAdGroup = initComplexCpmAdGroup();
        complexCpmAdGroup.getAdGroup().withBsRarelyLoaded(true);
        var cloned = cloneComplexCpmAdGroup(complexCpmAdGroup, null);

        assertThat("признак 'мало показов' не копируется",
                cloned.getAdGroup().getBsRarelyLoaded(), equalTo(false));
    }

    @Test
    public void resetBsRarelyLoadedOnCopiedComplexCpmAdGroup_ToOtherCampaign() {
        var complexCpmAdGroup = initComplexCpmAdGroup();
        complexCpmAdGroup.getAdGroup().withBsRarelyLoaded(true);
        Long otherCampaignId = complexCpmAdGroup.getAdGroup().getCampaignId() + 1;
        var cloned = cloneComplexCpmAdGroup(complexCpmAdGroup, otherCampaignId);

        assertThat("признак 'мало показов' не копируется",
                cloned.getAdGroup().getBsRarelyLoaded(), equalTo(false));
    }

    private <T> void assertNotNullFields(Class objectClass, T object) {
        assertThat(object, notNullValue());

        Map<String, Object> getterValues = gettersValues(objectClass, object);
        getterValues.remove("getRetargetingCondition");
        getterValues.forEach(
                (name, value) -> assertThat("check getter " + name + " not null", value, notNullValue()));
    }

    private <T> void assertCollectionNotEmptyAndNotNullElements(Collection<T> collection) {
        assertThat(collection, notNullValue());
        assertThat(collection, not(empty()));
        collection.forEach(item -> assertThat(item, notNullValue()));
    }

    private <T> void assertCollectionsHaveDifferentInstances(Collection<T> collectionA, Collection<T> collectionB) {
        assumeNotNull(collectionA, collectionB);
        assumeTrue(collectionA.size() == collectionB.size());

        Iterator<T> iteratorA = collectionA.iterator();
        Iterator<T> iteratorB = collectionB.iterator();

        while (iteratorA.hasNext()) {
            assertThat("elements of collections are not new instances", iteratorA.next() != iteratorB.next());
        }
    }

    private <T> Map<String, Object> gettersValues(Class objectClass, T object) {
        //noinspection unchecked
        Set<Method> getters =
                ReflectionUtils.getAllMethods(objectClass, withModifier(Modifier.PUBLIC), withPrefix("get"));

        Map<String, Object> values = new HashMap<>();
        for (Method getter : getters) {
            try {
                values.put(getter.getName(), getter.invoke(object));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Cannot invoke getter for object, check class object.", e);
            }
        }

        return values;
    }
}
