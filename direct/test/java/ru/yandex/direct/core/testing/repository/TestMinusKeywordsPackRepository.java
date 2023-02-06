package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.List;

import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.minuskeywordspack.MinusKeywordsPackUtils;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplierBuilder;

import static ru.yandex.direct.common.jooqmapperex.ReaderWriterBuildersEx.booleanProperty;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_MINUS_WORDS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_MINUS_WORDS;
import static ru.yandex.direct.dbschema.ppc.Tables.MINUS_WORDS;
import static ru.yandex.direct.jooqmapper.ReaderWriterBuilders.convertibleProperty;
import static ru.yandex.direct.jooqmapper.ReaderWriterBuilders.property;

public class TestMinusKeywordsPackRepository {
    private DslContextProvider dslContextProvider;

    private static final JooqMapperWithSupplier<MinusKeywordsPack> MINUS_WORDS_MAPPER =
            JooqMapperWithSupplierBuilder.builder(MinusKeywordsPack::new)
                    .map(property(MinusKeywordsPack.ID, MINUS_WORDS.MW_ID))
                    .map(property(MinusKeywordsPack.NAME, MINUS_WORDS.MW_NAME))
                    .map(convertibleProperty(MinusKeywordsPack.MINUS_KEYWORDS, MINUS_WORDS.MW_TEXT,
                            MinusKeywordsPackUtils::minusKeywordsFromJson, MinusKeywordsPackUtils::minusKeywordsToJson))
                    .map(convertibleProperty(MinusKeywordsPack.HASH, MINUS_WORDS.MW_HASH, ULong::toBigInteger,
                            ULong::valueOf))
                    .map(booleanProperty(MinusKeywordsPack.IS_LIBRARY, MINUS_WORDS.IS_LIBRARY))
                    .build();

    @Autowired
    public TestMinusKeywordsPackRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    public List<MinusKeywordsPack> get(int shard, Collection<Long> ids) {
        return dslContextProvider.ppc(shard)
                .select(MINUS_WORDS_MAPPER.getFieldsToRead())
                .from(MINUS_WORDS)
                .where(MINUS_WORDS.MW_ID.in(ids))
                .fetch()
                .map(MINUS_WORDS_MAPPER::fromDb);
    }

    public List<Long> getExistingPackIds(int shard, Collection<Long> ids) {
        return dslContextProvider.ppc(shard)
                .select(MINUS_WORDS.MW_ID)
                .from(MINUS_WORDS)
                .where(MINUS_WORDS.MW_ID.in(ids))
                .fetch(MINUS_WORDS.MW_ID);
    }

    public List<MinusKeywordsPack> getClientPacks(int shard, ClientId clientId) {
        return dslContextProvider.ppc(shard)
                .select(MINUS_WORDS_MAPPER.getFieldsToRead())
                .from(MINUS_WORDS)
                .where(MINUS_WORDS.CLIENT_ID.eq(clientId.asLong()))
                .fetch()
                .map(MINUS_WORDS_MAPPER::fromDb);
    }

    public void linkLibraryMinusKeywordPackToAdGroup(int shard, Long minusKeywordPackId, Long adGroupId) {
        dslContextProvider.ppc(shard)
                .insertInto(ADGROUPS_MINUS_WORDS)
                .set(ADGROUPS_MINUS_WORDS.MW_ID, minusKeywordPackId)
                .set(ADGROUPS_MINUS_WORDS.PID, adGroupId)
                .execute();
    }

    public void linkLibraryMinusKeywordPackToCampaign(int shard, Long minusKeywordPackId, Long campaignId) {
        dslContextProvider.ppc(shard)
                .insertInto(CAMPAIGNS_MINUS_WORDS)
                .set(CAMPAIGNS_MINUS_WORDS.MW_ID, minusKeywordPackId)
                .set(CAMPAIGNS_MINUS_WORDS.CID, campaignId)
                .execute();
    }

    public void deleteMinusKeywordPack(int shard, Long id) {
        dslContextProvider.ppc(shard)
                .deleteFrom(MINUS_WORDS)
                .where(MINUS_WORDS.MW_ID.eq(id))
                .execute();
    }
}
