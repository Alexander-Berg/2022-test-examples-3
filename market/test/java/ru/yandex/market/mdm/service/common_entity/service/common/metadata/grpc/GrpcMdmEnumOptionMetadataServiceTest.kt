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
import ru.yandex.market.mdm.fixtures.mdmEnumOption
import ru.yandex.market.mdm.http.enum_options.MdmEnumOptionList
import ru.yandex.market.mdm.http.enum_options.MdmEnumOptionServiceGrpc
import ru.yandex.market.mdm.http.enum_options.MdmEnumOptionsByFilterRequest
import ru.yandex.market.mdm.http.enum_options.MdmEnumOptionsByFilterResponse
import ru.yandex.market.mdm.http.enum_options.UpdateMdmEnumOptionsRequest
import ru.yandex.market.mdm.http.enum_options.UpdateMdmEnumOptionsResponse
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.service.common_entity.model.UpdateContext
import ru.yandex.market.mdm.service.common_entity.service.common.filters.SearchFilter
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.Update
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmEnumOptionSearchFilter

@RunWith(MockitoJUnitRunner::class)
class GrpcMdmEnumOptionMetadataServiceTest {

    @Mock
    lateinit var mdmEnumOptionService: MdmEnumOptionServiceGrpc.MdmEnumOptionServiceBlockingStub

    lateinit var enumOptionMetadataService: GrpcMdmEnumOptionMetadataService

    @Before
    fun init() {
        enumOptionMetadataService =
            GrpcMdmEnumOptionMetadataService(mdmEnumOptionService)
    }

    @Test
    fun `should return enum option by filter`() {
        // given
        val filter = MdmEnumOptionSearchFilter(
            ids = listOf(1L),
            version = SearchFilter.Version()
        )
        val returnedEnumOption = mdmEnumOption()
        given(mdmEnumOptionService.getEnumOptionsOnlyByFilter(any()))
            .willReturn(
                MdmEnumOptionsByFilterResponse.newBuilder()
                    .setMdmEnumOptions(
                        MdmEnumOptionList.newBuilder()
                            .addAllMdmEnumOptions(listOf(returnedEnumOption.toProto()))
                    )
                    .build()
            )

        // when
        val result = enumOptionMetadataService.findByFilter(filter)

        // then
        val requestCaptor = argumentCaptor<MdmEnumOptionsByFilterRequest>()
        verify(mdmEnumOptionService).getEnumOptionsOnlyByFilter(requestCaptor.capture())
        requestCaptor.firstValue.filter shouldBe filter.toProto()

        // and
        result shouldContain returnedEnumOption
    }

    @Test
    fun `should update enum option`() {
        // given
        val update = Update(mdmEnumOption())
        val returnedOption = mdmEnumOption()
        given(mdmEnumOptionService.updateEnumOptionsOnly(any()))
            .willReturn(
                UpdateMdmEnumOptionsResponse.newBuilder()
                    .setResults(
                        MdmEnumOptionList.newBuilder()
                            .addAllMdmEnumOptions(listOf(returnedOption.toProto()))
                    )
                    .build()
            )
        // when
        val result = enumOptionMetadataService.update(listOf(update),
            UpdateContext(commitMessage = "commit", userLogin = "user"))

        // then
        val requestCaptor = argumentCaptor<UpdateMdmEnumOptionsRequest>()
        verify(mdmEnumOptionService).updateEnumOptionsOnly(requestCaptor.capture())
        requestCaptor.firstValue.updates.mdmEnumOptionsList shouldContain update.update.toProto()
        requestCaptor.firstValue.context.commitMessage shouldBe "commit"
        requestCaptor.firstValue.context.userLogin shouldBe "user"

        // and
        result.results shouldContain returnedOption
    }
}

