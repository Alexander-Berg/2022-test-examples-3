package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.ComplexTextAdGroupAddOperation;
import ru.yandex.direct.core.entity.bids.validation.BidsDefects;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.validation.defects.params.CurrencyAmountDefectParams;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.defaultRelevanceMatch;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyAdGroupWithModelForAdd;
import static ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchDefects.maxRelevanceMatchesInAdGroup;
import static ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchValidationService.MAX_RELEVANCE_MATCHES_IN_GROUP;
import static ru.yandex.direct.currency.Money.valueOf;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexAddRelevanceMatchTest extends ComplexTextAddTestBase {
    private static final BigDecimal MIN_AUTOBUDGET_BID = new BigDecimal("0.3");

    @Test
    public void oneAdGroupWithRelevanceMatch() {
        ComplexTextAdGroup adGroup = adGroupWithRelevanceMatches(singletonList(defaultRelevanceMatch()));
        addAndCheckComplexAdGroups(singletonList(adGroup));
    }

    @Test
    public void oneAdGroupWithoutRelevanceMatchesAndOneWith() {
        ComplexTextAdGroup emptyComplexAdGroup = emptyTextAdGroup();
        ComplexTextAdGroup adGroupWithRelevanceMatches =
                adGroupWithRelevanceMatches(singletonList(defaultRelevanceMatch()));
        addAndCheckComplexAdGroups(asList(emptyComplexAdGroup, adGroupWithRelevanceMatches));
    }

    @Test
    public void adGroupWithMaxRelevanceMatches() {
        List<RelevanceMatch> relevanceMatches = new ArrayList<>();
        for (int i = 0; i < MAX_RELEVANCE_MATCHES_IN_GROUP; ++i) {
            relevanceMatches.add(defaultRelevanceMatch());
        }
        ComplexTextAdGroup adGroup = adGroupWithRelevanceMatches(relevanceMatches);
        addAndCheckComplexAdGroups(singletonList(adGroup));
    }

    /**
     * Проверяем, что режим {@code autoPrices} корректно прокидывается до
     * {@link ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchAddOperation}
     */
    @Test
    public void adGroupWithRelevanceMatchWithAutoPrices() {
        RelevanceMatch relevanceMatch = defaultRelevanceMatch()
                .withPrice(null)
                .withPriceContext(null);
        ComplexTextAdGroup adGroup = adGroupWithRelevanceMatches(singletonList(relevanceMatch));
        addWithAutoPricesAndCheckComplexAdGroups(singletonList(adGroup));

        List<RelevanceMatch> relevanceMatches = testCommons.getRelevanceMatchesOfAdGroup(
                adGroup.getAdGroup().getId(),
                campaign.getClientId(), campaign.getShard()
        );
        assertThat("у автотаргетинга проставилась автоматическая ставка",
                relevanceMatches.get(0).getPrice(),
                is(FIXED_AUTO_PRICE)
        );
    }

    @Test
    public void adGroupWithTooManyRelevanceMatches() {
        List<RelevanceMatch> relevanceMatches = new ArrayList<>();
        for (int i = 0; i < MAX_RELEVANCE_MATCHES_IN_GROUP + 1; ++i) {
            relevanceMatches.add(defaultRelevanceMatch());
        }
        ComplexTextAdGroup adGroup = adGroupWithRelevanceMatches(relevanceMatches);
        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat("превышено максимальное количество бесфразных таргетингов в группе", result.getValidationResult(),
                allOf(
                        hasDefectDefinitionWith(validationError(
                                path(index(0), field(ComplexTextAdGroup.RELEVANCE_MATCHES.name()), index(0)),
                                maxRelevanceMatchesInAdGroup())),
                        hasDefectDefinitionWith(validationError(
                                path(index(0), field(ComplexTextAdGroup.RELEVANCE_MATCHES.name()), index(1)),
                                maxRelevanceMatchesInAdGroup()))
                )
        );
    }

    @Test
    public void oneAdGroupWithValidAndOneWithInvalidRelevanceMatch() {
        ComplexTextAdGroup adGroupValid = adGroupWithRelevanceMatches(singletonList(defaultRelevanceMatch()));
        ComplexTextAdGroup adGroupInvalid = emptyTextAdGroup()
                .withRelevanceMatches(singletonList(defaultRelevanceMatch().withPrice(new BigDecimal(0.2))));
        ComplexTextAdGroupAddOperation addOperation = createOperation(asList(adGroupValid, adGroupInvalid));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(
                path(index(1), field(ComplexTextAdGroup.RELEVANCE_MATCHES.name()), index(0), field("price")),
                new Defect<>(
                        BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN,
                        new CurrencyAmountDefectParams(valueOf(MIN_AUTOBUDGET_BID, CurrencyCode.RUB))))));
    }

    protected ComplexTextAdGroup adGroupWithRelevanceMatches(List<RelevanceMatch> relevanceMatches) {
        return emptyAdGroupWithModelForAdd(campaignId, relevanceMatches, ComplexTextAdGroup.RELEVANCE_MATCHES);
    }
}
