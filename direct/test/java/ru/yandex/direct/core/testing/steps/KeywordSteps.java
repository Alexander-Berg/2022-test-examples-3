package ru.yandex.direct.core.testing.steps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.common.collect.ImmutableSet;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordWithText;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS;

public class KeywordSteps {

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private AdGroupSteps adGroupSteps;

    public KeywordInfo createDefaultKeyword() {
        return createKeyword(new KeywordInfo());
    }

    public KeywordInfo createDefaultKeywordWithText(String text) {
        return createKeyword(new KeywordInfo()
                .withKeyword(keywordWithText(text)));
    }

    public KeywordInfo createKeywordWithText(String text, AdGroupInfo adGroupInfo) {
        if (text == null) {
            return createKeyword(adGroupInfo);
        }
        if (adGroupInfo == null) {
            createDefaultKeywordWithText(text);
        }

        return createKeyword(new KeywordInfo()
                .withKeyword(keywordWithText(text))
                .withAdGroupInfo(adGroupInfo));
    }

    public KeywordInfo createKeyword(AdGroupInfo adGroupInfo) {
        return createKeyword(adGroupInfo, null);
    }

    public KeywordInfo createModifiedKeyword(
            AdGroupInfo adGroupInfo,
            Function<Keyword, Keyword> defaultKeywordModificator) {
        return createKeyword(adGroupInfo, defaultKeywordModificator.apply(defaultKeyword()));
    }

    public KeywordInfo createKeyword(Keyword keyword) {
        return createKeyword(new KeywordInfo()
                .withKeyword(keyword));
    }

    public KeywordInfo createKeyword(AdGroupInfo adGroupInfo, Keyword keyword) {
        return createKeyword(new KeywordInfo()
                .withKeyword(keyword)
                .withAdGroupInfo(adGroupInfo));
    }

    private KeywordInfo createKeyword(KeywordInfo keywordInfo) {
        if (keywordInfo.getKeyword() == null) {
            keywordInfo.withKeyword(defaultKeyword());
        }
        if (keywordInfo.getId() == null) {
            adGroupSteps.createAdGroup(keywordInfo.getAdGroupInfo());
            keywordInfo.getKeyword()
                    .withAdGroupId(keywordInfo.getAdGroupInfo().getAdGroupId())
                    .withCampaignId(keywordInfo.getAdGroupInfo().getCampaignInfo().getCampaignId());
            ensureValidPrices(keywordInfo);

            DSLContext dslContext = dslContextProvider.ppc(keywordInfo.getShard());
            keywordRepository.addKeywords(dslContext.configuration(), singletonList(keywordInfo.getKeyword()));
        }

        return keywordInfo;
    }

    public KeywordInfo addKeyword(AdGroupInfo existedAdGroupInfo, Keyword keyword) {
        return addKeyword(new KeywordInfo()
                .withKeyword(keyword)
                .withAdGroupInfo(existedAdGroupInfo));
    }

    private KeywordInfo addKeyword(KeywordInfo keywordInfo) {
        if (keywordInfo.getKeyword() == null) {
            keywordInfo.withKeyword(defaultKeyword());
        }
        if (keywordInfo.getId() == null) {
            keywordInfo.getKeyword()
                    .withAdGroupId(keywordInfo.getAdGroupInfo().getAdGroupId())
                    .withCampaignId(keywordInfo.getAdGroupInfo().getCampaignInfo().getCampaignId());

            DSLContext dslContext = dslContextProvider.ppc(keywordInfo.getShard());
            keywordRepository.addKeywords(dslContext.configuration(), singletonList(keywordInfo.getKeyword()));
        }

        return keywordInfo;
    }
    /**
     * Добавляет несколько ключевых фраз одним запросом в БД.
     * В качестве шарда используются шард, определённый в первой по списку ключевой фразе.
     */
    public List<KeywordInfo> createKeywords(List<KeywordInfo> keywordInfos) {
        if (keywordInfos.isEmpty()) {
            return keywordInfos;
        }

        List<Keyword> keywords = new ArrayList<>(keywordInfos.size());
        for (KeywordInfo keywordInfo : keywordInfos) {
            if (keywordInfo.getKeyword() == null) {
                keywordInfo.withKeyword(defaultKeyword());
            }
            if (keywordInfo.getId() == null) {
                keywords.add(keywordInfo.getKeyword()
                        .withAdGroupId(keywordInfo.getAdGroupInfo().getAdGroupId())
                        .withCampaignId(keywordInfo.getAdGroupInfo().getCampaignInfo().getCampaignId()));
            }
        }
        Integer shard = keywordInfos.get(0).getShard();
        keywordRepository.addKeywords(dslContextProvider.ppc(shard).configuration(), keywords);
        return keywordInfos;
    }

    public <V> void updateKeywordProperty(KeywordInfo keywordInfo, ModelProperty<? super Keyword, V> property,
                                          V value) {
        Keyword keywordToChange = keywordInfo.getKeyword();

        AppliedChanges<Keyword> appliedChanges = new ModelChanges<>(keywordInfo.getId(), Keyword.class)
                .process(value, property)
                .applyTo(keywordToChange);

        keywordRepository.update(keywordInfo.getShard(), ImmutableSet.of(appliedChanges));
    }

    public <V> void updateKeywordsProperty(List<KeywordInfo> keywordInfos, ModelProperty<? super Keyword, V> property,
                                           V value) {
        List<AppliedChanges<Keyword>> appliedChanges = new ArrayList<>();
        StreamEx.of(keywordInfos)
                .forEach(kwInfo -> appliedChanges.add(new ModelChanges<>(kwInfo.getId(), Keyword.class)
                        .process(value, property)
                        .applyTo(kwInfo.getKeyword())));

        int shard = keywordInfos.stream().findFirst().get().getShard();
        keywordRepository.update(shard, appliedChanges);
    }

    /**
     * проставить КФ валидные (минимальные) цены, если того требует ручная стратегия
     */
    private void ensureValidPrices(KeywordInfo keywordInfo) {
        Keyword keyword = keywordInfo.getKeyword();
        CurrencyCode clientCurrencyCode = keywordInfo.getAdGroupInfo().getClientInfo().getClient().getWorkCurrency();
        Boolean isCpm = keywordInfo.getAdGroupInfo().getAdGroupType().name().toLowerCase().contains("cpm");
        BigDecimal minPrice = isCpm
                ? clientCurrencyCode.getCurrency().getMinCpmPrice()
                : clientCurrencyCode.getCurrency().getMinPrice();
        // не определяем, какие ставки на самом деле нужны, т.к. в model0 это как-то сложно
        // в принципе, можно выставить сразу две, в базе тоже так бывает.
        if (keywordInfo.getAdGroupInfo().getCampaignInfo().getCampaign().getStrategy().isManual()) {
            if (ObjectUtils.compare(minPrice, keyword.getPrice()) > 0) {
                keyword.withPrice(minPrice);
            }
            if (ObjectUtils.compare(minPrice, keyword.getPriceContext()) > 0) {
                keyword.withPriceContext(minPrice);
            }
        } else {
            if (keyword.getAutobudgetPriority() == null) {
                keyword.withAutobudgetPriority(3);
            }
        }
    }

    public void resetKeywordsContextPrices(int shard, List<Long> keywordIds) {
        dslContextProvider.ppc(shard)
                .update(BIDS)
                .set(BIDS.PRICE_CONTEXT, BigDecimal.ZERO)
                .where(BIDS.ID.in(keywordIds))
                .execute();
    }

    public void resetKeywordsSearchPrices(int shard, List<Long> keywordIds) {
        dslContextProvider.ppc(shard)
                .update(BIDS)
                .set(BIDS.PRICE, BigDecimal.ZERO)
                .where(BIDS.ID.in(keywordIds))
                .execute();
    }

    public List<Keyword> getKeywordsByAdGroupId(int shard, long adGroupId) {
        return keywordRepository.getKeywordsByAdGroupId(shard, adGroupId);
    }
}
