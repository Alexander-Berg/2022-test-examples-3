package ru.yandex.direct.core.entity.adgroup.service.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestTagRepository;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.core.testing.steps.MinusKeywordsPackSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.TagCampaignSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupNameCantBeEmpty;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupNameIsNotSet;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupValidationService.MAX_NAME_LENGTH;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupValidationService.MAX_PAGE_GROUP_TAGS_COUNT;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupValidationService.MAX_TAGS_COUNT;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupValidationService.MAX_TAG_LENGTH;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupValidationService.MAX_TARGET_TAGS_COUNT;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.MAX_LINKED_PACKS_TO_ONE_AD_GROUP;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.minusWordsPackNotFound;
import static ru.yandex.direct.core.entity.region.validation.RegionIdDefects.geoIncorrectRegions;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_GOALS_PER_RULE;
import static ru.yandex.direct.core.entity.retargeting.Constants.MIN_GOALS_PER_RULE;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.feature.FeatureName.ZERO_SPEED_PAGE_ENABLED_FOR_GEOPRODUCT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.collectionSizeIsValid;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.StringDefects.admissibleChars;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class AdGroupValidationServiceTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final List<Long> INVALID_GEO = singletonList(12345678L);

    private final Boolean isPropertyChangedValue;
    private final BiFunction<AdGroup, ModelProperty, Boolean> isPropertyChanged;

    @Autowired
    private Steps steps;
    @Autowired
    private FeatureSteps featureSteps;
    @Autowired
    private CampaignSteps campaignSteps;
    @Autowired
    private TagCampaignSteps tagCampaignSteps;
    @Autowired
    private MinusKeywordsPackSteps packSteps;
    @Autowired
    private AdGroupValidationService adGroupValidationService;
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    @Autowired
    private TestTagRepository testTagRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private MinusKeywordsPackRepository packRepository;

    private int shard;
    private ClientId clientId;
    private GeoTree geoTree;
    private AdGroup defaultAdGroup;
    private ClientInfo clientInfo;

    public AdGroupValidationServiceTest(Boolean isPropertyChangedValue) {
        this.isPropertyChangedValue = isPropertyChangedValue;
        this.isPropertyChanged = (ag, mp) -> isPropertyChangedValue;
    }

    @Parameterized.Parameters(name = "isPropertyChangedValue={0}")
    public static Iterable<Boolean> getParameters() {
        return asList(true, false);
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);
        shard = campaignInfo.getShard();
        clientId = campaignInfo.getClientId();
        defaultAdGroup = defaultTextAdGroup(campaignInfo.getCampaignId());
        geoTree = geoTreeFactory.getGlobalGeoTree();

        featureSteps.addFeature(ZERO_SPEED_PAGE_ENABLED_FOR_GEOPRODUCT);
    }

    @Test
    public void positiveValidationResultWhenNoErrors() {
        ValidationResult<AdGroup, Defect> actual =
                adGroupValidationService
                        .validateAdGroup(defaultAdGroup, geoTree, (ag, mp) -> true, new HashMap<>(), emptySet(),
                                emptyMap(), false, false);
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_NameIsNull() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withName(null));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.NAME.name())), adGroupNameIsNotSet()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_Name_Empty() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withName(""));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.NAME.name())), adGroupNameCantBeEmpty()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_Name_spacesOnly() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withName("   "));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.NAME.name())), adGroupNameCantBeEmpty()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_Name_GreaterMax() {
        ValidationResult<AdGroup, Defect> actual =
                validate(defaultAdGroup.withName(randomAlphanumeric(MAX_NAME_LENGTH + 1)));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.NAME.name())), maxStringLength(MAX_NAME_LENGTH)))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_Name_maxAndSpaces() {
        ValidationResult<AdGroup, Defect> actual =
                validate(defaultAdGroup.withName(randomAlphanumeric(MAX_NAME_LENGTH - 1) + "   "));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_Name_InvalidSymbols() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withName("\uD83D\uDD71"));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.NAME.name())), admissibleChars()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_Name_ValidChineseJapaneseSymbols() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withName("大"));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_GeoIsNull() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withGeo(null));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.GEO.name())), notNull()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_Geo_Invalid() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withGeo(INVALID_GEO));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.GEO.name())),
                            geoIncorrectRegions(StringUtils.join(INVALID_GEO, ","))))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_MinusKeywords_Successful() {
        ValidationResult<AdGroup, Defect> actual = validate(
                defaultAdGroup.withMinusKeywords(singletonList("some words")));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_OneTag_Successful() {
        List<Long> tags = tagCampaignSteps.createDefaultTags(shard, clientId, defaultAdGroup.getCampaignId(), 1);
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withTags(tags));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_MaxTagsCount_Successful() {
        List<Long> tags =
                tagCampaignSteps.createDefaultTags(shard, clientId, defaultAdGroup.getCampaignId(), MAX_TAGS_COUNT);
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withTags(tags));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_TagsIsNull() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withTags(singletonList(null)));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.TAGS.name()), index(0)), notNull()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_TagsNotUnique() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withTags(asList(1L, 1L)));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.TAGS.name()), index(0)), duplicatedElement()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_TagsExist() {
        Long tag = tagCampaignSteps.createDefaultTags(shard, clientId, defaultAdGroup.getCampaignId(), 1).get(0);
        Long tagForDelete =
                tagCampaignSteps.createDefaultTags(shard, clientId, defaultAdGroup.getCampaignId(), 1).get(0);
        testTagRepository.deleteTags(shard, singletonList(tagForDelete));

        ValidationResult<AdGroup, Defect> actual =
                validate(defaultAdGroup.withTags(asList(tag, tagForDelete)));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.TAGS.name()), index(1)), objectNotFound()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_TagExists_AnotherCampaignTag() {
        CampaignInfo anotherCampaign = campaignSteps.createActiveTextCampaign(clientInfo);
        Long anotherCampaignTag = tagCampaignSteps
                .createDefaultTags(anotherCampaign.getShard(), anotherCampaign.getClientId(),
                        anotherCampaign.getCampaignId(), 1).get(0);

        ValidationResult<AdGroup, Defect> actual =
                validate(defaultAdGroup.withTags(singletonList(anotherCampaignTag)));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.TAGS.name()), index(0)), objectNotFound()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_TagExists_AnotherClientTag() {
        CampaignInfo anotherClientCampaign = campaignSteps.createDefaultCampaign();
        Long anotherClientTag = tagCampaignSteps
                .createDefaultTags(anotherClientCampaign.getShard(), anotherClientCampaign.getClientId(),
                        anotherClientCampaign.getCampaignId(), 1).get(0);

        ValidationResult<AdGroup, Defect> actual =
                validate(defaultAdGroup.withTags(singletonList(anotherClientTag)));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.TAGS.name()), index(0)), objectNotFound()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_MaxTagsCount() {
        List<Long> tags =
                tagCampaignSteps.createDefaultTags(shard, clientId, defaultAdGroup.getCampaignId(), MAX_TAGS_COUNT + 1);
        ValidationResult<AdGroup, Defect> actual =
                validate(defaultAdGroup.withTags(tags));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.TAGS.name())), maxCollectionSize(MAX_TAGS_COUNT)))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_NullTagList() {
        ValidationResult<AdGroup, Defect> actual =
                validate(defaultAdGroup.withTags(null));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_PageGroupTagsIsNull() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withPageGroupTags(singletonList(null)));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.PAGE_GROUP_TAGS.name()), index(0)), notNull()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_TargetTagsIsNull() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withTargetTags(singletonList(null)));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.TARGET_TAGS.name()), index(0)), notNull()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_PageGroupTagsLength() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withPageGroupTags(
                singletonList(StringUtils.repeat("-", MAX_TAG_LENGTH + 1))));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.PAGE_GROUP_TAGS.name()), index(0)), maxStringLength(MAX_TAG_LENGTH)))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_TargetTagsLength() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withTargetTags(
                singletonList(StringUtils.repeat("-", MAX_TAG_LENGTH + 1))));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.TARGET_TAGS.name()), index(0)), maxStringLength(MAX_TAG_LENGTH)))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_PageGroupTagsNotUnique() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withPageGroupTags(
                asList("page_group_tag", "unique_page_group_tag", "page_group_tag")));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.PAGE_GROUP_TAGS.name()), index(0)), duplicatedElement()))));
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.PAGE_GROUP_TAGS.name()), index(2)), duplicatedElement()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_TargetTagsNotUnique() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withTargetTags(
                asList("target_tag", "unique_target_tag", "target_tag")));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.TARGET_TAGS.name()), index(0)), duplicatedElement()))));
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.TARGET_TAGS.name()), index(2)), duplicatedElement()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_PageGroupTagsInvalid() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withPageGroupTags(
                singletonList("page group tag with space")));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.PAGE_GROUP_TAGS.name()), index(0)), invalidValue()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_TargetTagsInvalid() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withTargetTags(
                singletonList("target tag with space")));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.TARGET_TAGS.name()), index(0)), invalidValue()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_PageGroupAndTargetTags_Successful() {
        ValidationResult<AdGroup, Defect> actual = validate(
                defaultAdGroup.withPageGroupTags(asList("Page_group_tag-1", "Page_group_tag-2"))
                        .withTargetTags(asList("Target_tag-1", "Target_tag-2")));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_MaxPageGroupAndTargetTagsCount_Successful() {
        List<String> pageGroupTags = IntStream.range(0, MAX_PAGE_GROUP_TAGS_COUNT).mapToObj(String::valueOf).collect(Collectors.toList());
        List<String> targetTags = IntStream.range(0, MAX_TARGET_TAGS_COUNT).mapToObj(String::valueOf).collect(Collectors.toList());
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withPageGroupTags(pageGroupTags).withTargetTags(targetTags));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_MaxPageGroupTagsCount() {
        List<String> pageGroupTags = IntStream.range(0, MAX_PAGE_GROUP_TAGS_COUNT + 1).mapToObj(String::valueOf).collect(Collectors.toList());
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withPageGroupTags(pageGroupTags));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.PAGE_GROUP_TAGS.name())), maxCollectionSize(MAX_PAGE_GROUP_TAGS_COUNT)))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_MaxTargetTagsCount() {
        List<String> targetTags = IntStream.range(0, MAX_TARGET_TAGS_COUNT + 1).mapToObj(String::valueOf).collect(Collectors.toList());
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withTargetTags(targetTags));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.TARGET_TAGS.name())), maxCollectionSize(MAX_TARGET_TAGS_COUNT)))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_NullPageGroupAndTargetTagList() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withPageGroupTags(null).withTargetTags(null));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_OnePack_Successful() {
        List<Long> packs = packSteps.createLibraryMinusKeywordsPacks(clientInfo, 1);
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withLibraryMinusKeywordsIds(packs));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_MaxPacksCount_Successful() {
        List<Long> packs =
                packSteps.createLibraryMinusKeywordsPacks(clientInfo, MAX_LINKED_PACKS_TO_ONE_AD_GROUP);
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withLibraryMinusKeywordsIds(packs));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_PackIdIsNull_NotNullDefect() {
        ValidationResult<AdGroup, Defect> actual =
                validate(defaultAdGroup.withLibraryMinusKeywordsIds(singletonList(null)));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(0)),
                            notNull()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_PackIdsNotUnique_DuplicatedElementDefect() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withLibraryMinusKeywordsIds(asList(1L, 1L)));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(0)),
                            duplicatedElement()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_PackNotExist_MinusWordsPackNotFoundDefect() {
        Long pack = packSteps.createLibraryMinusKeywordsPacks(clientInfo, 1).get(0);
        Long packForDelete = packSteps.createLibraryMinusKeywordsPacks(clientInfo, 1).get(0);
        packRepository.delete(shard, clientId, singletonList(packForDelete));

        ValidationResult<AdGroup, Defect> actual =
                validate(defaultAdGroup.withLibraryMinusKeywordsIds(asList(pack, packForDelete)));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(1)),
                            minusWordsPackNotFound()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_AnotherClientPack_MinusWordsPackNotFoundDefect() {
        ClientInfo anotherClientInfo = campaignSteps.createDefaultCampaign().getClientInfo();

        Long anotherClientPack = packSteps.createLibraryMinusKeywordsPacks(anotherClientInfo, 1).get(0);

        ValidationResult<AdGroup, Defect> actual =
                validate(defaultAdGroup.withLibraryMinusKeywordsIds(singletonList(anotherClientPack)));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(0)),
                            minusWordsPackNotFound()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_MoreThanMaxPacksCount_MaxCollectionSizeDefect() {
        List<Long> packs = packSteps.createLibraryMinusKeywordsPacks(clientInfo, MAX_LINKED_PACKS_TO_ONE_AD_GROUP + 1);
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withLibraryMinusKeywordsIds(packs));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS)),
                            maxCollectionSize(MAX_LINKED_PACKS_TO_ONE_AD_GROUP)))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_NullPackList() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withLibraryMinusKeywordsIds(null));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_EmptyPackList() {
        ValidationResult<AdGroup, Defect> actual = validate(defaultAdGroup.withLibraryMinusKeywordsIds(emptyList()));
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_NullInContentCategoriesTargeting() {
        var rules = new ArrayList<ru.yandex.direct.core.entity.retargeting.model.Rule>();
        rules.add(null);
        var actual = validate(defaultAdGroup.withContentCategoriesRetargetingConditionRules(rules));
        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(AdGroup.CONTENT_CATEGORIES_RETARGETING_CONDITION_RULES), index(0)),
                        notNull()))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    @Test
    public void validate_InvalidContentCategoriesTargeting() {
        var rules = List.of(
                new ru.yandex.direct.core.entity.retargeting.model.Rule()
                        .withType(RuleType.OR)
                        .withGoals(emptyList()));
        var actual = validate(defaultAdGroup.withContentCategoriesRetargetingConditionRules(rules));

        if (isPropertyChangedValue) {
            assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                    validationError(
                            path(field(AdGroup.CONTENT_CATEGORIES_RETARGETING_CONDITION_RULES),
                                    index(0),
                                    field(ru.yandex.direct.core.entity.retargeting.model.Rule.GOALS)),
                            collectionSizeIsValid(MIN_GOALS_PER_RULE, MAX_GOALS_PER_RULE)))));
        } else {
            assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
        }
    }

    private ValidationResult<AdGroup, Defect> validate(AdGroup adGroup) {
        Map<Long, List<Long>> campaignsTagIds = new HashMap<>();
        if (adGroup.getTags() != null) {
            campaignsTagIds = tagRepository.getCampaignsTagIds(shard, clientId, new HashSet<>(adGroup.getTags()));
        }

        Set<Long> allPackIds = new HashSet<>();
        if (adGroup.getLibraryMinusKeywordsIds() != null) {
            allPackIds = packRepository.getClientExistingLibraryMinusKeywordsPackIds(shard, clientId,
                    adGroup.getLibraryMinusKeywordsIds());
        }

        return adGroupValidationService
                .validateAdGroup(adGroup, geoTree, isPropertyChanged, campaignsTagIds, allPackIds, emptyMap(),
                        false, false);
    }
}
