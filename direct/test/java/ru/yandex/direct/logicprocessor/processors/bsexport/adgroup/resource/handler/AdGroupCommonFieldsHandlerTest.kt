package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.adgroup.AdGroup
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.product.model.Product
import ru.yandex.direct.core.entity.product.service.ProductService
import ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup

const val SHARD = 1
const val AD_GROUP_ID = 4444L
const val CAMPAIGN_ID = 44L
const val AD_GROUP_NAME = "some name"
const val PRODUCT_ID = 333L
const val ENGINE_ID = 111L


internal class AdGroupCommonFieldsHandlerTest {
    private lateinit var adGroupCommonFieldsHandler: AdGroupCommonFieldsHandler
    private lateinit var campaignRepository: CampaignRepository
    private lateinit var productService: ProductService

    @BeforeEach
    fun before() {
        campaignRepository = mock()
        productService = mock {
            on { getProductById(PRODUCT_ID) } doReturn Product().withId(PRODUCT_ID).withEngineId(ENGINE_ID)
        }
        adGroupCommonFieldsHandler = AdGroupCommonFieldsHandler(campaignRepository, productService)
    }

    @Test
    fun withValidProductId() {
        whenever(campaignRepository.getProductIds(any(), any()))
            .thenReturn(mapOf(CAMPAIGN_ID to PRODUCT_ID))
        val adGroup = defaultTextAdGroup(CAMPAIGN_ID)
            .withId(AD_GROUP_ID)
            .withType(AdGroupType.BASE)
            .withName(AD_GROUP_NAME)
        val adGroupWithBuilder = AdGroupWithBuilder(
            adGroup,
            AdGroup.newBuilder().setAdGroupId(AD_GROUP_ID))
        adGroupCommonFieldsHandler.handle(SHARD, mapOf(AD_GROUP_ID to adGroupWithBuilder))

        assertThat(adGroupWithBuilder.protoBuilder)
            .hasFieldOrPropertyWithValue("type", ru.yandex.adv.direct.adgroup.AdGroupType.AD_GROUP_TYPE_BASE.number)
            .hasFieldOrPropertyWithValue("name", AD_GROUP_NAME)
            .hasFieldOrPropertyWithValue("engineId", ENGINE_ID)
    }

    @Test
    fun withoutValidProductId() {
        whenever(campaignRepository.getProductIds(any(), any()))
            .thenReturn(mapOf(CAMPAIGN_ID to null))
        val adGroup = defaultTextAdGroup(CAMPAIGN_ID)
            .withId(AD_GROUP_ID)
            .withType(AdGroupType.BASE)
            .withName(AD_GROUP_NAME)
        val adGroupWithBuilder = AdGroupWithBuilder(
            adGroup,
            AdGroup.newBuilder().setAdGroupId(AD_GROUP_ID))
        adGroupCommonFieldsHandler.handle(SHARD, mapOf(AD_GROUP_ID to adGroupWithBuilder))

        assertThat(adGroupWithBuilder.protoBuilder)
            .hasFieldOrPropertyWithValue("type", ru.yandex.adv.direct.adgroup.AdGroupType.AD_GROUP_TYPE_BASE.number)
            .hasFieldOrPropertyWithValue("name", AD_GROUP_NAME)
            .hasFieldOrPropertyWithValue("engineId", DIRECT_ENGINE_ID)
    }
}
