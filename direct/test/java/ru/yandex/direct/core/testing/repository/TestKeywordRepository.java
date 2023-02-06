package ru.yandex.direct.core.testing.repository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.Record1;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.util.RepositoryUtils;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.dbschema.ppc.enums.BidsStatusbssynced;
import ru.yandex.direct.dbschema.ppc.tables.Bids;
import ru.yandex.direct.dbschema.ppc.tables.records.BidsPhraseidHistoryRecord;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplierBuilder;
import ru.yandex.direct.jooqmapper.ReaderWriterBuilders;

import static ru.yandex.direct.common.jooqmapperex.ReaderWriterBuildersEx.integerProperty;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_HREF_PARAMS;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_MANUAL_PRICES;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_PHRASEID_HISTORY;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.jooqmapper.ReaderWriterBuilders.property;

@QueryWithoutIndex("Тестовый репозиторий")
public class TestKeywordRepository {

    private final DslContextProvider dslContextProvider;

    private final JooqMapperWithSupplier<Keyword> bidsMapper;

    @Autowired
    public TestKeywordRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
        bidsMapper = createBidsMapper();
    }

    /**
     * Возвращает из базы все ключевые слова данного клиента.
     *
     * @param shard    Шард
     * @param clientId ID клиента
     * @return Список ключевых слов клиента
     */
    public List<String> getClientPhrases(int shard, ClientId clientId) {
        return dslContextProvider.ppc(shard)
                .select(BIDS.PHRASE)
                .from(BIDS)
                .join(CAMPAIGNS).on(CAMPAIGNS.CID.eq(BIDS.CID))
                .where(CAMPAIGNS.CLIENT_ID.eq(clientId.asLong()))
                .fetch(Record1::value1);
    }

    /**
     * Устанавливает флаг is_suspended для фразы
     *
     * @param shard     Шард
     * @param adGroupId ID фразы
     */
    public void suspendAdGroupKeywords(int shard, Long adGroupId) {
        getDslContext(shard)
                .update(BIDS)
                .set(BIDS.IS_SUSPENDED, RepositoryUtils.booleanToLong(Boolean.TRUE))
                .where(BIDS.PID.eq(adGroupId))
                .execute();
    }

    /**
     * Возвращает список ключевых по условиям показа в таблице bids
     *
     * @param shard Шард
     * @param ids   Список ID условий показа
     * @return Список ключевых слов
     */
    public List<Keyword> getKeywordsFromBidsTable(int shard, Collection<Long> ids) {
        return getDslContext(shard)
                .select(bidsMapper.getFieldsToRead())
                .from(BIDS)
                .where(BIDS.ID.in(ids))
                .fetch()
                .map(bidsMapper::fromDb);
    }

    /**
     * Добавляет пользовательские параметры для ключевых слов в базу.
     *
     * @param shard   Шард
     * @param keyword Информация о ключевых словах с заполненными ID слова, ID кампании и HrefParam
     */
    public void addKeywordToBidsHrefParamsTable(int shard, Keyword keyword) {
        getDslContext(shard)
                .insertInto(BIDS_HREF_PARAMS)
                .set(BIDS_HREF_PARAMS.ID, keyword.getId())
                .set(BIDS_HREF_PARAMS.CID, keyword.getCampaignId())
                .set(BIDS_HREF_PARAMS.PARAM1, keyword.getHrefParam1())
                .set(BIDS_HREF_PARAMS.PARAM1, keyword.getHrefParam2())
                .execute();
    }

    /**
     * Добавляет ключевые слова в таблицу bids_phraseid_history.
     * Таблица используется для хранения временных данных при копировании ключевых слов
     *
     * @param shard         Шард
     * @param keyword       Информация о ключевых словах, включая ID фразы и ID кампании
     * @param localDateTime Дата и время обновления/добавления записи
     */
    public void addKeywordToBidsPhraseHistoryTable(int shard, Keyword keyword, LocalDateTime localDateTime) {
        String serializedHistory =
                keyword.getPhraseIdHistory() == null ? null : keyword.getPhraseIdHistory().serialize();
        InsertSetMoreStep<BidsPhraseidHistoryRecord> insertStep = getDslContext(shard)
                .insertInto(BIDS_PHRASEID_HISTORY)
                .set(BIDS_PHRASEID_HISTORY.ID, keyword.getId())
                .set(BIDS_PHRASEID_HISTORY.CID, keyword.getCampaignId())
                .set(BIDS_PHRASEID_HISTORY.PHRASE_ID_HISTORY, serializedHistory);
        if (localDateTime != null) {
            insertStep.set(BIDS_PHRASEID_HISTORY.UPDATE_TIME, localDateTime);
        }
        insertStep.execute();
    }

    /**
     * Добавляет ключевые слова в таблицу bids_phraseid_history.
     * Таблица используется для хранения временных данных при копировании ключевых слов
     *
     * @param shard   Шард
     * @param keyword Информация о ключевых словах, включая ID фразы и ID кампании
     */
    public void addKeywordToBidsPhraseHistoryTable(int shard, Keyword keyword) {
        addKeywordToBidsPhraseHistoryTable(shard, keyword, null);
    }

    /**
     * Возвращает список тех ID ключевых фраз, которых из данного списка имеются в таблице bids
     *
     * @param shard Шард
     * @param ids   Начальных список ID
     * @return список тех ID, которые есть в таблице bids
     */
    public Set<Long> getKeywordIdFromBidTable(int shard, Collection<Long> ids) {
        return getDslContext(shard)
                .select(BIDS.ID)
                .from(BIDS)
                .where(BIDS.ID.in(ids))
                .fetchSet(BIDS.ID);
    }

    /**
     * Возвращает список тех ID ключевых фраз, которых из данного списка имеются в таблице bids_href_params
     *
     * @param shard Шард
     * @param ids   Начальных список ID
     * @return список тех ID, которые есть в таблице bids_href_params
     */
    public Set<Long> getKeywordIdFromBidsHrefParamsTable(int shard, Collection<Long> ids) {
        return getDslContext(shard)
                .select(BIDS_HREF_PARAMS.ID)
                .from(BIDS_HREF_PARAMS)
                .where(BIDS_HREF_PARAMS.ID.in(ids))
                .fetchSet(BIDS_HREF_PARAMS.ID);
    }

    /**
     * Возвращает список тех ID ключевых фраз, которых из данного списка имеются в таблице bids_manual_prices
     *
     * @param shard Шард
     * @param ids   Начальных список ID
     * @return список тех ID, которые есть в таблице bids_manual_prices
     */
    public Set<Long> getKeywordIdFromBidsManualPricesTable(int shard, Collection<Long> ids) {
        return getDslContext(shard)
                .select(BIDS_MANUAL_PRICES.ID)
                .from(BIDS_MANUAL_PRICES)
                .where(BIDS_MANUAL_PRICES.ID.in(ids))
                .fetchSet(BIDS_MANUAL_PRICES.ID);
    }

    /**
     * Возвращает список тех ID ключевых фраз, которых из данного списка имеются в таблице bids_phraseid_history
     *
     * @param shard Шард
     * @param ids   Начальных список ID
     * @return список тех ID, которые есть в таблице bids_phraseid_history
     */
    public Set<Long> getKeywordIdFromBidsPhraseIdHistoryTable(int shard, Collection<Long> ids) {
        return getDslContext(shard)
                .select(BIDS_PHRASEID_HISTORY.ID)
                .from(BIDS_PHRASEID_HISTORY)
                .where(BIDS_PHRASEID_HISTORY.ID.in(ids))
                .fetchSet(BIDS_PHRASEID_HISTORY.ID);
    }

    /**
     * Обновить ID фразы для БК по ее ID в таблице bids
     *
     * @param shard       Шард
     * @param keywordInfo Информация о ключевой фразе, включая ее ID
     * @param phraseId    Новый ID баннера для БК
     */
    public void updatePhraseId(int shard, KeywordInfo keywordInfo, BigInteger phraseId) {
        dslContextProvider.ppc(shard)
                .update(BIDS)
                .set(BIDS.PHRASE_ID, ULong.valueOf(phraseId))
                .where(BIDS.ID.eq(keywordInfo.getId()))
                .execute();
    }

    private JooqMapperWithSupplier<Keyword> createBidsMapper() {
        return JooqMapperWithSupplierBuilder.builder(Keyword::new)
                .map(property(Keyword.ID, Bids.BIDS.ID))
                .map(property(Keyword.CAMPAIGN_ID, Bids.BIDS.CID))
                .map(property(Keyword.AD_GROUP_ID, Bids.BIDS.PID))
                .map(integerProperty(Keyword.AUTOBUDGET_PRIORITY, Bids.BIDS.AUTOBUDGET_PRIORITY))
                .map(property(Keyword.PRICE, Bids.BIDS.PRICE))
                .map(property(Keyword.PRICE_CONTEXT, Bids.BIDS.PRICE_CONTEXT))
                .map(property(Keyword.MODIFICATION_TIME, Bids.BIDS.MODTIME))
                .map(ReaderWriterBuilders
                        .convertibleProperty(Keyword.STATUS_BS_SYNCED, Bids.BIDS.STATUS_BS_SYNCED,
                                TestKeywordRepository::statusBsSyncedFromDbFormat,
                                TestKeywordRepository::statusBsSyncedToDbFormat))
                .build();
    }

    private DSLContext getDslContext(int shard) {
        return dslContextProvider.ppc(shard);
    }

    private static StatusBsSynced statusBsSyncedFromDbFormat(BidsStatusbssynced bidsStatusbssynced) {
        return bidsStatusbssynced != null ? StatusBsSynced.valueOfDbFormat(bidsStatusbssynced.toString()) : null;
    }

    public static BidsStatusbssynced statusBsSyncedToDbFormat(StatusBsSynced statusBsSynced) {
        return statusBsSynced != null ? BidsStatusbssynced.valueOf(statusBsSynced.toDbFormat()) : null;
    }

    public void setStatusModerate(int shard, Set<Long> keywordIds, StatusModerate statusModerate) {
        dslContextProvider.ppc(shard)
                .update(BIDS)
                .set(BIDS.STATUS_MODERATE, StatusModerate.toSource(statusModerate))
                .where(BIDS.ID.in(keywordIds))
                .execute();
    }

    public void suspendKeywords(int shard, Set<Long> keywordIds) {
        dslContextProvider.ppc(shard)
                .update(BIDS)
                .set(BIDS.IS_SUSPENDED, RepositoryUtils.booleanToLong(Boolean.TRUE))
                .where(BIDS.ID.in(keywordIds))
                .execute();
    }
}
