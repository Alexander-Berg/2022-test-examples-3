import io.kotest.assertions.asClue
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
import ru.yandex.market.mdm.fixtures.randomId
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.lib.model.mdm.MdmAttribute
import ru.yandex.market.mdm.service.common_entity.config.TestMetadataConfig
import ru.yandex.market.mdm.service.common_entity.controller.CommonEntityDispatcher
import ru.yandex.market.mdm.service.common_entity.controller.MetadataApiController
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
import ru.yandex.market.mdm.service.common_entity.service.constructor.MdmAttributeCommonEntityService
import ru.yandex.market.mdm.service.common_entity.service.constructor.converters.AttributeConverter
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmAttributeSearchFilter
import ru.yandex.market.mdm.service.common_entity.testutils.CommonEntityBaseTestClass
import ru.yandex.market.mdm.service.common_entity.util.commonEnumValue

@Import(TestMetadataConfig::class)
class ApiControllerMdmAttributeTest : CommonEntityBaseTestClass() {

    @Autowired
    lateinit var mdmAttributeCommonEntityService: MdmAttributeCommonEntityService

    @Autowired
    lateinit var mdmAttributeMetadataService : OneElementMetadataService<MdmAttribute, MdmAttributeSearchFilter>

    lateinit var controller: MetadataApiController

    @Before
    fun init() {
        controller = MetadataApiController(CommonEntityDispatcher(listOf(
            mdmAttributeCommonEntityService as CommonEntityService<SearchFilter>
        )))
    }

    @Test
    fun `successful load metadata for MDM_ATTR for EDITOR widget`() {
        val metadataResponse = controller.metadata(
            MetadataRequest(
            WidgetType.EDITOR, CommonEntityTypeEnum.MDM_ATTR, "latest")
        )
        val paramsMap = metadataResponse.commonEntityType.commonParams!!.associateBy { it.commonParamName }

        assertSoftly {
            metadataResponse.widgetType shouldBe WidgetType.EDITOR
            metadataResponse.commonEntityType.commonEntityTypeEnum shouldBe CommonEntityTypeEnum.MDM_ATTR
            paramsMap.keys shouldContainAll listOf("mdm_attr_reference", "mdm_entity_type_reference", "internal_name",
                "ru_title", "data_type", "is_multivalue", "version_from", "version_to", "version_status", "struct_type_reference")
            paramsMap["mdm_attr_reference"]!!.asClue {
                it.commonParamValueType shouldBe CommonParamValueType.NUMERIC
                it.readOnly shouldBe true
                it.required shouldBe false
                it.ruTitle shouldBe "Id атрибута"
            }
            paramsMap["mdm_entity_type_reference"]!!.asClue {
                it.commonParamValueType shouldBe CommonParamValueType.NUMERIC
                it.readOnly shouldBe false
                it.required shouldBe true
                it.ruTitle shouldBe "Id типа сущности"
            }
        }
    }

    @Test
    fun `find MDM_ATTR for EDITOR widget`() {
        // given
        val attribute = mdmAttribute()
        mdmAttributeMetadataService.update(updates = listOf(attribute), commitMessage = "test data")

        val request = FindRequest(
            WidgetType.EDITOR,
            CommonEntity(
                commonEntityType = CommonEntityType(CommonEntityTypeEnum.MDM_ATTR),
                commonParamValues = listOf(MdmProtocol.REQUEST_ENTITY_ID.byLong(attribute.mdmId))
            ),
            "latest"
        )

        // when
        val response = controller.find(request)

        // then
        assertSoftly {
            response.commonEntities.size shouldBe 1
            response.commonEntities[0].asClue {
                it.commonEntityType.commonEntityTypeEnum shouldBe CommonEntityTypeEnum.MDM_ATTR
                it.getLongValue("mdm_attr_reference")!! shouldBe attribute.mdmId
                it.getParamNames() shouldContainAll listOf("internal_name", "ru_title", "data_type", "is_multivalue",
                    "version_from", "version_to", "version_status", "struct_type_reference", "mdm_entity_type_reference")
            }
            mdmAttributeMetadataService.lastUsedFilter!!.ids shouldContain attribute.mdmId
        }
    }

    @Test
    fun `find MDM_ATTR for EDITOR new instance with pre-fill mdm_entity_type_reference`() {
        // given
        val entityTypeId = randomId()
        val request = FindRequest(
            WidgetType.EDITOR,
            CommonEntity(
                commonEntityType = CommonEntityType(CommonEntityTypeEnum.MDM_ATTR),
                commonParamValues = listOf(
                    MdmProtocol.REQUEST_ENTITY_ID.byLong(entityTypeId),
                    MdmProtocol.REQUEST_COMMON_ENTITY_TYPE.byEnum(commonEnumValue(CommonEntityTypeEnum.MDM_ENTITY_TYPE)),
                    MdmProtocol.REQUEST_NEW.byBool(true)
                )
            ),
            "latest"
        )

        // when
        val response = controller.find(request)

        // then
        assertSoftly {
            response.commonEntities.size shouldBe 1
            response.commonEntities[0].asClue {
                it.commonEntityType.commonEntityTypeEnum shouldBe CommonEntityTypeEnum.MDM_ATTR
                it.getLongValue("mdm_entity_type_reference") shouldBe entityTypeId
            }
        }
    }

    @Test
    fun `should save MDM_ATTR`() {
        // given
        val initialAttribute = mdmAttribute()
        mdmAttributeMetadataService.update(updates = listOf(initialAttribute), commitMessage = "test data")

        val updatedAttribute = initialAttribute.copy(ruTitle = "new title")
        val requestCommonEntity = AttributeConverter().mdm2common(updatedAttribute, CommonEntityType(CommonEntityTypeEnum.MDM_ATTR))

        // when
        val response = controller.save(SaveRequest(listOf(requestCommonEntity), WidgetType.EDITOR))
        val after = mdmAttributeMetadataService.lastEntity

        // then
        assertSoftly {
            after shouldNotBe null
            response.commonEntities[0].getStringValue("ru_title") shouldBe "new title"
            after!!.ruTitle shouldBe "new title"
        }
    }
}
