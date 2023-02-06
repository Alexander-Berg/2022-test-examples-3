package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.retargeting.Constants;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.showcondition.Constants.DEFAULT_AUTOBUDGET_PRIORITY;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationAutoPricesTest extends KeywordsAddOperationBaseTest {
    // число 32.5 получилось наощупь. Фейковый аукцион выдавал 25 как ставку в гарантии, а 25 + 30% = 32.5
    private static final BigDecimal AUCTION_AUTO_PRICE = BigDecimal.valueOf(32.5).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal DEFAULT_PRICE =
            CurrencyRub.getInstance().getDefaultPrice().setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal FIXED_AUTO_PRICE = BigDecimal.valueOf(358).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal OLD_SEARCH_PRICE = BigDecimal.valueOf(312).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal OLD_CONTEXT_PRICE = BigDecimal.valueOf(567).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal OLD_SEARCH_PRICE2 = BigDecimal.valueOf(729).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal OLD_CONTEXT_PRICE2 = BigDecimal.valueOf(18).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal CUSTOM_SEARCH_PRICE = BigDecimal.valueOf(259).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal CUSTOM_CONTEXT_PRICE =
            BigDecimal.valueOf(198).setScale(2, RoundingMode.UNNECESSARY);
    private static final int CUSTOM_AUTOBUDGET_PRIORITY = 5;

    @Autowired
    private ClientService clientService;
    private Currency clientCurrency;

    @Before
    public void before() {
        super.before();
        clientCurrency = clientService.getWorkCurrency(clientInfo.getClientId());
    }

    @Test
    public void cpmBannerAdGroupInAutobudgetCampaign() {
        CampaignInfo autobudgetCampaign = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(averageCpaStrategy()), clientInfo);
        AdGroupInfo adGroup = adGroupSteps.createActiveCpmBannerAdGroup(autobudgetCampaign, CriterionType.KEYWORD);

        Keyword keyword = addDefaultKeyword(adGroup);
        assertThat("цена в сетях корректная", keyword.getPriceContext(), Matchers.nullValue());
        assertThat("приоритет автобюджета корректный", keyword.getAutobudgetPriority(),
                is(Constants.DEFAULT_AUTOBUDGET_PRIORITY));
    }

    @Test
    public void cpmBannerAdGroupInManualStrategyCampaign() {
        AdGroupInfo adGroup = adGroupSteps.createActiveCpmBannerAdGroup(clientInfo, CriterionType.KEYWORD);

        double fixedPrice = 100;
        Keyword keyword = addDefaultKeyword(adGroup, BigDecimal.valueOf(fixedPrice));
        assertThat("цена в сетях корректная", moneyOf(keyword.getPriceContext().doubleValue()),
                equalTo(moneyOf(fixedPrice)));
        assertThat("приоритет автобюджета корректный", keyword.getAutobudgetPriority(), Matchers.nullValue());
    }

    @Test
    public void textAdGroupInAutoBudgetCampaign() {
        CampaignInfo autobudgetCampaign =
                steps.campaignSteps().createCampaign(activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(averageCpaStrategy()), clientInfo);
        AdGroupInfo textAdGroup = steps.adGroupSteps().createActiveTextAdGroup(autobudgetCampaign);

        Keyword keyword = addDefaultKeyword(textAdGroup);
        assertThat("цена в сетях корректная", keyword.getPriceContext(), Matchers.nullValue());
        assertThat("приоритет автобюджета корректный", keyword.getAutobudgetPriority(),
                is(Constants.DEFAULT_AUTOBUDGET_PRIORITY));
    }

    /**
     * Если создать операцию в режиме {@code autoPrices}, и не передать
     * контейнер с фиксированными ставками, конструктор сразу упадет.
     */
    @Test(expected = NullPointerException.class)
    public void create_withNullFixedAutoPrices_throwsException() {
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_1));
        createOperationWithAutoPrices(keywords, null, null, null);
    }

    /**
     * Если передать пустой контейнер с фиксированными ставками, они будут
     * посчитаны с помощью калькулятора (по торгам + по умолчанию).
     */
    @Test
    public void execute_withEmptyFixedAutoPrices_auctionAutoPrices() {
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = addKeywordsEmptyFixedAutoPrices(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
        assertAddedAdGroupKeywordsHavePrices(result, adGroupInfo1, AUCTION_AUTO_PRICE, DEFAULT_PRICE);
    }

    /**
     * Если передать контейнер с глобальной фиксированной автоставкой,
     * новым фразам будет выставлена она.
     */
    @Test
    public void execute_withGlobalFixedAutoPrice_fixedAutoPrice() {
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_1));
        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE);
        MassResult<AddedKeywordInfo> result = addKeywords(keywords, fixedAutoPrices);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
        assertAddedAdGroupKeywordsHavePrices(result, adGroupInfo1, FIXED_AUTO_PRICE, FIXED_AUTO_PRICE);
    }

    /**
     * Если передать контейнер с фиксированной автоставкой для группы,
     * новой фразе в этой группе будет выставлена она.
     */
    @Test
    public void execute_withAdGroupFixedAutoPrice_fixedAutoPrices() {
        createOneActiveAdGroup();
        List<Keyword> keywords = Arrays.asList(
                newKeywordEmptyPrices(adGroupInfo1, PHRASE_1),
                newKeywordEmptyPrices(adGroupInfo1, PHRASE_2)
        );
        ShowConditionFixedAutoPrices fixedAutoPrices = ShowConditionFixedAutoPrices.
                ofPerAdGroupFixedPrices(singletonMap(adGroupInfo1.getAdGroupId(), FIXED_AUTO_PRICE));
        MassResult<AddedKeywordInfo> result = addKeywords(keywords, fixedAutoPrices);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));
        assertAddedAdGroupKeywordsHavePrices(result, adGroupInfo1, FIXED_AUTO_PRICE, FIXED_AUTO_PRICE);
    }

    /**
     * Если передать контейнер с фиксированной автоставкой для группы 2,
     * новой фразе в группе 1 она выставлена не будет, а посчитается
     * ставка калькулятором.
     * А новой фразе в группе 2 будет выставлена фиксированная ставка.
     */
    @Test
    public void execute_withOtherAdGroupFixedAutoPrice_auctionPrices() {
        createTwoActiveAdGroups();
        List<Keyword> keywords = Arrays.asList(
                newKeywordEmptyPrices(adGroupInfo1, PHRASE_1),
                newKeywordEmptyPrices(adGroupInfo1, PHRASE_2),
                newKeywordEmptyPrices(adGroupInfo2, PHRASE_3)
        );
        ShowConditionFixedAutoPrices fixedAutoPrices = ShowConditionFixedAutoPrices.
                ofPerAdGroupFixedPrices(singletonMap(adGroupInfo2.getAdGroupId(), FIXED_AUTO_PRICE));
        MassResult<AddedKeywordInfo> result = addKeywords(keywords, fixedAutoPrices);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2), isAdded(PHRASE_3)));
        assertAddedAdGroupKeywordsHavePrices(result, adGroupInfo1, AUCTION_AUTO_PRICE, DEFAULT_PRICE);
        assertAddedAdGroupKeywordsHavePrices(result, adGroupInfo2, FIXED_AUTO_PRICE, FIXED_AUTO_PRICE);
    }

    /**
     * Если фраза добавляется в группу с автостратегией, не выставляются
     * ни фиксированные, ни посчитанные калькулятором ставки. Остается {@code null}.
     * Приоритет автобюджета для новой фразы выставляется в значение по умолчанию.
     */
    @Test
    public void execute_withFixedAutoPrice_inAutoStrategy_pricesAreNotSet() {
        createOneActiveAdGroupAutoStrategy();
        List<Keyword> keywords = singletonList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_1));
        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE);
        MassResult<AddedKeywordInfo> result = addKeywords(keywords, fixedAutoPrices);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
        assertAddedAdGroupKeywordsHaveAutobudgetPriority(result, adGroupInfo1);
    }

    /**
     * Если фиксированных автоставок нет, и в торги сходить не получилось,
     * выставляются ставки по умолчанию.
     */
    @Test
    public void execute_noFixedPrice_noAuction_defaultPrices() {
        createOneActiveAdGroupCpmBannerWithManualStrategy();
        List<Keyword> keywords = singletonList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = addKeywordsEmptyFixedAutoPrices(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
        assertAddedAdGroupKeywordsHavePrices(result, adGroupInfo1, DEFAULT_PRICE, DEFAULT_PRICE);
    }

    /**
     * Если фиксированных автоставок нет, а в группе уже есть фраза,
     * у новой фразы вытавляется такая же ставка.
     */
    @Test
    public void execute_noFixedPrice_withOneCommonPrice_setCommonPrices() {
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
        List<Keyword> keywords = singletonList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_2));
        MassResult<AddedKeywordInfo> result = addKeywordsEmptyFixedAutoPrices(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_2)));
        assertAddedAdGroupKeywordsHavePrices(result, adGroupInfo1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
    }

    /**
     * Если фиксированных автоставок нет, а в группе уже есть несколько
     * фраз с одинаковой ставкой, у новой фразы вытавляется такая же ставка.
     */
    @Test
    public void execute_noFixedPrice_withTwoCommonPrices_setCommonPrices() {
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
        createKeyword(adGroupInfo1, PHRASE_2, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
        List<Keyword> keywords = singletonList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_3));
        MassResult<AddedKeywordInfo> result = addKeywordsEmptyFixedAutoPrices(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_3)));
        assertAddedAdGroupKeywordsHavePrices(result, adGroupInfo1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
    }

    /**
     * Если фиксированных автоставок нет, и в группе есть фразы с разными ставками,
     * у новой фразы ставки считаются по торгам + по умолчанию.
     */
    @Test
    public void execute_noFixedPrice_withDifferentOldPrices_setAuctionPrices() {
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
        createKeyword(adGroupInfo1, PHRASE_2, OLD_SEARCH_PRICE2, OLD_CONTEXT_PRICE2);
        List<Keyword> keywords = singletonList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_3));
        MassResult<AddedKeywordInfo> result = addKeywordsEmptyFixedAutoPrices(keywords);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_3)));
        assertAddedAdGroupKeywordsHavePrices(result, adGroupInfo1, AUCTION_AUTO_PRICE, DEFAULT_PRICE);
    }

    /**
     * Если фиксированных автоставок нет, и в операцию передан калькулятор
     * с той же фразой, которую собираются добавлять (например, она была
     * удалена перед передобавлением), у передобавленной фразы будут выставлены
     * те же ставки, какие были раньше.
     * Здесь также нарочно добавляются существующие фразы с другими одинаковыми
     * ставками, чтобы проверить, что операция не использует свой калькулятор.
     */
    @Test
    public void execute_noFixedPrice_withSameDeletedPhrase_setOldPrice() {
        createOneActiveAdGroup();
        createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
        createKeyword(adGroupInfo1, PHRASE_2, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
        List<Keyword> keywords = singletonList(newKeywordEmptyPrices(adGroupInfo1, PHRASE_3));
        List<Keyword> deletedKeywords = singletonList(
                newKeywordEmptyPrices(adGroupInfo1, PHRASE_3)
                        .withPrice(OLD_SEARCH_PRICE2)
                        .withPriceContext(OLD_CONTEXT_PRICE2)
        );
        KeywordAutoPricesCalculator customCalculator = new KeywordAutoPricesCalculator(
                CurrencyRub.getInstance(), deletedKeywords, keywordNormalizer
        );
        MassResult<AddedKeywordInfo> result = addKeywordsEmptyFixedAutoPrices(keywords, customCalculator);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_3)));
        assertAddedAdGroupKeywordsHavePrices(result, adGroupInfo1, OLD_SEARCH_PRICE2, OLD_CONTEXT_PRICE2);
    }

    /**
     * Если добавляется фраза с явно указанными ставками, оставляем их и ничего
     * не придумываем.
     */
    @Test
    public void execute_withExplicitPrices_setExplicitPrices() {
        createOneActiveAdGroup();
        List<Keyword> keywords = singletonList(
                newKeywordEmptyPrices(adGroupInfo1, PHRASE_1)
                        .withPrice(CUSTOM_SEARCH_PRICE)
                        .withPriceContext(CUSTOM_CONTEXT_PRICE)
        );
        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE);
        MassResult<AddedKeywordInfo> result = addKeywords(keywords, fixedAutoPrices);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
        assertAddedAdGroupKeywordsHavePrices(result, adGroupInfo1, CUSTOM_SEARCH_PRICE, CUSTOM_CONTEXT_PRICE);
    }

    /**
     * Если добавляется фраза в автостратегии с явно указанным приоритетом
     * автобюджета, оставляем его и ничего не придумываем.
     */
    @Test
    public void execute_withExplicitAutobudgetPriority_setExplicitAutobudgetPriority() {
        createOneActiveAdGroupAutoStrategy();
        List<Keyword> keywords = singletonList(
                newKeywordEmptyPrices(adGroupInfo1, PHRASE_1)
                        .withAutobudgetPriority(CUSTOM_AUTOBUDGET_PRIORITY)
        );
        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE);
        MassResult<AddedKeywordInfo> result = addKeywords(keywords, fixedAutoPrices);
        assertThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));
        assertAddedAdGroupKeywordsHaveAutobudgetPriority(result, adGroupInfo1, CUSTOM_AUTOBUDGET_PRIORITY);
    }

    private Keyword addDefaultKeyword(AdGroupInfo adGroupInfo, BigDecimal fixedPrice) {
        List<Keyword> keywords = singletonList(defaultKeyword(adGroupInfo));

        KeywordsAddOperation addOperation = createOperationWithAutoPrices(Applicability.PARTIAL, fixedPrice, keywords);
        MassResult<AddedKeywordInfo> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        List<Keyword> actualKeywords =
                keywordRepository.getKeywordsByAdGroupId(clientInfo.getShard(), adGroupInfo.getAdGroupId());
        assertThat("в группе одна фраза", actualKeywords, hasSize(1));
        return actualKeywords.get(0);
    }

    private Keyword addDefaultKeyword(AdGroupInfo adGroupInfo) {
        return addDefaultKeyword(adGroupInfo, null);
    }

    private Keyword defaultKeyword(AdGroupInfo adGroupInfo) {
        return new Keyword()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withPhrase("some phrase");
    }

    private Money moneyOf(double price) {
        return Money.valueOf(price, clientCurrency.getCode());
    }

    /**
     * Добавление фраз с передачей пустого контейнера с фиксированными автоставками.
     */
    private MassResult<AddedKeywordInfo> addKeywordsEmptyFixedAutoPrices(List<Keyword> keywords) {
        ShowConditionFixedAutoPrices fixedAutoPrices = ShowConditionFixedAutoPrices.ofGlobalFixedPrice(null);
        return createOperationWithAutoPrices(keywords, null, fixedAutoPrices, null)
                .prepareAndApply();
    }


    /**
     * Добавление фраз с передачей пустого контейнера с фиксированными автоставками и
     * собственным калькулятором автоставок.
     */
    private MassResult<AddedKeywordInfo> addKeywordsEmptyFixedAutoPrices(List<Keyword> keywords,
                                                                         KeywordAutoPricesCalculator customCalculator) {
        ShowConditionFixedAutoPrices fixedAutoPrices = ShowConditionFixedAutoPrices.ofGlobalFixedPrice(null);
        return createOperationWithAutoPrices(keywords, null, fixedAutoPrices, customCalculator)
                .prepareAndApply();
    }

    /**
     * Добавление фраз с передачей собственного контейнера с фиксированными
     * автоставками.
     */
    private MassResult<AddedKeywordInfo> addKeywords(List<Keyword> keywords,
                                                     ShowConditionFixedAutoPrices fixedAutoPrices) {
        return createOperationWithAutoPrices(keywords, null, fixedAutoPrices, null)
                .prepareAndApply();
    }

    /**
     * Проверяет, что во всех новых фразах, добавленных в указанную группу
     * выставлены ставки {@code searchPrice} и {@code contextPrice},
     * а приоритет автобюджета остался в значении {@code null}.
     */
    private void assertAddedAdGroupKeywordsHavePrices(MassResult<AddedKeywordInfo> result, AdGroupInfo adGroupInfo,
                                                      BigDecimal searchPrice, BigDecimal contextPrice) {
        List<Long> addedAdGroupKeywordIds = result.getResult().stream()
                .filter(r -> r.getResult().isAdded() && r.getResult().getAdGroupId().equals(adGroupInfo.getAdGroupId()))
                .map(r -> r.getResult().getId())
                .collect(Collectors.toList());
        List<Keyword> keywords =
                keywordRepository.getKeywordsByIds(
                        adGroupInfo.getShard(), adGroupInfo.getClientId(), addedAdGroupKeywordIds
                );
        for (Keyword keyword : keywords) {
            assertThat("Не у всех фраз в группе выставлена правильная ставка на поиске", keyword.getPrice(),
                    is(searchPrice));
            assertThat("Не у всех фраз в группе выставлена правильная ставка в сети", keyword.getPriceContext(),
                    is(contextPrice));
            assertThat("У некоторых фраз был выставлен приоритет автобюджета", keyword.getAutobudgetPriority(),
                    nullValue());
        }
    }

    private void assertAddedAdGroupKeywordsHaveAutobudgetPriority(
            MassResult<AddedKeywordInfo> result, AdGroupInfo adGroupInfo) {
        assertAddedAdGroupKeywordsHaveAutobudgetPriority(result, adGroupInfo, DEFAULT_AUTOBUDGET_PRIORITY);
    }

    /**
     * Проверяет, что во всех новых фразах, добавленных в указанную группу
     * выставлен приоритет автобюджета по умолчанию, а ставки остались
     * в значении {@code null}.
     */
    private void assertAddedAdGroupKeywordsHaveAutobudgetPriority(
            MassResult<AddedKeywordInfo> result, AdGroupInfo adGroupInfo, int autobudgetPriority) {
        List<Long> addedAdGroupKeywordIds = result.getResult().stream()
                .filter(r -> r.getResult().isAdded() && r.getResult().getAdGroupId().equals(adGroupInfo.getAdGroupId()))
                .map(r -> r.getResult().getId())
                .collect(Collectors.toList());
        List<Keyword> keywords =
                keywordRepository.getKeywordsByIds(
                        adGroupInfo.getShard(), adGroupInfo.getClientId(), addedAdGroupKeywordIds
                );
        for (Keyword keyword : keywords) {
            assertThat("У некоторых фраз в группе была выставлена ставка на поиске", keyword.getPrice(), nullValue());
            assertThat("У некоторых фраз в группе была выставлена ставка в сети", keyword.getPriceContext(),
                    nullValue());
            assertThat("Не у всех фраз в группе был выставлен приоритет автобюджета", keyword.getAutobudgetPriority(),
                    is(autobudgetPriority));
        }
    }
}
