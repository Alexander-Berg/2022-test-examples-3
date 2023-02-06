package ru.yandex.direct.oneshot.oneshots.campaign

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito
import ru.yandex.direct.core.entity.keyword.model.Keyword
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository

@RunWith(Parameterized::class)
class RestoreCampaignMinusKeywordsOneshotFilterMinusKeywordsTest(
    private val phrases: List<String>,
    private val norm_phrases: List<String>,
    private val minusKeywords: Set<String>,
    private val expectedMinusKeywords: List<String>,
) {
    private val keywordRepository: KeywordRepository = mock()
    private var oneshot: RestoreCampaignMinusKeywordsOneshot = RestoreCampaignMinusKeywordsOneshot(
        mock(),
        mock(),
        mock(),
        keywordRepository,
        mock(),
        mock(),
        mock(),
        mock(),
        1
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(listOf("кони"), listOf("конь"), setOf("корова"), listOf("корова")),
            arrayOf(listOf(), listOf(), setOf("корова"), listOf("корова")),
            arrayOf(listOf("кони"), listOf("конь"), setOf("конь"), listOf()),
            arrayOf(listOf("кони"), listOf("конь"), setOf("кони"), listOf()),
            arrayOf(listOf("кони", "гуси"), listOf("конь", "гусь"), setOf("конь", "корова", "лошадь"),
                listOf("корова", "лошадь")),
        )
    }

    @Test
    fun test_wwwDomain_replace_domain_from_db() {
        val cid = 40L
        val shard = 1
        val keywords = (phrases zip norm_phrases).map {
            Keyword()
                .withPhrase(it.first)
                .withNormPhrase(it.second)
        }
        Mockito.`when`(keywordRepository.getKeywordsByCampaignId(shard, cid)).thenReturn(keywords)
        val result = oneshot.filterMinusKeywords(cid, minusKeywords, shard)
        Assertions.assertThat(result).containsExactlyInAnyOrderElementsOf(expectedMinusKeywords)
    }
}
