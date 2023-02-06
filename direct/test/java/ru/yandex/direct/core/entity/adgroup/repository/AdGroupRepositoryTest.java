package ru.yandex.direct.core.entity.adgroup.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.common.util.RepositoryUtils;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupBsTags;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.GeoproductAvailability;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageGroupTagEnum;
import ru.yandex.direct.core.entity.adgroup.model.StatusAutobudgetShow;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.internal.AdGroupBsTagsRepository;
import ru.yandex.direct.core.entity.adgroup.repository.internal.AdGroupTagsRepository;
import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.entity.bids.container.BidDynamicOpt;
import ru.yandex.direct.core.entity.container.CampaignIdAndAdGroupIdPair;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestAdGroupBsTagsRepository;
import ru.yandex.direct.core.testing.repository.TestAdGroupRepository;
import ru.yandex.direct.core.testing.steps.PerformanceFiltersSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.tables.AdgroupsPerformance;
import ru.yandex.direct.dbschema.ppc.tables.records.BidsDynamicRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.BidsPerformanceRecord;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.createIosMobAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.createMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordWithText;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.privateMinusKeywordsPack;
import static ru.yandex.direct.core.testing.data.TestMobileContents.androidMobileContent;
import static ru.yandex.direct.core.testing.data.TestMobileContents.mobileContentFromStoreUrl;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_PERFORMANCE;
import static ru.yandex.direct.dbschema.ppc.Tables.GROUP_PARAMS;
import static ru.yandex.direct.dbschema.ppc.Tables.MOBILE_CONTENT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupRepositoryTest {

    private static final CompareStrategy SIMPLE_GROUP_COMPARE = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"), newPath("statusModerate"), newPath("geo"));

    @Autowired
    public Steps steps;

    @Autowired
    private AdGroupRepository repoUnderTest;

    @Autowired
    private TestAdGroupRepository testAdGroupRepository;

    @Autowired
    private MinusKeywordsPackRepository minusKeywordsPackRepository;

    @Autowired
    private AdGroupTagsRepository adGroupTagsRepository;

    @Autowired
    private AdGroupBsTagsRepository adGroupBsTagsRepository;

    @Autowired
    private BidModifierRepository bidModifierRepository;

    @Autowired
    private TestAdGroupBsTagsRepository testAdGroupBsTagsRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    private CampaignInfo campaignInfo;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private long operatorUid;
    private int shard;
    private List<Long> campaignTags;
    private List<Long> libraryMinusKeywordsPacks;

    // при изменении значений в этой группе необходимо удостовериться,
    // что в тестах обновления группы обновленные значения не совпадают с выставляемыми здесь
    private AdGroup fullAdGroupWithNoDefaultValues(Long cid) {
        MinusKeywordsPackInfo minusKeywordsPack =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(campaignInfo.getClientInfo());
        return new TextAdGroup()
                .withCampaignId(cid)
                .withType(AdGroupType.BASE)
                .withName("test group " + randomNumeric(5))
                .withPriorityId(0L)
                .withGeo(asList(255L, 125L))
                .withStatusBsSynced(StatusBsSynced.YES)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES)
                .withMinusKeywordsId(minusKeywordsPack.getMinusKeywordPackId())
                .withMinusKeywords(minusKeywordsPack.getMinusKeywordsPack().getMinusKeywords())
                .withLibraryMinusKeywordsIds(libraryMinusKeywordsPacks)
                .withStatusAutobudgetShow(false)
                .withStatusShowsForecast(StatusShowsForecast.SENDING)
                .withBsRarelyLoaded(false)
                .withForecastDate(LocalDateTime.now().minusDays(3).withNano(0))
                .withPageGroupTags(emptyList())
                .withTargetTags(emptyList())
                .withUsersSegments(emptyList());
    }

    // addAdGroups

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientId = campaignInfo.getClientId();
        clientInfo = campaignInfo.getClientInfo();
        operatorUid = campaignInfo.getUid();
        shard = campaignInfo.getShard();
        campaignTags = steps.tagCampaignSteps()
                .createDefaultTags(shard, clientId, campaignInfo.getCampaignId(), 4);
        libraryMinusKeywordsPacks = steps.minusKeywordsPackSteps()
                .createLibraryMinusKeywordsPacks(clientInfo, 4);
    }

    @Test
    public void addAdGroups_OneGroup_ReturnsOnePositiveId() {
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId());
        List<Long> ids = createAdGroups(shard, clientId, adGroup);
        assertThat("репозиторий должен вернуть один положительный id", ids, contains(greaterThan(0L)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void addAdGroups_TwoGroups_ReturnsTwoPositiveIds() {
        AdGroup adGroup1 = defaultTextAdGroup(campaignInfo.getCampaignId());
        AdGroup adGroup2 = defaultTextAdGroup(campaignInfo.getCampaignId());
        List<Long> ids = createAdGroups(shard, clientId, adGroup1, adGroup2);
        assertThat("репозиторий должен вернуть два положительных id", ids, contains(greaterThan(0L), greaterThan(0L)));
    }

    @Test
    public void addAdGroups_OneGroup_SaveAdGroupDataCorrectly() {
        AdGroup adGroup = fullAdGroupWithNoDefaultValues(campaignInfo.getCampaignId());
        adGroup.setLastChange(LocalDateTime.now().withNano(0));
        List<Long> ids = createAdGroups(campaignInfo.getShard(), clientId, adGroup);
        assumeThat("репозиторий должен вернуть один положительный id", ids, contains(greaterThan(0L)));

        List<AdGroup> savedAdGroups = repoUnderTest.getAdGroups(shard, ids);
        assertThat("данные извлеченной группы не соответствуют данным ранее сохраненной",
                savedAdGroups, contains(beanDiffer(adGroup)));
    }

    @Test
    public void addAdGroups_OneGroup_WithTrackingParams_SaveAdGroupDataCorrectly() {
        AdGroup adGroup = fullAdGroupWithNoDefaultValues(campaignInfo.getCampaignId())
                .withTrackingParams("direct");

        adGroup.setLastChange(LocalDateTime.now().withNano(0));
        List<Long> ids = createAdGroups(campaignInfo.getShard(), clientId, adGroup);
        assumeThat("репозиторий должен вернуть один положительный id", ids, contains(greaterThan(0L)));

        List<AdGroup> savedAdGroups = repoUnderTest.getAdGroups(shard, ids);

        // репозиторий вернёт ещё hasPhraseIdHref = false, но это неважно
        assertThat("данные извлеченной группы не соответствуют данным ранее сохраненной",
                savedAdGroups, contains(beanDiffer(adGroup)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addAdGroups_OneGroup_SetIdToAdGroupModel() {
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId());
        List<Long> ids = createAdGroups(shard, clientId, adGroup);
        assumeThat("репозиторий должен вернуть один положительный id", ids, contains(greaterThan(0L)));
        assertThat("после добавления id в модели должен совпадать с возвращенным из метода id",
                adGroup.getId(), is(ids.get(0)));
    }

    @Test
    public void addAdGroups_AdGroupWithTags_TagsAdded() {
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId())
                .withTags(campaignTags);
        List<Long> ids = createAdGroups(shard, clientId, adGroup);
        assumeThat("репозиторий должен вернуть один положительный id", ids, contains(greaterThan(0L)));

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, ids);
        assumeThat("должна добавиться одна группа", adGroups, hasSize(1));
        List<Long> tagIds = adGroups.get(0).getTags();
        assertThat("к группе должны быть привязаны правильные метки", tagIds,
                containsInAnyOrder(campaignTags.toArray()));
    }

    @Test
    public void addAdGroups_AdGroupWithPageGroupAndTargetTags_TagsAdded() {
        List<String> pageGroupTags = asList("page_group_tag-1", "page_group_tag-2");
        List<String> targetTags = asList("target_tag-1", "target_tag-2");
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId())
                .withPageGroupTags(pageGroupTags)
                .withTargetTags(targetTags);
        List<Long> ids = createAdGroups(shard, clientId, adGroup);
        assumeThat("репозиторий должен вернуть один положительный id", ids, contains(greaterThan(0L)));

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, ids);
        assumeThat("должна добавиться одна группа", adGroups, hasSize(1));
        List<String> savedPageGroupTags = adGroups.get(0).getPageGroupTags();
        List<String> savedTargetTags = adGroups.get(0).getTargetTags();
        assertThat("к группе должны быть привязаны правильные теги", savedPageGroupTags,
                containsInAnyOrder(pageGroupTags.toArray()));
        assertThat("к группе должны быть привязаны правильные теги", savedTargetTags,
                containsInAnyOrder(targetTags.toArray()));
    }

    @Test
    public void addAdGroups_AdGroupWithLibraryMinusKeywords_MinusKeywordsAdded() {
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId())
                .withLibraryMinusKeywordsIds(libraryMinusKeywordsPacks);
        List<Long> ids = createAdGroups(shard, clientId, adGroup);
        assumeThat("репозиторий должен вернуть один положительный id", ids, contains(greaterThan(0L)));

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, ids);
        assumeThat("должна добавиться одна группа", adGroups, hasSize(1));
        List<Long> libraryMinusKeywordsIds = adGroups.get(0).getLibraryMinusKeywordsIds();
        assertThat("к группе должны быть привязаны правильные наборы библиотечных минус-фраз",
                libraryMinusKeywordsIds, containsInAnyOrder(libraryMinusKeywordsPacks.toArray()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void addAdGroups_TwoGroups_SaveAdGroupDataCorrectlyAndReturnIdsInValidOrder() {
        AdGroup adGroup1 = fullAdGroupWithNoDefaultValues(campaignInfo.getCampaignId());
        adGroup1.setLastChange(LocalDateTime.now().withNano(0));
        AdGroup adGroup2 = fullAdGroupWithNoDefaultValues(campaignInfo.getCampaignId());
        adGroup2.setLastChange(LocalDateTime.now().withNano(0));
        List<Long> ids = createAdGroups(shard, clientId, adGroup1, adGroup2);
        assumeThat("репозиторий должен вернуть два положительных id", ids, contains(greaterThan(0L), greaterThan(0L)));

        AdGroup savedAdGroup1 = repoUnderTest.getAdGroups(campaignInfo.getShard(), singletonList(ids.get(0))).get(0);
        AdGroup savedAdGroup2 = repoUnderTest.getAdGroups(campaignInfo.getShard(), singletonList(ids.get(1))).get(0);

        assertThat("данные группы, извлеченной по первому полученному id, "
                        + "не соответствуют данным группы, переданной первой в метод сохранения",
                savedAdGroup1, beanDiffer(adGroup1));
        assertThat("данные группы, извлеченной по второму полученному id, "
                        + "не соответствуют данным группы, переданной второй в метод сохранения",
                savedAdGroup2, beanDiffer(adGroup2));
    }

    @Test
    public void addAdGroups_AdGroupWithMinusKeywords_MinusKeywordsNotAdded() {
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId())
                .withMinusKeywords(singletonList("фраза"));

        List<Long> ids = createAdGroups(shard, clientId, adGroup);
        assumeThat("репозиторий должен вернуть один положительный id", ids, contains(greaterThan(0L)));

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, ids);
        assumeThat("должна добавиться одна группа", adGroups, hasSize(1));
        assertThat("к группе не должны быть добавлены минус-фразы", adGroups.get(0).getMinusKeywords(), empty());
    }

    @Test
    public void addAdGroups_AdGroupWithMinusKeywordsId_SaveMinusKeywordsIdCorrectly() {
        MinusKeywordsPackInfo minusKeywordsPack = steps.minusKeywordsPackSteps()
                .createMinusKeywordsPack(campaignInfo.getClientInfo());
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId())
                .withMinusKeywordsId(minusKeywordsPack.getMinusKeywordPackId());

        List<Long> ids = createAdGroups(shard, clientId, adGroup);
        assumeThat("репозиторий должен вернуть один положительный id", ids, contains(greaterThan(0L)));

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, ids);
        assumeThat("должна добавиться одна группа", adGroups, hasSize(1));
        assertThat("id набора минус-фраз должен корректно сохраниться",
                adGroups.get(0).getMinusKeywordsId(), is(minusKeywordsPack.getMinusKeywordPackId()));
        assertThat("у группы должен быть корректный список минус-фраз",
                adGroups.get(0).getMinusKeywords(), is(minusKeywordsPack.getMinusKeywordsPack().getMinusKeywords()));
    }

    // updateAdGroups

    @Test
    public void updateAdGroups_UpdateAllPossibleFieldsOfOneAdGroup_UpdatesDataCorrectly() {
        AdGroup adGroup = fullAdGroupWithNoDefaultValues(campaignInfo.getCampaignId());
        List<Long> ids = createAdGroups(campaignInfo.getShard(), clientId, adGroup);
        assumeThat("репозиторий должен вернуть один положительный id", ids, contains(greaterThan(0L)));
        assumeThat("после добавления id в модели должен совпадать с возвращенным из метода id",
                adGroup.getId(), is(ids.get(0)));

        MinusKeywordsPackInfo newMinusKeywordsPack = steps.minusKeywordsPackSteps().createMinusKeywordsPack(
                privateMinusKeywordsPack().withMinusKeywords(singletonList("обновленная фраза")),
                campaignInfo.getClientInfo());
        List<Long> newLibraryMinusKeywordsPacks = steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPacks(
                campaignInfo.getClientInfo(), 3);

        // изменения группы (все изменения должны отличаться от данных сохраненной группы
        ModelChanges<AdGroup> adGroupChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        adGroupChanges.process("modified name " + randomAlphanumeric(10), AdGroup.NAME);
        adGroupChanges.process(asList(35L, 45L), AdGroup.GEO);
        adGroupChanges.process(StatusBsSynced.NO, AdGroup.STATUS_BS_SYNCED);
        adGroupChanges.process(StatusModerate.NO, AdGroup.STATUS_MODERATE);
        adGroupChanges.process(StatusPostModerate.NO, AdGroup.STATUS_POST_MODERATE);
        adGroupChanges.process(newMinusKeywordsPack.getMinusKeywordPackId(), AdGroup.MINUS_KEYWORDS_ID);
        adGroupChanges.process(newLibraryMinusKeywordsPacks, AdGroup.LIBRARY_MINUS_KEYWORDS_IDS);
        adGroupChanges.process(true, AdGroup.STATUS_AUTOBUDGET_SHOW);
        adGroupChanges.process(StatusShowsForecast.PROCESSED, AdGroup.STATUS_SHOWS_FORECAST);
        adGroupChanges.process(LocalDateTime.now().minusMinutes(10).withNano(0), AdGroup.FORECAST_DATE);
        adGroupChanges.process(LocalDateTime.now().withNano(0), AdGroup.LAST_CHANGE);

        // примененные изменения
        AppliedChanges<AdGroup> appliedChanges = adGroupChanges.applyTo(adGroup);
        repoUnderTest.updateAdGroups(shard, clientId, singletonList(appliedChanges));

        AdGroup adGroupAfterModification = repoUnderTest.getAdGroups(shard, singletonList(adGroup.getId())).get(0);

        adGroup.withMinusKeywords(newMinusKeywordsPack.getMinusKeywordsPack().getMinusKeywords());
        assertThat("данные обновленной группы соответствуют ожидаемым",
                adGroupAfterModification,
                beanDiffer(adGroup));
    }

    @Test
    public void updateAdGroups_UpdateSomeFieldsWithoutMinusKeywordsOfOneAdGroup_UpdatesDataCorrectly() {
        AdGroup adGroup = fullAdGroupWithNoDefaultValues(campaignInfo.getCampaignId());
        List<Long> ids = createAdGroups(campaignInfo.getShard(), clientId, adGroup);
        assumeThat("репозиторий должен вернуть один положительный id", ids, contains(greaterThan(0L)));
        assumeThat("после добавления id в модели должен совпадать с возвращенным из метода id",
                adGroup.getId(), is(ids.get(0)));

        // изменения группы
        ModelChanges<AdGroup> adGroupChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        adGroupChanges.process("modified name " + randomAlphanumeric(10), AdGroup.NAME);
        adGroupChanges.process(StatusBsSynced.NO, AdGroup.STATUS_BS_SYNCED);
        adGroupChanges.process(true, AdGroup.STATUS_AUTOBUDGET_SHOW);
        adGroupChanges.process(LocalDateTime.now().withNano(0), AdGroup.LAST_CHANGE);

        // примененные изменения
        AppliedChanges<AdGroup> appliedChanges = adGroupChanges.applyTo(adGroup);
        repoUnderTest.updateAdGroups(shard, clientId, singletonList(appliedChanges));

        AdGroup adGroupAfterModification = repoUnderTest.getAdGroups(shard, singletonList(adGroup.getId())).get(0);

        assertThat("данные обновленной группы соответствуют ожидаемым",
                adGroupAfterModification,
                beanDiffer(adGroup));
    }

    @Test
    public void update_MinusKeywordsUpdatedToNull() {
        MinusKeywordsPackInfo minusKeywordsPack =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(campaignInfo.getClientInfo());
        AdGroupInfo adGroupInfo = steps.adGroupSteps()
                .createAdGroup(activeTextAdGroup().withMinusKeywordsId(minusKeywordsPack.getMinusKeywordPackId()),
                        campaignInfo);
        assumeThat(adGroupInfo.getAdGroup().getMinusKeywordsId(), notNullValue());

        List<AppliedChanges<AdGroup>> appliedChanges = getMinusKeywordsChanges(adGroupInfo.getAdGroup(), null);

        repoUnderTest.updateAdGroups(shard, clientId, appliedChanges);

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId()));
        assumeThat("должна вернуться одна группа", adGroups, hasSize(1));
        assertThat("у группы должны удалиться минус слова", adGroups.get(0).getMinusKeywordsId(), nullValue());
        assertThat("у группы должны удалиться минус слова", adGroups.get(0).getMinusKeywords(), empty());
    }

    //покрывается случай добавления новой метки и удаления старой
    @Test
    public void update_TagsUpdated() {
        List<Long> tagsBefore = campaignTags.subList(0, 3);
        AdGroupInfo adGroup =
                steps.adGroupSteps().createAdGroup(activeTextAdGroup().withTags(tagsBefore), campaignInfo);
        List<Long> expectedTags = campaignTags.subList(1, 4);
        List<AppliedChanges<AdGroup>> appliedChanges = getTagChanges(adGroup.getAdGroup(), expectedTags);

        repoUnderTest.updateAdGroups(shard, clientId, appliedChanges);

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assumeThat("должна вернуться одна группа", adGroups, hasSize(1));
        List<Long> actualTags = adGroups.get(0).getTags();
        assertThat("метки должны корректно обновиться", actualTags, containsInAnyOrder(expectedTags.toArray()));
    }

    @Test
    public void update_TagsUpdatedFromNull() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup();
        List<Long> expectedTags = campaignTags;
        List<AppliedChanges<AdGroup>> appliedChanges = getTagChanges(adGroup.getAdGroup(), expectedTags);

        repoUnderTest.updateAdGroups(shard, clientId, appliedChanges);

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assumeThat("должна вернуться одна группа", adGroups, hasSize(1));
        List<Long> actualTags = adGroups.get(0).getTags();
        assertThat("метки должны корректно обновиться", actualTags, containsInAnyOrder(expectedTags.toArray()));
    }

    @Test
    public void update_TagsUpdatedToNull() {
        List<Long> tagsBefore = campaignTags;
        AdGroupInfo adGroup =
                steps.adGroupSteps().createAdGroup(activeTextAdGroup().withTags(tagsBefore), campaignInfo);
        List<AppliedChanges<AdGroup>> appliedChanges = getTagChanges(adGroup.getAdGroup(), null);

        repoUnderTest.updateAdGroups(shard, clientId, appliedChanges);

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assumeThat("должна вернуться одна группа", adGroups, hasSize(1));
        List<Long> actualTags = adGroups.get(0).getTags();
        assertThat("у группы должны удалиться метки", actualTags, nullValue());
    }

    private List<AppliedChanges<AdGroup>> getTagChanges(AdGroup adGroup, List<Long> newTags) {
        return singletonList(
                new ModelChanges<>(adGroup.getId(), AdGroup.class).process(newTags, AdGroup.TAGS).applyTo(adGroup));
    }

    @Test
    public void update_PageGroupAndTargetTagsUpdated() {
        List<String> pageGroupTagsBefore = asList("old_page_group_tag-1", "old_page_group_tag-2");
        List<String> targetTagsBefore = asList("old_target_tag-1", "old_target_tag-2");
        AdGroupInfo adGroup =
                steps.adGroupSteps().createAdGroup(activeTextAdGroup().withPageGroupTags(pageGroupTagsBefore).withTargetTags(targetTagsBefore), campaignInfo);
        List<String> expectedPageGroupTags = asList("new_page_group_tag-1", "new_page_group_tag-2");
        List<String> expectedTargetTags = asList("new_target_tag-1", "new_target_tag-2");
        List<AppliedChanges<AdGroup>> appliedChanges = getPageGroupAndTargetTags(adGroup.getAdGroup(),
                expectedPageGroupTags, expectedTargetTags);

        repoUnderTest.updateAdGroups(shard, clientId, appliedChanges);

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assumeThat("должна вернуться одна группа", adGroups, hasSize(1));
        List<String> actualPageGroupTags = adGroups.get(0).getPageGroupTags();
        List<String> actualTargetTags = adGroups.get(0).getTargetTags();
        assertThat("метки должны корректно обновиться", actualPageGroupTags,
                containsInAnyOrder(expectedPageGroupTags.toArray()));
        assertThat("метки должны корректно обновиться", actualTargetTags,
                containsInAnyOrder(expectedTargetTags.toArray()));
    }

    @Test
    public void update_PageGroupAndTargetTagsUpdatedToEmpty_BsTagsDeleted() {
        List<String> pageGroupTagsBefore = emptyList();
        List<String> targetTagsBefore = asList("old_target_tag-1", "old_target_tag-2");
        AdGroupInfo adGroup =
                steps.adGroupSteps().createAdGroup(activeTextAdGroup().withPageGroupTags(pageGroupTagsBefore).withTargetTags(targetTagsBefore), campaignInfo);
        List<String> expectedPageGroupTags = emptyList();
        List<String> expectedTargetTags = emptyList();
        List<AppliedChanges<AdGroup>> appliedChanges = getTargetTags(adGroup.getAdGroup(), expectedTargetTags);

        repoUnderTest.updateAdGroups(shard, clientId, appliedChanges);

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assumeThat("должна вернуться одна группа", adGroups, hasSize(1));
        List<String> actualPageGroupTags = adGroups.get(0).getPageGroupTags();
        List<String> actualTargetTags = adGroups.get(0).getTargetTags();
        assertThat("метки должны корректно обновиться", actualPageGroupTags,
                containsInAnyOrder(expectedPageGroupTags.toArray()));
        assertThat("метки должны корректно обновиться", actualTargetTags,
                containsInAnyOrder(expectedTargetTags.toArray()));

        SoftAssertions.assertSoftly(soft ->
                testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbRawConsumer(
                        soft,
                        shard,
                        adGroups.get(0).getId(),
                        null, null)
        );
    }

    private List<AppliedChanges<AdGroup>> getPageGroupAndTargetTags(AdGroup adGroup, List<String> newPageGroupTags,
                                                                    List<String> newTargetTags) {
        return singletonList(
                new ModelChanges<>(adGroup.getId(), AdGroup.class)
                        .process(newPageGroupTags, AdGroup.PAGE_GROUP_TAGS)
                        .process(newTargetTags, AdGroup.TARGET_TAGS)
                        .applyTo(adGroup));
    }

    private List<AppliedChanges<AdGroup>> getTargetTags(AdGroup adGroup, List<String> newTargetTags) {
        return singletonList(
                new ModelChanges<>(adGroup.getId(), AdGroup.class)
                        .process(newTargetTags, AdGroup.TARGET_TAGS)
                        .applyTo(adGroup));
    }

    private List<AppliedChanges<AdGroup>> getMinusKeywordsChanges(AdGroup adGroup, Long newPackId) {
        return singletonList(new ModelChanges<>(adGroup.getId(), AdGroup.class)
                .process(newPackId, AdGroup.MINUS_KEYWORDS_ID)
                .applyTo(adGroup));
    }

    @Test
    public void deleteAdGroup_TagsDeleted() {
        Long adGroupId = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId()).withTags(campaignTags), campaignInfo)
                .getAdGroupId();
        List<Long> deletedAdGroupIds = repoUnderTest.delete(shard, clientId, operatorUid, singletonList(adGroupId));
        assumeThat("группа должна быть успешно удалена", deletedAdGroupIds, containsInAnyOrder(adGroupId));
        List<Long> adGroupTags = adGroupTagsRepository
                .getAdGroupsTags(dslContextProvider.ppc(shard).configuration(), singletonList(adGroupId))
                .get(adGroupId);
        assertThat("к удаленной группе не должны быть привязаны теги", adGroupTags, nullValue());
    }

    @Test
    public void deleteAdGroup_BsTagsDeleted() {
        Long adGroupId = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId()), campaignInfo)
                .getAdGroupId();
        AdGroupBsTags adGroupBsTags = new AdGroupBsTags()
                .withPageGroupTags(ImmutableList.of(PageGroupTagEnum.APP_METRO_TAG, PageGroupTagEnum.FRONTPAGE_TAG))
                .withTargetTags(ImmutableList.of(TargetTagEnum.FRONTPAGE_TAG));
        adGroupBsTagsRepository.addDefaultTagsForAdGroupList(clientId, singletonList(adGroupId), adGroupBsTags);

        List<Long> deletedAdGroupIds = repoUnderTest.delete(shard, clientId, operatorUid, singletonList(adGroupId));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(deletedAdGroupIds)
                    .describedAs("группа должна быть успешно удалена")
                    .matches(t -> containsInAnyOrder(adGroupId).matches(t));
            testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbRawConsumer(
                    soft,
                    shard,
                    adGroupId,
                    null, null);
        });
    }

    @Test
    public void deleteAdGroup_AdGroupsContentPromotion() {
        Long adGroupIdVideo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO).getAdGroupId();
        Long adGroupIdCollections = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.COLLECTION).getAdGroupId();

        repoUnderTest.delete(shard, clientId, operatorUid, asList(adGroupIdVideo, adGroupIdCollections));
        assertThat("Должен удаляться подтип группы продвижения", repoUnderTest
                .getContentPromotionAdGroupTypesByIds(clientInfo.getShard(),
                        asList(adGroupIdCollections, adGroupIdVideo)).entrySet(), hasSize(0));
    }

    @Test
    public void deleteOneOfTwoAdGroups_OnlyFirstGroupBsTagsDeleted() {
        Long adGroupId1 = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId()), campaignInfo)
                .getAdGroupId();
        Long adGroupId2 = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId()), campaignInfo)
                .getAdGroupId();
        AdGroupBsTags adGroupBsTags = new AdGroupBsTags()
                .withPageGroupTags(ImmutableList.of(PageGroupTagEnum.APP_METRO_TAG))
                .withTargetTags(ImmutableList.of(TargetTagEnum.FRONTPAGE_TAG));
        adGroupBsTagsRepository.addDefaultTagsForAdGroupList(clientId, List.of(adGroupId1, adGroupId2), adGroupBsTags);

        List<Long> deletedAdGroupIds = repoUnderTest.delete(shard, clientId, operatorUid, singletonList(adGroupId1));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(deletedAdGroupIds)
                    .describedAs("группа должна быть успешно удалена")
                    .matches(t -> containsInAnyOrder(adGroupId1).matches(t));
            testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbRawConsumer(
                    soft,
                    shard,
                    adGroupId1,
                    null, null);
            testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbRawConsumer(
                    soft,
                    shard,
                    adGroupId2,
                    "[\"app-metro\"]", "[\"portal-trusted\"]");
        });
    }

    @Test
    public void update_PacksUpdated() {
        List<Long> packsBefore = libraryMinusKeywordsPacks.subList(0, 3);
        AdGroupInfo adGroup =
                steps.adGroupSteps()
                        .createAdGroup(activeTextAdGroup().withLibraryMinusKeywordsIds(packsBefore), campaignInfo);
        List<Long> expectedPackIds = libraryMinusKeywordsPacks.subList(1, 4);
        List<AppliedChanges<AdGroup>> appliedChanges = getLibraryPackChanges(adGroup.getAdGroup(), expectedPackIds);

        repoUnderTest.updateAdGroups(shard, clientId, appliedChanges);

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assumeThat("должна вернуться одна группа", adGroups, hasSize(1));
        List<Long> actualPackIds = adGroups.get(0).getLibraryMinusKeywordsIds();
        assertThat("библотечные наборы минус-фраз должны корректно обновиться", actualPackIds,
                containsInAnyOrder(expectedPackIds.toArray()));
    }

    @Test
    public void update_PacksUpdatedFromNull() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup();
        List<Long> expectedPackIds = libraryMinusKeywordsPacks;
        List<AppliedChanges<AdGroup>> appliedChanges = getLibraryPackChanges(adGroup.getAdGroup(), expectedPackIds);

        repoUnderTest.updateAdGroups(shard, clientId, appliedChanges);

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assumeThat("должна вернуться одна группа", adGroups, hasSize(1));
        List<Long> actualPackIds = adGroups.get(0).getLibraryMinusKeywordsIds();
        assertThat("библотечные наборы минус-фраз должны корректно обновиться", actualPackIds,
                containsInAnyOrder(expectedPackIds.toArray()));
    }

    @Test
    public void update_PacksUpdatedToNull() {
        List<Long> packIdsBefore = libraryMinusKeywordsPacks;
        AdGroupInfo adGroup =
                steps.adGroupSteps()
                        .createAdGroup(activeTextAdGroup().withLibraryMinusKeywordsIds(packIdsBefore), campaignInfo);
        List<AppliedChanges<AdGroup>> appliedChanges = getLibraryPackChanges(adGroup.getAdGroup(), null);

        repoUnderTest.updateAdGroups(shard, clientId, appliedChanges);

        List<AdGroup> adGroups = repoUnderTest.getAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assumeThat("должна вернуться одна группа", adGroups, hasSize(1));
        List<Long> actualPackIds = adGroups.get(0).getLibraryMinusKeywordsIds();
        assertThat("у группы должны удалиться библотечные наборы минус-фраз", actualPackIds, empty());
    }

    private List<AppliedChanges<AdGroup>> getLibraryPackChanges(AdGroup adGroup, List<Long> newPackIds) {
        return singletonList(
                new ModelChanges<>(adGroup.getId(), AdGroup.class)
                        .process(newPackIds, AdGroup.LIBRARY_MINUS_KEYWORDS_IDS)
                        .applyTo(adGroup));
    }

    @Test
    public void deleteAdGroup_PacksDeleted() {
        Long adGroupId = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId())
                        .withLibraryMinusKeywordsIds(libraryMinusKeywordsPacks), campaignInfo)
                .getAdGroupId();
        List<Long> deletedAdGroupIds = repoUnderTest.delete(shard, clientId, operatorUid, singletonList(adGroupId));
        assumeThat("группа должна быть успешно удалена", deletedAdGroupIds, containsInAnyOrder(adGroupId));
        List<Long> adGroupLinkedPacks = minusKeywordsPackRepository
                .getAdGroupsLibraryMinusKeywordsPacks(shard, singletonList(adGroupId))
                .get(adGroupId);
        assertThat("к удаленной группе не должны быть привязаны библотечные наборы минус-фраз", adGroupLinkedPacks,
                empty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setHasPhraseIdHrefs_TwoGroups_UpdatesDataCorrectly() {
        AdGroup adGroup1 = defaultTextAdGroup(campaignInfo.getCampaignId());
        AdGroup adGroup2 = defaultTextAdGroup(campaignInfo.getCampaignId());
        List<Long> ids = createAdGroups(shard, clientId, adGroup1, adGroup2);
        assumeThat("репозиторий должен вернуть два положительных id", ids, contains(greaterThan(0L), greaterThan(0L)));

        repoUnderTest.setHasPhraseIdHrefs(shard, ids);

        List<Boolean> flagsFromDB = repoUnderTest.getAdGroups(shard, ids).stream()
                .map(AdGroup::getHasPhraseIdHref)
                .collect(toList());

        assertThat("данные обновленной группы соответствуют ожидаемым",
                flagsFromDB,
                contains(true, true));
    }

    // getAdGroupSimple

    @Test
    @SuppressWarnings("unchecked")
    public void setHasPhraseIdHrefs_TwoGroups_DuplicateKeyUpdatesDataCorrectly() {
        AdGroup adGroup1 = defaultTextAdGroup(campaignInfo.getCampaignId());
        AdGroup adGroup2 = defaultTextAdGroup(campaignInfo.getCampaignId());
        List<Long> ids = createAdGroups(shard, clientId, adGroup1, adGroup2);
        assumeThat("репозиторий должен вернуть два положительных id", ids, contains(greaterThan(0L), greaterThan(0L)));

        repoUnderTest.setHasPhraseIdHrefs(shard, ids);
        repoUnderTest.setHasPhraseIdHrefs(dslContextProvider.ppc(shard), ids, false);

        List<Boolean> flagsFromDB = repoUnderTest.getAdGroups(shard, ids).stream()
                .map(AdGroup::getHasPhraseIdHref)
                .collect(toList());

        assertThat("данные обновленной группы соответствуют ожидаемым",
                flagsFromDB,
                contains(false, false));
    }

    @Test
    public void getAdGroupSimple_GroupsByClientId_ReturnsOneGroupWithValidKeyIdAndClientId() {
        long adGroupId1 = steps.adGroupSteps().createDefaultAdGroup(campaignInfo).getAdGroupId();
        long adGroupId2 = steps.adGroupSteps().createDefaultAdGroup().getAdGroupId();

        Map<Long, AdGroupSimple> simpleAdGroups = repoUnderTest.getAdGroupSimple(campaignInfo.getShard(),
                campaignInfo.getClientId(), asList(adGroupId1, adGroupId2)
        );

        assertThat("вернулась только одна группа по клиенту", simpleAdGroups.size(), is(1));

        Long keyAdGroupId = simpleAdGroups.keySet().iterator().next();
        Long objectAdGroupId = simpleAdGroups.values().iterator().next().getId();
        assertThat("id группы соответствует запрошенному", keyAdGroupId, is(adGroupId1));
        assertThat("id группы в извлеченном объекте соответствует id группы в ключе мапы",
                objectAdGroupId, is(keyAdGroupId));
    }

    @Test
    public void getAdGroupSimple_OneGroup_ReturnsOneGroupWithValidKeyId() {
        long adGroupId = steps.adGroupSteps().createDefaultAdGroup(campaignInfo).getAdGroupId();

        Map<Long, AdGroupSimple> simpleAdGroups =
                repoUnderTest.getAdGroupSimple(campaignInfo.getShard(), null, singletonList(adGroupId));
        assertThat("вернулась одна группа", simpleAdGroups.size(), is(1));

        Long keyAdGroupId = simpleAdGroups.keySet().iterator().next();
        Long objectAdGroupId = simpleAdGroups.values().iterator().next().getId();
        assertThat("id группы соответствует запрошенному", keyAdGroupId, is(adGroupId));
        assertThat("id группы в извлеченном объекте соответствует id группы в ключе мапы",
                objectAdGroupId, is(keyAdGroupId));
    }

    @Test
    public void getAdGroupSimple_OneGroup_GroupDataIsValid() {
        AdGroup adGroup = fullAdGroupWithNoDefaultValues(null);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);

        Map<Long, AdGroupSimple> simpleAdGroups =
                repoUnderTest.getAdGroupSimple(adGroupInfo.getShard(), null, singletonList(adGroupInfo.getAdGroupId()));
        assumeThat("вернулась одна группа", simpleAdGroups.size(), is(1));

        AdGroupSimple extractedGroup = simpleAdGroups.values().iterator().next();
        assertThat("извлеченные данные группы SimpleGroup соответствуют ожидаемым",
                adGroup, beanDiffer(extractedGroup).useCompareStrategy(SIMPLE_GROUP_COMPARE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getAdGroupSimple_TwoGroups_GroupDataIsValid() {
        AdGroup adGroup1 = fullAdGroupWithNoDefaultValues(null);
        AdGroup adGroup2 = fullAdGroupWithNoDefaultValues(null);
        List<AdGroup> adGroups = asList(adGroup1, adGroup2);
        AdGroupInfo adGroupInfo1 = steps.adGroupSteps().createAdGroup(adGroup1, campaignInfo);
        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createAdGroup(adGroup2, campaignInfo);
        List<Long> adGroupIds = asList(adGroupInfo1.getAdGroupId(), adGroupInfo2.getAdGroupId());

        Map<Long, AdGroupSimple> simpleAdGroups =
                repoUnderTest.getAdGroupSimple(campaignInfo.getShard(), null, adGroupIds);
        assumeThat("вернулись две группы", simpleAdGroups.size(), is(2));

        List<AdGroupSimple> extractedGroups = new ArrayList<>(simpleAdGroups.values());
        assertThat("извлеченные данные групп SimpleGroup соответствуют ожидаемым",
                adGroups, containsInAnyOrder(
                        beanDiffer(extractedGroups.get(0)).useCompareStrategy(SIMPLE_GROUP_COMPARE),
                        beanDiffer(extractedGroups.get(1)).useCompareStrategy(SIMPLE_GROUP_COMPARE)));
    }

    @Test
    public void sendDraftAdGroupToModerate_DraftAdGroup() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        testAdGroupRepository
                .updateStatusModerate(shard, singletonList(adGroupInfo.getAdGroupId()), StatusModerate.NEW);
        AdGroup group = repoUnderTest.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId())).get(0);
        assumeThat(group.getStatusModerate(), is(StatusModerate.NEW));

        repoUnderTest.sendDraftAdGroupToModerate(shard, singletonList(adGroupInfo.getAdGroupId()));
        group = repoUnderTest.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId())).get(0);
        assertThat(group.getStatusModerate(), is(StatusModerate.READY));
    }

    @Test
    public void sendDraftAdGroupToModerate_NotDraftAdGroup() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        AdGroup group = repoUnderTest.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId())).get(0);
        assumeThat(group.getStatusModerate(), is(StatusModerate.YES));
        repoUnderTest.sendDraftAdGroupToModerate(shard, singletonList(adGroupInfo.getAdGroupId()));

        group = repoUnderTest.getAdGroups(shard, singletonList(adGroupInfo.getAdGroupId())).get(0);
        assertThat(group.getStatusModerate(), is(StatusModerate.YES));
    }

    @Test
    public void getArchivedAdGroupIds_AdGroupWithoutBanners() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        Set<Long> adGroupIds = repoUnderTest.getArchivedAdGroupIds(shard, singletonList(adGroupInfo.getAdGroupId()));
        assertThat("Пустая группа не считается архивной", adGroupIds, hasSize(0));
    }

    @Test
    public void getArchivedAdGroupIds_ArchivedAllBanners() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        steps.bannerSteps().createBanner(activeTextBanner().withStatusArchived(true), adGroupInfo);
        steps.bannerSteps().createBanner(activeTextBanner().withStatusArchived(true), adGroupInfo);
        Set<Long> adGroupIds = repoUnderTest.getArchivedAdGroupIds(shard, singletonList(adGroupInfo.getAdGroupId()));
        assertThat("Группа с архивными баннерами считается архивной", adGroupIds, hasSize(1));
    }

    @Test
    public void getArchivedAdGroupIds_ArchivedSomeBanners() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        steps.bannerSteps().createBanner(activeTextBanner().withStatusArchived(true), adGroupInfo);
        steps.bannerSteps().createDefaultBanner(adGroupInfo);
        Set<Long> adGroupIds = repoUnderTest.getArchivedAdGroupIds(shard, singletonList(adGroupInfo.getAdGroupId()));
        assertThat("Группа с некоторыми архивными баннерами не считается архивной", adGroupIds, hasSize(0));
    }

    @Test
    public void getArchivedAdGroupIds_NotArchivedAdGroup() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        steps.bannerSteps().createDefaultBanner(adGroupInfo);
        steps.bannerSteps().createDefaultBanner(adGroupInfo);
        Set<Long> adGroupIds = repoUnderTest.getArchivedAdGroupIds(shard, singletonList(adGroupInfo.getAdGroupId()));
        assertThat("Группа без архивных баннеров не считается архивной", adGroupIds, hasSize(0));
    }

    @Test
    public void getArchivedAdGroupIds_VariousAdGroups() {
        //группа без баннеров
        AdGroupInfo adGroupInfo1 = steps.adGroupSteps().createDefaultAdGroup();
        //группа с 1 НЕ архивным баннером
        AdGroupInfo adGroupInfo2 = steps.adGroupSteps().createDefaultAdGroup();
        steps.bannerSteps().createDefaultBanner(adGroupInfo2);
        //группа с 1 архивным баннером
        AdGroupInfo adGroupInfo3 = steps.adGroupSteps().createDefaultAdGroup();
        steps.bannerSteps().createBanner(activeTextBanner().withStatusArchived(true), adGroupInfo3);
        //группа с архивным и НЕ архивным баннером
        AdGroupInfo adGroupInfo4 = steps.adGroupSteps().createDefaultAdGroup();
        steps.bannerSteps().createDefaultBanner(adGroupInfo4);
        steps.bannerSteps().createBanner(activeTextBanner().withStatusArchived(true), adGroupInfo4);
        Set<Long> adGroupIds = repoUnderTest.getArchivedAdGroupIds(shard, asList(
                adGroupInfo1.getAdGroupId(),
                adGroupInfo2.getAdGroupId(),
                adGroupInfo3.getAdGroupId(),
                adGroupInfo4.getAdGroupId()));
        assertThat("Определили правильное количество архивных групп", adGroupIds, hasSize(1));
        assertThat("Архивная группа соответствует ожидаемой", adGroupIds, contains(adGroupInfo3.getAdGroupId()));
    }

    @Test
    public void getMobileContentAppIds_Android() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo androidCampaign = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
        MobileContent androidContent = androidMobileContent();
        MobileContentAdGroup androidGroup = createMobileAppAdGroup(androidCampaign.getCampaignId(), androidContent);
        Long androidGroupId = steps.adGroupSteps().createAdGroup(androidGroup, androidCampaign).getAdGroupId();

        assumeThat("Значение store_content_id и bundle_id отличны",
                androidContent.getStoreContentId(), not(equalTo(androidContent.getBundleId())));

        String expected = androidContent.getStoreContentId();

        Map<Long, String> repoResult = repoUnderTest.getMobileContentAppIds(shard, singletonList(androidGroupId));

        assumeThat("Вернулась одна запись",
                repoResult.keySet(), hasSize(1));
        assumeThat("Вернулась запись по интересующей нас группе",
                repoResult.keySet(), contains(androidGroupId));

        String actual = repoResult.get(androidGroupId);

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void getMobileContentAppIds_Ios() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo iosCampaign = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);
        MobileContent iosContent = steps.mobileContentSteps().createDefaultIosMobileContent().getMobileContent();
        MobileContentAdGroup iosGroup = createIosMobAppAdGroup(iosCampaign.getCampaignId(), iosContent);
        Long iosGroupId = steps.adGroupSteps().createAdGroup(iosGroup, iosCampaign).getAdGroupId();

        // к сожалению, при создании группы проставляется новый id контента
        iosContent.withId(iosGroup.getMobileContentId());

        assumeThat("Значение store_content_id и bundle_id отличны",
                iosContent.getStoreContentId(), not(equalTo(iosContent.getBundleId())));

        // Что сейчас произойдёт? – Мы вручную проставим в базу значение bundleId для теста.
        // Почему? – Потому что это поле не пишется ядром, вообще оно заполняется отдельной джобой.
        dslContextProvider.ppc(shard)
                .update(MOBILE_CONTENT)
                .set(MOBILE_CONTENT.BUNDLE_ID, iosContent.getBundleId())
                .where(MOBILE_CONTENT.MOBILE_CONTENT_ID.eq(iosContent.getId()))
                .execute();

        String expected = iosContent.getBundleId();

        Map<Long, String> repoResult = repoUnderTest.getMobileContentAppIds(shard, singletonList(iosGroupId));

        assumeThat("Вернулась одна запись",
                repoResult.keySet(), hasSize(1));
        assumeThat("Вернулась запись по интересующей нас группе",
                repoResult.keySet(), contains(iosGroupId));

        String actual = repoResult.get(iosGroupId);

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void deleteAdGroupWithBidModifiers() {
        Long adGroupId = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId()), campaignInfo)
                .getAdGroupId();
        BidModifierDemographics bidModifier = createEmptyDemographicsModifier()
                .withCampaignId(campaignInfo.getCampaignId())
                .withAdGroupId(adGroupId)
                .withDemographicsAdjustments(
                        singletonList(createDefaultDemographicsAdjustment().withAge(AgeType._25_34).withPercent(110))
                );

        DSLContext dslContext = dslContextProvider.ppc(shard);

        bidModifierRepository.replaceModifiers(dslContext, singletonList(bidModifier),
                singleton(new CampaignIdAndAdGroupIdPair()
                        .withCampaignId(campaignInfo.getCampaignId())
                        .withAdGroupId(adGroupId)), clientId, operatorUid);

        List<BidModifier> adGroupBidModifiers = bidModifierRepository.getByAdGroupIds(
                shard, singletonMap(adGroupId, campaignInfo.getCampaignId()),
                singleton(BidModifierType.DEMOGRAPHY_MULTIPLIER), singleton(BidModifierLevel.ADGROUP));
        assumeThat("корректиравка успешно установлена", adGroupBidModifiers.size(), equalTo(1));

        List<Long> deletedAdGroupIds = repoUnderTest.delete(shard, clientId, operatorUid, singletonList(adGroupId));
        assumeThat("группа должна быть успешно удалена", deletedAdGroupIds, containsInAnyOrder(adGroupId));

        List<BidModifier> deletedAdGroupBidModifiers = bidModifierRepository.getByAdGroupIds(
                shard, singletonMap(adGroupId, campaignInfo.getCampaignId()),
                singleton(BidModifierType.DEMOGRAPHY_MULTIPLIER), singleton(BidModifierLevel.ADGROUP));

        assertThat("корректиравка должна быть удалена вместе с группой", deletedAdGroupBidModifiers.size(), equalTo(0));
    }

    @Test
    public void delete_WithGroupParams_GroupParamsAreDeletedToo() {
        Long adGroupId = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(campaignInfo.getCampaignId()), campaignInfo)
                .getAdGroupId();

        dslContextProvider.ppc(shard)
                .insertInto(GROUP_PARAMS)
                .set(GROUP_PARAMS.PID, adGroupId)
                .execute();

        List<Long> deletedAdGroupIds = repoUnderTest.delete(shard, clientId, operatorUid, singletonList(adGroupId));
        assumeThat("Группа должна быть успешно удалена", deletedAdGroupIds, containsInAnyOrder(adGroupId));

        assertThat("Параметры группы должны быть удалены вместе с группой", dslContextProvider.ppc(shard)
                .select(GROUP_PARAMS.PID).from(GROUP_PARAMS)
                .where(GROUP_PARAMS.PID.eq(adGroupId))
                .fetch(), empty());
    }

    @Test
    public void delete_successPerformanceAdGroup() {
        //Создаём группу типа PerformanceAdGroup с записью в ppc.adgroups_performance
        PerformanceAdGroupInfo performanceAdGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        int shard = performanceAdGroupInfo.getShard();
        ClientId clientId = performanceAdGroupInfo.getClientId();
        long uid = performanceAdGroupInfo.getClientInfo().getUid();
        Long adGroupId = performanceAdGroupInfo.getAdGroupId();
        Optional<Record1<Long>> startRecord = readAdGroupsPerformanceTable(shard, adGroupId);
        checkState(startRecord.isPresent());

        //Удаляем группу
        repoUnderTest.delete(shard, clientId, uid, singletonList(adGroupId));

        //Проверяем, что запись в ppc.adgroups_performance тоже удалена
        Optional<Record1<Long>> actualRecord = readAdGroupsPerformanceTable(shard, adGroupId);
        Assertions.assertThat(actualRecord).isNotPresent();
    }

    private Optional<Record1<Long>> readAdGroupsPerformanceTable(int shard, Long adGroupId) {
        DSLContext dslContext = dslContextProvider.ppc(shard);
        return dslContext.select(ADGROUPS_PERFORMANCE.PID)
                .from(AdgroupsPerformance.ADGROUPS_PERFORMANCE)
                .where(AdgroupsPerformance.ADGROUPS_PERFORMANCE.PID.eq(adGroupId))
                .fetchOptional();
    }

    @Test
    public void updateStatusAutoBudgetShow_SetTrue() {
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId());
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);
        List<Long> adGroupIds = singletonList(adGroupInfo.getAdGroupId());

        repoUnderTest.updateStatusAutoBudgetShow(shard, adGroupIds, StatusAutobudgetShow.YES);
        assertThat("Поле должно быть в правильном состоянии",
                repoUnderTest.getAdGroups(shard, adGroupIds).get(0).getStatusAutobudgetShow(), is(true));
    }

    @Test
    public void updateStatusAutoBudgetShow_SetFalse() {
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId());
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);
        List<Long> adGroupIds = singletonList(adGroupInfo.getAdGroupId());

        repoUnderTest.updateStatusAutoBudgetShow(shard, adGroupIds, StatusAutobudgetShow.NO);
        assertThat("Поле должно быть в правильном состоянии",
                repoUnderTest.getAdGroups(shard, adGroupIds).get(0).getStatusAutobudgetShow(), is(false));
    }

    @Test
    public void dropStatusModerate_statusDropped() {
        AdGroup adGroup = defaultTextAdGroup(campaignInfo.getCampaignId());
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);
        List<Long> adGroupIds = singletonList(adGroupInfo.getAdGroupId());

        repoUnderTest.dropStatusModerateExceptTypesWithoutModeration(dslContextProvider.ppc(shard).dsl(), adGroupIds);

        assertThat("Статус должен быть в правильном состоянии",
                repoUnderTest.getAdGroups(shard, adGroupIds).get(0).getStatusModerate(), is(StatusModerate.READY));
        assertThat("Статус должен быть в правильном состоянии",
                repoUnderTest.getAdGroups(shard, adGroupIds).get(0).getStatusPostModerate(), is(StatusPostModerate.NO));
    }

    @Test
    public void dropStatusModerate_PerformanceAdgroups_statusNotDropped() {
        PerformanceAdGroupInfo performanceAdGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        List<Long> adGroupIds = singletonList(performanceAdGroupInfo.getAdGroupId());

        repoUnderTest.dropStatusModerateExceptTypesWithoutModeration(dslContextProvider.ppc(shard).dsl(), adGroupIds);

        StatusModerate expectedStatusModerate = performanceAdGroupInfo.getAdGroup().getStatusModerate();
        StatusPostModerate expectedStatusPostModerate = performanceAdGroupInfo.getAdGroup().getStatusPostModerate();

        assertThat("Статус не должен был измениться",
                repoUnderTest.getAdGroups(shard, adGroupIds).get(0).getStatusModerate(), is(expectedStatusModerate));
        assertThat("Статус не должен был измениться",
                repoUnderTest.getAdGroups(shard, adGroupIds).get(0).getStatusPostModerate(),
                is(expectedStatusPostModerate));
    }

    @Test
    public void dropStatusModerate_InternalAdgroups_statusNotDropped() {
        AdGroupInfo internalAdGroupInfo = steps.adGroupSteps().createDefaultInternalAdGroup();
        List<Long> adGroupIds = singletonList(internalAdGroupInfo.getAdGroupId());

        repoUnderTest.dropStatusModerateExceptTypesWithoutModeration(dslContextProvider.ppc(shard).dsl(), adGroupIds);

        StatusModerate expectedStatusModerate = internalAdGroupInfo.getAdGroup().getStatusModerate();
        StatusPostModerate expectedStatusPostModerate = internalAdGroupInfo.getAdGroup().getStatusPostModerate();

        assertThat("Статус не должен был измениться",
                repoUnderTest.getAdGroups(shard, adGroupIds).get(0).getStatusModerate(), is(expectedStatusModerate));
        assertThat("Статус не должен был измениться",
                repoUnderTest.getAdGroups(shard, adGroupIds).get(0).getStatusPostModerate(),
                is(expectedStatusPostModerate));
    }

    @Test
    public void getMobileStoreContentHrefs() {
        MobileContentAdGroup mobileContentAdGroup = createMobileAppAdGroup(null, androidMobileContent());
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(mobileContentAdGroup);
        Long adGroupId = adGroupInfo.getAdGroupId();
        Map<Long, String> storeContentHrefs = repoUnderTest.getMobileStoreContentHrefs(shard, singletonList(adGroupId));
        assertThat("У группы должен быть правильный store url", storeContentHrefs.get(adGroupId),
                is(mobileContentAdGroup.getStoreUrl()));
    }

    @Test
    public void getDynamicMainDomains() {
        DynamicTextAdGroup adGroup = activeDynamicTextAdGroup(null);
        steps.adGroupSteps().createAdGroup(adGroup);
        Map<Long, String> dynamicAdGroupsDomains =
                repoUnderTest.getDynamicMainDomains(shard, singletonList(adGroup.getId()));
        assertThat("У группы должен быть правильный domain url", adGroup.getDomainUrl(),
                is(dynamicAdGroupsDomains.get(adGroup.getId())));
    }

    @Test
    public void getPublisherDomainIds_OneGroupWithDomainAndOneWithout() {
        String storeUrl1 = "https://play.google.com/store/apps/details?id=aa.oo.zz1";
        String storeUrl2 = "https://play.google.com/store/apps/details?id=aa.oo.zz2";

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaign = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo);

        MobileContent mobileContentWithDomainId = steps.mobileContentSteps().createMobileContent(
                        new MobileContentInfo()
                                .withClientInfo(clientInfo)
                                .withMobileContent(mobileContentFromStoreUrl(storeUrl1)))
                .getMobileContent();
        MobileContent mobileContentWithNoDomainId = steps.mobileContentSteps().createMobileContent(
                        new MobileContentInfo()
                                .withClientInfo(clientInfo)
                                .withMobileContent(mobileContentFromStoreUrl(storeUrl2)), false)
                .getMobileContent();

        MobileContentAdGroup mobileAdGroupWithDomain =
                createMobileAppAdGroup(campaign.getCampaignId(), mobileContentWithDomainId)
                        .withStoreUrl(storeUrl1);
        steps.adGroupSteps().createAdGroup(mobileAdGroupWithDomain, campaign);

        MobileContentAdGroup mobileAdGroupWithNoDomain =
                createMobileAppAdGroup(campaign.getCampaignId(), mobileContentWithNoDomainId)
                        .withStoreUrl(storeUrl2);
        steps.adGroupSteps().createAdGroup(mobileAdGroupWithNoDomain, campaign);

        Map<Long, Long> domainIds = repoUnderTest.getPublisherDomainIds(shard,
                asList(mobileAdGroupWithDomain.getId(), mobileAdGroupWithNoDomain.getId()));

        assertThat("Только одна группа имеет домен", domainIds.values(), hasSize(1));
        assertThat("У группы должен быть правильный домен", domainIds.get(mobileAdGroupWithDomain.getId()),
                is(mobileContentWithDomainId.getPublisherDomainId()));
        assertThat("У группы должен быть правильный домен", domainIds.get(mobileAdGroupWithNoDomain.getId()),
                is(mobileContentWithNoDomainId.getPublisherDomainId()));
    }

    @Test
    public void getAdGroupIdsWithConditionsIsEmpty_WhenAdGroupIsEmpty() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);

        Set<Long> adGroupIdsWithConditions =
                repoUnderTest.getAdGroupIdsWithConditions(shard, singletonList(adGroupInfo.getAdGroupId()));

        Assertions.assertThat(adGroupIdsWithConditions)
                .isEmpty();
    }

    @Test
    public void getAdGroupIdsWithConditions_WhenAdGroupHasSuspendedTextBid() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        steps.keywordSteps().createKeyword(adGroupInfo, keywordWithText("a").withIsSuspended(Boolean.TRUE));

        Set<Long> adGroupIdsWithConditions =
                repoUnderTest.getAdGroupIdsWithConditions(shard, singletonList(adGroupInfo.getAdGroupId()));

        Assertions.assertThat(adGroupIdsWithConditions)
                .isEmpty();
    }

    @Test
    public void getAdGroupIdsWithAnyConditionsIsEmpty_WhenAdGroupIsEmpty() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);

        Set<Long> adGroupIdsWithConditions =
                repoUnderTest.getAdGroupIdsWithAnyConditions(shard, singletonList(adGroupInfo.getAdGroupId()));

        Assertions.assertThat(adGroupIdsWithConditions)
                .isEmpty();
    }

    @Test
    public void getAdGroupIdsWithAnyConditions_WhenAdGroupHasSuspendedTextBid() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        steps.keywordSteps().createKeyword(adGroupInfo, keywordWithText("a").withIsSuspended(Boolean.TRUE));

        Set<Long> adGroupIdsWithConditions =
                repoUnderTest.getAdGroupIdsWithAnyConditions(shard, singletonList(adGroupInfo.getAdGroupId()));

        Assertions.assertThat(adGroupIdsWithConditions)
                .containsExactly(adGroupInfo.getAdGroupId());
    }

    @Test
    public void getAdGroupIdsWithConditions_WhenAdGroupHasDynamicBid() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaignInfo);
        steps.dynamicConditionsFakeSteps().addDefaultBidsDynamic(adGroupInfo);

        Set<Long> adGroupIdsWithConditions =
                repoUnderTest.getAdGroupIdsWithConditions(shard, singletonList(adGroupInfo.getAdGroupId()));

        Assertions.assertThat(adGroupIdsWithConditions)
                .containsExactly(adGroupInfo.getAdGroupId());
    }

    @Test
    public void getAdGroupIdsWithConditionsIsEmpty_WhenAdGroupHasOnlySuspendedDynamicBid() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaignInfo);
        BidsDynamicRecord bidsDynamicRecord = new BidsDynamicRecord();
        bidsDynamicRecord.setOpts(BidDynamicOpt.SUSPENDED.getTypedValue());
        steps.dynamicConditionsFakeSteps().addBidsDynamic(adGroupInfo, bidsDynamicRecord);

        Set<Long> adGroupIdsWithConditions =
                repoUnderTest.getAdGroupIdsWithConditions(shard, singletonList(adGroupInfo.getAdGroupId()));

        Assertions.assertThat(adGroupIdsWithConditions)
                .isEmpty();
    }

    @Test
    public void getAdGroupIdsWithConditions_WhenAdGroupHasPerformanceBid() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        steps.performanceFilterSteps().addDefaultBidsPerformance(adGroupInfo);

        Set<Long> adGroupIdsWithConditions =
                repoUnderTest.getAdGroupIdsWithConditions(shard, singletonList(adGroupInfo.getAdGroupId()));

        Assertions.assertThat(adGroupIdsWithConditions)
                .containsExactly(adGroupInfo.getAdGroupId());
    }

    @Test
    public void getAdGroupIdsWithConditionsIsEmpty_WhenAdGroupHasOnlySuspendedPerformanceBid() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        BidsPerformanceRecord bidsPerformanceRecord = PerformanceFiltersSteps.getDefaultBidsPerformanceRecord();
        bidsPerformanceRecord.setIsSuspended(RepositoryUtils.TRUE);
        steps.performanceFilterSteps().addBidsPerformance(adGroupInfo, bidsPerformanceRecord);

        Set<Long> adGroupIdsWithConditions =
                repoUnderTest.getAdGroupIdsWithConditions(shard, singletonList(adGroupInfo.getAdGroupId()));

        Assertions.assertThat(adGroupIdsWithConditions)
                .isEmpty();
    }

    @Test
    public void getAdGroupIdsWithConditionsIsEmpty_WhenAdGroupHasOnlyDeletedPerformanceBid() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        BidsPerformanceRecord bidsPerformanceRecord = PerformanceFiltersSteps.getDefaultBidsPerformanceRecord();
        bidsPerformanceRecord.setIsDeleted(RepositoryUtils.TRUE);
        steps.performanceFilterSteps().addBidsPerformance(adGroupInfo, bidsPerformanceRecord);

        Set<Long> adGroupIdsWithConditions =
                repoUnderTest.getAdGroupIdsWithConditions(shard, singletonList(adGroupInfo.getAdGroupId()));

        Assertions.assertThat(adGroupIdsWithConditions)
                .isEmpty();
    }

    @Test
    public void getGeoproductAvailabilityByCampaignId() {
        CampaignInfo emptyCampaign = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);

        steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        CampaignInfo cmpCampaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        steps.adGroupSteps().createActiveCpmGeoproductAdGroup(cmpCampaignInfo);
        steps.adGroupSteps().createActiveCpmBannerAdGroup(cmpCampaignInfo);

        Long emptyCampaignId = emptyCampaign.getCampaignId();
        Long textCampaignId = campaignInfo.getCampaignId();
        Long cmpCampaignId = cmpCampaignInfo.getCampaignId();
        List<Long> campaignIds = List.of(emptyCampaignId, textCampaignId, cmpCampaignId);
        Map<Long, GeoproductAvailability> anyGeoproductByCampaignId =
                repoUnderTest.getGeoproductAvailabilityByCampaignId(shard, campaignIds);

        Map<Long, GeoproductAvailability> expectedResult = new HashMap<>();
        expectedResult.put(emptyCampaignId, GeoproductAvailability.EMPTY);
        expectedResult.put(textCampaignId, GeoproductAvailability.NO);
        expectedResult.put(cmpCampaignId, GeoproductAvailability.YES);

        assertThat(anyGeoproductByCampaignId, beanDiffer(expectedResult));
    }

    private List<Long> createAdGroups(int shard, ClientId clientId, AdGroup... adGroups) {
        return repoUnderTest.addAdGroups(dslContextProvider.ppc(shard).configuration(), clientId, asList(adGroups));
    }
}
