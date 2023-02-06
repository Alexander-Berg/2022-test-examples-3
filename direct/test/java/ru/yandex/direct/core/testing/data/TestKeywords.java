package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.bshistory.History;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.Place;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;

import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

public final class TestKeywords {

    public static final BigDecimal DEFAULT_PRICE_SEARCH = BigDecimal.ONE;
    public static final BigDecimal DEFAULT_PRICE_CONTEXT = BigDecimal.ONE;

    private TestKeywords() {
    }

    public static Keyword defaultClientKeyword() {
        return defaultClientKeyword(null, null);
    }

    public static Keyword defaultClientKeyword(Long adGroupId) {
        return defaultClientKeyword(null, adGroupId);
    }

    public static Keyword defaultClientKeyword(Long id, Long adGroupId) {
        String randomNumber = RandomStringUtils.randomNumeric(5);
        return new Keyword()
                .withId(id)
                .withAdGroupId(adGroupId)
                .withPhrase("продать трактор ТР-ТР " + randomNumber)
                .withIsAutotargeting(false)
                .withPrice(BigDecimal.ONE)
                .withPriceContext(BigDecimal.ONE)
                .withAutobudgetPriority(3);
    }

    public static Keyword defaultKeyword() {
        String randomNumber = RandomStringUtils.randomNumeric(5);
        return new Keyword()
                .withId(null)
                .withPhrase("продать трактор ТР-ТР " + randomNumber)
                .withIsAutotargeting(false)
                .withNormPhrase(randomNumber + " продавать тр трактор")
                .withWordsCount(4)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusModerate(StatusModerate.NEW)
                .withPrice(DEFAULT_PRICE_SEARCH)
                .withPriceContext(DEFAULT_PRICE_CONTEXT)
                .withAutobudgetPriority(3)
                .withPhraseBsId(BigInteger.ZERO)
                .withShowsForecast(RandomUtils.nextLong(1, 1000))
                .withModificationTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .withIsSuspended(Boolean.FALSE)
                .withNeedCheckPlaceModified(Boolean.FALSE);
    }

    public static Keyword keywordWithText(String text) {
        return defaultKeyword()
                .withPhrase(text)
                .withNormPhrase(text)
                .withWordsCount(ifNotNull(text, txt -> txt.split("\\s").length));
    }

    public static Keyword fullKeyword() {
        return defaultKeyword()
                .withHrefParam1("href_val_01")
                .withHrefParam2("href_val_02")
                .withPlace(Place.FIRST)
                .withIsSuspended(true)
                .withPhraseIdHistory(History.parse("O1;G1"))
                .withNeedCheckPlaceModified(true);
    }

    public static Keyword keywordForCpmBanner() {
        return defaultKeyword()
                .withPrice(BigDecimal.TEN)
                .withPriceContext(BigDecimal.TEN);
    }

    public static Keyword createKeyword(BigDecimal price, BigDecimal priceContext, Integer autobudgetPriority) {
        return TestKeywords.defaultKeyword()
                .withPrice(price)
                .withPriceContext(priceContext)
                .withAutobudgetPriority(autobudgetPriority);
    }

    public static Keyword keywordForContentPromotionVideo() {
        return defaultKeyword();
    }
}
