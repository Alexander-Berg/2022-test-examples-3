package ru.yandex.market.contentmapping.services.datacamp.offer.export

import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue
import ru.yandex.market.contentmapping.dto.model.MarketParameterValue
import ru.yandex.market.contentmapping.dto.model.ValueSource
import ru.yandex.market.contentmapping.kotlin.typealiases.ParamId
import ru.yandex.market.contentmapping.services.category.info.DataCampGroupIdService
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.contentmapping.utils.MboParameterConstants

internal class DatacampGroupIdConverterTest : BaseAppTestClass() {

    @Autowired
    private lateinit var dataCampGroupIdService: DataCampGroupIdService

    @Autowired
    private lateinit var datacampGroupIdConverter: DatacampGroupIdConverter

    private val shopId = 1L

    @Test
    fun `test empty conversion`() {
        val paramsOriginal: Map<ParamId, List<MarketParameterValue>> = emptyMap()
        val (params, converted) = datacampGroupIdConverter.convertGroupId(paramsOriginal, shopId)
        params shouldBeSameInstanceAs paramsOriginal
        converted shouldBe null
    }

    @Test
    fun `test groupId conversion`() {
        val values = HashMap<ParamId, MutableList<MarketParameterValue>>()
        val valueGroupId = "id"
        values.addParameterValueString(MboParameterConstants.PARTNER_GROUP_ID, valueGroupId)
        val (params, converted) = datacampGroupIdConverter.convertGroupId(values, shopId)

        val convertedValue = dataCampGroupIdService.getGroupIdIntForGroupId(shopId, null, valueGroupId)
        converted?.id shouldBe convertedValue
        converted?.name shouldBe null

        params shouldNotContainKey MboParameterConstants.PARTNER_GROUP_ID
    }

    @Test
    fun `test groupName conversion`() {
        val values = HashMap<ParamId, MutableList<MarketParameterValue>>()
        val valueGroupName = "name"
        values.addParameterValueString(MboParameterConstants.DATACAMP_GROUP_NAME, valueGroupName)
        val (params, converted) = datacampGroupIdConverter.convertGroupId(values, shopId)

        val convertedValue = dataCampGroupIdService.getGroupIdIntForGroupId(shopId, valueGroupName, null)
        converted?.id shouldBe convertedValue
        converted?.name shouldBe valueGroupName

        params shouldNotContainKey MboParameterConstants.DATACAMP_GROUP_NAME
    }

    @Test
    fun `test groupId and groupName conversion`() {
        val values = HashMap<ParamId, MutableList<MarketParameterValue>>()
        val valueGroupId = "id"
        values.addParameterValueString(MboParameterConstants.PARTNER_GROUP_ID, valueGroupId)
        val valueGroupName = "name"
        values.addParameterValueString(MboParameterConstants.DATACAMP_GROUP_NAME, valueGroupName)
        val (params, converted) = datacampGroupIdConverter.convertGroupId(values, shopId)

        val convertedValueCombined = dataCampGroupIdService.getGroupIdIntForGroupId(
                shopId,
                valueGroupName,
                valueGroupId
        )
        converted?.id shouldBe convertedValueCombined
        converted?.name shouldBe valueGroupName

        params shouldNotContainKey MboParameterConstants.PARTNER_GROUP_ID
        params shouldNotContainKey MboParameterConstants.DATACAMP_GROUP_NAME
    }

    private fun MutableMap<ParamId, MutableList<MarketParameterValue>>.addParameterValueString(paramId: ParamId, value: String) {
        this.computeIfAbsent(paramId) { ArrayList() }
                .add(MarketParameterValue(paramId, ValueSource.MANUAL, MarketParamValue.StringValue(value)))
    }
}
