package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.relevancematch;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotLessThan;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тесты линковки ошибок бесфразного таргетинга с группой
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexUpdateRelevanceMatchValidationLinkingTest extends ComplexUpdateRelevanceMatchTestBase {

    @Test
    public void update_AdGroupWithErrorInRelevanceMatchAdd_ErrorInResultIsValid() {
        ComplexTextAdGroup adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1);

        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withPrice(BigDecimal.ZERO)
                .withIsSuspended(true);

        adGroupForUpdate.withRelevanceMatches(singletonList(relevanceMatch));

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(adGroupForUpdate));

        Path relevanceMatchPath = path(index(0), field(ComplexTextAdGroup.RELEVANCE_MATCHES.name()),
                index(0), field(RelevanceMatch.PRICE.name()));
        Currency currency = campaignInfo.getClientInfo().getClient().getWorkCurrency().getCurrency();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(relevanceMatchPath,
                        invalidValueNotLessThan(Money.valueOf(currency.getMinPrice(), currency.getCode())))));
    }

    @Test
    public void update_FirstAdGroupValidAndSecondAdGroupWithErrorInRelevanceMatchForAdd_ErrorsInResultAreValid() {
        createSecondAdGroup();

        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2);

        RelevanceMatch relevanceMatch = new RelevanceMatch()
                .withPrice(BigDecimal.ZERO)
                .withIsSuspended(true);

        adGroupForUpdate2.withRelevanceMatches(singletonList(relevanceMatch));


        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(asList(adGroupForUpdate1, adGroupForUpdate2));

        Path relevanceMatchPath = path(index(1), field(ComplexTextAdGroup.RELEVANCE_MATCHES.name()),
                index(0), field(RelevanceMatch.PRICE.name()));
        Currency currency = campaignInfo.getClientInfo().getClient().getWorkCurrency().getCurrency();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(relevanceMatchPath,
                        invalidValueNotLessThan(Money.valueOf(currency.getMinPrice(), currency.getCode())))));
    }

    @Test
    public void update_FirstAdGroupWithErrorInRelevanceMatchForAddAndSecondAdGroupWithErrorInRelevanceMatchForAdd_ErrorsInResultAreValid() {
        createSecondAdGroup();
        RelevanceMatch relevanceMatch1 = new RelevanceMatch()
                .withPrice(BigDecimal.ZERO)
                .withIsSuspended(true);

        ComplexTextAdGroup adGroupForUpdate1 = createValidAdGroupForUpdate(adGroupInfo1);
        adGroupForUpdate1.withRelevanceMatches(singletonList(relevanceMatch1));

        ComplexTextAdGroup adGroupForUpdate2 = createValidAdGroupForUpdate(adGroupInfo2);

        RelevanceMatch relevanceMatch2 = new RelevanceMatch()
                .withPrice(BigDecimal.ZERO)
                .withIsSuspended(true);

        adGroupForUpdate2.withRelevanceMatches(singletonList(relevanceMatch2));


        MassResult<Long> result = updateAndCheckBothItemsAreInvalid(asList(adGroupForUpdate1, adGroupForUpdate2));

        Path relevanceMatch1Path = path(index(0), field(ComplexTextAdGroup.RELEVANCE_MATCHES.name()),
                index(0), field(RelevanceMatch.PRICE.name()));
        Path relevanceMatch2Path = path(index(1), field(ComplexTextAdGroup.RELEVANCE_MATCHES.name()),
                index(0), field(RelevanceMatch.PRICE.name()));

        Currency currency = campaignInfo.getClientInfo().getClient().getWorkCurrency().getCurrency();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(relevanceMatch1Path,
                        invalidValueNotLessThan(Money.valueOf(currency.getMinPrice(), currency.getCode())))));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(relevanceMatch2Path,
                        invalidValueNotLessThan(Money.valueOf(currency.getMinPrice(), currency.getCode())))));
    }
}
