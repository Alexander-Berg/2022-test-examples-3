package ru.yandex.market.mdm.storage.repository.index

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.Before
import org.junit.Test
import ru.yandex.inside.yt.kosher.common.YtTimestamp
import ru.yandex.market.mbo.yt.index.read.Direction
import ru.yandex.market.mdm.http.entity.SaveMdmEntityResult
import ru.yandex.market.mdm.lib.model.mdm.I18nStrings
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValue
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValues
import ru.yandex.market.mdm.lib.model.mdm.MdmEntity
import ru.yandex.market.mdm.lib.model.mdm.ProtoEntity
import ru.yandex.market.mdm.storage.repository.MdmEntityIntegrationTestBase
import ru.yandex.market.mdm.storage.repository.NamedMdmField
import ru.yandex.market.mdm.storage.repository.filter.IndexFieldValues
import ru.yandex.market.mdm.storage.repository.filter.MdmEntityStorageFilter
import kotlin.random.Random

class MdmSingleEntityIndexTest : MdmEntityIntegrationTestBase() {

    private lateinit var mskuIdIndexField: NamedMdmField
    private lateinit var businessIdIndexField: NamedMdmField
    private lateinit var sskuIdIndexField: NamedMdmField

    @Before
    fun before() {
        random = Random(5603087122L)
        super.setUp()
        mskuIdIndexField = NamedMdmField(settings.tableModels[0].primaryIndexModel.searchColumns[0].name, MdmAttributeDataType.INT64)
        businessIdIndexField = NamedMdmField(settings.tableModels[0].secondaryIndexModels[0].searchColumns[0].name, MdmAttributeDataType.INT64)
        sskuIdIndexField = NamedMdmField(settings.tableModels[0].secondaryIndexModels[0].searchColumns[1].name, MdmAttributeDataType.STRING )
    }

    @Test
    fun testInsertUpdatesIndex() {
        // given
        val mskuId = idGenerator.id
        val entity = mskuEntity(mdmId = idGenerator.id, mskuId = mskuId, from = 0)
        repository.save(entity.toProto(), eventContext)

        // when
        val found = repository.find(mskuIdFilter(mskuId))

        // then
        found shouldNotBe emptyList<ProtoEntity>()
        wipeVersions(found[0]) shouldBe wipeVersions(entity.toProto())
    }

    @Test
    fun testCompositeIndex() {
        // given
        val sskuId = I18nStrings.fromRu("a\\b\\c\\").ru()
        val businessId = idGenerator.id
        val mskuId = random.nextLong()
        val entity = sskuEntity(
            mdmId = idGenerator.id,
            from = 0,
            sskuId = sskuId.string,
            businessId = businessId,
            mskuId = mskuId
        )
        val otherSskuId = I18nStrings.fromRu("\\e\\f\\g\\t'ololo\\n").ru()
        val otherBusinessId = idGenerator.id
        val otherEntity = sskuEntity(
            mdmId = idGenerator.id,
            from = 0,
            sskuId = otherSskuId.string,
            businessId = otherBusinessId,
            mskuId = idGenerator.id
        )

        repository.save(entity.toProto(), eventContext)
        repository.save(otherEntity.toProto(), eventContext)
        val filter = MdmEntityStorageFilter().idxIn(
            listOf(
                IndexFieldValues(businessIdIndexField, listOf(businessId, otherBusinessId)),
                IndexFieldValues(sskuIdIndexField, listOf(sskuId.string, otherSskuId.string)),
            )
        ).setTimeMarker(YtTimestamp.SYNC_LAST_COMMITTED) as MdmEntityStorageFilter

        // when
        val found = repository.find(filter)

        // then
        found shouldNotBe emptyList<ProtoEntity>()
        wipeVersions(found[0]) shouldBe wipeVersions(entity.toProto())
        wipeVersions(found[1]) shouldBe wipeVersions(otherEntity.toProto())
    }

    @Test
    fun testCompositeIndexShouldNotFoundCrossedValues() {
        // given
        val sskuId = I18nStrings.fromRu("125L").ru()
        val businessId = idGenerator.id
        val entity = sskuEntity(
            mdmId = idGenerator.id,
            from = 0,
            sskuId = sskuId.string,
            businessId = businessId)

        val otherSskuId = I18nStrings.fromRu("126L").ru()
        val otherBusinessId = idGenerator.id
        val otherEntity = sskuEntity(
            mdmId = idGenerator.id,
            from = 0,
            sskuId = sskuId.string,
            businessId = businessId)

        repository.save(entity.toProto(), eventContext)
        repository.save(otherEntity.toProto(), eventContext)
        val filter = MdmEntityStorageFilter().idxIn(
            listOf(
                IndexFieldValues(businessIdIndexField, listOf(businessId, otherBusinessId)),
                IndexFieldValues(sskuIdIndexField, listOf(otherSskuId.string, sskuId.string)),
            )
        ).setTimeMarker(YtTimestamp.SYNC_LAST_COMMITTED) as MdmEntityStorageFilter

        // when
        val found = repository.find(filter)

        // then
        found shouldBe emptyList()
    }

    @Test
    fun testPartialIndexUsedWhenNotAllFieldsPresented() {
        // given
        val sskuId = I18nStrings.fromRu("121L").ru()
        val businessId = idGenerator.id
        val entity = sskuEntity(
            mdmId = idGenerator.id,
            from = 0,
            sskuId = sskuId.string,
            businessId = businessId)

        repository.save(entity.toProto(), eventContext)
        val filter = MdmEntityStorageFilter().idxIn(
            listOf(
                IndexFieldValues(businessIdIndexField, listOf(businessId)),
            )
        ).setTimeMarker(YtTimestamp.SYNC_LAST_COMMITTED) as MdmEntityStorageFilter

        // when
        val found = repository.find(filter)

        // then
        found shouldNotBe emptyList<ProtoEntity>()
        wipeVersions(found[0]) shouldBe wipeVersions(entity.toProto())
    }

    @Test
    fun testUpdateUpdatesIndex() {
        // given
        val mskuId = idGenerator.id
        val entity = mskuEntity(idGenerator.id, 0, mskuId)
        val updatedVersion = repository.save(entity.toProto(), eventContext).from

        val updatedMsku = idGenerator.id
        val updatedEntity = mskuEntity(entity.mdmId, updatedVersion, updatedMsku)

        // when
        repository.save(updatedEntity.toProto(), eventContext)
        val found = repository.find(mskuIdFilter(updatedMsku))

        // then
        found shouldNotBe emptyList<ProtoEntity>()
        wipeVersions(found[0]) shouldBe wipeVersions(updatedEntity.toProto())
    }

    @Test
    fun testMissingAttributeYieldsError() {
        // given
        val entity = sskuEntity(
            mdmId = idGenerator.id,
            from = 0,
            mskuId = null,
            sskuId = null,
            businessId = null)
        // when
        val error = repository.save(entity.toProto(), eventContext)
        // then
        error.code shouldBe SaveMdmEntityResult.Code.ERROR
        error.details shouldContain "missing required indexed attribute"
    }

    @Test
    fun testAttributeChangeRemovesOldIndex() {
        // given
        val oldMsku = idGenerator.id
        val entity = mskuEntity(idGenerator.id, 0, oldMsku)
        val updatedVersion = repository.save(entity.toProto(), eventContext).from

        val updatedMsku = idGenerator.id
        val updatedEntity = mskuEntity(entity.mdmId, updatedVersion, updatedMsku)

        // when 1
        repository.save(updatedEntity.toProto(), eventContext)

        // then 1
        repository.find(mskuIdFilter(oldMsku)) shouldBe emptyList()

        // when 2
        val found = repository.find(mskuIdFilter(updatedMsku))

        // then 2
        found shouldNotBe emptyList<ProtoEntity>()
        wipeVersions(found[0]) shouldBe wipeVersions(updatedEntity.toProto())
    }

    @Test
    fun testEntityChangeOnSameIndexOverridesExistingBinding() {
        // given
        val mskuId = idGenerator.id
        val entity = mskuEntity(idGenerator.id, 0, mskuId)
        val version = repository.save(entity.toProto(), eventContext).from
        val otherEntity = mskuEntity(idGenerator.id, version, mskuId)
        // when
        repository.save(otherEntity.toProto(), eventContext)
        val found = repository.find(mskuIdFilter(mskuId))

        // then
        found.size shouldBe 1
        wipeVersions(found[0]) shouldBe wipeVersions(otherEntity.toProto())
    }

    @Test
    fun testSearchByMissingIndex() {
        val mskuId = idGenerator.id
        val entity = mskuEntity(idGenerator.id, 0, mskuId)
        repository.save(entity.toProto(), eventContext)
        repository.find(mskuIdFilter(mskuId + 10000000000)) shouldBe emptyList()
    }

    @Test
    fun testSwapIndexValueWithinBatch() {
        // given
        val msku1 = idGenerator.id
        val msku2 = idGenerator.id
        val entity1 = mskuEntity(idGenerator.id, 0, msku1)
        val entity2 = mskuEntity(idGenerator.id, 0, msku2)
        val updatedVersion = repository.save(listOf(entity1.toProto(), entity2.toProto()), eventContext)[0].from

        // свопаем mskuId у этих двух энтитей
        val updatedEntity1 = mskuEntity(entity1.mdmId, updatedVersion, msku2)
        val updatedEntity2 = mskuEntity(entity2.mdmId, updatedVersion, msku1)

        // when
        repository.save(listOf(updatedEntity1.toProto(), updatedEntity2.toProto()), eventContext)

        // then
        val foundByMsku1 = wipeVersions(repository.find(mskuIdFilter(msku1))[0])
        val foundByMsku2 = wipeVersions(repository.find(mskuIdFilter(msku2))[0])
        // помним, что сущности свопнуты относительно mskuId
        foundByMsku1 shouldBe wipeVersions(updatedEntity2.toProto())
        foundByMsku2 shouldBe wipeVersions(updatedEntity1.toProto())
        foundByMsku1.mdmAttributeValuesMap[mskuIdAttributeId]!!.valuesList[0].int64 shouldBe msku1
        foundByMsku2.mdmAttributeValuesMap[mskuIdAttributeId]!!.valuesList[0].int64 shouldBe msku2
    }

    @Test
    fun testFindPaginatedEntity() {
        // given
        val msku1 = idGenerator.id
        val msku2 = idGenerator.id
        val newEntity1 = mskuEntity(idGenerator.id, 0, msku1).toProto()
        val newEntity2 = mskuEntity(idGenerator.id, 0, msku2).toProto()
        repository.save(newEntity1, eventContext)
        repository.save(newEntity2, eventContext)

        val firstPageFilter = MdmEntityStorageFilter()
        val secondPageFilter = MdmEntityStorageFilter()
        firstPageFilter.addOrderBy(mskuIdIndexField, Direction.ASC)
        secondPageFilter.addOrderBy(mskuIdIndexField, Direction.ASC)
        firstPageFilter.limit = 1
        firstPageFilter.offset = 0
        secondPageFilter.limit = 1
        secondPageFilter.offset = 1

        // when
        val firstPage = repository.find(firstPageFilter)
        val secondPage = repository.find(secondPageFilter)

        // then
        firstPage.size shouldBe 1
        secondPage.size shouldBe 1
        firstPage shouldNotBe secondPage
    }

    @Test
    fun testEntityDeduplicationWhenUseOne2ManyCartesian() {
        // given
        val silverRepository =
            ytTableManager.getMdmEntityRepositoryForMdmEntityType(settings.tableModels[1].mdmEntityTypeId)

        val serviceIdIndexField = NamedMdmField(
            name = settings.tableModels[1].secondaryIndexModels[0].searchColumns[0].name,
            type = MdmAttributeDataType.INT64
        )
        val shopSkuIndexField = NamedMdmField(
            name = settings.tableModels[1].secondaryIndexModels[0].searchColumns[1].name,
            type = MdmAttributeDataType.STRING
        )
        val sourceTypeIndexField = NamedMdmField(
            name = settings.tableModels[1].secondaryIndexModels[0].searchColumns[2].name,
            type = MdmAttributeDataType.STRING
        )
        val sourceIdIndexField = NamedMdmField(
            name = settings.tableModels[1].secondaryIndexModels[0].searchColumns[3].name,
            type = MdmAttributeDataType.STRING
        )

        val silverSskuTableModel = settings.tableModels[1]
        val businessIdAttrId = silverSskuTableModel.primaryIndexModel.searchColumns[0].mdmAttributePath[0]
        val shopSkuAttrId = silverSskuTableModel.primaryIndexModel.searchColumns[1].mdmAttributePath[0]
        val sourceTypeAttrId = silverSskuTableModel.primaryIndexModel.searchColumns[2].mdmAttributePath[0]
        val sourceIdAttrId = silverSskuTableModel.primaryIndexModel.searchColumns[3].mdmAttributePath[0]
        val serviceIdAttrId = settings.tableModels[1].secondaryIndexModels[0].searchColumns[0].mdmAttributePath[2]
        val serviceSskuAttrId = settings.tableModels[1].secondaryIndexModels[0].searchColumns[0].mdmAttributePath[1]
        val silverSskuAttrId = settings.tableModels[1].secondaryIndexModels[0].searchColumns[0].mdmAttributePath[0]

        val businessId = idGenerator.id
        val serviceId1 = idGenerator.id
        val serviceId2 = idGenerator.id
        val shopSku = "Pu-239"
        val sourceType = "WAREHOUSE"
        val sourceId = "145"

        val mdmEntity = MdmEntity(
            mdmId = idGenerator.id,
            mdmEntityTypeId = settings.tableModels[1].mdmEntityTypeId,
            values = mapOf(
                businessIdAttrId to MdmAttributeValues(
                    mdmAttributeId = businessIdAttrId,
                    values = listOf(MdmAttributeValue(int64 = businessId))
                ),
                shopSkuAttrId to MdmAttributeValues(
                    mdmAttributeId = shopSkuAttrId,
                    values = listOf(MdmAttributeValue(string = I18nStrings.fromRu(shopSku)))
                ),
                sourceTypeAttrId to MdmAttributeValues(
                    mdmAttributeId = sourceTypeAttrId,
                    values = listOf(MdmAttributeValue(string = I18nStrings.fromRu(sourceType)))
                ),
                sourceIdAttrId to MdmAttributeValues(
                    mdmAttributeId = sourceIdAttrId,
                    values = listOf(MdmAttributeValue(string = I18nStrings.fromRu(sourceId)))
                ),
                silverSskuAttrId to MdmAttributeValues(
                    mdmAttributeId = silverSskuAttrId,
                    values = listOf(
                        MdmAttributeValue(
                            struct = MdmEntity(
                                mdmEntityTypeId = 0,
                                values = mapOf(
                                    serviceSskuAttrId to MdmAttributeValues(
                                        mdmAttributeId = serviceSskuAttrId,
                                        values = listOf(
                                            MdmAttributeValue(
                                                struct = MdmEntity(
                                                    mdmEntityTypeId = 0,
                                                    values = mapOf(
                                                        serviceIdAttrId to MdmAttributeValues(
                                                            mdmAttributeId = serviceIdAttrId,
                                                            values = listOf(MdmAttributeValue(int64 = serviceId1))
                                                        )
                                                    )
                                                )
                                            )
                                        ),
                                    )
                                )
                            )
                        ),
                        MdmAttributeValue(
                            struct = MdmEntity(
                                mdmEntityTypeId = 0,
                                values = mapOf(
                                    serviceSskuAttrId to MdmAttributeValues(
                                        mdmAttributeId = serviceSskuAttrId,
                                        values = listOf(
                                            MdmAttributeValue(
                                                struct = MdmEntity(
                                                    mdmEntityTypeId = 0,
                                                    values = mapOf(
                                                        serviceIdAttrId to MdmAttributeValues(
                                                            mdmAttributeId = serviceIdAttrId,
                                                            values = listOf(MdmAttributeValue(int64 = serviceId2))
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        silverRepository.save(mdmEntity.toProto(), eventContext)

        // when
        val filter = MdmEntityStorageFilter().idxIn(
            listOf(
                IndexFieldValues(serviceIdIndexField, listOf(serviceId1, serviceId2)),
                IndexFieldValues(shopSkuIndexField, listOf(shopSku, shopSku)),
                IndexFieldValues(sourceTypeIndexField, listOf(sourceType, sourceType)),
                IndexFieldValues(sourceIdIndexField, listOf(sourceId, sourceId))
            )
        ).setTimeMarker(YtTimestamp.SYNC_LAST_COMMITTED) as MdmEntityStorageFilter
        val searchResult = silverRepository.find(filter)

        //then
        searchResult shouldHaveSize 1
        wipeVersions(searchResult[0]) shouldBe wipeVersions(mdmEntity.toProto())
    }

    private fun mskuIdFilter(mskuId: Long): MdmEntityStorageFilter {
        val filter = MdmEntityStorageFilter().idxIn(IndexFieldValues(mskuIdIndexField, listOf(mskuId)))
            .setTimeMarker(YtTimestamp.SYNC_LAST_COMMITTED)
        return filter as MdmEntityStorageFilter
    }
}
