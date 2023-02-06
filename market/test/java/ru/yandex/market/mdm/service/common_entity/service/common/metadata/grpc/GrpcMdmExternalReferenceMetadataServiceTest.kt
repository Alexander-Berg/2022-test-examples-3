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
import ru.yandex.market.mdm.fixtures.mdmExternalReferenceForAttribute
import ru.yandex.market.mdm.http.external_references.MdmExternalReferenceList
import ru.yandex.market.mdm.http.external_references.MdmExternalReferenceServiceGrpc
import ru.yandex.market.mdm.http.external_references.MdmExternalReferencesByFilterRequest
import ru.yandex.market.mdm.http.external_references.MdmExternalReferencesByFilterResponse
import ru.yandex.market.mdm.http.external_references.UpdateMdmExternalReferencesRequest
import ru.yandex.market.mdm.http.external_references.UpdateMdmExternalReferencesResponse
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType
import ru.yandex.market.mdm.lib.model.mdm.MdmPath
import ru.yandex.market.mdm.service.common_entity.model.UpdateContext
import ru.yandex.market.mdm.service.common_entity.service.common.filters.SearchFilter
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.Update
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmExternalReferenceSearchFilter

@RunWith(MockitoJUnitRunner::class)
class GrpcMdmExternalReferenceMetadataServiceTest  {

    @Mock
    lateinit var mdmExternalReferenceService: MdmExternalReferenceServiceGrpc.MdmExternalReferenceServiceBlockingStub

    lateinit var externalReferenceMetadataService: GrpcMdmExternalReferenceMetadataService

    @Before
    fun init() {
        externalReferenceMetadataService =
            GrpcMdmExternalReferenceMetadataService(mdmExternalReferenceService)
    }

    @Test
    fun `should return external reference by filter`() {
        // given
        val filter = MdmExternalReferenceSearchFilter(
            mdmPath = MdmPath.fromLongs(listOf(1,2), MdmMetaType.MDM_ATTR),
            version = SearchFilter.Version()
        )
        val returnedExternalReference = mdmExternalReferenceForAttribute()
        given(mdmExternalReferenceService.getExternalReferencesByFilter(any()))
            .willReturn(
                MdmExternalReferencesByFilterResponse.newBuilder()
                    .setMdmExternalReferences(
                        MdmExternalReferenceList.newBuilder()
                            .addAllMdmExternalReferences(listOf(returnedExternalReference.toProto()))
                    )
                    .build()
            )

        // when
        val result = externalReferenceMetadataService.findByFilter(filter)

        // then
        val requestCaptor = argumentCaptor<MdmExternalReferencesByFilterRequest>()
        verify(mdmExternalReferenceService).getExternalReferencesByFilter(requestCaptor.capture())
        requestCaptor.firstValue.filter shouldBe filter.toProto()

        // and
        result shouldContain returnedExternalReference
    }

    @Test
    fun `should update attribute external reference`() {
        // given
        val update = Update(mdmExternalReferenceForAttribute())
        val returnedReference = mdmExternalReferenceForAttribute()
        given(mdmExternalReferenceService.updateExternalReferences(any()))
            .willReturn(
                UpdateMdmExternalReferencesResponse.newBuilder()
                    .setResults(
                        MdmExternalReferenceList.newBuilder()
                            .addAllMdmExternalReferences(listOf(returnedReference.toProto()))
                    )
                    .build()
            )
        // when
        val result = externalReferenceMetadataService.update(listOf(update),
            UpdateContext(commitMessage = "commit", userLogin = "user")
        )

        // then
        val requestCaptor = argumentCaptor<UpdateMdmExternalReferencesRequest>()
        verify(mdmExternalReferenceService).updateExternalReferences(requestCaptor.capture())
        requestCaptor.firstValue.updates.mdmExternalReferencesList shouldContain update.update.toProto()
        requestCaptor.firstValue.context.commitMessage shouldBe "commit"
        requestCaptor.firstValue.context.userLogin shouldBe "user"

        // and
        result.results shouldContain returnedReference
    }
}
