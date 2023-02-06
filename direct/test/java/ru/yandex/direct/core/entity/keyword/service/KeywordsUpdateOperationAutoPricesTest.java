package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.retargeting.Constants;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionFixedAutoPrices;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.entity.showcondition.Constants.DEFAULT_AUTOBUDGET_PRIORITY;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForCpmBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationAutoPricesTest extends KeywordsUpdateOperationBaseTest {
    // число 32.5 получилось наощупь. Фейковый аукцион выдавал 25 как ставку в гарантии, а 25 + 30% = 32.5
    private static final BigDecimal AUCTION_AUTO_PRICE = BigDecimal.valueOf(32.5).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal DEFAULT_PRICE =
            CurrencyRub.getInstance().getDefaultPrice().setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal OLD_SEARCH_PRICE = BigDecimal.valueOf(72).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal OLD_CONTEXT_PRICE = BigDecimal.valueOf(299).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal OLD_SEARCH_PRICE2 = BigDecimal.valueOf(683).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal OLD_CONTEXT_PRICE2 = BigDecimal.valueOf(679).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal FIXED_AUTO_PRICE = BigDecimal.valueOf(503).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal CUSTOM_SEARCH_PRICE = BigDecimal.valueOf(229).setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal CUSTOM_CONTEXT_PRICE =
            BigDecimal.valueOf(301).setScale(2, RoundingMode.UNNECESSARY);

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
        KeywordInfo keyword = keywordSteps.createKeyword(adGroup, keywordForCpmBanner());

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keyword.getId(), "brand new phrase"));
        KeywordsUpdateOperation operation = createOperationWithAutoPrices(null, changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        Keyword actualKeyword =
                keywordRepository.getKeywordsByIds(autobudgetCampaign.getShard(), autobudgetCampaign.getClientId(),
                        singletonList(keyword.getId())).get(0);

        assertThat("цена в сетях корректная", actualKeyword.getPriceContext(), Matchers.nullValue());
        assertThat("приоритет автобюджета корректный", actualKeyword.getAutobudgetPriority(),
                is(Constants.DEFAULT_AUTOBUDGET_PRIORITY));
    }

    @Test
    public void cpmBannerAdGroupInManualStrategyCampaign() {
        AdGroupInfo adGroup = adGroupSteps.createActiveCpmBannerAdGroup(clientInfo, CriterionType.KEYWORD);
        KeywordInfo keyword = keywordSteps.createKeyword(adGroup, keywordForCpmBanner());

        double fixedPrice = 200;
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keyword.getId(), "brand new phrase"));
        KeywordsUpdateOperation operation =
                createOperationWithAutoPrices(BigDecimal.valueOf(fixedPrice), changesKeywords);
        MassResult<UpdatedKeywordInfo> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        Keyword actualKeyword =
                keywordRepository.getKeywordsByIds(adGroup.getShard(), adGroup.getClientId(),
                        singletonList(keyword.getId())).get(0);

        assertThat("цена в сетях корректная", moneyOf(actualKeyword.getPriceContext().doubleValue()),
                equalTo(moneyOf(fixedPrice)));
        assertThat("приоритет автобюджета корректный", actualKeyword.getAutobudgetPriority(), Matchers.nullValue());
    }

    /**
     * Если создать операцию в режиме {@code autoPrices}, и не передать
     * контейнер с фиксированными ставками, конструктор сразу упадет.
     */
    @Test(expected = NullPointerException.class)
    public void create_withNullFixedAutoPrices_throwsException() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));

        createOperationWithAutoPrices(changesKeywords, null, null);
    }

    /**
     * Если у фразы поменять текст, и не указать фиксированную автоставку,
     * она будет посчитана калькулятором. В данном случае - по торгам.
     * Если бы в группе была только одна фраза, было бы применено правило
     * "выставить общую ставку для всей группы", т.е. ту же ставку, которая
     * была у фразы до изменения.
     */
    @Test
    public void execute_ChangePhraseNormalFormWithEmptyFixedAutoPrices_auctionPricesAreSet() {
        createOneActiveAdGroup();
        String phrase1 = "слон розовый";
        String phrase2 = "слон розовый пятнистый";
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE2, OLD_CONTEXT_PRICE2).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, phrase2));

        ShowConditionFixedAutoPrices fixedAutoPrices = ShowConditionFixedAutoPrices.ofGlobalFixedPrice(null);
        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, phrase2)));
        assertKeywordPrices(keywordIdToUpdate, AUCTION_AUTO_PRICE, DEFAULT_PRICE);
    }

    /**
     * Если у фразы поменять текст и указать глобальную фиксированную
     * автоставку, фразе будет выставлена она.
     */
    @Test
    public void execute_ChangePhraseNormalFormWithGlobalFixedAutoPrice_fixedPriceIsSet() {
        createOneActiveAdGroup();
        String phrase1 = "слон розовый";
        String phrase2 = "слон розовый пятнистый";
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, phrase2));

        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE);
        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, phrase2)));
        assertKeywordPrices(keywordIdToUpdate, FIXED_AUTO_PRICE, FIXED_AUTO_PRICE);
    }

    /**
     * Если у фразы незначительно поменять текст (нормальная форма без учета минус-слов не меняется),
     * то существующая ставка не изменится.
     */
    @Test
    public void execute_AddMinusWordWithEmptyFixedAutoPrices_OldPricesAreSaved() {
        createOneActiveAdGroup();
        String phrase1 = "слон";
        String phrase2 = "слон -розовый";
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        createKeyword(adGroupInfo1, "конь", OLD_SEARCH_PRICE2, OLD_CONTEXT_PRICE2).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, phrase2));

        ShowConditionFixedAutoPrices fixedAutoPrices = ShowConditionFixedAutoPrices.ofGlobalFixedPrice(null);
        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, phrase2)));
        assertKeywordPrices(keywordIdToUpdate, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
    }

    /**
     * Если у фразы незначительно поменять текст (нормальная форма без учета минус-слов не меняется),
     * то существующая ставка не изменится.
     */
    @Test
    public void execute_DeleteMinusWordWithEmptyFixedAutoPrices_OldPricesAreSaved() {
        createOneActiveAdGroup();
        String phrase1 = "слон -розовый -африканский";
        String phrase2 = "слон -розовый";
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        createKeyword(adGroupInfo1, "конь", OLD_SEARCH_PRICE2, OLD_CONTEXT_PRICE2).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, phrase2));

        ShowConditionFixedAutoPrices fixedAutoPrices = ShowConditionFixedAutoPrices.ofGlobalFixedPrice(null);
        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, phrase2)));
        assertKeywordPrices(keywordIdToUpdate, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
    }

    /**
     * Если у фразы незначительно поменять текст (нормальная форма без учета минус-слов не меняется),
     * то существующая ставка не изменится.
     */
    @Test
    public void execute_AddMinusWordWithFilledFixedAutoPrices_OldPricesAreSaved() {
        createOneActiveAdGroup();
        String phrase1 = "слон";
        String phrase2 = "слон -розовый";
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        createKeyword(adGroupInfo1, "конь", OLD_SEARCH_PRICE2, OLD_CONTEXT_PRICE2).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, phrase2));

        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE);
        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, phrase2)));
        assertKeywordPrices(keywordIdToUpdate, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
    }

    /**
     * Если у фразы незначительно поменять текст (нормальная форма без учета минус-слов не меняется),
     * то существующая ставка не изменится.
     */
    @Test
    public void execute_DeleteMinusWordWithFilledFixedAutoPrices_OldPricesAreSaved() {
        createOneActiveAdGroup();
        String phrase1 = "слон -розовый -африканский";
        String phrase2 = "слон -розовый";
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        createKeyword(adGroupInfo1, "конь", OLD_SEARCH_PRICE2, OLD_CONTEXT_PRICE2).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, phrase2));

        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE);
        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, phrase2)));
        assertKeywordPrices(keywordIdToUpdate, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
    }

    /**
     * Если у фразы поменять текст и указать фиксированную ставку для группы,
     * фразе будет выставлена она.
     */
    @Test
    public void execute_withAdGroupFixedAutoPrice_fixedAutoPrices() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));
        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices
                        .ofPerAdGroupFixedPrices(singletonMap(adGroupInfo1.getAdGroupId(), FIXED_AUTO_PRICE));

        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));
        assertKeywordPrices(keywordIdToUpdate, FIXED_AUTO_PRICE, FIXED_AUTO_PRICE);
    }

    /**
     * Если поменять текст у двух фраз из разных групп, но фиксированную
     * автоставку указать только для второй группы, у фразы из первой группы
     * ставка будет посчитана калькулятором. В данном случае, по торгам.
     */
    @Test
    public void execute_withOtherAdGroupFixedAutoPrice_auctionPrices() {
        createTwoActiveAdGroups();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        // тут важно добавить в первую группу еще одну фразу с другими ставками,
        // т.к. иначе при вычислении автоставки калькулятором сработает правило
        // "выставить общую ставку для всех старых фраз", т.е. она не поменяется,
        // что не раскрывает тему автоставок.
        createKeyword(adGroupInfo1, PHRASE_2, OLD_SEARCH_PRICE2, OLD_CONTEXT_PRICE2);
        Long otherKeywordIdToUpdate =
                createKeyword(adGroupInfo2, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate, PHRASE_3),
                keywordModelChanges(otherKeywordIdToUpdate, PHRASE_2)
        );
        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices
                        .ofPerAdGroupFixedPrices(singletonMap(adGroupInfo2.getAdGroupId(), FIXED_AUTO_PRICE));

        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_3),
                isUpdated(otherKeywordIdToUpdate, PHRASE_2)));
        assertKeywordPrices(keywordIdToUpdate, AUCTION_AUTO_PRICE, DEFAULT_PRICE);
        assertKeywordPrices(otherKeywordIdToUpdate, FIXED_AUTO_PRICE, FIXED_AUTO_PRICE);
    }

    /**
     * Если у фразы поменять текст и указать глобальную фиксированную
     * автоставку, фразе будет выставлена она.
     */
    @Test
    public void execute_withFixedAutoPrice_inAutoStrategy_pricesAreNotSet() {
        createOneActiveAdGroupAutoStrategy();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));

        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE);
        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));
        assertKeywordNoPrices(keywordIdToUpdate);
    }

    /**
     * Если фиксированных автоставок нет, и в торги сходить не получилось,
     * у фразы с измененным текстом выставляются ставки по умолчанию.
     */
    @Test
    public void execute_noFixedPrice_noAuction_defaultPrices() {
        createOneActiveAdGroupCpmBannerWithManualStrategy();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        // тут важно добавить в первую группу еще одну фразу с другими ставками,
        // т.к. иначе при вычислении автоставки калькулятором сработает правило
        // "выставить общую ставку для всех старых фраз", т.е. она не поменяется,
        // что не раскрывает тему автоставок.
        createKeyword(adGroupInfo1, PHRASE_2, OLD_SEARCH_PRICE2, OLD_CONTEXT_PRICE2).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_3));

        ShowConditionFixedAutoPrices fixedAutoPrices = ShowConditionFixedAutoPrices.ofGlobalFixedPrice(null);
        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_3)));
        assertKeywordPrices(keywordIdToUpdate, DEFAULT_PRICE, DEFAULT_PRICE);
    }

    /**
     * Если у фразы поменять текст, не указать фиксированную ставку, и
     * в группе была только одна фраза, ее ставка не поменяется.
     * В калькуляторе сработает правило "выставить общую для всех фраз в группе
     * ставку".
     */
    @Test
    public void execute_oneCommonPrice_setCommonPrice() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));

        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(null);
        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));
        assertKeywordPrices(keywordIdToUpdate, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
    }

    /**
     * Если у фразы поменять текст, не указать фиксированную ставку, и
     * в группе было две фразы с одинаковыми ставками, ставка у измененной
     * фразы не поменяется.
     */
    @Test
    public void execute_twoCommonPrices_setCommonPrice() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        createKeyword(adGroupInfo1, PHRASE_2, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_3));

        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(null);
        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_3)));
        assertKeywordPrices(keywordIdToUpdate, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE);
    }

    /**
     * Если при обновлении текста фразы в запросе была явно указана
     * поисковая ставка, она и выставляются. А в сети выставляется
     * автоматическая ставка.
     */
    @Test
    public void execute_withExplicitSearchPrice_setExplicitSearchPrice() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(
                        keywordModelChanges(keywordIdToUpdate, PHRASE_2)
                                .process(CUSTOM_SEARCH_PRICE, Keyword.PRICE)
                );

        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE);
        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));
        assertKeywordPrices(keywordIdToUpdate, CUSTOM_SEARCH_PRICE, FIXED_AUTO_PRICE);
    }

    /**
     * Если при обновлении текста фразы в запросе была явно указана
     * ставка в сети, она и выставляются. А на поиске выставляется
     * автоматическая ставка.
     */
    @Test
    public void execute_withExplicitContextPrice_setExplicitContextPrice() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(
                        keywordModelChanges(keywordIdToUpdate, PHRASE_2)
                                .process(CUSTOM_CONTEXT_PRICE, Keyword.PRICE_CONTEXT)
                );

        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(FIXED_AUTO_PRICE);
        MassResult<UpdatedKeywordInfo> result = executeWithAutoPrices(changesKeywords, fixedAutoPrices, null);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));
        assertKeywordPrices(keywordIdToUpdate, FIXED_AUTO_PRICE, CUSTOM_CONTEXT_PRICE);
    }

    /**
     * Если передать в операцию внешний калькулятор, который инициализировали
     * списком со старой фразой (которую, допустим, уже удалили из группы),
     * а в группе поменять текст существующей фразы на текст старой фразы,
     * должны выставиться ставки от старой фразы.
     */
    @Test
    public void execute_withExternalCalculatorWithSameOldPhrase_setOldPrice() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1, OLD_SEARCH_PRICE, OLD_CONTEXT_PRICE).getId();
        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));

        ShowConditionFixedAutoPrices fixedAutoPrices =
                ShowConditionFixedAutoPrices.ofGlobalFixedPrice(null);
        List<Keyword> deletedKeywords = singletonList(
                newKeywordEmptyPrices(adGroupInfo1, PHRASE_2)
                        .withPrice(OLD_SEARCH_PRICE2)
                        .withPriceContext(OLD_CONTEXT_PRICE2)
        );
        KeywordAutoPricesCalculator customCalculator = new KeywordAutoPricesCalculator(
                CurrencyRub.getInstance(), deletedKeywords, keywordNormalizer
        );
        MassResult<UpdatedKeywordInfo> result =
                executeWithAutoPrices(changesKeywords, fixedAutoPrices, customCalculator);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));
        assertKeywordPrices(keywordIdToUpdate, OLD_SEARCH_PRICE2, OLD_CONTEXT_PRICE2);
    }

    private Money moneyOf(double price) {
        return Money.valueOf(price, clientCurrency.getCode());
    }

    /**
     * Проверка, что у фразы выставились указанные ставки, а приоритет
     * автобюджета сбросился в {@code null}
     */
    private void assertKeywordPrices(long keywordId, BigDecimal searchPrice, BigDecimal contextPrice) {
        Keyword keyword = getKeyword(keywordId);
        assertThat(keyword.getPrice(), is(searchPrice));
        assertThat(keyword.getPriceContext(), is(contextPrice));
        assertThat(keyword.getAutobudgetPriority(), nullValue());
    }

    /**
     * Проверка, что у фразы выставлен приоритет автобюджета по умолчанию,
     * а ставки сброшены в {@code null}
     */
    private void assertKeywordNoPrices(long keywordId) {
        Keyword keyword = getKeyword(keywordId);
        assertThat(keyword.getPrice(), nullValue());
        assertThat(keyword.getPriceContext(), nullValue());
        assertThat(keyword.getAutobudgetPriority(), is(DEFAULT_AUTOBUDGET_PRIORITY));
    }
}
