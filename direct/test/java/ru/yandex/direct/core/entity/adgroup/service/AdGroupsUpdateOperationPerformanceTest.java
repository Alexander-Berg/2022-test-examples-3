package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;
import java.util.Map;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.service.validation.types.PerformanceAdGroupValidation;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.entity.feed.validation.FeedDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.BaseDynSmartAdGroupValidationService.MAX_BODY_LENGTH;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.BaseDynSmartAdGroupValidationService.MAX_NAME_LENGTH;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentCreativeGeoToAdGroupGeo;
import static ru.yandex.direct.core.testing.data.TestBanners.activePerformanceBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestGroups.draftPerformanceAdGroup;
import static ru.yandex.direct.operation.Applicability.FULL;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class AdGroupsUpdateOperationPerformanceTest extends AdGroupsUpdateOperationTestBase {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    protected Steps steps;
    @Autowired
    private OldBannerRepository bannerRepository;
    @Autowired
    private CreativeRepository creativeRepository;

    @Test
    public void prepareAndApply_success_noChanges() {
        //Подготвавливаем исходные данные
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        PerformanceAdGroup adGroup = adGroupInfo.getPerformanceAdGroup();
        assumeThat(adGroup.getStatusBLGenerated(), is(StatusBLGenerated.YES));
        assumeThat(adGroup.getStatusBsSynced(), is(StatusBsSynced.YES));
        Integer shard = adGroupInfo.getShard();
        Long adGroupId = adGroupInfo.getAdGroupId();
        String oldName = adGroup.getFieldToUseAsName();
        String oldBody = adGroup.getFieldToUseAsBody();

        //Ожидаемое состояние группы
        PerformanceAdGroup expectedGroup = new PerformanceAdGroup()
                .withId(adGroupId)
                .withFieldToUseAsName(oldName)
                .withFieldToUseAsBody(oldBody)
                .withStatusBLGenerated(StatusBLGenerated.YES)
                .withStatusBsSynced(StatusBsSynced.YES);

        //Выполняем операцию
        ModelChanges<PerformanceAdGroup> modelWithoutChanges = new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                .process(oldName, PerformanceAdGroup.FIELD_TO_USE_AS_NAME)
                .process(oldBody, PerformanceAdGroup.FIELD_TO_USE_AS_BODY);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelWithoutChanges.castModelUp(AdGroup.class)), adGroupInfo);
        MassResult<Long> longMassResult = updateOperation.prepareAndApply();
        checkState(longMassResult.getErrorCount() == 0);

        //Читаем актаульное состояние из базы
        AdGroup actual = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        //Сверяем прочитанное из базы с ожиданиями.
        assertThat(actual)
                .is(matchedBy(beanDiffer(expectedGroup)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void prepareAndApply_success_withChanges() {
        //Подготвавливаем исходные данные
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        PerformanceAdGroup adGroup = adGroupInfo.getPerformanceAdGroup();
        assumeThat(adGroup.getStatusBLGenerated(), is(StatusBLGenerated.YES));
        assumeThat(adGroup.getStatusBsSynced(), is(StatusBsSynced.YES));
        Integer shard = adGroupInfo.getShard();
        Long adGroupId = adGroupInfo.getAdGroupId();
        String newName = "The changed name";
        String newBody = "The changed body";

        //Ожидаемое состояние группы
        PerformanceAdGroup expectedGroup = new PerformanceAdGroup()
                .withStatusBLGenerated(StatusBLGenerated.PROCESSING)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withFieldToUseAsName(newName)
                .withFieldToUseAsBody(newBody);

        //Выполняем операцию
        ModelChanges<PerformanceAdGroup> modelChanges = new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                .process(newName, PerformanceAdGroup.FIELD_TO_USE_AS_NAME)
                .process(newBody, PerformanceAdGroup.FIELD_TO_USE_AS_BODY);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)), adGroupInfo);
        MassResult<Long> longMassResult = updateOperation.prepareAndApply();
        checkState(longMassResult.getErrorCount() == 0);

        //Читаем актаульное состояние из базы
        AdGroup actual = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        //Сверяем прочитанное из базы с ожиданиями.
        assertThat(actual)
                .is(matchedBy(beanDiffer(expectedGroup)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void prepareAndApply_moderateAdGroup_onChangeAdGroupWithForceModerate() {
        // делаем update группе-черновику с ModerationMode=FORCE_MODERATE
        // ожидаем, что группа сразу получит statusModerate=Yes
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed();
        CampaignInfo campaignInfo = steps.campaignSteps().createActivePerformanceCampaign(feedInfo.getClientInfo());
        AdGroupInfo draftAdGroup = steps.adGroupSteps().createAdGroup(
                draftPerformanceAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId()), campaignInfo);
        assumeThat(draftAdGroup.getAdGroup().getStatusModerate(),
                is(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.NEW));
        Long adGroupId = draftAdGroup.getAdGroupId();

        ModelChanges<PerformanceAdGroup> modelChanges = new ModelChanges<>(adGroupId, PerformanceAdGroup.class);

        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)),
                        ModerationMode.FORCE_MODERATE,
                        draftAdGroup.getUid(), draftAdGroup.getClientId(), draftAdGroup.getShard());

        MassResult<Long> result = updateOperation.prepareAndApply();
        assumeThat(result.getErrorCount(), is(0));

        AdGroup actual = adGroupRepository.getAdGroups(draftAdGroup.getShard(), singletonList(adGroupId)).get(0);
        PerformanceAdGroup expectedGroup = new PerformanceAdGroup()
                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
        assertThat(actual)
                .is(matchedBy(beanDiffer(expectedGroup)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void prepareAndApply_failureWhenTryToChangeFeed() {
        //Подготвавливаем данные в базе (группу и новый фид)
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        Integer shard = adGroupInfo.getShard();
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long startFeedId = adGroupInfo.getFeedId();
        FeedInfo newFeedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long newFeedId = newFeedInfo.getFeedId();

        //Группа с новыми полями
        PerformanceAdGroup groupWithChangedFields = new PerformanceAdGroup()
                .withFeedId(newFeedId);

        //Выполняем операцию
        ModelChanges<PerformanceAdGroup> modelChanges = new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                .process(groupWithChangedFields.getFeedId(), PerformanceAdGroup.FEED_ID)
                .process(groupWithChangedFields.getStatusBLGenerated(), PerformanceAdGroup.STATUS_B_L_GENERATED)
                .process(groupWithChangedFields.getFieldToUseAsName(), PerformanceAdGroup.FIELD_TO_USE_AS_NAME)
                .process(groupWithChangedFields.getFieldToUseAsBody(), PerformanceAdGroup.FIELD_TO_USE_AS_BODY);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)), adGroupInfo);
        MassResult<Long> longMassResult = updateOperation.prepareAndApply();
        ValidationResult<?, Defect> validationResult = longMassResult.getValidationResult();

        //Читаем актаульное состояние из базы
        AdGroup actual = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        PerformanceAdGroup actualAdGroup = (PerformanceAdGroup) actual;
        Long actualFeedId = actualAdGroup.getFeedId();

        //Сверяем ожидания и реальность.
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualFeedId).isEqualTo(startFeedId);
            soft.assertThat(validationResult).is(matchedBy(hasDefectDefinitionWith(
                    validationError(path(index(0), field(PerformanceAdGroup.FEED_ID)),
                            FeedDefects.feedNotExist()))));
        });
    }

    @Test
    public void prepareAndApply_failureWhenTryFieldsNameIsTooLong() {
        //Подготвавливаем данные в базе (группу и новый фид)
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        Long adGroupId = adGroupInfo.getAdGroupId();

        //Группа с новыми полями
        PerformanceAdGroup groupWithChangedFields = new PerformanceAdGroup()
                .withFieldToUseAsName(StringUtils.repeat('A', MAX_NAME_LENGTH + 1))
                .withFieldToUseAsBody(StringUtils.repeat('A', MAX_BODY_LENGTH + 1));

        //Выполняем операцию
        ModelChanges<PerformanceAdGroup> modelChanges = new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                .process(groupWithChangedFields.getFeedId(), PerformanceAdGroup.FEED_ID)
                .process(groupWithChangedFields.getStatusBLGenerated(), PerformanceAdGroup.STATUS_B_L_GENERATED)
                .process(groupWithChangedFields.getFieldToUseAsName(), PerformanceAdGroup.FIELD_TO_USE_AS_NAME)
                .process(groupWithChangedFields.getFieldToUseAsBody(), PerformanceAdGroup.FIELD_TO_USE_AS_BODY);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)), adGroupInfo);
        MassResult<Long> longMassResult = updateOperation.prepareAndApply();
        ValidationResult<?, Defect> validationResult = longMassResult.getValidationResult();

        //Сверяем ожидания и реальность.
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(validationResult).is(matchedBy(hasDefectDefinitionWith(
                    validationError(path(index(0), field(PerformanceAdGroup.FIELD_TO_USE_AS_NAME)),
                            FeedDefects.feedNameFieldIsTooLong(PerformanceAdGroupValidation.MAX_NAME_LENGTH)))));
            soft.assertThat(validationResult).is(matchedBy(hasDefectDefinitionWith(
                    validationError(path(index(0), field(PerformanceAdGroup.FIELD_TO_USE_AS_BODY)),
                            FeedDefects.feedBodyFieldIsTooLong(MAX_BODY_LENGTH)))));
        });
    }

    @Test
    public void prepareAndApply_successSetAppropriateGeo() {
        //Подготвавливаем исходные данные
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        ClientId clientId = adGroupInfo.getClientId();
        Creative creative = defaultPerformanceCreative(clientId, null)
                .withSumGeo(singletonList(Region.RUSSIA_REGION_ID));
        ClientInfo clientInfo = adGroupInfo.getClientInfo();
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        Long campaignId = adGroupInfo.getCampaignId();
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long creativeId = creativeInfo.getCreativeId();
        int shard = adGroupInfo.getShard();
        OldPerformanceBanner banner = activePerformanceBanner(campaignId, adGroupId, creativeId);
        bannerRepository.addBanners(shard, singletonList(banner));
        List<Long> newGeo = asList(Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID, -Region.MOSCOW_REGION_ID,
                Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID, -Region.SAINT_PETERSBURG_REGION_ID);

        //Группа с новыми полями
        PerformanceAdGroup groupWithChangedFields = new PerformanceAdGroup()
                .withGeo(newGeo);

        //Выполняем операцию
        ModelChanges<PerformanceAdGroup> modelChanges =
                new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                        .process(groupWithChangedFields.getGeo(), PerformanceAdGroup.GEO);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)), adGroupInfo);
        MassResult<Long> longMassResult = updateOperation.prepareAndApply();
        ValidationResult<?, Defect> validationResult = longMassResult.getValidationResult();

        //Сверяем ожидания и реальность.
        AdGroup actual = adGroupRepository.getAdGroups(this.shard, singletonList(adGroupId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actual.getGeo()).containsSequence(newGeo);
            soft.assertThat(validationResult).is(matchedBy(hasNoErrorsAndWarnings()));
        });
    }

    public Object[][] prepareAndApply_successUpdateCreativesSumGeo_params() {
        return new Object[][]{
                {"if StatusModerate is NEW then SumGeo has to be updated",
                        StatusModerate.NEW,
                        asList(Region.UKRAINE_REGION_ID, Region.RUSSIA_REGION_ID),
                        singletonList(Region.SAINT_PETERSBURG_REGION_ID),
                        new Long[]{Region.RUSSIA_REGION_ID}},
                {"if StatusModerate is YES then SumGeo has not to be updated",
                        StatusModerate.YES,
                        asList(Region.UKRAINE_REGION_ID, Region.RUSSIA_REGION_ID),
                        singletonList(Region.SAINT_PETERSBURG_REGION_ID),
                        new Long[]{Region.RUSSIA_REGION_ID, Region.UKRAINE_REGION_ID}},
                {"if StatusModerate is ERROR then SumGeo has to be updated",
                        StatusModerate.ERROR,
                        asList(Region.UKRAINE_REGION_ID, Region.RUSSIA_REGION_ID),
                        singletonList(Region.SAINT_PETERSBURG_REGION_ID),
                        new Long[]{Region.RUSSIA_REGION_ID}},
                {"if StatusModerate is NO then SumGeo has to be updated",
                        StatusModerate.NO,
                        asList(Region.UKRAINE_REGION_ID, Region.RUSSIA_REGION_ID),
                        singletonList(Region.SAINT_PETERSBURG_REGION_ID),
                        new Long[]{Region.RUSSIA_REGION_ID}},
        };
    }

    @Test
    @Parameters(method = "prepareAndApply_successUpdateCreativesSumGeo_params")
    public void prepareAndApply_successUpdateCreativesSumGeo(String description,
                                                             StatusModerate statusModerate,
                                                             List<Long> startSumGeoRegionIds,
                                                             List<Long> newAdGroupGeoRegionIds,
                                                             Long[] expectedSumGeoRegionIds) {
        //Подготвавливаем исходные группу и креатив
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        ClientId clientId = adGroupInfo.getClientId();
        Creative creative = defaultPerformanceCreative(clientId, null)
                .withStatusModerate(statusModerate)
                .withSumGeo(startSumGeoRegionIds);
        ClientInfo clientInfo = adGroupInfo.getClientInfo();
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        Long campaignId = adGroupInfo.getCampaignId();
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long creativeId = creativeInfo.getCreativeId();
        int shard = adGroupInfo.getShard();
        OldPerformanceBanner banner = activePerformanceBanner(campaignId, adGroupId, creativeId);
        bannerRepository.addBanners(shard, singletonList(banner));

        //Обновляем гео в группе
        PerformanceAdGroup groupWithChangedFields = new PerformanceAdGroup()
                .withGeo(newAdGroupGeoRegionIds);
        ModelChanges<PerformanceAdGroup> modelChanges =
                new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                        .process(groupWithChangedFields.getGeo(), PerformanceAdGroup.GEO);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)), adGroupInfo);
        updateOperation.prepareAndApply();

        //Проеверяем гео в креативе
        Map<Long, List<Creative>> creativesByPerformanceAdGroupIds =
                creativeRepository.getCreativesByPerformanceAdGroupIds(shard, clientId, null, singletonList(adGroupId));
        List<Long> actualSumGeo = creativesByPerformanceAdGroupIds.get(adGroupId).get(0).getSumGeo();
        assertThat(actualSumGeo).as(description).containsExactlyInAnyOrder(expectedSumGeoRegionIds);
    }

    @Test
    public void prepareAndApply_failureSetInappropriateGeo() {
        //Подготвавливаем данные
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        ClientId clientId = adGroupInfo.getClientId();
        Creative creative = defaultPerformanceCreative(clientId, null)
                .withSumGeo(singletonList(Region.RUSSIA_REGION_ID));
        ClientInfo clientInfo = adGroupInfo.getClientInfo();
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        Long campaignId = adGroupInfo.getCampaignId();
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long creativeId = creativeInfo.getCreativeId();
        int shard = adGroupInfo.getShard();
        OldPerformanceBanner banner = activePerformanceBanner(campaignId, adGroupId, creativeId);
        bannerRepository.addBanners(shard, singletonList(banner));
        List<Long> startGeo = adGroupInfo.getAdGroup().getGeo();
        List<Long> newGeo = asList(Region.TURKEY_REGION_ID, -Region.ISTANBUL_REGION_ID);

        //Группа с новыми полями
        PerformanceAdGroup groupWithChangedFields = new PerformanceAdGroup()
                .withGeo(newGeo);

        //Выполняем операцию
        ModelChanges<PerformanceAdGroup> modelChanges =
                new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                        .process(groupWithChangedFields.getGeo(), PerformanceAdGroup.GEO);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)), adGroupInfo);
        MassResult<Long> longMassResult = updateOperation.prepareAndApply();
        ValidationResult<?, Defect> validationResult = longMassResult.getValidationResult();

        //Сверяем ожидания и реальность.
        AdGroup actual = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actual.getGeo()).containsSequence(startGeo);
            soft.assertThat(validationResult).is(matchedBy(hasDefectDefinitionWith(
                    validationError(path(index(0), field(PerformanceAdGroup.GEO)),
                            inconsistentCreativeGeoToAdGroupGeo()))));
        });
    }


    public Object[][] prepareAndApply_updateGeoCreativeStatusModerate_params() {
        return new Object[][]{
                {"if StatusModerate is NEW then status has not to be changed",
                        StatusModerate.NEW,
                        StatusModerate.NEW},
                {"if StatusModerate is YES then status has not to be changed",
                        StatusModerate.YES,
                        StatusModerate.YES},
                {"if StatusModerate is ERROR then status has not to be changed",
                        StatusModerate.ERROR,
                        StatusModerate.ERROR},
                {"if StatusModerate is READY then status has not to be changed",
                        StatusModerate.READY,
                        StatusModerate.READY},
                {"if StatusModerate is NO then SumGeo has to be changed",
                        StatusModerate.NO,
                        StatusModerate.READY},
        };
    }

    @Test
    @Parameters(method = "prepareAndApply_updateGeoCreativeStatusModerate_params")
    public void prepareAndApply_updateGeoCreativeStatusModerate(String description,
                                                                StatusModerate statusModerateBeforeGeoUpdate,
                                                                StatusModerate expectedStatusModerateAfterGeoUpdate) {
        // Подготавливаем данные
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        ClientId clientId = adGroupInfo.getClientId();
        Creative creative = defaultPerformanceCreative(clientId, null)
                .withStatusModerate(statusModerateBeforeGeoUpdate)
                .withSumGeo(asList(Region.UKRAINE_REGION_ID, Region.RUSSIA_REGION_ID));
        ClientInfo clientInfo = adGroupInfo.getClientInfo();
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        Long campaignId = adGroupInfo.getCampaignId();
        Long adGroupId = adGroupInfo.getAdGroupId();
        Long creativeId = creativeInfo.getCreativeId();
        int shard = adGroupInfo.getShard();
        OldPerformanceBanner banner = activePerformanceBanner(campaignId, adGroupId, creativeId);
        bannerRepository.addBanners(shard, singletonList(banner));
        List<Long> startGeo = adGroupInfo.getAdGroup().getGeo();
        List<Long> newGeo = singletonList(Region.SAINT_PETERSBURG_REGION_ID);

        //Группа с новыми полями
        PerformanceAdGroup groupWithChangedFields = new PerformanceAdGroup()
                .withGeo(newGeo);

        //Выполняем операцию
        ModelChanges<PerformanceAdGroup> modelChanges =
                new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                        .process(groupWithChangedFields.getGeo(), PerformanceAdGroup.GEO);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)), adGroupInfo);

        updateOperation.prepareAndApply();

        // Проверяем статус модерации в креативе
        Map<Long, List<Creative>> creativesByPerformanceAdGroupIds =
                creativeRepository.getCreativesByPerformanceAdGroupIds(shard, clientId, null, singletonList(adGroupId));
        StatusModerate actualStatusModerate =
                creativesByPerformanceAdGroupIds.get(adGroupId).get(0).getStatusModerate();
        assertThat(actualStatusModerate).isEqualTo(expectedStatusModerateAfterGeoUpdate);
    }

    @Test
    public void prepareAndApply_updateGeoDoesNotAffectObsoleteCreative() {
        // Подготавливаем данные
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        Long adGroupId = adGroupInfo.getAdGroupId();

        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(
                defaultPerformanceCreative(adGroupInfo.getClientId(), null)
                        .withLayoutId(31L) // устаревший "Большое изображение товара с описанием и каруселью"
                        .withStatusModerate(StatusModerate.NO)
                        .withSumGeo(asList(Region.UKRAINE_REGION_ID, Region.RUSSIA_REGION_ID)),
                adGroupInfo.getClientInfo());
        OldPerformanceBanner banner = activePerformanceBanner(
                adGroupInfo.getCampaignId(), adGroupId, creativeInfo.getCreativeId());
        bannerRepository.addBanners(adGroupInfo.getShard(), singletonList(banner));

        // Выполняем операцию
        ModelChanges<PerformanceAdGroup> modelChanges =
                new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                        .process(List.of(Region.SAINT_PETERSBURG_REGION_ID), PerformanceAdGroup.GEO);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)), adGroupInfo);
        updateOperation.prepareAndApply();

        // Проверяем статус модерации в креативе
        Map<Long, List<Creative>> creativesByPerformanceAdGroupIds =
                creativeRepository.getCreativesByPerformanceAdGroupIds(adGroupInfo.getShard(),
                        adGroupInfo.getClientId(), null, singletonList(adGroupId));
        StatusModerate actualStatusModerate =
                creativesByPerformanceAdGroupIds.get(adGroupId).get(0).getStatusModerate();
        assertThat(actualStatusModerate).isEqualTo(StatusModerate.NO);
    }

    @Test
    public void prepareAndApply_successUpdateFeedId() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        ClientInfo clientInfo = adGroupInfo.getClientInfo();
        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();
        Long adGroupId = adGroupInfo.getAdGroupId();

        PerformanceAdGroup groupWithChangedFields = new PerformanceAdGroup().withFeedId(feedId);

        ModelChanges<PerformanceAdGroup> modelChanges = new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                .process(groupWithChangedFields.getFeedId(), PerformanceAdGroup.FEED_ID);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)), adGroupInfo);
        MassResult<Long> longMassResult = updateOperation.prepareAndApply();
        ValidationResult<?, Defect> validationResult = longMassResult.getValidationResult();

        AdGroup actual = adGroupRepository.getAdGroups(this.shard, singletonList(adGroupId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            assertThat(actual).isInstanceOf(PerformanceAdGroup.class);
            soft.assertThat(((PerformanceAdGroup) actual).getFeedId()).isEqualTo(feedId);
            soft.assertThat(validationResult).is(matchedBy(hasNoErrorsAndWarnings()));
        });
    }
}
