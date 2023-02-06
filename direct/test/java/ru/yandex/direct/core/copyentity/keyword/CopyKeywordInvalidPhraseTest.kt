package ru.yandex.direct.core.copyentity.keyword

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.copyentity.adgroup.BaseCopyAdGroupTest
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.entity.keyword.model.Keyword
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestKeywords.keywordWithText
import ru.yandex.direct.core.testing.info.KeywordInfo

@CoreTest
class CopyKeywordInvalidPhraseTest : BaseCopyAdGroupTest() {

    @Autowired
    private lateinit var copyAssert: CopyAssert

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun `copy unglued phrase`() {
        val adGroup = steps.adGroupSteps().createActiveTextAdGroup(client)
        val texts = listOf(
            "живи +в рыбацком жилой комплекс",
            "комплекс живи +в рыбацком -жить",
        )
        val keywords = steps.keywordSteps().createKeywords(texts.map { text ->
            KeywordInfo()
                .withAdGroupInfo(adGroup)
                .withKeyword(keywordWithText(text))
        })

        val result = sameCampaignAdGroupCopyOperation(adGroup).copy()

        val keywordIds = keywords.map { it.id }
        val copiedKeywords: List<Keyword> = copyAssert.getCopiedEntities(Keyword::class.java, keywordIds, result)
        val copiedPhrases: List<String> = copiedKeywords.map(Keyword::getPhrase)
        assertThat(copiedPhrases)
            .containsExactlyElementsOf(texts)
    }

    @Test
    fun `copy phrase with too many words`() {
        val adGroup = steps.adGroupSteps().createActiveTextAdGroup(client)
        val text = "Соответствие охраны труда ФСС.004.0.07-2021"
        val keyword = steps.keywordSteps().createKeywordWithText(text, adGroup)

        val result = sameCampaignAdGroupCopyOperation(adGroup).copy()

        val copiedKeyword: Keyword = copyAssert.getCopiedEntity(keyword.id, result)
        assertThat(copiedKeyword.phrase)
            .isEqualTo(text)
    }
}
