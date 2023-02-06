package ru.yandex.market.mdm.service.common_entity.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.given
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEntity
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmTreeEdge
import ru.yandex.market.mdm.lib.util.MdmProperties
import ru.yandex.market.mdm.service.common_entity.model.BinaryBooleanFunction
import ru.yandex.market.mdm.service.common_entity.model.CommonEntity
import ru.yandex.market.mdm.service.common_entity.model.CommonEntityType
import ru.yandex.market.mdm.service.common_entity.model.CommonFilter
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValue
import ru.yandex.market.mdm.service.common_entity.model.CommonPredicate
import ru.yandex.market.mdm.service.common_entity.model.UpdateContext
import ru.yandex.market.mdm.service.common_entity.service.arm.MdmEntityCommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.arm.enrichers.NoopCommonEntityEnricher
import ru.yandex.market.mdm.service.common_entity.service.arm.filters.MdmEntitySearchFilter
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.MetadataFactory
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.model.FlatMdmMetadata
import ru.yandex.market.mdm.service.common_entity.service.arm.storage.entity.StorageService
import ru.yandex.market.mdm.service.common_entity.service.common.filters.SearchFilter
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.MetadataService
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmEntityTypeSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmTreeEdgeSearchFilter
import ru.yandex.market.mdm.service.common_entity.testutils.CommonEntityBaseTestClass
import java.math.BigDecimal

class MdmEntityCommonEntityServiceTest : CommonEntityBaseTestClass() {

    @Mock
    private lateinit var mdmEntityTypeMetadataService: MetadataService<MdmEntityType, MdmEntityTypeSearchFilter>

    @Mock
    private lateinit var treeEdgeSearchMetadataService: MetadataService<MdmTreeEdge, MdmTreeEdgeSearchFilter>

    @Mock
    private lateinit var storageService: StorageService

    @Mock
    private lateinit var metadataFactory : MetadataFactory

    private lateinit var entityService: MdmEntityCommonEntityService

    @Before
    fun init() {
        entityService = MdmEntityCommonEntityService(
            mdmEntityTypeMetadataService = mdmEntityTypeMetadataService,
            treeEdgeSearchMetadataService = treeEdgeSearchMetadataService,
            storageService = storageService,
            mdmMetadataFactory = metadataFactory,
            byRelationEnricher = NoopCommonEntityEnricher(),
        )

        given(mdmEntityTypeMetadataService.findByFilter(any())).willReturn(listOf(mdmEntityType()))
    }

    @Test
    fun `findTree without ids should return root and its children`() {
        // given
        val treeId = MdmProperties.TREE_ID_BY_COMMON_ENTITY_TYPE[CommonEntityTypeEnum.MDM_ENTITY]!!
        val entityType = mdmEntityType()

        given(treeEdgeSearchMetadataService.findByFilter(MdmTreeEdgeSearchFilter(treeId, 0)))
            .willReturn(listOf(MdmTreeEdge(treeId, entityType.mdmId, 0)))

        val filter = MdmEntitySearchFilter(emptyList(), 1234, SearchFilter.Version(), false)

        // when
        val response = entityService.findTree(filter)

        // then
        val root = response.first().commonParamValues!!.associateBy { it.commonParamName }
        root["tree_node_id"]!!.numerics!!.first().intValueExact() shouldBe 0
        root["tree_node_title"]!!.strings!!.first() shouldBe "ARM"
        root["parent_id"]!!.numerics!!.isEmpty() shouldBe true

        val commonEntityType = response[1]
        commonEntityType shouldNotBe null
        val entityParams = commonEntityType.commonParamValues!!.associateBy { it.commonParamName }
        entityParams["tree_node_title"]!!.strings!!.first() shouldBe entityType.ruTitle
        entityParams["parent_id"]!!.numerics!!.first().intValueExact() shouldBe 0
    }

    @Test
    fun `findTable with filter should return list commonEntities`() {
        // given
        val returnedEntity = mdmEntity()
        val mdmEntityType = mdmEntityType(attributes = listOf(mdmAttribute(internalName = "category_id")))
        val metadata = FlatMdmMetadata(mdmEntityType, listOf(mdmEntityType), listOf())
        given(storageService.findByFilter(any())).willReturn(listOf(returnedEntity))
        given(metadataFactory.create(any(), any(), any())).willReturn(metadata)

        val commonParamValues: ArrayList<CommonParamValue> = ArrayList()
        commonParamValues.add(
            CommonParamValue(
                commonParamName = "entity_type_id",
                numerics = listOf(BigDecimal("34657"))
            )
        )
        val commonEntity = CommonEntity(
            123,
            CommonEntityType(CommonEntityTypeEnum.MDM_ENTITY),
            commonParamValues,
            CommonFilter(
                listOf(
                    listOf(
                        CommonPredicate(
                            CommonParamValue.byInt64("category_id", 34561L),
                            BinaryBooleanFunction.EQ
                        )
                    )
                )
            )
        )

        // when
        val response = entityService.findTable(entityService.convertFilter(commonEntity))

        // then
        response.size shouldBe 1
        response.first().entityId shouldBe returnedEntity.mdmId
    }

    @Test
    fun `saveSingle should return entity copy`() {
        //given
        val commonParamValues: List<CommonParamValue> = listOf(
            CommonParamValue(
                commonParamName = "entity_type_id",
                numerics = listOf(BigDecimal("34657"))
            ),
            CommonParamValue(
                commonParamName = "request_entity_id",
                numerics = listOf(BigDecimal("34561"))
            )
        )
        val commonEntity = CommonEntity(
            123, CommonEntityType(CommonEntityTypeEnum.MDM_ENTITY),
            commonParamValues
        )
        val mdmEntityType = mdmEntityType(mdmId = 34657)
        given(metadataFactory.create(any(), any(), any())).willReturn(FlatMdmMetadata(mdmEntityType, listOf(), listOf()))

        // when
        val response = entityService.updateSingle(commonEntity, UpdateContext())

        // then
        response.errors.size shouldBe 0
        response.commonEntities.size shouldBe 1
        response.commonEntities.get(0) shouldBe commonEntity
    }
}
