package ru.yandex.market.mdm.storage.repository

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import ru.yandex.market.mdm.http.entity.SaveMdmEntityResult
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValue
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValues
import ru.yandex.market.mdm.lib.model.mdm.MdmEntity
import ru.yandex.market.mdm.lib.model.mdm.MdmStorageStateTracker
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import ru.yandex.market.mdm.lib.model.mdm.ProtoEntity
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MdmEntityRepositoryNoAuditTest : MdmEntityIntegrationTestBase() {

    private var executor: ExecutorService? = null

    @Before
    fun before() {
        random = Random(1064320L)
        super.setUp()
        mdmEntityTypeId = settings.tableModels[2].mdmEntityTypeId
        mskuIdAttributeId = settings.tableModels[2].primaryIndexModel.searchColumns[0].mdmAttributePath[0]
        businessIdAttributeId = settings.tableModels[2].secondaryIndexModels[0].searchColumns[0].mdmAttributePath[0]
        sskuIdAttributeId = settings.tableModels[2].secondaryIndexModels[0].searchColumns[1].mdmAttributePath[0]
        repository = ytTableManager.getMdmEntityRepositoryForMdmEntityType(mdmEntityTypeId)
    }

    @After
    fun after() {
        executor?.shutdownNow()
    }

    @Test
    fun testNoSnapshots() {
        val exception = shouldThrowAny {
            repository.findSnapshot(listOf())
        }
        exception.message shouldBe "Storage for requested entity type does not support snapshots"
    }

    @Test
    fun testInsert() {
        // given
        val epochMillisBeforeSave = Instant.now().toEpochMilli()
        val entity = entity(idGenerator.id, 0L).toProto()
        repository.find(entity.mdmId) shouldBe null

        // when
        val saveResult = repository.save(entity, eventContext)

        // then
        saveResult.code shouldBe SaveMdmEntityResult.Code.OK
        val stored = repository.find(entity.mdmId)
        stored shouldNotBe null
        stored!!.mdmUpdateMeta.from shouldBe saveResult.from
        saveResult.from shouldBeGreaterThanOrEqual epochMillisBeforeSave

        val storedData = wipeVersions(stored)
        storedData shouldBe entity
    }

    @Test
    fun testInsertNewEntity() {
        // given
        val newEntity = entity(0L, 0L).toProto()

        // when
        val saveResult = repository.save(newEntity, eventContext)

        // then
        saveResult.code shouldBe SaveMdmEntityResult.Code.OK
        saveResult.mdmId shouldBeGreaterThan 0L
        val stored = repository.find(saveResult.mdmId)
        stored shouldNotBe null
    }

    @Test
    fun testInsertNewGeneratesId() {
        // given
        val entity = entity(0L, 0L).toProto()

        // when
        val saveResult = repository.save(entity, eventContext)

        // then
        saveResult.code shouldBe SaveMdmEntityResult.Code.OK
        saveResult.mdmId shouldBeGreaterThan 0L

        val id = saveResult.mdmId
        wipeVersions(repository.find(id)!!) shouldBe wipeVersions(entity.toBuilder().setMdmId(id).build())
    }

    @Test
    fun testBatchUpdateWithNewEntitiesGeneratesId() {
        // given
        val entityNew = entity(0L, 0L).toProto()
        val entityOld1 = entity(idGenerator.id, 0L).toProto()
        val entityOld2 = entity(idGenerator.id, 0L).toProto()

        // when 1
        val saveResultNew = repository.save(listOf(entityOld1, entityNew, entityOld2), eventContext)[1]

        // then 1
        saveResultNew.code shouldBe SaveMdmEntityResult.Code.OK
        saveResultNew.mdmId shouldBeGreaterThan 0L

        val id = saveResultNew.mdmId
        wipeVersions(repository.find(id)!!) shouldBe wipeVersions(entityNew.toBuilder().setMdmId(id).build())
        wipeVersions(repository.find(entityOld1.mdmId)!!) shouldBe wipeVersions(entityOld1)
        wipeVersions(repository.find(entityOld2.mdmId)!!) shouldBe wipeVersions(entityOld2)
    }

    @Test
    fun testUpdate() {
        // given
        val entity = entity(idGenerator.id, 0L).toProto()
        val saveResult = repository.save(entity, eventContext)
        saveResult.code shouldBe SaveMdmEntityResult.Code.OK

        // when
        // обновим сущность - просто перегенерим все парамы, но оставим старый ИД и актуальную версию.
        val updatedEntity = entity(entity.mdmId, saveResult.from).toProto()
        repository.save(updatedEntity, eventContext).code shouldBe SaveMdmEntityResult.Code.OK

        // then
        val stored = wipeVersions(repository.find(entity.mdmId)!!)
        val expected = wipeVersions(updatedEntity)
        stored shouldBe expected
    }

    @Test
    fun testConcurrentUpdate() {
        // given
        val entity = entity(idGenerator.id, 0L).toProto()
        val saveResult = repository.save(entity, eventContext)
        saveResult.code shouldBe SaveMdmEntityResult.Code.OK

        // when + then
        // повторное сохранение исходной сущности со старой версией должно выдать ошибку параллельного апдейта
        repository.save(entity, eventContext).code shouldBe SaveMdmEntityResult.Code.CONCURRENT_UPDATE
    }

    @Test
    fun testBatchInsert() {
        // given
        val entities = List(100) { entity(idGenerator.id, 0L).toProto() }

        // when
        val saveResults = repository.save(entities, eventContext)

        // then 1
        // Проверим контракт, что статусы возвращаются ровно в том порядке, в котором подавали сущности на вход
        saveResults.size shouldBe entities.size
        saveResults.zip(entities).forEach { (result, expectedEntity) ->
            result.code shouldBe SaveMdmEntityResult.Code.OK
            result.mdmId shouldBe expectedEntity.mdmId
            result.from shouldBeGreaterThan expectedEntity.mdmUpdateMeta.from
        }

        // then 2
        // И сравним сами объекты
        val allStored = repository.find(entities.map { it.mdmId })
        val expectedEntities = entities.map { it.mdmId to wipeVersions(it) }.toMap()

        allStored.size shouldBe entities.size
        allStored.map { wipeVersions(it) }.forEach { stored ->
            val expected = expectedEntities[stored.mdmId]!!
            stored shouldBe expected
        }
    }

    @Test
    fun testBatchUpdate() {
        // given
        val entities = List(100) { entity(idGenerator.id, 0L).toProto() }
        repository.save(entities, eventContext).forEach { it.code shouldBe SaveMdmEntityResult.Code.OK }

        val allStored = repository.find(entities.map { it.mdmId })
        val updated = allStored.map { stored ->
            entity(stored.mdmId, stored.mdmUpdateMeta.from).toProto()
        }

        // when
        repository.save(updated, eventContext).forEach { it.code shouldBe SaveMdmEntityResult.Code.OK }

        // then
        val actuals = repository.find(entities.map { it.mdmId }).map { wipeVersions(it) }
        val expectedEntities = updated.map { it.mdmId to wipeVersions(it) }.toMap()

        actuals.size shouldBe entities.size
        actuals.map { wipeVersions(it) }.forEach { actual ->
            val expected = expectedEntities[actual.mdmId]!!
            actual shouldBe expected
        }
    }

    @Test
    fun testBatchDeleteTrue() {
        // given
        // Заметка: речь идёт об убиении модельки через версию "to", а не о физической вычистке строки из БД.
        val entities = List(100) { entity(idGenerator.id, 0L) }
        val results = repository.save(entities.map { it.toProto() }, eventContext)
        results.forEach { it.code shouldBe SaveMdmEntityResult.Code.OK }

        val toDelete = entities.zip(results).map { (entity, result) ->
            entity.toBuilder().version(
                version = MdmVersion(
                    result.from,
                    result.from + 1
                )
            ).build().toProto()
        }

        // when
        repository.save(toDelete, eventContext).forEach { it.code shouldBe SaveMdmEntityResult.Code.OK }

        // then
        val actuals = repository.find(entities.map { it.mdmId })
        actuals.size shouldBe 0
    }

    @Test
    fun testMixedBatchInsertUpdateDelete() {
        // given
        val updateEntitySource = entity(idGenerator.id, 0L)
        val deleteEntitySource = entity(idGenerator.id, 0L)

        val insertEntity = entity(idGenerator.id, 0L).toProto()
        val updateEntity = repository.save(updateEntitySource.toProto(), eventContext).let { result ->
            result.code shouldBe SaveMdmEntityResult.Code.OK
            entity(updateEntitySource.mdmId, result.from)
        }.toProto()
        val deleteEntity = repository.save(deleteEntitySource.toProto(), eventContext).let { result ->
            result.code shouldBe SaveMdmEntityResult.Code.OK
            deleteEntitySource.toBuilder().version(MdmVersion(result.from, result.from + 1)).build()
        }.toProto()

        // when
        repository.save(listOf(insertEntity, updateEntity, deleteEntity), eventContext).forEach { result ->
            result.code shouldBe SaveMdmEntityResult.Code.OK
        }

        // then
        wipeVersions(repository.find(insertEntity.mdmId)!!) shouldBe wipeVersions(insertEntity)
        wipeVersions(repository.find(updateEntity.mdmId)!!) shouldBe wipeVersions(updateEntity)
        repository.find(deleteEntity.mdmId) shouldBe null
    }

    @Test
    fun testConcurrentUpdateSingleWriterWithBatch() {
        // given
        val entities = List(100) { entity(idGenerator.id, 0L).toProto() }
        repository.save(entities, eventContext).forEach { it.code shouldBe SaveMdmEntityResult.Code.OK }

        val allStored = repository.find(entities.map { it.mdmId })

        // одной сущности выставим некорректную версию, чтобы спровоцировать падение по CONCURRENT_UPDATE.
        val failingIndex = 17
        val updated = allStored.mapIndexed { idx, stored ->
            val addition = if (idx == failingIndex) 1L else 0L
            randomMdmEntityWithSpecificAttributeVersion(stored, addition)
        }

        // when + then
        repository.save(updated, eventContext).forEachIndexed { idx, result ->
            if (idx == failingIndex) {
                result.code shouldBe SaveMdmEntityResult.Code.CONCURRENT_UPDATE
            } else {
                result.code shouldBe SaveMdmEntityResult.Code.NO_OP
            }
        }
    }

    @Test
    fun testConcurrentUpdateTwoWriters() {
        val executor = Executors.newFixedThreadPool(2)

        // given
        val entity = entity(idGenerator.id, 0L).toProto()
        val saveResult = repository.save(entity, eventContext)
        val updatedEntity = entity(entity.mdmId, saveResult.from)
        val successfulThreadBlocker = CountDownLatch(1)
        val failingThreadBlocker = CountDownLatch(1)

        val results = ConcurrentHashMap<Int, SaveMdmEntityResult.Code>()

        // when
        // В первом потоке попытаемся проапдейтить сущность, но нарочно подзависнем перед сохранением
        val hungTask = executor.submit {
            val artificialHangUp = MdmStorageStateTracker {
                successfulThreadBlocker.countDown()
                failingThreadBlocker.await()
            }
            val result = repository.save(updatedEntity.toProto(), eventContext.copy(stateTracker = artificialHangUp))
            // пока тред висел, параллельный процесс успел модифицировать строчку.
            results[0] = result.code
        }

        // when
        // В параллельном процессе обновим эту же строчку и сделаем это ПОСЛЕ того, как первый поток подгрузил existing
        // версии. Таким образом мы проверим, что первый поток проверит на конкарентность не только по версиям в коде,
        // но и внутренними механизмами транзакций Yt при сохранении.
        val successfulTask = executor.submit {
            successfulThreadBlocker.await()
            val result = repository.save(updatedEntity.toProto(), eventContext)
            failingThreadBlocker.countDown()
            results[1] = result.code
        }
        hungTask.get(1, TimeUnit.MINUTES)
        successfulTask.get(1, TimeUnit.MINUTES)

        // then
        results[0] shouldBe SaveMdmEntityResult.Code.CONCURRENT_UPDATE
        results[1] shouldBe SaveMdmEntityResult.Code.OK
    }

    private fun randomMdmEntityWithSpecificAttributeVersion(
        stored: ProtoEntity,
        addition: Long
    ): ProtoEntity {
        val entity = entity(stored.mdmId, stored.mdmUpdateMeta.from + addition)
        val mskuId = MdmAttributeValues(
            mskuIdAttributeId, listOf(
                MdmAttributeValue(
                    int64 = stored.mdmAttributeValuesMap[mskuIdAttributeId]!!
                        .valuesList[0].int64
                )
            ),
            MdmVersion(
                stored.mdmAttributeValuesMap[mskuIdAttributeId]!!.mdmUpdateMeta.from,
                null
            )
        )
        return MdmEntity(
            entity.mdmId,
            entity.mdmEntityTypeId,
            entity.values.plus(mskuIdAttributeId to mskuId),
            entity.version
        ).toProto()
    }
}
