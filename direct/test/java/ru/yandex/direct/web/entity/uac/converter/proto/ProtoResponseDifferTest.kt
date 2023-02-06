package ru.yandex.direct.web.entity.uac.converter.proto

import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.recursive.comparison.ComparisonDifference
import org.assertj.core.api.recursive.comparison.DualValue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.web.entity.uac.campaignProtoResponse
import ru.yandex.direct.web.entity.uac.toResponse
import ru.yandex.direct.web.entity.uac.uacCampaign

@RunWith(SpringJUnit4ClassRunner::class)
class ProtoResponseDifferTest {
    private var oldResponseCallCount = 0
    private var protoResponseCallCount = 0

    @Test
    fun disabled() {
        val differ = getDirectCampaignProtoResponseDiffer { 0 }
        val result = differ.process({
            ++oldResponseCallCount
            ResponseEntity(HttpStatus.OK)
        }, {
            ++protoResponseCallCount
            ResponseEntity(HttpStatus.OK)
        })
        SoftAssertions().apply {
            assertThat(oldResponseCallCount).isEqualTo(1)
            assertThat(protoResponseCallCount).isEqualTo(0)
            assertThat(result.diff).isNull()
        }.assertAll()
    }

    @Test
    fun justWorks() {
        val differ = getDirectCampaignProtoResponseDiffer { 100 }
        val result = differ.process({
            ++oldResponseCallCount
            ResponseEntity(uacCampaign().toResponse(), HttpStatus.OK)
        }, {
            ++protoResponseCallCount
            ResponseEntity(campaignProtoResponse(), HttpStatus.OK)
        })
        SoftAssertions().apply {
            assertThat(oldResponseCallCount).isEqualTo(1)
            assertThat(protoResponseCallCount).isEqualTo(1)
            assertThat(result.diff).contains(
                ComparisonDifference(DualValue(listOf("result", "status"), Status.STARTED, Status.DRAFT)))
        }.assertAll()
    }
}
