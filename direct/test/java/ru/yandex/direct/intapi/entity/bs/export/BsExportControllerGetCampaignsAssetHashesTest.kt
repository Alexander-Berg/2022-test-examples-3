package ru.yandex.direct.intapi.entity.bs.export

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.yandex.direct.core.entity.uac.createDirectAd
import ru.yandex.direct.core.entity.uac.createDirectAdGroup
import ru.yandex.direct.core.entity.uac.createDirectCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdGroupRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectAdRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.intapi.configuration.IntApiTest
import ru.yandex.direct.utils.JsonUtils


@IntApiTest
@RunWith(SpringJUnit4ClassRunner::class)
class BsExportControllerGetCampaignsAssetHashesTest {

    @Autowired
    private lateinit var bsExportController: BsExportController

    @Autowired
    private lateinit var uacYdbDirectCampaignRepository: UacYdbDirectCampaignRepository

    @Autowired
    private lateinit var uacYdbDirectAdGroupRepository: UacYdbDirectAdGroupRepository

    @Autowired
    private lateinit var uacYdbDirectAdRepository: UacYdbDirectAdRepository

    private lateinit var mockMvc: MockMvc

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.standaloneSetup(bsExportController).build()
    }

    /**
     * Проверяем получение хешей ассетов по одной кампании
     */
    @Test
    fun test() {
        val directCampaignId = 1L
        val directAdGroupId = 3L
        val directAdId = 20L
        val uacDirectCampaignId = "2"
        val uacDirectAdGroupId = "4"

        val directImageContentId = "10"
        val directVideoContentId = "11"
        val textContentId = "12"
        val titleContentId = "13"
        val directHtml5ContentId = "14"

        createCampaignAndGroup(directCampaignId, uacDirectCampaignId, uacDirectAdGroupId, directAdGroupId)

        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(
            directAdGroupId = uacDirectAdGroupId,
            directAdId = directAdId,
            directImageContentId = directImageContentId,
            directVideoContentId = directVideoContentId,
            textContentId = textContentId,
            titleContentId = titleContentId,
            directHtml5ContentId = directHtml5ContentId,
        ))

        val result = performRequest(listOf(directCampaignId))

        val expected = mapOf(
            directCampaignId.toString() to mapOf(
                directAdId.toString() to mapOf(
                    "TitleAssetHash" to titleContentId,
                    "TextBodyAssetHash" to textContentId,
                    "ImageAssetHash" to directImageContentId,
                    "VideoAssetHash" to directVideoContentId,
                    "Html5AssetHash" to directHtml5ContentId,
                )
            )
        )

        Assertions.assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    /**
     * Проверяем что ничего не получаем, если у кампании нет ассетов
     */
    @Test
    fun testWithoutAssets() {
        val directCampaignId = 2000L
        val directAdGroupId = 3000L
        val uacDirectCampaignId = "2000"
        val uacDirectAdGroupId = "4000"

        createCampaignAndGroup(directCampaignId, uacDirectCampaignId, uacDirectAdGroupId, directAdGroupId)

        val result = performRequest(listOf(directCampaignId))

        val expected = mapOf<String, Any>()

        Assertions.assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    /**
     * Проверяем получение хешей ассетов для 2ух переданных кампаний
     */
    @Test
    fun testWithTwoCampaigns() {
        val directCampaignIdA = 100L
        val directCampaignIdB = 101L
        val directAdGroupIdA = 300L
        val directAdGroupIdB = 301L
        val directAdIdA1 = 200L
        val directAdIdA2 = 201L
        val directAdIdB = 202L
        val uacDirectCampaignIdA = "20"
        val uacDirectCampaignIdB = "21"
        val uacDirectAdGroupIdA = "40"
        val uacDirectAdGroupIdB = "41"

        val textContentIdA1 = "120"
        val textContentIdA2 = "121"
        val textContentIdB = "123"
        val titleContentIdA1 = "130"
        val titleContentIdA2 = "131"
        val titleContentIdB = "132"

        createCampaignAndGroup(directCampaignIdA, uacDirectCampaignIdA, uacDirectAdGroupIdA, directAdGroupIdA)
        createCampaignAndGroup(directCampaignIdB, uacDirectCampaignIdB, uacDirectAdGroupIdB, directAdGroupIdB)

        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(
            directAdGroupId = uacDirectAdGroupIdA,
            directAdId = directAdIdA1,
            textContentId = textContentIdA1,
            titleContentId = titleContentIdA1,
        ))
        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(
            directAdGroupId = uacDirectAdGroupIdA,
            directAdId = directAdIdA2,
            textContentId = textContentIdA2,
            titleContentId = titleContentIdA2,
        ))
        uacYdbDirectAdRepository.saveDirectAd(createDirectAd(
            directAdGroupId = uacDirectAdGroupIdB,
            directAdId = directAdIdB,
            textContentId = textContentIdB,
            titleContentId = titleContentIdB,
        ))

        val result = performRequest(listOf(directCampaignIdA, directCampaignIdB))

        val expected = mapOf(
            directCampaignIdA.toString() to mapOf(
                directAdIdA1.toString() to mapOf(
                    "TitleAssetHash" to titleContentIdA1,
                    "TextBodyAssetHash" to textContentIdA1,
                ),
                directAdIdA2.toString() to mapOf(
                    "TitleAssetHash" to titleContentIdA2,
                    "TextBodyAssetHash" to textContentIdA2,
                )
            ),
            directCampaignIdB.toString() to mapOf(
                directAdIdB.toString() to mapOf(
                    "TitleAssetHash" to titleContentIdB,
                    "TextBodyAssetHash" to textContentIdB,
                ),
            )
        )

        Assertions.assertThat(result)
            .`as`("Хеши ассетов")
            .isEqualTo(expected)
    }

    private fun performRequest(campaignIds: Collection<Long>): Map<String, Any> {
        val result = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/bsexport/get-campaigns-asset-hashes")
                    .content(campaignIds.toString())
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        return JsonUtils.fromJson(result, object : TypeReference<Map<String, Any>>() {})
    }

    private fun createCampaignAndGroup(
        directCampaignId: Long,
        uacDirectCampaignId: String,
        uacDirectAdGroupId: String,
        directAdGroupId: Long,
    ) {
        uacYdbDirectCampaignRepository.saveDirectCampaign(createDirectCampaign(
            directCampaignId = directCampaignId,
            id = uacDirectCampaignId,
        ))

        uacYdbDirectAdGroupRepository.saveDirectAdGroup(createDirectAdGroup(
            id = uacDirectAdGroupId,
            directAdGroupId = directAdGroupId,
            directCampaignId = uacDirectCampaignId,
        ))
    }
}
