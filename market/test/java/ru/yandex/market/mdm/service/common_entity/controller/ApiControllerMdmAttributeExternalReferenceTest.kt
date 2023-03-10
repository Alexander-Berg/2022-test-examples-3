package ru.yandex.market.mdm.service.common_entity.controller

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.mdm.fixtures.mdmExternalReferenceForAttribute
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalReference
import ru.yandex.market.mdm.service.common_entity.config.TestMetadataConfig
import ru.yandex.market.mdm.service.common_entity.controller.dto.MetadataRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.SaveRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.WidgetType
import ru.yandex.market.mdm.service.common_entity.model.CommonEntityType
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValueType
import ru.yandex.market.mdm.service.common_entity.service.common.CommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.common.filters.SearchFilter
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.mock.OneElementMetadataService
import ru.yandex.market.mdm.service.common_entity.service.constructor.MdmAttributeExternalReferenceCommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.constructor.converters.AttributeExternalReferenceConverter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmExternalReferenceSearchFilter
import ru.yandex.market.mdm.service.common_entity.testutils.CommonEntityBaseTestClass

@Import(TestMetadataConfig::class)
class ApiControllerMdmAttributeExternalReferenceTest : CommonEntityBaseTestClass() {

    @Autowired
    lateinit var mdmAttributeExternalReferenceCommonEntityService: MdmAttributeExternalReferenceCommonEntityService

    @Autowired
    lateinit var mdmExternalReferenceMetadataService : OneElementMetadataService<MdmExternalReference, MdmExternalReferenceSearchFilter>

    lateinit var controller: MetadataApiController

    @Before
    fun init() {
        controller = MetadataApiController(CommonEntityDispatcher(listOf(
            mdmAttributeExternalReferenceCommonEntityService as CommonEntityService<SearchFilter>
        )))
    }

    @Test
    fun `should load metadata for EDITOR widget`() {
        val metadataResponse = controller.metadata(
            MetadataRequest(
                WidgetType.EDITOR, CommonEntityTypeEnum.MDM_ATTR_EXTERNAL_REFERENCE, "latest")
        )
        val paramsMap = metadataResponse.commonEntityType.commonParams!!.associateBy { it.commonParamName }

        assertSoftly {
            metadataResponse.widgetType shouldBe WidgetType.EDITOR
            metadataResponse.commonEntityType.commonEntityTypeEnum shouldBe CommonEntityTypeEnum.MDM_ATTR_EXTERNAL_REFERENCE
            paramsMap.keys shouldContainAll listOf("mdm_attr_external_reference_id", "mdm_reference_path",
                "external_attr_id", "external_system", "external_attr_name", "version_from", "version_to", "version_status")
            paramsMap["mdm_reference_path"]!!.let {
                it.commonParamValueType shouldBe CommonParamValueType.STRING
                it.readOnly shouldBe true
                it.ruTitle shouldBe "???????? ???? ????????????????"
            }
            paramsMap["mdm_attr_external_reference_id"]!!.let {
                it.commonParamValueType shouldBe CommonParamValueType.NUMERIC
                it.readOnly shouldBe true
                it.ruTitle shouldBe "Id"
            }
        }
    }

    @Test
    fun `should save MDM_ATTR_EXTERNAL_REFERENCE`() {
        // given
        val initialReference = mdmExternalReferenceForAttribute()
        mdmExternalReferenceMetadataService.update(updates = listOf(initialReference), commitMessage = "test data")

        val updatedReference = initialReference.copy(externalId = 123)
        val requestCommonEntity = AttributeExternalReferenceConverter()
            .mdm2common(updatedReference, CommonEntityType(CommonEntityTypeEnum.MDM_ATTR_EXTERNAL_REFERENCE))

        // when
        val response = controller.save(SaveRequest(listOf(requestCommonEntity), WidgetType.EDITOR))
        val after = mdmExternalReferenceMetadataService.lastEntity

        // then
        assertSoftly {
            after shouldNotBe null
            response.commonEntities[0].getLongValue("external_attr_id") shouldBe 123
            after!!.externalId shouldBe 123L
        }
    }
}
