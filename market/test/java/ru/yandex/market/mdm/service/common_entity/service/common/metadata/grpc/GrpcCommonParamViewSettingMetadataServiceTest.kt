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
import ru.yandex.market.mdm.fixtures.commonParamViewSetting
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.http.common_view.CommonParamViewSettingList
import ru.yandex.market.mdm.http.common_view.CommonParamViewSettingsByFilterRequest
import ru.yandex.market.mdm.http.common_view.CommonParamViewSettingsByFilterResponse
import ru.yandex.market.mdm.http.common_view.CommonParamViewTypeServiceGrpc
import ru.yandex.market.mdm.http.common_view.UpdateCommonParamViewSettingsRequest
import ru.yandex.market.mdm.http.common_view.UpdateCommonParamViewSettingsResponse
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.service.common_entity.model.UpdateContext
import ru.yandex.market.mdm.service.common_entity.service.common.filters.SearchFilter
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.Update
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.UpdateRule
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.CommonParamViewSettingSearchFilter

@RunWith(MockitoJUnitRunner::class)
class GrpcCommonParamViewSettingMetadataServiceTest {

    @Mock
    lateinit var commonParamViewSettingService: CommonParamViewTypeServiceGrpc.CommonParamViewTypeServiceBlockingStub

    lateinit var grpcCommonParamViewSettingMetadataService: GrpcCommonParamViewSettingMetadataService

    @Before
    fun init() {
        grpcCommonParamViewSettingMetadataService =
            GrpcCommonParamViewSettingMetadataService(commonParamViewSettingService)
    }

    @Test
    fun `should return param settings by filter`() {
        // given
        val filter = CommonParamViewSettingSearchFilter(
            ids = listOf(1L),
            version = SearchFilter.Version()
        )
        val returnedEnumOption = commonParamViewSetting()
        given(commonParamViewSettingService.getCommonParamViewSettingsByFilter(any()))
            .willReturn(
                CommonParamViewSettingsByFilterResponse.newBuilder()
                    .setSettings(
                        CommonParamViewSettingList.newBuilder()
                            .addAllSetting(listOf(returnedEnumOption.toProto()))
                    )
                    .build()
            )

        // when
        val result = grpcCommonParamViewSettingMetadataService.findByFilter(filter)

        // then
        val requestCaptor = argumentCaptor<CommonParamViewSettingsByFilterRequest>()
        verify(commonParamViewSettingService).getCommonParamViewSettingsByFilter(requestCaptor.capture())
        requestCaptor.firstValue.filter shouldBe filter.toProto()

        // and
        result shouldContain returnedEnumOption
    }

    @Test
    fun `should update param settings`() {
        // given
        val update = Update(commonParamViewSetting(), listOf(UpdateRule.PROPAGATE_DOWN))
        val returnedOption = commonParamViewSetting()
        given(commonParamViewSettingService.updateCommonParamViewSettings(any()))
            .willReturn(
                UpdateCommonParamViewSettingsResponse.newBuilder()
                    .setResults(
                        CommonParamViewSettingList.newBuilder()
                            .addAllSetting(listOf(returnedOption.toProto()))
                    )
                    .build()
            )
        // when
        val result = grpcCommonParamViewSettingMetadataService.update(
            listOf(update),
            UpdateContext(commitMessage = "commit", userLogin = "user")
        )

        // then
        val requestCaptor = argumentCaptor<UpdateCommonParamViewSettingsRequest>()
        verify(commonParamViewSettingService).updateCommonParamViewSettings(requestCaptor.capture())
        requestCaptor.firstValue.updates.updateList.map { it.setting } shouldContain update.update.toProto()
        requestCaptor.firstValue.updates.updateList.flatMap { it.rulesList } shouldContain MdmBase.UpdateRule.PROPAGATE_DOWN
        requestCaptor.firstValue.context.commitMessage shouldBe "commit"
        requestCaptor.firstValue.context.userLogin shouldBe "user"

        // and
        result.results shouldContain returnedOption
    }
}
