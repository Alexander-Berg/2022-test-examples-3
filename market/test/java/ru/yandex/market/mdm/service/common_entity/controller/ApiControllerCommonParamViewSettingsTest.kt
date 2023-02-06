package ru.yandex.market.mdm.service.common_entity.controller

import com.nhaarman.mockitokotlin2.given
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.mdm.fixtures.commonParamViewSetting
import ru.yandex.market.mdm.fixtures.commonViewType
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.lib.model.common.CommonParamViewSetting
import ru.yandex.market.mdm.service.common_entity.config.TestMetadataConfig
import ru.yandex.market.mdm.service.common_entity.controller.dto.MetadataRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.SaveRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.WidgetType
import ru.yandex.market.mdm.service.common_entity.model.CommonEntityType
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValueType
import ru.yandex.market.mdm.service.common_entity.service.common.CommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.common.filters.SearchFilter
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.grpc.GrpcCommonViewTypeMetadataService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.mock.OneElementMetadataService
import ru.yandex.market.mdm.service.common_entity.service.constructor.ParamViewSettingCommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.constructor.converters.ParamViewSettingConverter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.CommonParamViewSettingSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.metadata.ParamViewSettingUiMetadataProvider
import ru.yandex.market.mdm.service.common_entity.testutils.CommonEntityBaseTestClass
import ru.yandex.market.mdm.service.common_entity.util.commonEnumValue

@Import(TestMetadataConfig::class)
class ApiControllerCommonParamViewSettingsTest : CommonEntityBaseTestClass() {

    lateinit var paramViewSettingsCommonEntityService: ParamViewSettingCommonEntityService

    lateinit var paramViewSettingConverter: ParamViewSettingConverter

    @Autowired
    lateinit var commonParamViewSettingsMetadataService: OneElementMetadataService<CommonParamViewSetting, CommonParamViewSettingSearchFilter>

    @Mock
    lateinit var commonViewTypeMetadataService: GrpcCommonViewTypeMetadataService

    lateinit var controller: MetadataApiController

    @Before
    fun init() {
        paramViewSettingConverter = ParamViewSettingConverter(commonViewTypeMetadataService)
        paramViewSettingsCommonEntityService =
            ParamViewSettingCommonEntityService(paramViewSettingConverter,
                commonParamViewSettingsMetadataService,
                ParamViewSettingUiMetadataProvider(paramViewSettingConverter))
        controller = MetadataApiController(CommonEntityDispatcher(listOf(
            paramViewSettingsCommonEntityService as CommonEntityService<SearchFilter>
        )))

    }

    @Test
    fun `should load metadata for EDITOR widget`() {
        // given
        val commonViewTypeElement1 = commonViewType()
        val commonViewTypeElement2 = commonViewType()
        given(commonViewTypeMetadataService.findAll())
            .willReturn(listOf(commonViewTypeElement1, commonViewTypeElement2))

        // when
        val metadataResponse = controller.metadata(
            MetadataRequest(
                WidgetType.EDITOR, CommonEntityTypeEnum.COMMON_PARAM_VIEW_SETTING, "latest")
        )
        val paramsMap = metadataResponse.commonEntityType.commonParams!!.associateBy { it.commonParamName }

        // then
        metadataResponse.widgetType shouldBe WidgetType.EDITOR
        metadataResponse.commonEntityType.commonEntityTypeEnum shouldBe CommonEntityTypeEnum.COMMON_PARAM_VIEW_SETTING
        paramsMap.keys shouldContainAll listOf("common_param_view_setting_reference", "common_view_type_reference",
            "mdm_reference_path", "ui_order", "is_enabled", "version_from", "version_to", "version_status")
        paramsMap["common_param_view_setting_reference"]!!.let {
            it.commonParamValueType shouldBe CommonParamValueType.NUMERIC
            it.readOnly shouldBe true
            it.required shouldBe false
            it.ruTitle shouldBe "Id"
        }
        paramsMap["common_view_type_reference"]!!.let {
            it.commonParamValueType shouldBe CommonParamValueType.ENUM
            it.readOnly shouldBe false
            it.required shouldBe true
            it.ruTitle shouldBe "Имя представления"
            it.options shouldContain commonEnumValue(commonViewTypeElement1)
            it.options shouldContain commonEnumValue(commonViewTypeElement2)
        }
        paramsMap["mdm_reference_path"]!!.let {
            it.commonParamValueType shouldBe CommonParamValueType.STRING
            it.readOnly shouldBe true
            it.ruTitle shouldBe "Путь до атрибута"
        }
        paramsMap["ui_order"]!!.let {
            it.commonParamValueType shouldBe CommonParamValueType.NUMERIC
            it.readOnly shouldBe false
            it.required shouldBe true
            it.ruTitle shouldBe "Номер в ui"
        }
        paramsMap["is_enabled"]!!.let {
            it.commonParamValueType shouldBe CommonParamValueType.BOOLEAN
            it.readOnly shouldBe false
            it.required shouldBe true
            it.ruTitle shouldBe "Активирован"
        }
        paramsMap["version_from"]!!.let {
            it.commonParamValueType shouldBe CommonParamValueType.TIMESTAMP
            it.readOnly shouldBe true
            it.required shouldBe false
            it.ruTitle shouldBe "Дата начала действия"
        }
        paramsMap["version_to"]!!.let {
            it.commonParamValueType shouldBe CommonParamValueType.TIMESTAMP
            it.readOnly shouldBe false
            it.required shouldBe false
            it.ruTitle shouldBe "Дата окончания действия"
        }
        paramsMap["version_status"]!!.let {
            it.commonParamValueType shouldBe CommonParamValueType.ENUM
            it.readOnly shouldBe true
            it.required shouldBe false
            it.ruTitle shouldBe "Статус"

        }
    }

    @Test
    fun `should save COMMON_PARAM_VIEW_SETTING`() {
        // given
        val initialCommonViewType = commonViewType()
        val secondCommonViewType = commonViewType()
        given(commonViewTypeMetadataService.findById(initialCommonViewType.mdmId))
            .willReturn(initialCommonViewType)
        given(commonViewTypeMetadataService.findById(secondCommonViewType.mdmId))
            .willReturn(secondCommonViewType)
        given(commonViewTypeMetadataService.findAll())
            .willReturn(listOf(initialCommonViewType, secondCommonViewType))
        val initialReference = commonParamViewSetting(commonViewTypeId = initialCommonViewType.mdmId)
        commonParamViewSettingsMetadataService.update(updates = listOf(initialReference), commitMessage = "test data")

        val updatedReference = initialReference.copy(commonViewTypeId = secondCommonViewType.mdmId)
        val requestCommonEntity = paramViewSettingConverter
            .mdm2common(updatedReference, CommonEntityType(CommonEntityTypeEnum.COMMON_PARAM_VIEW_SETTING))

        // when
        val response = controller.save(SaveRequest(listOf(requestCommonEntity), WidgetType.EDITOR))
        val after = commonParamViewSettingsMetadataService.lastEntity

        // then
        assertSoftly {
            after shouldNotBe null
            response.commonEntities.first().getOptionId("common_view_type_reference") shouldBe secondCommonViewType.mdmId
            response.commonEntities.first().getOptionValue("common_view_type_reference") shouldBe secondCommonViewType.internalName
            after!!.commonViewTypeId shouldBe secondCommonViewType.mdmId
        }
    }
}
