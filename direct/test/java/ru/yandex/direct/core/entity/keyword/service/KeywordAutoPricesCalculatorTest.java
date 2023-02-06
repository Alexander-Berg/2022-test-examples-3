package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;

import ru.yandex.direct.core.entity.auction.container.bs.Block;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordBidBsAuctionData;
import ru.yandex.direct.core.entity.auction.container.bs.Position;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.keyword.container.InternalKeyword;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.libs.keywordutils.model.KeywordWithMinuses;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.libs.keywordutils.parser.KeywordParser.parseWithMinuses;

public class KeywordAutoPricesCalculatorTest {
    private final long adGroupIdUnderTest = 1L;
    private final long otherAdGroupId = 2L;
    private final String phraseUnderTest = "   test keyword";
    private final Currency currency = CurrencyRub.getInstance();
    private final BigDecimal defaultPrice = currency.getDefaultPrice();
    private final CampaignType campaignType = CampaignType.TEXT;
    private final KeywordBidBsAuctionData auctionData = new KeywordBidBsAuctionData()
            .withPremium(new Block(asList(
                    new Position(Money.valueOf(80.0, currency.getCode()), Money.valueOf(85.0, currency.getCode())),
                    new Position(Money.valueOf(50.0, currency.getCode()), Money.valueOf(55.0, currency.getCode()))
            )))
            .withGuarantee(new Block(asList(
                    new Position(Money.valueOf(40.0, currency.getCode()), Money.valueOf(45.0, currency.getCode())),
                    new Position(Money.valueOf(10.0, currency.getCode()), Money.valueOf(15.0, currency.getCode()))
            )));
    // bidPrice первого места Гарантии + 30% = 45.0 * 1.3 = 58.5
    private final BigDecimal expectedAuctionPrice = BigDecimal.valueOf(58.5);

    private KeywordNormalizer keywordNormalizer;
    private InternalKeyword internalKeywordUnderTest;

    private KeywordAutoPricesCalculator calculator;

    @Before
    public void setUp() {
        keywordNormalizer = mock(KeywordNormalizer.class);
        // местный нормализатор гарантированно отдает нормальную форму отличающуюся от текста фразы
        when(keywordNormalizer.normalizeKeywordWithMinuses(ArgumentMatchers.any()))
                .thenAnswer((Answer<KeywordWithMinuses>) invocation ->
                        parseWithMinuses("normal " + invocation.getArgument(0).toString())
                );

        Keyword keywordUnderTest = makeKeyword(adGroupIdUnderTest).withPhrase(phraseUnderTest);
        KeywordWithMinuses keywordWithMinuses = parseWithMinuses(keywordUnderTest.getPhrase());
        KeywordWithMinuses normalPhraseUnderTest = keywordNormalizer.normalizeKeywordWithMinuses(keywordWithMinuses);
        internalKeywordUnderTest = new InternalKeyword(keywordUnderTest, keywordWithMinuses, normalPhraseUnderTest);
    }

    @Test
    public void noOldKeywordsNoAuctions_minimalPrices() {
        makeCalculator(Collections.emptyList());

        assertThat("Ожидалась ставка по умолчанию", calcSearchAutoPriceNoAuction(), is(defaultPrice));
        assertThat("Ожидалась ставка по умолчанию", calcContextAutoPrice(), is(defaultPrice));
    }

    @Test
    public void noOldKeywordsWithAuction_priceFromAuction() {
        makeCalculator(Collections.emptyList());

        assertThat("Ожидалась ставка по результатам торгов", calcSearchAutoPrice(), is(expectedAuctionPrice));
    }

    @Test
    public void oldKeywordsFromDifferentGroup_priceFromAuction() {
        makeCalculator(Collections.singletonList(makeKeyword(otherAdGroupId)));

        assertThat("Ожидалась ставка по результатам торгов", calcSearchAutoPrice(), is(expectedAuctionPrice));
        assertThat("Ожидалась ставка по умолчанию", calcContextAutoPrice(), is(defaultPrice));
    }

    @Test
    public void oneOldKeywords_samePrice() {
        Keyword oldKeyword = makeKeyword(adGroupIdUnderTest);
        Keyword oldKeywordOtherGroup = makeKeyword(otherAdGroupId);

        makeCalculator(asList(oldKeyword, oldKeywordOtherGroup));

        assertThat("Ожидалась ставка от старой фразы", calcSearchAutoPrice(), is(oldKeyword.getPrice()));
        assertThat("Ожидалась ставка от старой фразы", calcContextAutoPrice(), is(oldKeyword.getPriceContext()));
    }

    @Test
    public void twoOldKeywordsDifferentPrices_priceFromAuction() {
        Keyword oldKeyword1 = makeKeyword(adGroupIdUnderTest);
        Keyword oldKeyword2 = makeKeyword(adGroupIdUnderTest);

        makeCalculator(asList(oldKeyword1, oldKeyword2));

        assertThat("Ожидалась ставка по результатам торгов", calcSearchAutoPrice(), is(expectedAuctionPrice));
        assertThat("Ожидалась ставка по умолчанию", calcContextAutoPrice(), is(defaultPrice));
    }

    @Test
    public void twoOldKeywordsSamePrices_samePrices() {
        Keyword oldKeyword1 = makeKeyword(adGroupIdUnderTest);
        Keyword oldKeyword2 = makeKeyword(adGroupIdUnderTest)
                .withPrice(oldKeyword1.getPrice())
                .withPriceContext(oldKeyword1.getPriceContext());

        makeCalculator(asList(oldKeyword1, oldKeyword2));

        assertThat("Ожидалась ставка от старой фразы", calcSearchAutoPrice(), is(oldKeyword1.getPrice()));
        assertThat("Ожидалась ставка от старой фразы", calcContextAutoPrice(), is(oldKeyword1.getPriceContext()));
    }

    @Test
    public void twoOldKeywordsSameSearchPriceDifferentContextPrice_sameSearchPrice() {
        Keyword oldKeyword1 = makeKeyword(adGroupIdUnderTest);
        Keyword oldKeyword2 = makeKeyword(adGroupIdUnderTest)
                .withPrice(oldKeyword1.getPrice());

        makeCalculator(asList(oldKeyword1, oldKeyword2));

        assertThat("Ожидалась ставка от старой фразы", calcSearchAutoPrice(), is(oldKeyword1.getPrice()));
        assertThat("Ожидалась ставка по умолчанию", calcContextAutoPrice(), is(defaultPrice));
    }

    @Test
    public void twoOldKeywordsSameContextPriceDifferentContextPrice_sameContextPrice() {
        Keyword oldKeyword1 = makeKeyword(adGroupIdUnderTest);
        Keyword oldKeyword2 = makeKeyword(adGroupIdUnderTest)
                .withPriceContext(oldKeyword1.getPriceContext());

        makeCalculator(asList(oldKeyword1, oldKeyword2));

        assertThat("Ожидалась ставка от старой фразы", calcSearchAutoPrice(), is(expectedAuctionPrice));
        assertThat("Ожидалась ставка по умолчанию", calcContextAutoPrice(), is(oldKeyword1.getPriceContext()));
    }

    /**
     * В двух группах есть старые фразы с одинаковыми фразами,
     * ставки должны быть скопированы с фразы в тестируемой группе.
     * Тут также проверяется, что калькулятор пересчитывает нормальную
     * форму у фразы, т.к. {@link #makeKeyword(long)} создает треш в поле
     * с нормальной формой в модели.
     */
    @Test
    public void oldKeywordSamePhrase_samePrices() {
        Keyword oldKeyword = makeKeyword(adGroupIdUnderTest)
                .withPhrase(phraseUnderTest);
        Keyword oldKeywordOtherGroup = makeKeyword(otherAdGroupId)
                .withPhrase(phraseUnderTest);

        makeCalculator(asList(oldKeyword, oldKeywordOtherGroup));

        assertThat("Ожидалась ставка от старой фразы", calcSearchAutoPrice(), is(oldKeyword.getPrice()));
        assertThat("Ожидалась ставка от старой фразы", calcContextAutoPrice(), is(oldKeyword.getPriceContext()));
    }

    /**
     * Здесь проверяется, что калькулятор ищет старую фразу не по
     * тексту фразы, а по нормальной форме.
     * Это достигается тем, что к тексту фразы приписываются пробелы,
     * которые отрезаются в {@link KeywordUtils#safeParseWithMinuses}.
     */
    @Test
    public void oldKeywordSameNormalPhrase_samePrices() {
        Keyword oldKeyword = makeKeyword(adGroupIdUnderTest)
                .withPhrase(phraseUnderTest + " ");
        Keyword oldKeywordOtherGroup = makeKeyword(otherAdGroupId)
                .withPhrase(phraseUnderTest + " ");

        makeCalculator(asList(oldKeyword, oldKeywordOtherGroup));

        assertThat("Ожидалась ставка от старой фразы", calcSearchAutoPrice(), is(oldKeyword.getPrice()));
        assertThat("Ожидалась ставка от старой фразы", calcContextAutoPrice(), is(oldKeyword.getPriceContext()));
    }

    private void makeCalculator(List<Keyword> oldKeywords) {
        calculator = new KeywordAutoPricesCalculator(
                currency,
                oldKeywords,
                keywordNormalizer
        );
    }

    /**
     * вызов без результата торгов
     */
    private BigDecimal calcSearchAutoPriceNoAuction() {
        return calculator.calcSearchAutoPrice(internalKeywordUnderTest, campaignType, null).bigDecimalValue();
    }

    /**
     * вызов с результатами торгов
     */
    private BigDecimal calcSearchAutoPrice() {
        return calculator.calcSearchAutoPrice(internalKeywordUnderTest, campaignType, auctionData).bigDecimalValue();
    }

    private BigDecimal calcContextAutoPrice() {
        return calculator.calcContextAutoPrice(internalKeywordUnderTest).bigDecimalValue();
    }

    private int increment = 0;

    /**
     * делает фразу в группе {@code adGroupId}.
     * Все фразы отличаются друг от друга текстом и ставками.
     * У всех фраз поисковая ставка отличается от ставки в сети.
     * У всех фраз выставлена кривая нормальная форма ({@link #keywordNormalizer} выдаст другую)
     * Все фразы должны отличаться от {@link #internalKeywordUnderTest}
     */
    private Keyword makeKeyword(long adGroupId) {
        Keyword result = new Keyword()
                .withPhrase("some keyword " + increment)
                .withAdGroupId(adGroupId)
                .withId(2L + increment)
                .withNormPhrase("db some keyword " + increment)
                .withCampaignId(3L)
                .withPrice(BigDecimal.valueOf(10 + increment))
                .withPriceContext(BigDecimal.valueOf(20 + increment));
        ++increment;
        return result;
    }
}
