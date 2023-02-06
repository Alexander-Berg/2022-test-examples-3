package ru.yandex.market.mdm.service.common_entity.controller

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmTreeEdge
import ru.yandex.market.mdm.lib.util.MdmProperties
import ru.yandex.market.mdm.service.common_entity.config.TestMetadataConfig
import ru.yandex.market.mdm.service.common_entity.controller.dto.FindRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.MetadataRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.SaveRequest
import ru.yandex.market.mdm.service.common_entity.controller.dto.WidgetType
import ru.yandex.market.mdm.service.common_entity.model.CommonEntity
import ru.yandex.market.mdm.service.common_entity.model.CommonEntityType
import ru.yandex.market.mdm.service.common_entity.model.CommonEnumValue
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValue
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValueType
import ru.yandex.market.mdm.service.common_entity.service.common.CommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.common.filters.SearchFilter
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.mock.OneElementMetadataService
import ru.yandex.market.mdm.service.common_entity.service.constructor.MdmEntityTypeCommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.constructor.MdmNavigationTreeCommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.constructor.MdmNavigationTreeNodeCommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmEntityTypeSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmTreeEdgeSearchFilter
import ru.yandex.market.mdm.service.common_entity.testutils.CommonEntityBaseTestClass
import java.math.BigDecimal

@Import(TestMetadataConfig::class)
class ApiControllerMdmNavigationTreeTest : CommonEntityBaseTestClass() {

    @Autowired
    lateinit var mdmEntityTypeMetadataService: OneElementMetadataService<MdmEntityType, MdmEntityTypeSearchFilter>

    @Autowired
    lateinit var mdmTreeEdgeMetadataService: OneElementMetadataService<MdmTreeEdge, MdmTreeEdgeSearchFilter>

    @Autowired
    lateinit var mdmEntityTypeCommonEntityService: MdmEntityTypeCommonEntityService

    @Autowired
    lateinit var mdmNavigationTreeNodeCommonEntityService: MdmNavigationTreeNodeCommonEntityService

    @Autowired
    lateinit var mdmNavigationTreeCommonEntityService: MdmNavigationTreeCommonEntityService


    lateinit var controller: MetadataApiController

    @Before
    fun init() {
        controller = MetadataApiController(
            CommonEntityDispatcher(
                listOf(
                    mdmEntityTypeCommonEntityService as CommonEntityService<SearchFilter>,
                    mdmNavigationTreeNodeCommonEntityService as CommonEntityService<SearchFilter>,
                    mdmNavigationTreeCommonEntityService as CommonEntityService<SearchFilter>,
                )
            )
        )
    }

    @Test
    fun `successful load metadata for NAVIGATION_TREE widget`() {
        val request = MetadataRequest(
            WidgetType.TREE,
            CommonEntityTypeEnum.MDM_NAVIGATION_TREE, "latest"
        )

        val metadataResponse = controller.metadata(request)
        val paramsMap = metadataResponse.commonEntityType.commonParams!!.associateBy { it.commonParamName }

        assertSoftly {
            metadataResponse.widgetType shouldBe WidgetType.TREE
            metadataResponse.commonEntityType.commonEntityTypeEnum shouldBe CommonEntityTypeEnum.MDM_NAVIGATION_TREE
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
    fun `find entities for widget NAVIGATION_TREE`() {
        // given
        val entityType = mdmEntityType()
        mdmEntityTypeMetadataService.update(updates = listOf(entityType), commitMessage = "test data")

        val request = FindRequest(
            WidgetType.TREE,
            CommonEntity(
                commonEntityType = CommonEntityType(CommonEntityTypeEnum.MDM_NAVIGATION_TREE)
            ),
            "latest"
        )

        // when
        val response = controller.find(request)

        // then
        assertSoftly {
            response.commonEntities.size shouldBe 2
            response.commonEntities[0].commonEntityType.commonEntityTypeEnum shouldBe
                CommonEntityTypeEnum.MDM_NAVIGATION_TREE_NODE
            response.commonEntities[1].commonEntityType.commonEntityTypeEnum shouldBe
                CommonEntityTypeEnum.MDM_ENTITY_TYPE
            response.commonEntities[1].getLongValue("parent_id") shouldBe 0
            response.commonEntities[1].getLongValue("tree_node_id") shouldBe 1
        }
    }

    @Test
    fun `successful save tree node with edge for NAVIGATION_TREE_NODE`() {
        val request = SaveRequest(
            listOf(
                CommonEntity(
                    commonEntityType = CommonEntityType(CommonEntityTypeEnum.MDM_NAVIGATION_TREE_NODE),
                    commonParamValues = listOf(
                        CommonParamValue("request_entity_id", numerics = listOf(BigDecimal(1))),
                        CommonParamValue("parent_id", numerics = listOf(BigDecimal(2))),
                        CommonParamValue("internal_name", strings = listOf("test name")),
                        CommonParamValue("description", strings = listOf("test name")),
                        CommonParamValue("ru_title", strings = listOf("тестовый заголовок")),
                        CommonParamValue("entity_kind", options = listOf(CommonEnumValue("PROTOTYPE")))
                    )
                )
            ),
            WidgetType.EDITOR
        )

        val saveResponse = controller.save(request)
        val savedEntity = mdmTreeEdgeMetadataService.lastEntity

        assertSoftly {
            savedEntity shouldBe
                MdmTreeEdge(
                    MdmProperties.TREE_ID_BY_COMMON_ENTITY_TYPE[CommonEntityTypeEnum.MDM_NAVIGATION_TREE_NODE]!!,
                    1, 2
                )
        }
    }
}
