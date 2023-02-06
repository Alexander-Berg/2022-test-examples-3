package ru.yandex.direct.core.entity.adgroup.service.complex.suboperation.update;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.repository.TestTargetingCategoriesRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingUtils.convertRetargetingsToTargetInterests;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.requiredAtLeastOneOfFields;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultTargetInterest;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotLessThan;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateRetargetingsSubOperationTest {
    private static final CompareStrategy RETARGETING_COMPARE_STRATEGY = onlyExpectedFields()
            .forFields(newPath("lastChangeTime")).useMatcher(approximatelyNow())
            .forFields(newPath("priceContext")).useDiffer(new BigDecimalDiffer())
            .forFields(newPath("statusBsSynced")).useMatcher(is(StatusBsSynced.NO));

    @Autowired
    private RetargetingService retargetingService;
    @Autowired
    private RetargetingRepository retargetingRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private TestTargetingCategoriesRepository testTargetingCategoriesRepository;

    private AdGroupInfo adGroup;
    private ClientInfo clientInfo;
    private long retConditionId;
    private int shard;
    private TargetingCategory targetingCategory;

    @Before
    public void before() {
        adGroup = steps.adGroupSteps().createActiveMobileContentAdGroup();
        clientInfo = adGroup.getClientInfo();
        shard = clientInfo.getShard();

        RetConditionInfo retCondition = steps.retConditionSteps().createDefaultRetCondition(clientInfo);
        retConditionId = retCondition.getRetConditionId();

        targetingCategory = new TargetingCategory(54L, null, "", "", BigInteger.valueOf(10000L), true);
        testTargetingCategoriesRepository.addTargetingCategory(targetingCategory);
    }

    @Test
    public void addRetargetings() {
        Retargeting retargeting = defaultRetargeting(null, adGroup.getAdGroupId(), retConditionId);

        updateRetargetings(singletonList(retargeting));

        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assertThat("должен быть добавлен 1 ретаргетинг", retargetings, hasSize(1));
        Retargeting actual = retargetings.get(0);
        assertThat("ретаргетинг добавлен верно", actual,
                beanDiffer(retargeting).useCompareStrategy(RETARGETING_COMPARE_STRATEGY));
    }

    @Test
    public void deleteRetargeting() {
        steps.retargetingSteps().createDefaultRetargeting(adGroup);
        List<Retargeting> retargetingsBefore =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assertThat("в группе должен быть 1 ретаргетинг", retargetingsBefore, hasSize(1));

        updateRetargetings(emptyList());

        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assertThat("не должно быть ретаргетингов на группе", retargetings, hasSize(0));
    }

    @Test
    public void updateRetargeting() {
        RetargetingInfo defaultRetargeting = steps.retargetingSteps().createDefaultRetargeting(adGroup);
        Retargeting retargeting = defaultRetargeting.getRetargeting().withRetargetingConditionId(retConditionId);

        updateRetargetings(singletonList(retargeting));

        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assertThat("в группе должен быть 1 ретаргетинг", retargetings, hasSize(1));
        Retargeting actual = retargetings.get(0);
        assertThat("ретаргетинг успешно обновлен", actual,
                beanDiffer(retargeting).useCompareStrategy(RETARGETING_COMPARE_STRATEGY));
    }

    @Test
    public void addAndUpdateRetargetings() {
        RetargetingInfo defaultRetargeting = steps.retargetingSteps().createDefaultRetargeting(adGroup);
        Retargeting retargeting = defaultRetargeting.getRetargeting()
                .withRetargetingConditionId(retConditionId);
        Long retConditionId2 = steps.retConditionSteps().createDefaultRetCondition(clientInfo).getRetConditionId();
        Retargeting retargetingToAdd = defaultRetargeting(null, adGroup.getAdGroupId(), retConditionId2);

        List<Retargeting> expected = asList(retargeting, retargetingToAdd);
        updateRetargetings(expected);

        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assertThat("в группе должно быть 2 ретаргетинга", retargetings, hasSize(2));
        assertThat("ретаргетинги успешно обновлены", retargetings, containsInAnyOrder(
                mapList(expected, r -> beanDiffer(r).useCompareStrategy(RETARGETING_COMPARE_STRATEGY))));
    }

    @Test
    public void deleteAndUpdateRetargetings() {
        steps.retargetingSteps().createDefaultRetargeting(adGroup);
        RetargetingInfo reatrgetingInfoToUpdate = steps.retargetingSteps().createDefaultRetargeting(adGroup);

        Long retConditionId2 = steps.retConditionSteps().createDefaultRetCondition(clientInfo).getRetConditionId();
        Retargeting retargetingToUpdate = reatrgetingInfoToUpdate.getRetargeting()
                .withRetargetingConditionId(retConditionId2);

        List<Retargeting> expected = singletonList(retargetingToUpdate);
        updateRetargetings(expected);

        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assertThat("в группе должен быть 1 ретаргетинг", retargetings, hasSize(1));
        Retargeting retargeting = retargetings.get(0);
        assertThat("ретаргетинги успешно обновлены", retargeting,
                beanDiffer(expected.get(0)).useCompareStrategy(RETARGETING_COMPARE_STRATEGY));
    }

    @Test
    public void addAndDeleteRetargetings() {
        steps.retargetingSteps().createDefaultRetargeting(adGroup);

        Long retConditionId2 = steps.retConditionSteps().createDefaultRetCondition(clientInfo).getRetConditionId();
        Retargeting retargetingToAdd = defaultRetargeting(null, adGroup.getAdGroupId(), retConditionId2);

        List<Retargeting> expected = singletonList(retargetingToAdd);
        updateRetargetings(expected);

        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assertThat("в группе должен быть 1 ретаргетинг", retargetings, hasSize(1));
        assertThat("ретаргетинги успешно обновлены", retargetings, containsInAnyOrder(
                mapList(expected, r -> beanDiffer(r).useCompareStrategy(RETARGETING_COMPARE_STRATEGY))));
    }

    @Test
    public void addAndUpdateRetargetingsWithValidationErrorOnAdded() {
        RetargetingInfo defaultRetargeting = steps.retargetingSteps().createDefaultRetargeting(adGroup);
        Retargeting retargeting = defaultRetargeting.getRetargeting()
                .withRetargetingConditionId(retConditionId);
        Retargeting retargetingToAdd = defaultRetargeting(null, adGroup.getAdGroupId(), null);

        List<Retargeting> expected = asList(retargeting, retargetingToAdd);
        UpdateRetargetingsSubOperation retargetingsOperation = createRetargetingsOperation(expected);
        ValidationResult<List<TargetInterest>, Defect> vr = retargetingsOperation.prepare();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(1)),
                requiredAtLeastOneOfFields(path(field("InterestId")), path(field("RetargetingListId"))))));
    }

    @Test
    public void addAndUpdateRetargetingsWithValidationErrorOnUpdated() {
        RetargetingInfo defaultRetargeting = steps.retargetingSteps().createDefaultRetargeting(adGroup);
        Retargeting retargeting = defaultRetargeting.getRetargeting();
        Long retConditionId2 = steps.retConditionSteps().createDefaultRetCondition(clientInfo).getRetConditionId();
        Retargeting retargetingToAdd = defaultRetargeting(null, adGroup.getAdGroupId(), retConditionId2);

        List<Retargeting> expected = asList(retargeting, retargetingToAdd);
        UpdateRetargetingsSubOperation retargetingsOperation = createRetargetingsOperation(expected, BigDecimal.ZERO);
        ValidationResult<List<TargetInterest>, Defect> vr = retargetingsOperation.prepare();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(Retargeting.PRICE_CONTEXT.name())),
                invalidValueNotLessThan(Money.valueOf(0.3, CurrencyCode.RUB)))));
    }

    @Test
    public void addRetargetingsWithValidationResult() {
        Retargeting retargetingToAdd = defaultRetargeting(null, adGroup.getAdGroupId(), null);
        List<Retargeting> expected = singletonList(retargetingToAdd);
        UpdateRetargetingsSubOperation retargetingsOperation = createRetargetingsOperation(expected);
        ValidationResult<List<TargetInterest>, Defect> vr = retargetingsOperation.prepare();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)),
                requiredAtLeastOneOfFields(path(field("InterestId")), path(field("RetargetingListId"))))));
    }

    @Test
    public void updateRetargetingWithValidationError() {
        RetargetingInfo defaultRetargeting = steps.retargetingSteps().createDefaultRetargeting(adGroup);
        Retargeting retargeting = defaultRetargeting.getRetargeting();

        List<Retargeting> expected = singletonList(retargeting);
        UpdateRetargetingsSubOperation retargetingsOperation = createRetargetingsOperation(expected, BigDecimal.ZERO);
        ValidationResult<List<TargetInterest>, Defect> vr = retargetingsOperation.prepare();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field(Retargeting.PRICE_CONTEXT.name())),
                invalidValueNotLessThan(Money.valueOf(0.3, CurrencyCode.RUB)))));
    }

    private void updateRetargetings(List<Retargeting> retargetings) {
        updateRetargetings(createRetargetingsOperation(retargetings));
    }

    private void updateInterests(List<TargetInterest> targetInterests) {
        updateRetargetings(createRetargetingsSubOperation(targetInterests));
    }

    private void updateRetargetings(UpdateRetargetingsSubOperation updateRetargetingsSubOperation) {
        ValidationResult<List<TargetInterest>, Defect> vr = updateRetargetingsSubOperation.prepare();
        assertThat(vr, hasNoDefectsDefinitions());
        updateRetargetingsSubOperation.apply();
    }

    private UpdateRetargetingsSubOperation createRetargetingsOperation(List<Retargeting> retargetings) {
        return createRetargetingsSubOperation(convertRetargetingsToTargetInterests(retargetings, emptyList()));
    }

    private UpdateRetargetingsSubOperation createRetargetingsSubOperation(List<TargetInterest> targetInterests) {
        UpdateRetargetingsSubOperation updateRetargetingsSubOperation =
                new UpdateRetargetingsSubOperation(retargetingService, targetInterests,
                        adGroup.getAdGroupType(), false, null, clientInfo.getUid(), clientInfo.getClientId(),
                        clientInfo.getUid(), shard);

        updateRetargetingsSubOperation
                .setAffectedAdGroupsMap(singletonMap(adGroup.getAdGroupId(), adGroup.getAdGroup()));
        updateRetargetingsSubOperation.setAffectedAdGroupsPriceRestrictions(emptyMap());
        return updateRetargetingsSubOperation;
    }

    private UpdateRetargetingsSubOperation createRetargetingsOperation(List<Retargeting> retargetings,
                                                                       BigDecimal newPrice) {
        List<TargetInterest> targetInterests = convertRetargetingsToTargetInterests(retargetings, emptyList());

        UpdateRetargetingsSubOperation updateRetargetingsSubOperation =
                new UpdateRetargetingsSubOperation(retargetingService, targetInterests,
                        adGroup.getAdGroupType(), true, ShowConditionFixedAutoPrices.ofGlobalFixedPrice(newPrice),
                        clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(), shard);

        updateRetargetingsSubOperation
                .setAffectedAdGroupsMap(singletonMap(adGroup.getAdGroupId(), adGroup.getAdGroup()));
        updateRetargetingsSubOperation.setAffectedAdGroupsPriceRestrictions(emptyMap());
        return updateRetargetingsSubOperation;
    }

    /**
     *  Проверяем создание условия нацеливания по интересам (для таргетинга по интересам)
     */
    @Test
    public void addInterest() {
        TargetInterest targetInterest = targetInterestOnInterestId();

        updateInterests(singletonList(targetInterest));

        Retargeting expectRetargeting = new Retargeting()
                .withRetargetingConditionId(targetInterest.getRetargetingConditionId())
                .withPriceContext(targetInterest.getPriceContext());

        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assertThat("в группе должен быть 1 ретаргетинг", retargetings, hasSize(1));
        assertThat("ретаргетинг успешно обновлен", retargetings.get(0),
                beanDiffer(expectRetargeting).useCompareStrategy(RETARGETING_COMPARE_STRATEGY));
    }

    /**
     *  Проверяем удаление условия нацеливания по интересам (для таргетинга по интересам)
     */
    @Test
    public void deleteInterest() {
        createTargetInterest(clientInfo);
        List<Retargeting> retargetingsBefore =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assertThat("в группе должен быть 1 ретаргетинг", retargetingsBefore, hasSize(1));

        updateRetargetings(emptyList());

        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assertThat("не должно быть ретаргетингов на группе", retargetings, hasSize(0));
    }

    /**
     *  Проверяем обновление условия нацеливания по интересам (для таргетинга по интересам)
     */
    @Test
    public void updateInterest() {
        createTargetInterest(clientInfo);

        TargetInterest targetInterest = targetInterestOnInterestId();

        updateInterests(singletonList(targetInterest));

        Retargeting expectRetargeting = new Retargeting()
                .withRetargetingConditionId(targetInterest.getRetargetingConditionId())
                .withPriceContext(targetInterest.getPriceContext());

        List<Retargeting> retargetings =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroup.getAdGroupId()));
        assertThat("в группе должен быть 1 ретаргетинг", retargetings, hasSize(1));
        assertThat("ретаргетинг успешно обновлен", retargetings.get(0),
                beanDiffer(expectRetargeting).useCompareStrategy(RETARGETING_COMPARE_STRATEGY));
    }

    private void createTargetInterest(ClientInfo clientInfo) {
        List<TargetInterest> targetInterests = singletonList(
                targetInterestOnInterestId());
        MassResult<Long> result = retargetingService
                .createAddOperation(Applicability.PARTIAL, targetInterests, clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid())
                .prepareAndApply();
        assumeThat(result.get(0).getValidationResult(), hasNoDefectsDefinitions());
        assumeThat(result, isFullySuccessful());
        result.get(0).getResult();
    }

    private TargetInterest targetInterestOnInterestId() {
        return defaultTargetInterest()
                .withCampaignId(adGroup.getCampaignId())
                .withAdGroupId(adGroup.getAdGroupId())
                .withRetargetingConditionId(null)
                .withInterestId(targetingCategory.getTargetingCategoryId());
    }
}
