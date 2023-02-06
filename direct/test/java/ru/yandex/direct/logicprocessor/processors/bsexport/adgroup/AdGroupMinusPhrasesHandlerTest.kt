package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup


import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers.anyString
import ru.yandex.direct.core.bsexport.repository.adgroup.resources.BsExportAdgroupMinusPhrasesRepository
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer
import ru.yandex.direct.core.entity.stopword.service.StopWordService
import ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler.minusphrases.AdGroupMinusPhrasesInfo
import ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler.minusphrases.MinusPhrasesBsPrepareService

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdGroupMinusPhrasesHandlerTest {
    private lateinit var minusPhrasesBsPrepareService: MinusPhrasesBsPrepareService
    private lateinit var stopWordService: StopWordService
    private lateinit var keywordNormalizer: KeywordNormalizer
    private lateinit var minusPhrasesRepository: BsExportAdgroupMinusPhrasesRepository

    @BeforeEach
    fun before() {
        keywordNormalizer = mock()
        stopWordService = mock()
        minusPhrasesRepository = mock()
        minusPhrasesBsPrepareService = MinusPhrasesBsPrepareService(keywordNormalizer, stopWordService, minusPhrasesRepository)
    }

    @Suppress("UNUSED")
    fun simpleMinusPhrases() = listOf(
        "кредит" to "кредит",
        "-кредит" to "кредит",
        "-" to "",
        "" to "",
        "- питер" to "питер",
        "!ананас" to "!ананас",
        "+апельсин" to "+апельсин",
        "'%^(&*<%&,\$.@>^^%/\$|#}@{@^\$+*'%&=(%&)*^ ~@`&^ ^&*(^ \$&%^^#\$^&*?" to "",
        "36.6" to "36.6",
        "36....6" to "36.6",
        "[36.6]" to "[36.6]",
        "[36....6]" to "[36.6]",
        "\"36.6\"" to "\"36.6\"",
        "\"36....6\"" to "\"36.6\"",
        "!55.88.77." to "!55.88.77",
        "город.герой" to "город герой",
        "[город.герой]" to "[город герой]",
        "\"город.герой\"" to "\"город герой\"",
        "!прямо1.2брюхо." to "!прямо1 2брюхо",
        "[   !авто купить]" to "[!авто купить]",
        "[авто купить   ]" to "[авто купить]",
        "[  авто купить  ]" to "[авто купить]",
        "\"   !авто купить\"" to "\"!авто купить\"",
        "\"авто купить   \"" to "\"авто купить\"",
        "\"  авто купить  \"" to "\"авто купить\"",
        "питер!" to "питер",
        "питер!?" to "питер",
        "говорить по-английски" to "говорить по-английски",
        "говорить -по английски" to "говорить по английски",
    )

    @Suppress("UNUSED")
    fun phrasesWithStopWords() = listOf(
        "говорить по английски" to "говорить !по английски",
        "говорить +по английски" to "говорить +по английски",
        "говорить !по английски" to "говорить !по английски",
        "говорить -по английски" to "говорить !по английски",
        "[говорить по английски]" to "[говорить по английски]",
    )

    @ParameterizedTest
    @MethodSource("simpleMinusPhrases")
    fun test(testCase: Pair<String, String>) {
        whenever(stopWordService.isStopWord(any(), any())).thenReturn(false)
        whenever(keywordNormalizer.normalizeKeyword(anyString())).thenReturn("")
        val got = minusPhrasesBsPrepareService.processMinusPhrase(testCase.first)
        assertThat(got.phrase).isEqualTo(testCase.second)
    }

    @ParameterizedTest
    @MethodSource("phrasesWithStopWords")
    fun phrasesWithStopWordsTest(testCase: Pair<String, String>) {
        whenever(stopWordService.isStopWord(any(), any())).thenReturn(false)
        whenever(stopWordService.isStopWord(eq("по"), any())).thenReturn(true)
        whenever(keywordNormalizer.normalizeKeyword(anyString())).thenReturn("")
        val got = minusPhrasesBsPrepareService.processMinusPhrase(testCase.first)
        assertThat(got.phrase).isEqualTo(testCase.second)
    }

    @Test
    fun minusPhrasesSortTest() {
        val phrase1 = "кредит"
        val phrase2 = "автомобиль"
        val phrase3 = "азбука"
        val adGroup = TextAdGroup()
            .withId(1234L).withCampaignId(3456L).withMinusKeywordsId(1L).withLibraryMinusKeywordsIds(listOf(2L, 3L))
        val minusWordsMap = mapOf(1L to listOf(phrase1), 2L to listOf(phrase2), 3L to listOf(phrase3))
        whenever(keywordNormalizer.normalizeKeyword(eq(phrase1))).thenReturn(phrase1)
        whenever(keywordNormalizer.normalizeKeyword(eq(phrase2))).thenReturn(phrase2)
        whenever(keywordNormalizer.normalizeKeyword(eq(phrase3))).thenReturn(phrase3)
        val gotPhrases = minusPhrasesBsPrepareService.processAdGroupsMinusPhrases(listOf(adGroup), minusWordsMap)[0].minusPhrases
        assertThat(gotPhrases).isEqualTo(listOf(phrase2, phrase3, phrase1))
    }

    @Test
    fun minusPhrasesQuotedPhrasesTest() {
        val adGroup = TextAdGroup().withId(1234L).withCampaignId(3456L).withLibraryMinusKeywordsIds(listOf(1L))
        val phrase1 = "\"авто купить\""
        val phrase2 = "\"говорить по английски\""
        val minusWordsMap = mapOf(1L to listOf(phrase1, phrase2))
        whenever(keywordNormalizer.normalizeKeyword(eq(phrase1))).thenReturn(phrase1)
        whenever(keywordNormalizer.normalizeKeyword(eq(phrase2))).thenReturn(phrase2)
        whenever(stopWordService.isStopWord(eq("по"))).thenReturn(true)
        val gotPhrases = minusPhrasesBsPrepareService.processAdGroupsMinusPhrases(listOf(adGroup), minusWordsMap)[0].minusPhrases
        assertThat(gotPhrases).isEqualTo(listOf("авто купить ~0", "говорить +по английски ~0"))
    }

    @Test
    fun noProcessForOnlyPrivateMinusPhrasesTest() {
        val adGroup = TextAdGroup().withId(1234L)
            .withCampaignId(3456L)
            .withMinusKeywordsId(1L)
            .withLibraryMinusKeywordsIds(listOf())
        val phrases = listOf("авто", "говорить по английски", "авто")
        val minusWordsMap = mapOf(1L to phrases)
        phrases.forEach { whenever(keywordNormalizer.normalizeKeyword(eq(it))).thenReturn(it) }
        whenever(stopWordService.isStopWord(eq("по"))).thenReturn(true)
        val gotPhrases = minusPhrasesBsPrepareService.processAdGroupsMinusPhrases(listOf(adGroup), minusWordsMap)[0].minusPhrases
        assertThat(gotPhrases).isEqualTo(phrases)
    }

    @Test
    fun processQuotedPhrasesForOnlyPrivateMinusPhrasesTest() {
        val adGroup = TextAdGroup().withId(1234L)
            .withCampaignId(3456L)
            .withMinusKeywordsId(1L)
            .withLibraryMinusKeywordsIds(listOf())
        val phrases = listOf("\"говорить по английски\"")
        val minusWordsMap = mapOf(1L to phrases)
        phrases.forEach { whenever(keywordNormalizer.normalizeKeyword(eq(it))).thenReturn(it) }
        whenever(stopWordService.isStopWord(eq("по"))).thenReturn(true)
        val gotPhrases = minusPhrasesBsPrepareService.processAdGroupsMinusPhrases(listOf(adGroup), minusWordsMap)[0].minusPhrases
        assertThat(gotPhrases).containsExactly("говорить +по английски ~0")
    }

    @Test
    fun noMinusPhrasesAdGroupTest() {
        val adGroup = TextAdGroup().withId(1234L)
            .withCampaignId(3456L)
            .withLibraryMinusKeywordsIds(listOf())

        val gotAdGroupWithMinusPhrases = minusPhrasesBsPrepareService.processAdGroupsMinusPhrases(listOf(adGroup), mapOf())
        assertThat(gotAdGroupWithMinusPhrases).containsExactly(AdGroupMinusPhrasesInfo(1234L, 3456L, listOf()))
    }

    @Test
    fun minusPhrasesQuotedPhrasesSortTest() {
        val adGroup = TextAdGroup().withId(1234L)
            .withCampaignId(3456L)
            .withLibraryMinusKeywordsIds(listOf(1L))
        val phrases = listOf("\"fotofusion\"", "\"psd\"", "джинсовый", "скачать", "\"сценарий\"",
            "фабрика", "фотокниги", "\"шаблоны купить\"")
        val minusPhrasesMap = mapOf(
            1L to phrases
        )
        phrases.forEach { whenever(keywordNormalizer.normalizeKeyword(eq(it))).thenReturn(it) }
        val gotPhrases = minusPhrasesBsPrepareService.processAdGroupsMinusPhrases(listOf(adGroup), minusPhrasesMap)[0].minusPhrases
        val expectedPhrases = listOf("fotofusion ~0",
            "psd ~0",
            "сценарий ~0",
            "шаблоны купить ~0",
            "джинсовый",
            "скачать",
            "фабрика",
            "фотокниги")
        assertThat(gotPhrases).isEqualTo(expectedPhrases)
    }

    @Test
    fun twoGroupsWithMinusWordsIntersectionTestTest() {
        val adGroup1 = TextAdGroup().withId(1234L)
            .withCampaignId(3456L)
            .withLibraryMinusKeywordsIds(listOf(1L))
        val phrases1 = listOf("недорого", "авто", "купить", "автомобиль")
        val adGroup2 = TextAdGroup().withId(1235L)
            .withCampaignId(3457L)
            .withLibraryMinusKeywordsIds(listOf(2L))
        val phrases2 = listOf("авто", "продать", "автомобиль", "купить")
        val minusPhrasesMap = mapOf(
            1L to phrases1,
            2L to phrases2,
        )
        (phrases1 + phrases2).forEach { whenever(keywordNormalizer.normalizeKeyword(eq(it))).thenReturn(it) }
        val gotAdGroupsWithMinusPhrases = minusPhrasesBsPrepareService.processAdGroupsMinusPhrases(listOf(adGroup1, adGroup2), minusPhrasesMap)
        assertThat(gotAdGroupsWithMinusPhrases).containsExactlyInAnyOrder(
            AdGroupMinusPhrasesInfo(1234L, 3456L, listOf("авто", "автомобиль", "купить", "недорого")),
            AdGroupMinusPhrasesInfo(1235L, 3457L, listOf("авто", "автомобиль", "купить", "продать")))
    }

    @Test
    fun minusPhrasesLowerCaseSortTest() {
        val adGroup = TextAdGroup().withId(1234L)
            .withCampaignId(3456L)
            .withLibraryMinusKeywordsIds(listOf(1L))
        val phrases = listOf("BBbb",
            "zzzz",
            "bbB",
            "aa",
            "Abaa",
            "abAB",
            "BaBa")
        val minusPhrasesMap = mapOf(
            1L to phrases
        )
        phrases.forEach { whenever(keywordNormalizer.normalizeKeyword(eq(it))).thenReturn(it) }
        val gotPhrases = minusPhrasesBsPrepareService.processAdGroupsMinusPhrases(listOf(adGroup), minusPhrasesMap)[0].minusPhrases
        val expectedPhrases = listOf("aa", "Abaa", "abAB", "BaBa", "bbB", "BBbb", "zzzz")
        assertThat(gotPhrases).isEqualTo(expectedPhrases)
    }

    @Test
    fun skipEmptyMinusPhrasesTest() {
        val adGroup = TextAdGroup().withId(1234L)
            .withCampaignId(3456L)
            .withLibraryMinusKeywordsIds(listOf(1L))
        val phrases = listOf("кредит", "автомобиль", "")
        val minusPhrasesMap = mapOf(
            1L to phrases
        )
        phrases.forEach { whenever(keywordNormalizer.normalizeKeyword(eq(it))).thenReturn(it) }

        val gotPhrases = minusPhrasesBsPrepareService.processAdGroupsMinusPhrases(listOf(adGroup), minusPhrasesMap)[0].minusPhrases
        assertThat(gotPhrases).isEqualTo(listOf("автомобиль", "кредит"))
    }

    @Test
    fun normWordFormTest() {
        val phrase = "говорить по английски"
        whenever(stopWordService.isStopWord(any(), any())).thenReturn(false)
        whenever(keywordNormalizer.normalizeKeyword(eq(phrase))).thenReturn("английский говорить")
        val got = minusPhrasesBsPrepareService.processMinusPhrase(phrase)
        assertThat(got.phrase).isEqualTo(phrase)
        assertThat(got.normForm).isEqualTo("английский говорить")
    }

    @Test
    fun multiplePhrasesWithNoNormWordFormTest() {
        val phrase1 = "само"
        val phrase2 = "наса"
        val adGroup = TextAdGroup().withId(1234L)
            .withCampaignId(3456L)
            .withLibraryMinusKeywordsIds(listOf(1L))

        val minusPhrasesMap = mapOf(
            1L to listOf(phrase1, phrase2)
        )
        whenever(stopWordService.isStopWord(any(), any())).thenReturn(false)
        whenever(keywordNormalizer.normalizeKeyword(anyString())).thenReturn("")
        val gotPhrases = minusPhrasesBsPrepareService.processAdGroupsMinusPhrases(listOf(adGroup), minusPhrasesMap)[0].minusPhrases
        assertThat(gotPhrases).containsExactly(phrase2, phrase1)
    }

    @Test
    fun minusPhrasesDistinctByNormFormTest() {
        val adGroup = TextAdGroup().withId(1234L)
            .withCampaignId(3456L)
            .withLibraryMinusKeywordsIds(listOf(1L))
        val phrase1 = "говорить по английски"
        val phrase2 = "говорил по английски"
        val phrase3 = "говорить по испанский"
        val phrases = listOf(phrase1, phrase2, phrase3)
        val minusPhrasesMap = mapOf(
            1L to phrases
        )
        phrases.forEach { whenever(keywordNormalizer.normalizeKeyword(eq(it))).thenReturn(it) }


        whenever(keywordNormalizer.normalizeKeyword(eq(phrase1))).thenReturn("английский говорить")
        whenever(keywordNormalizer.normalizeKeyword(eq(phrase2))).thenReturn("английский говорить")
        whenever(keywordNormalizer.normalizeKeyword(eq(phrase3))).thenReturn("испанский говорить")
        val gotPhrases = minusPhrasesBsPrepareService.processAdGroupsMinusPhrases(listOf(adGroup), minusPhrasesMap)[0].minusPhrases
        assertThat(gotPhrases).isEqualTo(listOf(phrase1, phrase3))
    }

    @Test
    fun badSymbolInMinusPhrasesTest() {
        val phrase = "̇говорить по английски"
        whenever(stopWordService.isStopWord(any(), any())).thenReturn(false)
        whenever(keywordNormalizer.normalizeKeyword(eq(phrase))).thenReturn("английский говорить")
        val got = minusPhrasesBsPrepareService.processMinusPhrase(phrase)
        assertThat(got.phrase).isEqualTo("")
        assertThat(got.normForm).isEqualTo("")
    }
}
