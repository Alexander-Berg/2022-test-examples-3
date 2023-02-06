package ru.yandex.market.adv.incut.clients

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import net.javacrumbs.jsonunit.JsonAssert
import net.javacrumbs.jsonunit.core.Option
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.adv.incut.AbstractFunctionalTest
import ru.yandex.market.vendor.api.client.CategoryRequest
import ru.yandex.market.vendor.api.client.ModelsRequest
import ru.yandex.market.vendor.api.client.VendorPartnerClient
import ru.yandex.market.vendor.api.view.CategoryCollectorType

class VendorPartnerClientTest(
    @Autowired
    private val vendorPartnerMock: WireMockServer,
    @Autowired
    private val vendorPartnerClient: VendorPartnerClient

) : AbstractFunctionalTest() {


    @Test
    fun `test get category models`() {
        vendorPartnerMock.stubFor(
            WireMock.get(WireMock.anyUrl())
                .willReturn(WireMock.okJson(getStringResource("models_response.json")))
        )
        val response = vendorPartnerClient.getVendorModelsInCategory(
            1L, 1L, "Text"
        ).schedule().get()

        val expected = getStringResource("models_response.json")

        JsonAssert.assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    fun `test get categories by ids`() {
        vendorPartnerMock.stubFor(
            WireMock.get(WireMock.anyUrl())
                .willReturn(WireMock.okJson(getStringResource("categories_response.json")))
        )

        val response = vendorPartnerClient.getCategories(
            CategoryRequest.bilder()
                .setRelativeCategoryId(4953550)
                .setCollectorType(CategoryCollectorType.PARENT)
                .build()
        ).schedule().get()

        val expected = getStringResource("categories_response.json")

        JsonAssert.assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    fun `test get models`() {

        vendorPartnerMock.stubFor(
            WireMock.post(WireMock.anyUrl())
                .willReturn(WireMock.okJson(getStringResource("models.json")))
        )

        val response = vendorPartnerClient.getModels(
            ModelsRequest.newBuilder()
                .setVendorId(19708)
                .setModels(listOf(1429703292, 1448810179))
                .build()
        ).schedule().get()

        val expected = getStringResource("models.json")

        JsonAssert.assertJsonEquals(
            expected,
            response,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )

    }

}
