package ru.yandex.direct.grid.processing.service.group.converter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateMobileContentAdGroupItem;
import ru.yandex.direct.test.utils.RandomEnumUtils;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.service.group.converter.AdGroupsMutationDataConverter.toMobileContentAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
public class AdGroupsMutationDataConverterMobileContentAdGroupTest {

    private Long adGroupId;
    private String adGroupName;
    private List<String> adGroupMinusKeywords;
    private List<Long> libraryMinusKeywordsIds;
    private List<Integer> regionIds;
    private AdGroupType adGroupType;
    private MobileContentAdGroup expectedAdGroup;
    private String storeUrl = "http://itunes.apple.com/app/chatlas/id555";

    @Before
    public void prepare() {
        adGroupId = RandomNumberUtils.nextPositiveLong();
        adGroupName = RandomStringUtils.randomAlphabetic(12);
        storeUrl = RandomStringUtils.randomAlphabetic(12);
        adGroupMinusKeywords = List.of(RandomStringUtils.randomAlphabetic(21));
        libraryMinusKeywordsIds = List.of(RandomNumberUtils.nextPositiveLong());
        regionIds = List.of(RandomNumberUtils.nextPositiveInteger());
        adGroupType = RandomEnumUtils.getRandomEnumValue(AdGroupType.class);
        expectedAdGroup = new MobileContentAdGroup()
                .withId(adGroupId)
                .withName(adGroupName)
                .withMinusKeywords(adGroupMinusKeywords)
                .withLibraryMinusKeywordsIds(libraryMinusKeywordsIds)
                .withGeo(mapList(regionIds, Integer::longValue))
                .withType(adGroupType)
                .withStoreUrl(storeUrl);
    }

    @Test
    public void toMobileContentAdGroupTest() {
        var adGroupUpdateItem = new GdUpdateMobileContentAdGroupItem()
                .withAdGroupId(adGroupId)
                .withAdGroupName(adGroupName)
                .withAdGroupMinusKeywords(adGroupMinusKeywords)
                .withLibraryMinusKeywordsIds(libraryMinusKeywordsIds)
                .withRegionIds(regionIds);
        var adGroup = toMobileContentAdGroup(adGroupUpdateItem, adGroupType, storeUrl);
        assertThat(adGroup instanceof MobileContentAdGroup).isTrue();
        assertThat(adGroup)
                .is(matchedBy(BeanDifferMatcher.beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void toMobileContentAdGroupWithTagsTest() {
        var adGroupUpdateItem = new GdUpdateMobileContentAdGroupItem()
                .withAdGroupId(adGroupId)
                .withAdGroupName(adGroupName)
                .withAdGroupMinusKeywords(adGroupMinusKeywords)
                .withLibraryMinusKeywordsIds(libraryMinusKeywordsIds)
                .withRegionIds(regionIds)
                .withPageGroupTags(List.of("aaa"))
                .withTargetTags(List.of("sss"));
        expectedAdGroup
                .withPageGroupTags(List.of("aaa"))
                .withTargetTags(List.of("sss"));
        var adGroup = toMobileContentAdGroup(adGroupUpdateItem, adGroupType, storeUrl);
        assertThat(adGroup instanceof MobileContentAdGroup).isTrue();
        assertThat(adGroup)
                .is(matchedBy(BeanDifferMatcher.beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
    }
}
