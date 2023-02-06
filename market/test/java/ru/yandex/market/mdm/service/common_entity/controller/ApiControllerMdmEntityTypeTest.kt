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
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum.MDM_ENTITY_TYPE
import ru.yandex.market.mdm.lib.model.mdm.MdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.service.common_entity.config.TestMetadataConfig
import ru.yandex.market.mdm.service.common_entity.controller.dto.FindRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.MetadataRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.SaveRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.WidgetType
import ru.yandex.market.mdm.service.common_entity.model.CommonEntity
import ru.yandex.market.mdm.service.common_entity.model.CommonEntityType
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValue
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValueType
import ru.yandex.market.mdm.service.common_entity.model.MdmProtocol
import ru.yandex.market.mdm.service.common_entity.service.common.CommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.common.filters.SearchFilter
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.mock.OneElementMetadataService
import ru.yandex.market.mdm.service.common_entity.service.constructor.MdmEntityTypeCommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.constructor.converters.EntityTypeConverter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmAttributeSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmEntityTypeSearchFilter
import ru.yandex.market.mdm.service.common_entity.testutils.CommonEntityBaseTestClass
import java.math.BigDecimal

@Import(TestMetadataConfig::class)
class ApiControllerMdmEntityTypeTest : CommonEntityBaseTestClass() {

    @Autowired
    lateinit var mdmAttributeMetadataService: OneElementMetadataService<MdmAttribute, MdmAttributeSearchFilter>
    @Autowired
    lateinit var mdmEntityTypeMetadataService: OneElementMetadataService<MdmEntityType, MdmEntityTypeSearchFilter>

    @Autowired
    lateinit var mdmEntityTypeCommonEntityService: MdmEntityTypeCommonEntityService
    lateinit var controller: MetadataApiController

    @Before
    fun init() {
        controller = MetadataApiController(
            CommonEntityDispatcher(
                listOf(
                    mdmEntityTypeCommonEntityService as CommonEntityService<SearchFilter>
                )
            )
        )
    }

    @Test
    fun `successful load metadata for ENTITY_TYPE_WITH_ATTRS_TREE widget`() {
        val request = MetadataRequest(
            WidgetType.TREE,
            MDM_ENTITY_TYPE, "latest"
        )

        val metadataResponse = controller.metadata(request)
        val paramsMap = metadataResponse.commonEntityType.commonParams!!.associateBy { it.commonParamName }

        assertSoftly {
            metadataResponse.widgetType shouldBe WidgetType.TREE
            metadataResponse.commonEntityType.commonEntityTypeEnum shouldBe MDM_ENTITY_TYPE
            paramsMap.keys shouldContainAll listOf(
                "tree_node_id", "tree_node_title", "parent_id",
                "request_entity_id", "request_common_entity_type"
            )
            paramsMap["tree_node_id"]!!.commonParamValueType shouldBe CommonParamValueType.NUMERIC
            paramsMap["tree_node_title"]!!.commonParamValueType shouldBe CommonParamValueType.STRING
            paramsMap["parent_id"]!!.commonParamValueType shouldBe CommonParamValueType.NUMERIC
        }
    }

    @Test
    fun `successful load metadata for MDM_ENTITY_TYPE for EDITOR widget`() {
        val request = MetadataRequest(WidgetType.EDITOR, MDM_ENTITY_TYPE, "latest")

        val metadataResponse = controller.metadata(request)
        val paramsMap = metadataResponse.commonEntityType.commonParams!!.associateBy { it.commonParamName }

        assertSoftly {
            metadataResponse.widgetType shouldBe WidgetType.EDITOR
            metadataResponse.commonEntityType.commonEntityTypeEnum shouldBe MDM_ENTITY_TYPE
            paramsMap.keys shouldContainAll listOf(
                "mdm_entity_type_reference", "internal_name", "description",
                "ru_title", "entity_kind", "version_from", "version_to", "version_status"
            )
            paramsMap["mdm_entity_type_reference"]!!.let {
                it.commonParamValueType shouldBe CommonParamValueType.NUMERIC
                it.readOnly shouldBe true
                it.required shouldBe false
                it.ruTitle shouldBe "Id типа сущности"
            }
        }
    }

    @Test
    fun `find common entities for widget ENTITY_TYPE_WITH_ATTRS_TREE widget`() {
        // given
        val entityType = mdmEntityType()
        val attribute = mdmAttribute(mdmEntityTypeId = entityType.mdmId)
        mdmAttributeMetadataService.update(updates = listOf(attribute), commitMessage = "test data")
        mdmEntityTypeMetadataService.update(updates = listOf(entityType), commitMessage = "test data")

        val request = FindRequest(
            WidgetType.TREE,
            CommonEntity(
                commonEntityType = CommonEntityType(MDM_ENTITY_TYPE),
                commonParamValues = listOf(MdmProtocol.REQUEST_ENTITY_ID.byLong(entityType.mdmId))
            ),
            "latest"
        )

        // when
        val response = controller.find(request)

        // then
        assertSoftly {
            response.commonEntities.size shouldBe 2
            response.commonEntities[0].commonEntityType.commonEntityTypeEnum shouldBe
                MDM_ENTITY_TYPE

            response.commonEntities[1].commonEntityType.commonEntityTypeEnum shouldBe
                CommonEntityTypeEnum.MDM_ATTR
            response.commonEntities[1].getLongValue("parent_id") shouldBe 0
            response.commonEntities[1].getLongValue("tree_node_id") shouldBe 1

            mdmAttributeMetadataService.lastUsedFilter!!.mdmEntityTypeId shouldBe entityType.mdmId
            mdmEntityTypeMetadataService.lastUsedFilter!!.ids shouldContain entityType.mdmId

        }
    }

    @Test
    fun `find MDM_ENTITY_TYPE for EDITOR widget`() {
        // given
        val entityType = mdmEntityType()
        mdmEntityTypeMetadataService.update(updates = listOf(entityType), commitMessage = "test data")

        val request = FindRequest(
            WidgetType.EDITOR,
            CommonEntity(
                commonEntityType = CommonEntityType(MDM_ENTITY_TYPE),
                commonParamValues = listOf(
                    CommonParamValue("request_entity_id", numerics = listOf(BigDecimal.valueOf(entityType.mdmId)))
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
                it.commonEntityType.commonEntityTypeEnum shouldBe MDM_ENTITY_TYPE
                it.getLongValue("mdm_entity_type_reference") shouldBe entityType.mdmId
                it.getParamNames() shouldContainAll listOf("internal_name", "description", "ru_title", "entity_kind",
                    "version_from", "version_to", "version_status")
                mdmEntityTypeMetadataService.lastUsedFilter!!.ids shouldContain entityType.mdmId
            }
        }
    }

    @Test
    fun `should save MDM_ENTITY_TYPE`() {
        // given
        val initialEntityType = mdmEntityType()
        mdmEntityTypeMetadataService.update(updates = listOf(initialEntityType), commitMessage = "test data")

        val updatedEntityType = initialEntityType.toBuilder().ruTitle("new title").build()
        val requestCommonEntity = EntityTypeConverter().mdm2common(updatedEntityType, CommonEntityType(MDM_ENTITY_TYPE))

        // when
        val response = controller.save(SaveRequest(listOf(requestCommonEntity), WidgetType.EDITOR))
        val after = mdmEntityTypeMetadataService.lastEntity

        // then
        assertSoftly {
            after shouldNotBe null
            response.commonEntities[0].getStringValue("ru_title") shouldBe "new title"
            after!!.ruTitle shouldBe "new title"
        }
    }
}
