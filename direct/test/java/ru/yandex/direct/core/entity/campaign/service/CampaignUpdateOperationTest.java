package ru.yandex.direct.core.entity.campaign.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.MinusKeywordPreparingTool;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignOpts;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateCampaignValidationService;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignUpdateOperationTest {

    private static final CompareStrategy STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath("lastChange"), newPath("minusKeywords"));

    private static final String NEW_NAME = "такоевотновоеимечко " + randomAlphanumeric(5);
    private static final List<String> NEW_MINUS_KEYWORDS = singletonList("деревянный карлик " + randomNumeric(5));

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private BannerCommonRepository bannerCommonRepository;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private UpdateCampaignValidationService updateCampaignValidationService;

    @Autowired
    private MinusKeywordPreparingTool minusKeywordPreparingTool;

    private Campaign campaign1;
    private AdGroup adGroup;

    private Campaign campaign2;

    private CampaignInfo campaignInfoWithMinusKeywords;

    private long operatorUid;
    private ClientId clientId;
    private int shard;

    @Before
    public void before() {
        LocalDateTime longTimeAgo = LocalDateTime.now().minusDays(2).withNano(0);

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest1 =
                TestCampaigns.activeTextCampaign(null, null)
                        .withAutobudgetForecastDate(longTimeAgo);
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest2 =
                TestCampaigns.activeTextCampaign(null, null)
                        .withAutobudgetForecastDate(longTimeAgo);

        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTestWithMinusKeywords =
                TestCampaigns.activeTextCampaign(null, null)
                        .withAutobudgetForecastDate(longTimeAgo)
                        .withMinusKeywords(NEW_MINUS_KEYWORDS);

        CampaignInfo campaignInfo1 = steps.campaignSteps().createCampaign(campaignTest1, clientInfo);
        CampaignInfo campaignInfo2 = steps.campaignSteps().createCampaign(campaignTest2, clientInfo);
        campaignInfoWithMinusKeywords = steps.campaignSteps().createCampaign(campaignTestWithMinusKeywords, clientInfo);

        adGroup = TestGroups.defaultTextAdGroup(null).withStatusShowsForecast(StatusShowsForecast.SENDING);
        steps.adGroupSteps().createAdGroup(adGroup, campaignInfo1);

        operatorUid = campaignInfo1.getUid();
        clientId = campaignInfo1.getClientId();
        shard = campaignInfo1.getShard();


        campaign1 = getCampaign(shard, campaignInfo1.getCampaignId());
        campaign2 = getCampaign(shard, campaignInfo2.getCampaignId());
    }

    // возвращаемый результат при обновлении одной кампании

    @Test
    public void prepareAndApply_OneValidItem_ResultIsExpected() {
        ModelChanges<Campaign> modelChanges = modelChangesWithValidName(campaign1.getId());
        updateAndAssertResult(Applicability.PARTIAL, modelChanges, true);
    }

    @Test
    public void prepareAndApply_OneItemWithFailedPreValidation_ResultHasItemError() {
        ModelChanges<Campaign> modelChanges = modelChangesWithInvalidMinusKeywords(campaign1.getId());
        updateAndAssertResult(Applicability.PARTIAL, modelChanges, false);
    }

    @Test
    public void prepareAndApply_OneItemWithFailedPreValidationAndValidation_OnlyPreValidated() {
        ModelChanges<Campaign> modelChanges = modelChangesWithInvalidMinusKeywords(campaign1.getId());
        modelChanges.process(null, Campaign.NAME);
        CampaignsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.PARTIAL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat("результат операции должен быть положительный", result.isSuccessful(), is(true));
        assertThat("результат обновления элемента должен содержать только ошибку предварительной валидации",
                result.getResult().get(0).getErrors(), hasSize(1));
    }

    @Test
    public void prepareAndApply_OneItemWithFailedValidation_ResultHasItemError() {
        ModelChanges<Campaign> modelChanges = modelChangesWithInvalidName(campaign1.getId());
        updateAndAssertResult(Applicability.PARTIAL, modelChanges, false);
    }

    private void updateAndAssertResult(Applicability applicability, ModelChanges<Campaign> modelChanges,
                                       boolean itemResult) {
        CampaignsUpdateOperation updateOperation = createUpdateOperation(applicability, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat("результат операции должен быть положительный", result.isSuccessful(), is(true));
        assertThat("результат обновления элемента не соответствует ожидаемому",
                result.getResult().get(0).isSuccessful(), is(itemResult));
    }

    // ошибка операции при обновлении кампаний

    @Test
    public void prepareAndApply_TooManyItems_ResultIsFailed() {
        List<ModelChanges<Campaign>> modelChangesList = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            modelChangesList.add(modelChangesWithValidName(campaign1.getId()));
        }
        CampaignsUpdateOperation updateOperation = createUpdateOperation(Applicability.PARTIAL, modelChangesList);
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat("результат операции должен быть отрицательный", result.isSuccessful(), is(false));
    }

    // возвращаемый результат при обновлении двух кампаний

    @Test
    public void prepareAndApply_PartialYes_TwoValidItems_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                modelChangesWithValidName(campaign1.getId()),
                modelChangesWithValidMinusKeywords(campaign2.getId()), true, true);
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidAndOneInvalidItemOnPreValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                campaignModelChanges(-1L),
                modelChangesWithValidMinusKeywords(campaign2.getId()),
                false, true);
    }

    @Test
    public void prepareAndApply_PartialYes_TwoInvalidItemsOnPreValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                campaignModelChanges(-1L),
                modelChangesWithInvalidMinusKeywords(campaign2.getId()),
                false, false);
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidAndOneInvalidItemOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                modelChangesWithValidName(campaign1.getId()),
                modelChangesWithInvalidName(campaign2.getId()),
                true, false);
    }

    @Test
    public void prepareAndApply_PartialYes_TwoInvalidItemsOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                modelChangesWithInvalidName(campaign1.getId()),
                modelChangesWithInvalidName(campaign2.getId()),
                false, false);
    }

    @Test
    public void prepareAndApply_PartialNo_TwoValidItems_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithValidName(campaign1.getId()),
                modelChangesWithValidMinusKeywords(campaign2.getId()),
                true, true);
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItemOnPreValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                campaignModelChanges(-1L),
                modelChangesWithValidMinusKeywords(campaign2.getId()),
                false, true);
    }

    @Test
    public void prepareAndApply_PartialNo_TwoInvalidItemsOnPreValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                campaignModelChanges(-1L),
                modelChangesWithInvalidMinusKeywords(campaign2.getId()),
                false, false);
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItemOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithValidName(campaign1.getId()),
                modelChangesWithInvalidName(campaign2.getId()),
                true, false);
    }

    @Test
    public void prepareAndApply_PartialNo_TwoInvalidItemsOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithInvalidName(campaign1.getId()),
                modelChangesWithInvalidName(campaign2.getId()),
                false, false);
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItem_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                new ModelChanges<>(-1L, Campaign.class),
                modelChangesWithValidMinusKeywords(campaign2.getId()),
                false, true);
    }

    private void checkUpdateResultOfTwoItems(Applicability applicability,
                                             ModelChanges<Campaign> modelChanges1, ModelChanges<Campaign> modelChanges2,
                                             boolean modelChanges1Valid, boolean modelChanges2Valid) {
        List<ModelChanges<Campaign>> modelChangesList = asList(modelChanges1, modelChanges2);

        CampaignsUpdateOperation updateOperation = createUpdateOperation(applicability, modelChangesList);
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(modelChanges1Valid, modelChanges2Valid));
    }

    // корректность обновленных данных кампании в БД

    @Test
    public void prepareAndApply_NameIsChanged_UpdatedDataIsValid() {
        ModelChanges<Campaign> modelChanges = modelChangesWithValidName(campaign1.getId());
        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        Campaign expectedCampaign = campaign1.withName(NEW_NAME)
                .withStatusBsSynced(StatusBsSynced.YES);

        Campaign actualCampaign = getCampaign(shard, campaign1.getId());
        assertThat("обновленная кампания не соответствует ожидаемой",
                actualCampaign, beanDiffer(expectedCampaign).useCompareStrategy(STRATEGY));

        assertThat("LastChange выходит за ожидаемые границы", isAlmostNow(actualCampaign.getLastChange()), is(true));
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChanged_UpdatedDataIsValid() {
        ModelChanges<Campaign> modelChanges = modelChangesWithValidMinusKeywords(campaign1.getId());
        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        Campaign expectedCampaign = campaign1.withMinusKeywords(NEW_MINUS_KEYWORDS)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withAutobudgetForecastDate(null);

        Campaign actualCampaign = getCampaign(shard, campaign1.getId());
        assertThat("обновленная кампания не соответствует ожидаемой",
                actualCampaign, beanDiffer(expectedCampaign).useCompareStrategy(STRATEGY));

        assertThat("LastChange выходит за ожидаемые границы", isAlmostNow(actualCampaign.getLastChange()), is(true));
    }

    @Test
    public void prepareAndApply_AdvancedGeoTargeting_UpdatedDataIsValid() {
        ModelChanges<Campaign> modelChanges = modelChangesWithValidAdvancedGeoTargeting(campaign1.getId());
        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        Campaign expectedCampaign = campaign1
                .withOpts(EnumSet.of(CampaignOpts.USE_CURRENT_REGION, CampaignOpts.USE_REGULAR_REGION));

        Campaign actualCampaign = getCampaign(shard, campaign1.getId());
        assertThat("обновленная кампания не соответствует ожидаемой",
                actualCampaign, beanDiffer(expectedCampaign).useCompareStrategy(STRATEGY));

        assertThat("LastChange выходит за ожидаемые границы", isAlmostNow(actualCampaign.getLastChange()), is(true));
    }

    // должен проверять, что была выполнена предобработка отдельных минус-фраз,
    // удаление дублей и сортировка
    @Test
    public void prepareAndApply_MinusKeywordsIsChanged_MinusKeywordsArePreparedBeforeSaving() {
        List<String> rawMinusKeywords = asList("купит слона", "как пройти в библиотеку ", "!купил слона", "бизнес");
        List<String> expectedPreparedMinusKeywords = asList("!как пройти !в библиотеку", "бизнес", "купит слона");
        ModelChanges<Campaign> modelChanges =
                ModelChanges.build(campaign1.getId(), Campaign.class, Campaign.MINUS_KEYWORDS, rawMinusKeywords);

        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        Campaign actualCampaign = getCampaign(shard, campaign1.getId());
        assertThat("минус-фразы должны быть нормализованы перед сохранением",
                actualCampaign.getMinusKeywords(), beanDiffer(expectedPreparedMinusKeywords));
    }

    @Test
    public void prepareAndApply_MinusKeywordUpdateCampaignWithExistingMinusKeywords_UpdatedDataIsValid() {
        ModelChanges<Campaign> modelChanges =
                ModelChanges.build(campaignInfoWithMinusKeywords.getCampaignId(),
                        Campaign.class,
                        Campaign.MINUS_KEYWORDS,
                        singletonList("other keyword"));

        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        Campaign expectedCampaign = getCampaign(shard, campaignInfoWithMinusKeywords.getCampaignId())
                .withMinusKeywords(NEW_MINUS_KEYWORDS)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withAutobudgetForecastDate(null);

        Campaign actualCampaign = getCampaign(shard, campaignInfoWithMinusKeywords.getCampaignId());
        assertThat("обновленная кампания не соответствует ожидаемой",
                actualCampaign, beanDiffer(expectedCampaign).useCompareStrategy(STRATEGY));

        assertThat("LastChange выходит за ожидаемые границы", isAlmostNow(actualCampaign.getLastChange()), is(true));
    }

    @Test
    public void prepareAndApply_MinusKeywordDuplicatesNotChanged_NotUpdatedData() {
        List<String> newDuplicatedMinusKeywords =
                new ArrayList<>(campaignInfoWithMinusKeywords.getCampaign().getMinusKeywords());
        newDuplicatedMinusKeywords.add(NEW_MINUS_KEYWORDS.get(0));

        ModelChanges<Campaign> modelChanges = ModelChanges
                .build(campaignInfoWithMinusKeywords.getCampaignId(),
                        Campaign.class,
                        Campaign.MINUS_KEYWORDS,
                        newDuplicatedMinusKeywords);

        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        Campaign expectedCampaign = getCampaign(shard, campaignInfoWithMinusKeywords.getCampaignId())
                .withMinusKeywords(NEW_MINUS_KEYWORDS)
                .withStatusBsSynced(StatusBsSynced.YES);

        Campaign actualCampaign =
                getCampaign(shard, campaignInfoWithMinusKeywords.getCampaignId());
        assertThat("обновленная кампания не соответствует ожидаемой",
                actualCampaign, beanDiffer(expectedCampaign).useCompareStrategy(STRATEGY));

        assertThat("LastChange выходит за ожидаемые границы", isAlmostNow(actualCampaign.getLastChange()), is(true));
    }

    // обновление при наличии изменений в минус-фразах

    @Test
    public void prepareAndApply_ContainsInvalidMinusKeywordsItem_ResultIsExpected() {
        List<ModelChanges<Campaign>> modelChangesList = asList(
                modelChangesWithValidName(campaign1.getId()),
                modelChangesWithInvalidMinusKeywords(campaign2.getId()));
        CampaignsUpdateOperation updateOperation = createUpdateOperation(Applicability.PARTIAL, modelChangesList);
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(true, false));
    }

    @Test
    public void prepareAndApply_ContainsValidMinusKeywordsItem_ResultIsExpected() {
        List<ModelChanges<Campaign>> modelChangesList = asList(
                modelChangesWithValidName(campaign1.getId()),
                modelChangesWithValidMinusKeywords(campaign2.getId()));
        CampaignsUpdateOperation updateOperation = createUpdateOperation(Applicability.PARTIAL, modelChangesList);
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isFullySuccessful());
    }

    // обновления в смежных таблицах

    @Test
    public void prepareAndApply_NameIsChanged_AdGroupStatusShowForecastNotChanged() {
        StatusShowsForecast expectedStatusShowForecast = adGroup.getStatusShowsForecast();
        checkState(adGroup.getStatusShowsForecast().equals(StatusShowsForecast.SENDING));

        ModelChanges<Campaign> modelChanges = modelChangesWithValidName(campaign1.getId());
        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroup.getId())).get(0);
        assertThat("adGroup.statusShowForecast не должен был измениться",
                actualAdGroup.getStatusShowsForecast(), is(expectedStatusShowForecast));
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChanged_AdGroupStatusShowForecastChanged() {
        checkState(adGroup.getStatusShowsForecast() != StatusShowsForecast.NEW);

        ModelChanges<Campaign> modelChanges = modelChangesWithValidMinusKeywords(campaign1.getId());
        updateAndAssumeResultIsSuccessful(Applicability.FULL, modelChanges);

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroup.getId())).get(0);
        assertThat("campaigns.autobudget_forecast_date должен был сброситься",
                actualAdGroup.getStatusShowsForecast(),
                is(StatusShowsForecast.NEW));
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChangedInTextCampaign_NotUpdateBannerStatusBsSynced() {
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(activeTextCampaign(null, null));
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(null, campaignInfo);
        checkState(bannerInfo.getBanner().getStatusBsSynced() == StatusBsSynced.YES,
                "баннер должен быть синхронизирован с БК");
        checkState(bannerInfo.getCampaignInfo().getCampaign().getType() == CampaignType.TEXT,
                "тип кампании должен быть TEXT");

        ModelChanges<Campaign> modelChanges = modelChangesWithValidMinusKeywords(bannerInfo.getCampaignId());
        updateAndCheckBannerStatusBsSynced(bannerInfo, modelChanges, StatusBsSynced.YES);
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChangedInDynamicCampaign_UpdateBannerStatusBsSynced() {
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(activeDynamicCampaign(null, null));
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(null, campaignInfo);
        checkState(bannerInfo.getBanner().getStatusBsSynced() == StatusBsSynced.YES,
                "баннер должен быть синхронизирован с БК");
        checkState(bannerInfo.getCampaignInfo().getCampaign().getType() == CampaignType.DYNAMIC,
                "тип кампании должен быть DYNAMIC");

        ModelChanges<Campaign> modelChanges = modelChangesWithValidMinusKeywords(bannerInfo.getCampaignId());
        updateAndCheckBannerStatusBsSynced(bannerInfo, modelChanges, StatusBsSynced.NO);
    }

    @Test
    public void prepareAndApply_MinusKeywordsIsChangedInPerformanceCampaign_UpdateBannerStatusBsSynced() {
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(activePerformanceCampaign(null, null));
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(null, campaignInfo);
        checkState(bannerInfo.getBanner().getStatusBsSynced() == StatusBsSynced.YES,
                "баннер должен быть синхронизирован с БК");
        checkState(bannerInfo.getCampaignInfo().getCampaign().getType() == CampaignType.PERFORMANCE,
                "тип кампании должен быть PERFORMANCE");

        ModelChanges<Campaign> modelChanges = modelChangesWithValidMinusKeywords(bannerInfo.getCampaignId());
        updateAndCheckBannerStatusBsSynced(bannerInfo, modelChanges, StatusBsSynced.NO);
    }

    @Test
    public void prepareAndApply_NameIsChangedInPerformanceCampaign_NotUpdateBannerStatusBsSynced() {
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(activePerformanceCampaign(null, null));
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(null, campaignInfo);
        checkState(bannerInfo.getBanner().getStatusBsSynced() == StatusBsSynced.YES,
                "баннер должен быть синхронизирован с БК");
        checkState(bannerInfo.getCampaignInfo().getCampaign().getType() == CampaignType.PERFORMANCE,
                "тип кампании должен быть PERFORMANCE");

        ModelChanges<Campaign> modelChanges = modelChangesWithValidName(bannerInfo.getCampaignId());
        updateAndCheckBannerStatusBsSynced(bannerInfo, modelChanges, StatusBsSynced.YES);
    }

    @Test
    public void prepareAndApply_CpmYndxCampaign_NoError() {
        ClientInfo clientInfo = campaignInfoWithMinusKeywords.getClientInfo();
        CampaignInfo cpmYndxCampaign = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign(clientInfo);
        ModelChanges<Campaign> modelChanges = modelChangesWithValidName(cpmYndxCampaign.getCampaignId());
        updateAndAssertResult(Applicability.PARTIAL, modelChanges, true);
    }


    private void updateAndCheckBannerStatusBsSynced(AbstractBannerInfo<?> bannerInfo,
                                                    ModelChanges<Campaign> modelChanges,
                                                    StatusBsSynced expectedStatus) {
        CampaignsUpdateOperation updateOperation = createUpdateOperation(Applicability.PARTIAL,
                singletonList(modelChanges), bannerInfo.getUid(),
                bannerInfo.getClientId(), bannerInfo.getShard());
        MassResult<Long> result = updateOperation.prepareAndApply();
        assumeThat(result, isFullySuccessful());

        OldBanner banner = bannerRepository.getBanners(shard, singletonList(bannerInfo.getBannerId())).get(0);
        assertThat(banner.getStatusBsSynced(), equalTo(expectedStatus));
    }

    private void updateAndAssumeResultIsSuccessful(Applicability applicability, ModelChanges<Campaign> modelChanges) {
        CampaignsUpdateOperation updateOperation = createUpdateOperation(applicability, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assumeThat(result, isFullySuccessful());
    }

    private CampaignsUpdateOperation createUpdateOperation(Applicability applicability,
                                                           List<ModelChanges<Campaign>> modelChangesList) {
        return createUpdateOperation(applicability, modelChangesList, operatorUid, clientId, shard);
    }

    private CampaignsUpdateOperation createUpdateOperation(Applicability applicability,
                                                           List<ModelChanges<Campaign>> modelChangesList, long operatorUid,
                                                           ClientId clientId, int shard) {
        return new CampaignsUpdateOperation(applicability, modelChangesList,
                campaignRepository, adGroupRepository, bannerCommonRepository, updateCampaignValidationService,
                minusKeywordPreparingTool, MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE, operatorUid,
                clientId, shard
        );
    }

    private ModelChanges<Campaign> modelChangesWithValidName(long id) {
        return ModelChanges.build(id, Campaign.class, Campaign.NAME, NEW_NAME);
    }

    private ModelChanges<Campaign> modelChangesWithInvalidName(long id) {
        return ModelChanges.build(id, Campaign.class, Campaign.NAME, "");
    }

    private ModelChanges<Campaign> modelChangesWithValidMinusKeywords(long id) {
        return ModelChanges.build(id, Campaign.class, Campaign.MINUS_KEYWORDS, NEW_MINUS_KEYWORDS);
    }

    private ModelChanges<Campaign> modelChangesWithValidAdvancedGeoTargeting(long id) {
        var advancedGeoOpts = EnumSet.of(CampaignOpts.USE_CURRENT_REGION, CampaignOpts.USE_REGULAR_REGION);
        return ModelChanges.build(id, Campaign.class, Campaign.OPTS, advancedGeoOpts);
    }

    private ModelChanges<Campaign> modelChangesWithInvalidMinusKeywords(long id) {
        return ModelChanges.build(id, Campaign.class, Campaign.MINUS_KEYWORDS, singletonList("[]"));
    }

    private static ModelChanges<Campaign> campaignModelChanges(Long id) {
        return new ModelChanges<>(id, Campaign.class);
    }

    public Campaign getCampaign(int shard, long id) {
        return campaignRepository.getCampaigns(shard, singletonList(id)).iterator().next();
    }

    private boolean isAlmostNow(LocalDateTime time) {
        final int epsilonMinutes = 1;

        return time.isAfter(LocalDateTime.now().minusMinutes(epsilonMinutes)) &&
                time.isBefore(LocalDateTime.now().plusMinutes(epsilonMinutes));
    }

}
