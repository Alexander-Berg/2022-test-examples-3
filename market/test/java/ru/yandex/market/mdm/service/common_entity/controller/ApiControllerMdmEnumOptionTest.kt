package ru.yandex.market.mdm.service.common_entity.controller

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.mdm.fixtures.mdmEnumOption
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum.MDM_ENUM_OPTION
import ru.yandex.market.mdm.lib.model.mdm.MdmEnumOption
import ru.yandex.market.mdm.service.common_entity.config.TestMetadataConfig
import ru.yandex.market.mdm.service.common_entity.controller.dto.FindRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.MetadataRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.SaveRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.WidgetType
import ru.yandex.market.mdm.service.common_entity.model.CommonEntity
import ru.yandex.market.mdm.service.common_entity.model.CommonEntityType
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValueType
import ru.yandex.market.mdm.service.common_entity.model.MdmProtocol
import ru.yandex.market.mdm.service.common_entity.service.common.CommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.common.filters.SearchFilter
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.mock.OneElementMetadataService
import ru.yandex.market.mdm.service.common_entity.service.constructor.MdmEnumOptionCommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.constructor.converters.EnumOptionConverter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmEnumOptionSearchFilter
import ru.yandex.market.mdm.service.common_entity.testutils.CommonEntityBaseTestClass
import ru.yandex.market.mdm.service.common_entity.util.commonEnumValue

@Import(TestMetadataConfig::class)
class ApiControllerMdmEnumOptionTest : CommonEntityBaseTestClass() {

    @Autowired
    lateinit var mdmEnumOptionCommonEntityService: MdmEnumOptionCommonEntityService

    @Autowired
    lateinit var mdmEnumOptionMetadataService : OneElementMetadataService<MdmEnumOption, MdmEnumOptionSearchFilter>

    lateinit var controller: MetadataApiController

    @Before
    fun init() {
        controller = MetadataApiController(CommonEntityDispatcher(listOf(
            mdmEnumOptionCommonEntityService as CommonEntityService<SearchFilter>
        )))
    }

    @Test
    fun `successful load metadata for MDM_ENUM_OPTION for EDITOR widget`() {
        val metadataResponse = controller.metadata(
            MetadataRequest(WidgetType.EDITOR, MDM_ENUM_OPTION, "latest")
        )
        val paramsMap = metadataResponse.commonEntityType.commonParams!!.associateBy { it.commonParamName }

        assertSoftly {
            metadataResponse.widgetType shouldBe WidgetType.EDITOR
            metadataResponse.commonEntityType.commonEntityTypeEnum shouldBe MDM_ENUM_OPTION
            paramsMap.keys shouldContainAll listOf("mdm_attr_reference", "mdm_enum_option_reference", "value",
                "version_from", "version_to", "version_status")
            paramsMap["mdm_attr_reference"]!!.let {
                it.commonParamValueType shouldBe CommonParamValueType.NUMERIC
                it.readOnly shouldBe true
                it.required shouldBe false
                it.ruTitle shouldBe "Id атрибута"
            }
            paramsMap["mdm_enum_option_reference"]!!.let {
                it.commonParamValueType shouldBe CommonParamValueType.NUMERIC
                it.readOnly shouldBe true
                it.required shouldBe false
                it.ruTitle shouldBe "Id значения"
            }
        }
    }

    @Test
    fun `find for ENUM_OPTIONS_TABLE widget`() {
        // given
        val enumOption = mdmEnumOption()
        mdmEnumOptionMetadataService.update(updates = listOf(enumOption), commitMessage = "test data")
        val request = FindRequest(
            WidgetType.TABLE,
            CommonEntity(
                commonEntityType = CommonEntityType(MDM_ENUM_OPTION),
                commonParamValues = listOf(
                    MdmProtocol.REQUEST_ENTITY_ID.byLong(enumOption.mdmAttributeId),
                    MdmProtocol.REQUEST_COMMON_ENTITY_TYPE.byEnum(commonEnumValue(CommonEntityTypeEnum.MDM_ATTR))
                )
            ),
            "latest"
        )

        // when
        val response = controller.find(request)

        // then
        assertSoftly {
            response.commonEntities.size shouldBe 1
            response.commonEntities[0].let {
                it.commonEntityType.commonEntityTypeEnum shouldBe MDM_ENUM_OPTION
                it.getLongValue("mdm_attr_reference") shouldBe enumOption.mdmAttributeId
                it.getParamNames() shouldContainAll listOf("mdm_attr_reference", "mdm_enum_option_reference", "value",
                    "version_from", "version_to", "version_status", "request_entity_id", "request_common_entity_type")
            }
            mdmEnumOptionMetadataService.lastUsedFilter!!.mdmAttributeId shouldBe enumOption.mdmAttributeId
        }
    }

    @Test
    fun `find MDM_ENUM_OPTION for EDITOR widget`() {
        // given
        val enumOption = mdmEnumOption()
        mdmEnumOptionMetadataService.update(updates = listOf(enumOption), commitMessage = "test data")

        val request = FindRequest(
            WidgetType.EDITOR,
            CommonEntity(
                commonEntityType = CommonEntityType(MDM_ENUM_OPTION),
                commonParamValues = listOf(MdmProtocol.REQUEST_ENTITY_ID.byLong(enumOption.mdmId))
            ),
            "latest"
        )

        // when
        val response = controller.find(request)

        // then
        assertSoftly {
            response.commonEntities.size shouldBe 1
            response.commonEntities[0].let {
                it.commonEntityType.commonEntityTypeEnum shouldBe MDM_ENUM_OPTION
                it.getLongValue("mdm_enum_option_reference") shouldBe enumOption.mdmId
                it.getParamNames() shouldContainAll listOf("mdm_attr_reference", "mdm_enum_option_reference", "value",
                    "version_from", "version_to", "version_status")
            }
            mdmEnumOptionMetadataService.lastUsedFilter!!.ids shouldContain enumOption.mdmId
        }
    }

    @Test
    fun `should save MDM_ENUM_OPTION`() {
        // given
        val initialEnumOption = mdmEnumOption()
        mdmEnumOptionMetadataService.update(updates = listOf(initialEnumOption), commitMessage = "test data")

        val updatedEnumOption = initialEnumOption.copy(value = "new value")
        val requestCommonEntity = EnumOptionConverter().mdm2common(updatedEnumOption, CommonEntityType(MDM_ENUM_OPTION))

        // when
        val response = controller.save(SaveRequest(listOf(requestCommonEntity), WidgetType.EDITOR))
        val after = mdmEnumOptionMetadataService.lastEntity

        // then
        assertSoftly {
            after shouldNotBe null
            response.commonEntities[0].getStringValue("value") shouldBe "new value"
            after!!.value shouldBe "new value"
        }
    }
}
