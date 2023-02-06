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
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.http.entity_types.MdmEntityTypeServiceGrpc
import ru.yandex.market.mdm.http.entity_types.MdmEntityTypesByFilterRequest
import ru.yandex.market.mdm.http.entity_types.MdmEntityTypesByFilterResponse
import ru.yandex.market.mdm.http.entity_types.MdmEntityTypesList
import ru.yandex.market.mdm.http.entity_types.UpdateMdmEntityTypesRequest
import ru.yandex.market.mdm.http.entity_types.UpdateMdmEntityTypesResponse
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.service.common_entity.model.UpdateContext
import ru.yandex.market.mdm.service.common_entity.service.common.filters.SearchFilter
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.Update
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmEntityTypeSearchFilter

@RunWith(MockitoJUnitRunner::class)
class GrpcMdmEntityTypeMetadataServiceTest {

    @Mock
    lateinit var mdmEntityTypeService: MdmEntityTypeServiceGrpc.MdmEntityTypeServiceBlockingStub

    lateinit var attributeMetadataService: GrpcMdmEntityTypeMetadataService

    @Before
    fun init() {
        attributeMetadataService = GrpcMdmEntityTypeMetadataService(mdmEntityTypeService)
    }

    @Test
    fun `should return entity types by filter`() {
        // given
        val filter = MdmEntityTypeSearchFilter(
            ids = listOf(1L),
            version = SearchFilter.Version()
        )
        val returnedAttribute = mdmEntityType()
        given(mdmEntityTypeService.getEntityTypesByFilter(any()))
            .willReturn(
                MdmEntityTypesByFilterResponse.newBuilder()
                    .setMdmEntityTypes(
                        MdmEntityTypesList.newBuilder().addAllMdmEntityTypes(listOf(returnedAttribute.toProto())))
                    .build()
            )

        // when
        val result = attributeMetadataService.findByFilter(filter)

        // then
        val requestCaptor = argumentCaptor<MdmEntityTypesByFilterRequest>()
        verify(mdmEntityTypeService).getEntityTypesByFilter(requestCaptor.capture())
        requestCaptor.firstValue.filter shouldBe filter.toProto()

        // and
        result shouldContain returnedAttribute
    }

    @Test
    fun `should update entity types`() {
        // given
        val update = Update(mdmEntityType())
        val returnedAttribute = mdmEntityType()
        given(mdmEntityTypeService.updateEntityTypesOnly(any()))
            .willReturn(
                UpdateMdmEntityTypesResponse.newBuilder()
                    .setResults(
                        MdmEntityTypesList.newBuilder().addAllMdmEntityTypes(listOf(returnedAttribute.toProto()))
                    )
                    .build()
            )


        // when
        val result = attributeMetadataService.update(listOf(update),
            UpdateContext(commitMessage = "commit", userLogin = "user"))

        // then
        val requestCaptor = argumentCaptor<UpdateMdmEntityTypesRequest>()
        verify(mdmEntityTypeService).updateEntityTypesOnly(requestCaptor.capture())
        requestCaptor.firstValue.updates.mdmEntityTypesList shouldContain update.update.toProto()
        requestCaptor.firstValue.context.commitMessage shouldBe "commit"
        requestCaptor.firstValue.context.userLogin shouldBe "user"

        // and
        result.results shouldContain returnedAttribute
    }
}
