package ru.yandex.direct.core.entity.relevancematch.service.addoperation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.keyword.service.KeywordRecentStatisticsProvider;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchAddOperation;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionAutoPriceParams;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestKeywords;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.operation.AddedModelId;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.showcondition.Constants.DEFAULT_AUTOBUDGET_PRIORITY;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchAddAutoPricesTest extends RelevanceMatchAddOperationBaseTest {
    private static final double DEFAULT_PRICE = 3;

    /**
     * Если в режиме {@code autoPrices} передать в конструктор null вместо
     * контейнера с параметрами для автоцен, конструктор кинет исключение.
     */
    @Test(expected = IllegalArgumentException.class)
    public void create_withNullAutoPriceParams_throwException() {
        RelevanceMatch relevanceMatch = makeRelevanceMatchNoPrices(defaultAdGroup);
        createAddOperationWithAutoPrices(defaultAdGroup, relevanceMatch, null);
    }

    /**
     * Если в группе, куда добавляют автотаргетинг, нет фраз, выставляются
     * ставки по умолчанию.
     */
    @Test
    public void execute_withNoKeywords_defaultPrices() {
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveTextAdGroup(activeManualCampaign);
        RelevanceMatch relevanceMatch = makeRelevanceMatchNoPrices(adGroupInfo);
        ShowConditionAutoPriceParams autoPriceParams = makeEmptyAutoPriceParams();
        MassResult<AddedModelId> result = execute(adGroupInfo, relevanceMatch, autoPriceParams);
        assertThat(result, isSuccessfulWithMatchers(notNullValue(AddedModelId.class)));

        assertRelevanceMatchPrices(adGroupInfo, result.get(0).getResult().getId(), DEFAULT_PRICE, DEFAULT_PRICE);
    }

    /**
     * Если в кампании, куда добавляют автотаргетинг, автобюджетная стратегия,
     * ставки не выставляются, а выставляется приоритет автобюджета.
     */
    @Test
    public void execute_withNoKeywordsInAutoStrategy_defaultPrices() {
        RelevanceMatch relevanceMatch = makeRelevanceMatchNoPrices(defaultAdGroup);
        ShowConditionAutoPriceParams autoPriceParams = makeEmptyAutoPriceParams();
        MassResult<AddedModelId> result = execute(defaultAdGroup, relevanceMatch, autoPriceParams);
        assertThat(result, isSuccessfulWithMatchers(notNullValue(AddedModelId.class)));

        assertRelevanceMatchAutobudgetPriority(defaultAdGroup, result.get(0).getResult().getId());
    }

    /**
     * Если в группе, куда добавляют автотаргетинг, есть фразы, ставки
     * считаются калькулятором на основе цен в фразах.
     */
    @Test
    public void execute_witKeywords_calcPricesByKeyword() {
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveTextAdGroup(activeManualCampaign);
        createKeyword(adGroupInfo, 123, 456);
        createKeyword(adGroupInfo, 321, 654);

        RelevanceMatch newRelevanceMatch = makeRelevanceMatchNoPrices(adGroupInfo);
        ShowConditionAutoPriceParams autoPriceParams = makeEmptyAutoPriceParams();
        MassResult<AddedModelId> result = execute(adGroupInfo, newRelevanceMatch, autoPriceParams);
        assertThat(result, isSuccessfulWithMatchers(notNullValue(AddedModelId.class)));

        // (321 - 123) * 0.3 + 123 = 182.4
        double expectedSearchPrice = 182.4;
        double expectedContextPrice = (654 + 456) / 2d;
        assertRelevanceMatchPrices(adGroupInfo, result.get(0).getResult().getId(), expectedSearchPrice,
                expectedContextPrice);
    }

    /**
     * Если указана фиксированная автоставка, а кампания автобюджетная,
     * фиксированная ставка не выставляется у автотаргетинга.
     */
    @Test
    public void execute_withKeywordsWithGlobalFixedPriceInAutobudgetStrategy_noPrices() {
        createKeyword(defaultAdGroup, 123, 456);

        BigDecimal fixedPrice = BigDecimal.valueOf(134);
        RelevanceMatch newRelevanceMatch = makeRelevanceMatchNoPrices(defaultAdGroup);
        ShowConditionAutoPriceParams autoPriceParams =
                makeAutoPriceParams(ShowConditionFixedAutoPrices.ofGlobalFixedPrice(fixedPrice));
        MassResult<AddedModelId> result = execute(defaultAdGroup, newRelevanceMatch, autoPriceParams);
        assertThat(result, isSuccessfulWithMatchers(notNullValue(AddedModelId.class)));

        assertRelevanceMatchAutobudgetPriority(defaultAdGroup, result.get(0).getResult().getId());
    }

    /**
     * Если указана глобальная фиксированная ставка, она выставляется, даже
     * если в группе есть фразы.
     */
    @Test
    public void execute_withKeywordsWithGlobalFixedPrice_setFixedPrice() {
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveTextAdGroup(activeManualCampaign);
        createKeyword(adGroupInfo, 123, 456);

        BigDecimal fixedPrice = BigDecimal.valueOf(134);
        RelevanceMatch newRelevanceMatch = makeRelevanceMatchNoPrices(adGroupInfo);
        ShowConditionAutoPriceParams autoPriceParams =
                makeAutoPriceParams(ShowConditionFixedAutoPrices.ofGlobalFixedPrice(fixedPrice));
        MassResult<AddedModelId> result = execute(adGroupInfo, newRelevanceMatch, autoPriceParams);
        assertThat(result, isSuccessfulWithMatchers(notNullValue(AddedModelId.class)));

        assertRelevanceMatchPrices(adGroupInfo, result.get(0).getResult().getId(), fixedPrice.doubleValue(),
                fixedPrice.doubleValue());
    }

    /**
     * Если добавляются два автотаргетинга в две группы, но фиксированная
     * ставка указана только для второй группы, в первой группе ставки
     * считаются калькулятором на основе ставок из фраз первой группы.
     * А во второй группе выставляются фиксированные ставки.
     */
    @Test
    public void execute_withKeywordsWithPerAdGroupFixedPrice_setFixedPrice() {
        double keywordSearchPrice = 123;
        double keywordContextPrice = 456;
        AdGroupInfo adGroupInfo1 = adGroupSteps.createActiveTextAdGroup(activeManualCampaign);
        createKeyword(adGroupInfo1, keywordSearchPrice, keywordContextPrice);
        AdGroupInfo adGroupInfo2 = adGroupSteps.createActiveTextAdGroup(activeManualCampaign);
        createKeyword(adGroupInfo2, 321, 654);

        BigDecimal fixedPrice = BigDecimal.valueOf(134);
        RelevanceMatch newRelevanceMatch1 = makeRelevanceMatchNoPrices(adGroupInfo1);
        RelevanceMatch newRelevanceMatch2 = makeRelevanceMatchNoPrices(adGroupInfo2);
        ShowConditionAutoPriceParams autoPriceParams =
                makeAutoPriceParams(ShowConditionFixedAutoPrices
                        .ofPerAdGroupFixedPrices(singletonMap(adGroupInfo2.getAdGroupId(), fixedPrice)));
        MassResult<AddedModelId> result = execute(asList(adGroupInfo1, adGroupInfo2),
                asList(newRelevanceMatch1, newRelevanceMatch2),
                autoPriceParams
        );
        assertThat(result,
                isSuccessfulWithMatchers(notNullValue(AddedModelId.class), notNullValue(AddedModelId.class)));

        assertRelevanceMatchPrices(adGroupInfo1, result.get(0).getResult().getId(), keywordSearchPrice,
                keywordContextPrice);
        assertRelevanceMatchPrices(adGroupInfo2, result.get(1).getResult().getId(), fixedPrice.doubleValue(),
                fixedPrice.doubleValue());
    }

    private void createKeyword(AdGroupInfo adGroupInfo, double searchPrice, double contextPrice) {
        steps.keywordSteps().createKeyword(
                adGroupInfo,
                TestKeywords.defaultKeyword()
                        .withPrice(BigDecimal.valueOf(searchPrice))
                        .withPriceContext(BigDecimal.valueOf(contextPrice))
        );
    }

    private void assertRelevanceMatchPrices(AdGroupInfo adGroupInfo, Long id, Double searchPrice, Double contextPrice) {
        RelevanceMatch relevanceMatch = getRelevanceMatch(adGroupInfo, id);
        BigDecimal expectedSearchPrice =
                searchPrice != null ? BigDecimal.valueOf(searchPrice).setScale(2, RoundingMode.UNNECESSARY) : null;
        assertThat("Поисковая ставка не соответствует ожидаемой", relevanceMatch.getPrice(), is(expectedSearchPrice));
        BigDecimal expectedContextPrice =
                contextPrice != null ? BigDecimal.valueOf(contextPrice).setScale(2, RoundingMode.UNNECESSARY) : null;
        assertThat("Ставка в сети не соответствует ожидаемой", relevanceMatch.getPriceContext(),
                is(expectedContextPrice));
        assertThat(relevanceMatch.getAutobudgetPriority(), nullValue());
    }

    private void assertRelevanceMatchAutobudgetPriority(AdGroupInfo adGroupInfo, Long id) {
        RelevanceMatch relevanceMatch = getRelevanceMatch(adGroupInfo, id);
        assertThat(relevanceMatch.getPrice(), nullValue());
        assertThat(relevanceMatch.getPriceContext(), nullValue());
        assertThat(relevanceMatch.getAutobudgetPriority(), is(DEFAULT_AUTOBUDGET_PRIORITY));
    }

    private RelevanceMatch getRelevanceMatch(AdGroupInfo adGroupInfo, Long id) {
        return relevanceMatchRepository.getRelevanceMatchesByIds(
                adGroupInfo.getShard(),
                adGroupInfo.getClientId(),
                singletonList(id)
        ).get(id);
    }

    private ShowConditionAutoPriceParams makeEmptyAutoPriceParams() {
        return makeAutoPriceParams(ShowConditionFixedAutoPrices.ofGlobalFixedPrice(null));
    }

    private ShowConditionAutoPriceParams makeAutoPriceParams(ShowConditionFixedAutoPrices fixedAutoPrices) {
        KeywordRecentStatisticsProvider recentStats = keywordRequests -> emptyMap();
        return new ShowConditionAutoPriceParams(
                fixedAutoPrices,
                recentStats
        );
    }

    private MassResult<AddedModelId> execute(
            AdGroupInfo adGroupInfo,
            RelevanceMatch relevanceMatche,
            ShowConditionAutoPriceParams autoPriceParams) {
        RelevanceMatchAddOperation addOperation =
                createAddOperationWithAutoPrices(adGroupInfo, relevanceMatche, autoPriceParams);
        return addOperation.prepareAndApply();
    }

    private MassResult<AddedModelId> execute(
            List<AdGroupInfo> adGroupInfos,
            List<RelevanceMatch> relevanceMatches,
            ShowConditionAutoPriceParams autoPriceParams) {
        RelevanceMatchAddOperation addOperation =
                createAddOperationWithAutoPrices(adGroupInfos, relevanceMatches, autoPriceParams);
        return addOperation.prepareAndApply();
    }
}
