package ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.mdm.fixtures.commonViewType
import ru.yandex.market.mdm.http.common_view.CommonParamViewTypeServiceGrpc
import ru.yandex.market.mdm.http.common_view.CommonViewTypeList
import ru.yandex.market.mdm.http.common_view.CommonViewTypeListResponse
import ru.yandex.market.mdm.http.common_view.CommonViewTypesByFilterRequest
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.CommonViewTypeSearchFilter

@RunWith(MockitoJUnitRunner::class)
class GrpcCommonViewTypeMetadataServiceTest {

    @Mock
    lateinit var commonViewTypeService: CommonParamViewTypeServiceGrpc.CommonParamViewTypeServiceBlockingStub

    lateinit var commonViewTypeMetadataService: GrpcCommonViewTypeMetadataService

    @Before
    fun init() {
        commonViewTypeMetadataService =
            GrpcCommonViewTypeMetadataService(commonViewTypeService)
    }

    @Test
    fun `should return common view types by filter`() {
        // given
        val filter = CommonViewTypeSearchFilter(
            ids = listOf(1L)
        )
        val returnedCommonViewType = commonViewType()
        given(commonViewTypeService.getCommonViewTypesByFilter(any()))
            .willReturn(
                CommonViewTypeListResponse.newBuilder()
                    .setViewTypes(
                        CommonViewTypeList.newBuilder()
                            .addAllViewType(listOf(returnedCommonViewType.toProto()))
                    )
                    .build()
            )

        // when
        val result = commonViewTypeMetadataService.findByFilter(filter)

        // then
        val requestCaptor = argumentCaptor<CommonViewTypesByFilterRequest>()
        verify(commonViewTypeService).getCommonViewTypesByFilter(requestCaptor.capture())
        requestCaptor.firstValue.filter shouldBe filter.toProto()

        // and
        result shouldContain returnedCommonViewType
    }

    @Test
    fun `should return all common view types`() {
        // given
        val returnedCommonViewType1 = commonViewType()
        val returnedCommonViewType2 = commonViewType()
        val returnedCommonViewType3 = commonViewType()
        given(commonViewTypeService.getAllCommonViewTypes(any()))
            .willReturn(
                CommonViewTypeListResponse.newBuilder()
                    .setViewTypes(
                        CommonViewTypeList.newBuilder()
                            .addAllViewType(listOf(
                                returnedCommonViewType1.toProto(),
                                returnedCommonViewType2.toProto(),
                                returnedCommonViewType3.toProto()
                            ))
                    )
                    .build()
            )

        // when
        val result = commonViewTypeMetadataService.findAll()

        // then
        result shouldHaveSize 3

        // and
        assertSoftly {
            result shouldContain returnedCommonViewType1
            result shouldContain returnedCommonViewType2
            result shouldContain returnedCommonViewType3
        }
    }
}
