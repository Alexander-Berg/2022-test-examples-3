package ru.yandex.direct.logicprocessor.processors.mysql2grut.steps.repository

import org.jooq.types.ULong
import org.springframework.stereotype.Repository
import ru.yandex.direct.common.util.RepositoryUtils
import ru.yandex.direct.core.entity.minuskeywordspack.MinusKeywordsPackUtils
import ru.yandex.direct.core.mysql2grut.repository.MinusPhraseRepository
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.wrapper.DslContextProvider

@Repository
class MinusPhraseTestRepository(
    private val dslContextProvider: DslContextProvider,
    private val shardHelper: ShardHelper,
) : MinusPhraseRepository(dslContextProvider) {

    fun generateMinusPhraseId(): Long {
        return shardHelper.generateMinusWordsIds(1).first()
    }

    fun createMinusPhrase(shard: Int, clientId: Long, name: String?, phrases: List<String>, isLibrary: Boolean): Long {
        val mwId = shardHelper.generateMinusWordsIds(1).first()

        val text = MinusKeywordsPackUtils.minusKeywordsToJson(phrases)
        val textHash = MinusKeywordsPackUtils.calcHash(phrases)

        if (!isLibrary && name != null) {
            throw RuntimeException("nonnull name for non-library minus phrase")
        } else if (isLibrary && name == null) {
            throw RuntimeException("null name for library minus phrase")
        }

        dslContextProvider.ppc(shard).insertInto(Tables.MINUS_WORDS)
            .columns(
                Tables.MINUS_WORDS.MW_ID,
                Tables.MINUS_WORDS.CLIENT_ID,
                Tables.MINUS_WORDS.MW_NAME,
                Tables.MINUS_WORDS.MW_TEXT,
                Tables.MINUS_WORDS.MW_HASH,
                Tables.MINUS_WORDS.IS_LIBRARY,
            )
            .values(
                mwId,
                clientId,
                name,
                text,
                ULong.valueOf(textHash),
                RepositoryUtils.booleanToLong(isLibrary),
            )
            .execute()
        return mwId
    }

    fun updateMinusPhrase(shard: Int, minusPhraseId: Long, newText: String) {
        dslContextProvider.ppc(shard)
            .update(Tables.MINUS_WORDS)
            .set(Tables.MINUS_WORDS.MW_TEXT, newText)
            .where(Tables.MINUS_WORDS.MW_ID.eq(minusPhraseId))
            .execute()
    }

    fun linkNonLibraryMinusPhrase(shard: Int, minusPhraseId: Long, adGroupId: Long) {
        dslContextProvider.ppc(shard)
            .update(Tables.PHRASES)
            .set(Tables.PHRASES.MW_ID, minusPhraseId)
            .where(Tables.PHRASES.PID.eq(adGroupId))
            .execute()
    }

    fun linkLibraryMinusPhrasesToAdGroup(shard: Int, minusPhrasesIds: List<Long>, adGroupId: Long) {
        var step = dslContextProvider.ppc(shard)
            .insertInto(Tables.ADGROUPS_MINUS_WORDS, Tables.ADGROUPS_MINUS_WORDS.MW_ID, Tables.ADGROUPS_MINUS_WORDS.PID)
        for (mwId in minusPhrasesIds) {
            step = step.values(mwId, adGroupId)
        }
        step.execute()
    }


}
