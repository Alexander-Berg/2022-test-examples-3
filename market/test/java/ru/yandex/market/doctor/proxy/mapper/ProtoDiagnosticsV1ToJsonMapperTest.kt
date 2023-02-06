package ru.yandex.market.doctor.proxy.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.StringValue
import com.google.protobuf.Timestamp
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import ru.yandex.market.doctor.api.DoctorApi
import ru.yandex.market.doctor.proxy.ProxyResponse
import ru.yandex.market.doctor.testutils.BaseAppTest
import java.io.ByteArrayInputStream

internal class ProtoDiagnosticsV1ToJsonMapperTest : BaseAppTest() {
    @Autowired
    private lateinit var mapper: ProtoDiagnosticsV1ToJsonMapper

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `mapper maps`() {
        val proto = DoctorApi.DoctorDiagnosticResponseV1.newBuilder().apply {
            businessId = 11111
            shopSku = "ssku"
            addSections(DoctorApi.Section.newBuilder().apply {
                title = "s1"
                expanded = true
                addChildren(DoctorApi.Section.newBuilder().apply {
                    title = "s1c1"
                    expanded = false
                    setLinks(DoctorApi.Section.LinksValue.newBuilder().apply {
                        addData(DoctorApi.Link.newBuilder().apply {
                            url = "link url"
                            title = "link title"
                            source = StringValue.of("link src")
                        })
                    })
                })
                addChildren(DoctorApi.Section.newBuilder().apply {
                    title = "Section child title"
                    expanded = false
                    setChecks(DoctorApi.Section.ChecksValue.newBuilder().apply {
                        addData(DoctorApi.Check.newBuilder().apply {
                            title = "link title"
                            source = StringValue.of("link src")
                        })
                    })
                })
                setTimings(DoctorApi.Section.TimingsValue.newBuilder().apply {
                    addData(DoctorApi.Timing.newBuilder().apply {
                        title = "Timing title"
                        timestamp = Timestamp.newBuilder().setSeconds(1000000).setNanos(100).build()
                        addAllMeta(mapOf("metaKey" to "metaValue").map {
                            DoctorApi.Meta.newBuilder().setKey(it.key).setValue(it.value).build()
                        })
                    })
                })
            })
        }.build()
        val originalResponse = ProxyResponse(
            status = HttpStatus.OK,
            headers = mapOf("header" to listOf("value")),
            proxiedUrl = "yrl",
            contentType = "application/protobuf",
            content = ByteArrayInputStream(proto.toByteArray())
        )
        val mappedResponse = mapper.mapResponse(originalResponse)

        mappedResponse.status shouldBe originalResponse.status
        mappedResponse.headers shouldBe originalResponse.headers
        mappedResponse.proxiedUrl shouldBe originalResponse.proxiedUrl
        mappedResponse.contentType shouldBe "application/json;charset=utf-8"

        val content = objectMapper.readValue(
            mappedResponse.content,
            ProtoDiagnosticsV1ToJsonMapper.DoctorDiagnosticResponseV1::class.java
        )
        content.businessId shouldBe proto.businessId
        content.shopSku shouldBe proto.shopSku
        content.sections should { sections ->
            sections.forEachIndexed { i, section ->
                val protoSection = proto.sectionsList[i]
                section.title shouldBe protoSection.title
                section.expanded shouldBe protoSection.expanded
            }
        }
    }
}
