package ru.yandex.direct.core.entity.relevancematch.service.addoperation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchAddOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.operation.AddedModelId;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.notNullValue;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchAddOperationTest extends RelevanceMatchAddOperationBaseTest {
    @Autowired
    private RelevanceMatchRepository relevanceMatchRepository;

    @Test
    public void prepareAndApply_PartialNo_OneValidItem_ResultIsFullySuccessful() {
        RelevanceMatch relevanceMatch = getValidRelevanceMatch();

        RelevanceMatchAddOperation fullAddOperation =
                getFullAddOperation(relevanceMatch);
        MassResult<AddedModelId> massResult = fullAddOperation.prepareAndApply();
        assertThat(massResult, isSuccessfulWithMatchers(notNullValue(AddedModelId.class)));
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidItem_WithContainerFilledInService_ResultIsFullySuccessful() {
        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withPrice(BigDecimal.TEN)
                .withPriceContext(BigDecimal.TEN.add(BigDecimal.ONE))
                .withIsSuspended(true);

        RelevanceMatchAddOperation fullAddOperation = relevanceMatchService
                .createFullAddOperation(defaultUser.getClientInfo().getClient().getWorkCurrency().getCurrency(),
                        defaultUser.getClientInfo().getClientId(),
                        defaultUser.getUid(),
                        Collections.singletonList(relevanceMatch));
        MassResult<AddedModelId> massResult = fullAddOperation.prepareAndApply();
        assertThat(massResult, isSuccessfulWithMatchers(notNullValue(AddedModelId.class)));
    }

    @Test
    public void prepareAndApply_PartialNo_OneInvalidItem_ResultHasElementError() {
        RelevanceMatch relevanceMatch = getInvalidRelevanceMatch();

        RelevanceMatchAddOperation fullAddOperation =
                getFullAddOperation(relevanceMatch);
        MassResult<AddedModelId> massResult = fullAddOperation.prepareAndApply();
        assertThat(massResult, isSuccessful(false));
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidItem_SavedCorrectly() {
        RelevanceMatch relevanceMatch = getValidRelevanceMatch();

        RelevanceMatchAddOperation fullAddOperation = getFullAddOperation(relevanceMatch);
        MassResult<AddedModelId> massResult = fullAddOperation.prepareAndApply();
        RelevanceMatch actualRelevanceMatch = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        Collections.singletonList(massResult.get(0).getResult().getId()))
                .get(massResult.get(0).getResult().getId());

        RelevanceMatch expectedRelevanceMatch = getValidRelevanceMatch()
                .withStatusBsSynced(StatusBsSynced.NO)
                .withLastChangeTime(LocalDateTime.now())
                .withIsDeleted(false)
                .withIsSuspended(true)
                .withId(massResult.get(0).getResult().getId())
                .withRelevanceMatchCategories(emptySet());
        DefaultCompareStrategy compareStrategy = DefaultCompareStrategies.allFieldsExcept(
                BeanFieldPath.newPath("lastChangeTime"),
                BeanFieldPath.newPath("campaignId")
        );
        assertThat(actualRelevanceMatch, beanDiffer(expectedRelevanceMatch).useCompareStrategy(
                compareStrategy));
    }

    @Test
    public void prepareAndApply_PartialNo_OneEmptyRelevanceMatchCategories_SavedCorrectly() {
        RelevanceMatch relevanceMatch = getValidRelevanceMatch()
                .withRelevanceMatchCategories(emptySet());

        RelevanceMatchAddOperation fullAddOperation = getFullAddOperation(relevanceMatch);
        MassResult<AddedModelId> massResult = fullAddOperation.prepareAndApply();
        RelevanceMatch actualRelevanceMatch = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        Collections.singletonList(massResult.get(0).getResult().getId()))
                .get(massResult.get(0).getResult().getId());

        RelevanceMatch expectedRelevanceMatch = getValidRelevanceMatch()
                .withStatusBsSynced(StatusBsSynced.NO)
                .withLastChangeTime(LocalDateTime.now())
                .withIsDeleted(false)
                .withIsSuspended(true)
                .withId(massResult.get(0).getResult().getId())
                .withRelevanceMatchCategories(emptySet());
        DefaultCompareStrategy compareStrategy = DefaultCompareStrategies.allFieldsExcept(
                BeanFieldPath.newPath("lastChangeTime"),
                BeanFieldPath.newPath("campaignId")
        );
        assertThat(actualRelevanceMatch, beanDiffer(expectedRelevanceMatch).useCompareStrategy(
                compareStrategy));
    }

    @Test
    public void prepareAndApply_PartialNo_OneRelevanceMatchCategories_SavedCorrectly() {
        RelevanceMatch relevanceMatch = getValidRelevanceMatch()
                .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.values()));

        RelevanceMatchAddOperation fullAddOperation = getFullAddOperation(relevanceMatch);
        MassResult<AddedModelId> massResult = fullAddOperation.prepareAndApply();
        RelevanceMatch actualRelevanceMatch = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        Collections.singletonList(massResult.get(0).getResult().getId()))
                .get(massResult.get(0).getResult().getId());

        RelevanceMatch expectedRelevanceMatch = getValidRelevanceMatch()
                .withStatusBsSynced(StatusBsSynced.NO)
                .withLastChangeTime(LocalDateTime.now())
                .withIsDeleted(false)
                .withIsSuspended(true)
                .withId(massResult.get(0).getResult().getId())
                .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.values()));
        DefaultCompareStrategy compareStrategy = DefaultCompareStrategies.allFieldsExcept(
                BeanFieldPath.newPath("lastChangeTime"),
                BeanFieldPath.newPath("campaignId")
        );
        assertThat(actualRelevanceMatch, beanDiffer(expectedRelevanceMatch).useCompareStrategy(
                compareStrategy));
    }
}
