package ru.yandex.direct.web.entity.uac.converter.proto

import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.entity.uac.campaignProtoResponse
import ru.yandex.direct.web.entity.uac.model.UacCampaignAction

@RunWith(SpringJUnit4ClassRunner::class)
class UacGetCampaignProtoResponseMapperTest {

    private val objectMapper = JsonUtils.MAPPER

    @Test
    fun testCampaignProtoResponseMapper() {
        val mapper = UacGetCampaignProtoResponseMapper(objectMapper)
        val protoResponse = campaignProtoResponse()
        val mappedResponse = mapper.convert(protoResponse)
        val result = mappedResponse.result!!
        SoftAssertions().apply {
            assertThat(mappedResponse.reqId)
                .isEqualTo(protoResponse.reqId)
            assertThat(result.displayName)
                .isEqualTo(protoResponse.result!!.campaign.spec.campaignData.name)
            assertThat(result.accessKt.actions)
                .containsExactlyInAnyOrder(UacCampaignAction.BAN_PAY, UacCampaignAction.AGENCY_SERVICED)
            assertThat(result.agencyInfoKt).isNotNull
            assertThat(result.managerInfoKt).isNotNull
            assertThat(result.hyperGeo).isNotNull
            assertThat(result.brandSurveyStatus).isNotNull
            assertThat(result.adjustments).isNotEmpty
            assertThat(result.appInfo).isNotNull
            assertThat(result.contents).isNotEmpty
        }.assertAll()
    }
}
