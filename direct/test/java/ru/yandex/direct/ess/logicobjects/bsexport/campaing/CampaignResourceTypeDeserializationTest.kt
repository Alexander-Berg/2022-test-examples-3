package ru.yandex.direct.ess.logicobjects.bsexport.campaing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.direct.utils.JsonUtils

internal class CampaignResourceTypeDeserializationTest {
    @Test
    fun knownValue1() {
        val json = """"CAMPAIGN_STRATEGY""""
        assertThat(JsonUtils.fromJson(json, CampaignResourceType::class.java))
            .isEqualTo(CampaignResourceType.CAMPAIGN_STRATEGY)
    }

    @Test
    fun knownValue2() {
        val json = """"UNKNOWN""""
        assertThat(JsonUtils.fromJson(json, CampaignResourceType::class.java))
            .isEqualTo(CampaignResourceType.UNKNOWN)
    }

    @Test
    fun unknownValue() {
        val json = """"XXX-XXX""""
        assertThat(JsonUtils.fromJson(json, CampaignResourceType::class.java))
            .isEqualTo(CampaignResourceType.UNKNOWN)
    }

    @Test
    fun complexObjectWithKnownValue() {
        val json = """{"cid":32,"order_id":33,"resource_type":"CAMPAIGN_STRATEGY","reqid":0,"service":"","method":""}"""
        val deserialized = BsExportCampaignObject.Builder()
            .setCid(32L)
            .setOrderId(33L)
            .setCampaignResourceType(CampaignResourceType.CAMPAIGN_STRATEGY)
            .build()
        assertThat(JsonUtils.fromJson(json, BsExportCampaignObject::class.java))
            .isEqualToComparingFieldByField(deserialized)
    }

    @Test
    fun complexObjectWithUnknownValue() {
        val json = """{"cid":32,"order_id":33,"resource_type":"XXX","reqid":0,"service":"","method":""}"""
        val deserialized = BsExportCampaignObject.Builder()
            .setCid(32L)
            .setOrderId(33L)
            .setCampaignResourceType(CampaignResourceType.UNKNOWN)
            .build()
        assertThat(JsonUtils.fromJson(json, BsExportCampaignObject::class.java))
            .isEqualToComparingFieldByField(deserialized)
    }

    @Test
    fun complexObjectWithNullValue() {
        val json = """{"cid":32,"order_id":33,"resource_type":null,"reqid":0,"service":"","method":""}"""
        val deserialized = BsExportCampaignObject.Builder()
            .setCid(32L)
            .setOrderId(33L)
            .setCampaignResourceType(null)
            .build()
        assertThat(JsonUtils.fromJson(json, BsExportCampaignObject::class.java))
            .isEqualToComparingFieldByField(deserialized)
    }

    @Test
    fun objectWithMultiplierInfo() {
        val json = """{
            |"resource_type": "MULTIPLIERS",
            |"additional_info": {
                |"type": "MultiplierInfo",
                |"hierarchical_multiplier_id": 42
            |},
            |"hierarchical_multiplier_id": 42,
            |"reqid": 0,
            |"service": "",
            |"method": ""
        |}""".trimMargin()

        val deserialized = BsExportCampaignObject.Builder()
            .setCampaignResourceType(CampaignResourceType.MULTIPLIERS)
            .setAdditionalInfo(MultiplierInfo(42))
            .setHierarchicalMultiplierId(42)

        assertThat(JsonUtils.fromJson(json, BsExportCampaignObject::class.java))
            .isEqualToComparingFieldByField(deserialized)
    }

    @Test
    fun objectWithRetargetingConditionInfo() {
        val json = """{
            |"resource_type": "MULTIPLIERS",
            |"additional_info": {
                |"type": "RetargetingConditionInfo",
                |"retargeting_condition_id": 42
            |},
            |"retargeting_condition_id": 42,
            |"reqid": 0,
            |"service": "",
            |"method": ""
        |}""".trimMargin()

        val deserialized = BsExportCampaignObject.Builder()
            .setCampaignResourceType(CampaignResourceType.MULTIPLIERS)
            .setAdditionalInfo(RetargetingConditionInfo(42))
            .setRetargetingConditionId(42)

        assertThat(JsonUtils.fromJson(json, BsExportCampaignObject::class.java))
            .isEqualToComparingFieldByField(deserialized)
    }
}
