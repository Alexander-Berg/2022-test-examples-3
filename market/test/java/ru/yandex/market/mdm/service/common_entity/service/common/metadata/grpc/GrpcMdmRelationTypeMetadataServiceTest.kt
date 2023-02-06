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
import ru.yandex.market.mdm.fixtures.mdmRelationType
import ru.yandex.market.mdm.http.relations.MdmRelationTypeServiceGrpc
import ru.yandex.market.mdm.http.relations.MdmRelationTypesByFilterRequest
import ru.yandex.market.mdm.http.relations.MdmRelationTypesByFilterResponse
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.service.common_entity.service.common.filters.SearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmRelationTypeSearchFilter

@RunWith(MockitoJUnitRunner::class)
class GrpcMdmRelationTypeMetadataServiceTest {

    @Mock
    lateinit var mdmRelationEntityTypeService: MdmRelationTypeServiceGrpc.MdmRelationTypeServiceBlockingStub

    lateinit var relationTypeMetadataService: GrpcMdmRelationTypeMetadataService

    @Before
    fun init() {
        relationTypeMetadataService = GrpcMdmRelationTypeMetadataService(mdmRelationEntityTypeService)
    }

    @Test
    fun `should return relation types by filter`() {
        // given
        val filter = MdmRelationTypeSearchFilter(
            ids = listOf(1L),
            version = SearchFilter.Version()
        )
        val mdmRelationType = mdmRelationType()
        given(mdmRelationEntityTypeService.getRelationTypeBySearchFilter(any()))
            .willReturn(
                MdmRelationTypesByFilterResponse.newBuilder()
                    .addAllMdmRelationTypes(
                        listOf(mdmRelationType.toProto()))
                    .build()
            )

        // when
        val result = relationTypeMetadataService.findByFilter(filter)

        // then
        val requestCaptor = argumentCaptor<MdmRelationTypesByFilterRequest>()
        verify(mdmRelationEntityTypeService).getRelationTypeBySearchFilter(requestCaptor.capture())
        requestCaptor.firstValue.filter shouldBe filter.toProto()

        // and
        result shouldContain mdmRelationType
    }

}
