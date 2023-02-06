package ru.yandex.direct.core.entity.relevancematch.service.updateoperation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModificationBaseTest;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchUpdateOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.notNullValue;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchMapping.relevanceMatchesToCoreModelChanges;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchUpdateOperationTest extends RelevanceMatchModificationBaseTest {
    @Test
    public void prepareAndApply_PartialNo_OneValidItem_ResultIsFullySuccessful() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withAutobudgetPriority(5);

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        MassResult<Long> massResult = relevanceMatchUpdateOperation.prepareAndApply();
        assertThat(massResult, isSuccessfulWithMatchers(notNullValue(Long.class)));
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidItem_WithContainerFilledInService_ResultIsFullySuccessful() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withPrice(BigDecimal.TEN)
                .withPriceContext(BigDecimal.TEN.add(BigDecimal.ONE));

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = relevanceMatchService
                .createFullUpdateOperation(defaultUser.getClientInfo().getClient().getWorkCurrency().getCurrency(),
                        defaultUser.getClientInfo().getClientId(),
                        defaultUser.getClientInfo().getClient().getChiefUid(),
                        defaultUser.getUid(),
                        relevanceMatchesToCoreModelChanges(Collections.singletonList((relevanceMatchChanges))));
        MassResult<Long> massResult = relevanceMatchUpdateOperation.prepareAndApply();
        assertThat(massResult, isSuccessfulWithMatchers(notNullValue(Long.class)));
    }

    @Test
    public void prepareAndApply_PartialNo_OneInvalidItem_FailsOnPreValidation_ResultHasElementErrorl() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withAutobudgetPriority(5);

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        MassResult<Long> massResult = relevanceMatchUpdateOperation.prepareAndApply();
        assertThat(massResult, isSuccessfulWithMatchers(notNullValue(Long.class)));
    }

    @Test
    public void prepareAndApply_PartialNo_OneInValidItem_ResultHasElementError() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withAutobudgetPriority(1111);

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        MassResult<Long> massResult = relevanceMatchUpdateOperation.prepareAndApply();
        assertThat(massResult, isSuccessful(false));
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidItem_SavedCorrectly() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withAutobudgetPriority(5);

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        MassResult<Long> massResult = relevanceMatchUpdateOperation.prepareAndApply();

        RelevanceMatch actualRelevanceMatch = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        Collections.singletonList(massResult.get(0).getResult()))
                .get(massResult.get(0).getResult());

        RelevanceMatch expectedRelevanceMatch = relevanceMatchChanges
                .withStatusBsSynced(StatusBsSynced.NO)
                .withCampaignId(activeCampaign.getCampaignId())
                .withLastChangeTime(LocalDateTime.now())
                .withIsDeleted(false)
                .withRelevanceMatchCategories(emptySet());
        DefaultCompareStrategy compareStrategy = DefaultCompareStrategies.allFieldsExcept(
                BeanFieldPath.newPath("lastChangeTime")
        );
        assertThat(actualRelevanceMatch, beanDiffer(expectedRelevanceMatch).useCompareStrategy(
                compareStrategy));
    }

    @Test
    public void prepareAndApply_PartialNo_OneEmptyRelevanceMatchCategories_SavedCorrectly() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withAutobudgetPriority(5)
                .withRelevanceMatchCategories(emptySet());

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        MassResult<Long> massResult = relevanceMatchUpdateOperation.prepareAndApply();

        RelevanceMatch actualRelevanceMatch = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        Collections.singletonList(massResult.get(0).getResult()))
                .get(massResult.get(0).getResult());

        RelevanceMatch expectedRelevanceMatch = relevanceMatchChanges
                .withStatusBsSynced(StatusBsSynced.NO)
                .withCampaignId(activeCampaign.getCampaignId())
                .withLastChangeTime(LocalDateTime.now())
                .withIsDeleted(false)
                .withRelevanceMatchCategories(emptySet());
        DefaultCompareStrategy compareStrategy = DefaultCompareStrategies.allFieldsExcept(
                BeanFieldPath.newPath("lastChangeTime")
        );
        assertThat(actualRelevanceMatch, beanDiffer(expectedRelevanceMatch).useCompareStrategy(
                compareStrategy));
    }

    @Test
    public void prepareAndApply_PartialNo_OneRelevanceMatchCategories_SavedCorrectly() {
        RelevanceMatch relevanceMatchChanges = getValidRelevanceMatch()
                .withId(getSavedRelevanceMatch().getId())
                .withAutobudgetPriority(5)
                .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.values()));

        RelevanceMatchUpdateOperation relevanceMatchUpdateOperation = getFullUpdateOperation(relevanceMatchChanges);
        MassResult<Long> massResult = relevanceMatchUpdateOperation.prepareAndApply();

        RelevanceMatch actualRelevanceMatch = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        Collections.singletonList(massResult.get(0).getResult()))
                .get(massResult.get(0).getResult());

        RelevanceMatch expectedRelevanceMatch = relevanceMatchChanges
                .withStatusBsSynced(StatusBsSynced.NO)
                .withCampaignId(activeCampaign.getCampaignId())
                .withLastChangeTime(LocalDateTime.now())
                .withIsDeleted(false)
                .withRelevanceMatchCategories(asSet(RelevanceMatchCategory.values()));
        DefaultCompareStrategy compareStrategy = DefaultCompareStrategies.allFieldsExcept(
                BeanFieldPath.newPath("lastChangeTime")
        );
        assertThat(actualRelevanceMatch, beanDiffer(expectedRelevanceMatch).useCompareStrategy(
                compareStrategy));
    }

}
