package ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.http.attributes.MdmAttributeList
import ru.yandex.market.mdm.http.attributes.MdmAttributeServiceGrpc
import ru.yandex.market.mdm.http.attributes.MdmAttributesByFilterRequest
import ru.yandex.market.mdm.http.attributes.MdmAttributesByFilterResponse
import ru.yandex.market.mdm.http.attributes.MdmUpdateAttributesRequest
import ru.yandex.market.mdm.http.attributes.MdmUpdateAttributesResponse
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.service.common_entity.model.UpdateContext
import ru.yandex.market.mdm.service.common_entity.service.common.filters.SearchFilter
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.Update
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmAttributeSearchFilter

@RunWith(MockitoJUnitRunner::class)
class GrpcMdmAttributeMetadataServiceTest {

    @Mock
    lateinit var mdmAttributeService: MdmAttributeServiceGrpc.MdmAttributeServiceBlockingStub

    lateinit var attributeMetadataService: GrpcMdmAttributeMetadataService

    @Before
    fun init() {
        attributeMetadataService = GrpcMdmAttributeMetadataService(mdmAttributeService)
    }

    @Test
    fun `should return attributes by filter`() {
        // given
        val filter = MdmAttributeSearchFilter(
            ids = listOf(1L),
            version = SearchFilter.Version()
        )
        val returnedAttribute = mdmAttribute()
        given(mdmAttributeService.getAttributesByFilter(any()))
            .willReturn(
                MdmAttributesByFilterResponse.newBuilder()
                    .setAttributes(MdmAttributeList.newBuilder().addAllAttribute(listOf(returnedAttribute.toProto())))
                    .build()
            )

        // when
        val result = attributeMetadataService.findByFilter(filter)

        // then
        val requestCaptor = argumentCaptor<MdmAttributesByFilterRequest>()
        verify(mdmAttributeService).getAttributesByFilter(requestCaptor.capture())
        requestCaptor.firstValue.filter shouldBe filter.toProto()

        // and
        result shouldContain returnedAttribute
    }

    @Test
    fun `should update attributes`() {
        // given
        val update = Update(mdmAttribute())
        val returnedAttribute = mdmAttribute()
        given(mdmAttributeService.updateAttributesOnly(any()))
            .willReturn(
                MdmUpdateAttributesResponse.newBuilder()
                    .setResults(
                        MdmAttributeList.newBuilder().addAllAttribute(listOf(returnedAttribute.toProto()))
                    )
                    .build()
            )
        // when
        val result = attributeMetadataService.update(listOf(update),
            UpdateContext(commitMessage = "commit", userLogin = "user"))

        // then
        val requestCaptor = argumentCaptor<MdmUpdateAttributesRequest>()
        verify(mdmAttributeService).updateAttributesOnly(requestCaptor.capture())
        requestCaptor.firstValue.updates.attributeList shouldContain update.update.toProto()
        requestCaptor.firstValue.context.commitMessage shouldBe "commit"
        requestCaptor.firstValue.context.userLogin shouldBe "user"

        // and
        result.results shouldContain returnedAttribute
    }
}
