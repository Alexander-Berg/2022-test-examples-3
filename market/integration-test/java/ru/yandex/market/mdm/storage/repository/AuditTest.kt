package ru.yandex.market.mdm.storage.repository

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import ru.yandex.market.mdm.http.entity.SaveMdmEntityResult
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import ru.yandex.market.mdm.storage.model.unique.SnapshotKey
import ru.yandex.market.mdm.storage.repository.filter.MdmEntitySnapshotFilter
import ru.yandex.market.mdm.storage.service.logical.diff
import kotlin.random.Random

class AuditTest: MdmEntityIntegrationTestBase() {
    @Before
    fun before() {
        random = Random(1064321L)
        super.setUp()
    }

    @Test
    fun `when insert new entity should create new snapshot and event`() {
        // given
        val newEntity = entity(0L, 0L).toProto()

        // when
        val saveResult = repository.save(newEntity, eventContext)

        // then
        saveResult.code shouldBe SaveMdmEntityResult.Code.OK
        val snapshot = repository.findSnapshot(MdmEntitySnapshotFilter().mdmIdsIn(listOf(saveResult.mdmId))).first()
        wipeVersions(newEntity.toBuilder().setMdmId(saveResult.mdmId).build()) shouldBe wipeVersions(snapshot.mdmEntity)
    }

    @Test
    fun `when update entity should create new snapshot and event`() {
        val entity = entity(idGenerator.id, 0L).toProto()
        val saveResult = repository.save(entity, eventContext)
        saveResult.code shouldBe SaveMdmEntityResult.Code.OK

        // Проверим, что снепшот совпадает с сохраненным entity.
        val snapshot = repository.findSnapshot(MdmEntitySnapshotFilter().mdmIdsIn(listOf(entity.mdmId)))[0]
        wipeVersions(entity) shouldBe wipeVersions(snapshot.mdmEntity)

        // Обновим сущность.
        val updatedEntity = entity(entity.mdmId, saveResult.from).toProto()
        repository.save(updatedEntity, eventContext).code shouldBe SaveMdmEntityResult.Code.OK

        // Проверим, что после обновления появилось новое событие и снепшот.
        val snapshotAfterUpdating = repository.findSnapshot(MdmEntitySnapshotFilter().mdmIdsIn(listOf(entity.mdmId)))
            .first{ it.mdmEventId != snapshot.mdmEventId }
        wipeVersions(snapshotAfterUpdating.mdmEntity) shouldBe wipeVersions(diff(entity, updatedEntity)!!)
    }

    @Test
    fun `test find snapshot with different filters`() {
        val entity = entity(idGenerator.id, 0L).toProto()
        val saveResult = repository.save(entity, eventContext)
        saveResult.code shouldBe SaveMdmEntityResult.Code.OK

        // Проверим, что поиск по mdm id работает
        val snapshot = repository.findSnapshot(MdmEntitySnapshotFilter().mdmIdsIn(listOf(entity.mdmId)))[0]
        wipeVersions(entity) shouldBe wipeVersions(snapshot.mdmEntity)

        // Проверим, что поиск по полному первичному ключу работает
        val snapshotsByFullKey = repository.findSnapshot(
            listOf(SnapshotKey(snapshot.mdmEventId, snapshot.mdmEntity.mdmId)))
        snapshotsByFullKey shouldHaveSize 1
        wipeVersions(snapshotsByFullKey[0].mdmEntity) shouldBe wipeVersions(snapshot.mdmEntity)

        // Проверим, что поиск по event id работает
        val snapshotsByEventId = repository.findSnapshot(
            MdmEntitySnapshotFilter().eventIdsIn(listOf(snapshot.mdmEventId))
        )
        snapshotsByEventId shouldHaveSize 1
        wipeVersions(snapshotsByEventId[0].mdmEntity) shouldBe wipeVersions(snapshot.mdmEntity)
    }


    @Test
    fun `test consequent updates provide consistent snapshots`() {
        // given
        val mdmId = idGenerator.id
        val entity = entity(mdmId, 0L).toProto()

        // when
        val result0 = repository.save(entity, eventContext) // event_id #0
        val result1 = repository.save(entity(mdmId, result0.from).toProto(), eventContext) // event_id #1
        val result2 = repository.save(entity(mdmId, result1.from).toProto(), eventContext) // event_id #2

        // then
        val snapshot0 = repository.findSnapshot(listOf(SnapshotKey(result0.eventId, mdmId)))[0]
        val snapshot1 = repository.findSnapshot(listOf(SnapshotKey(result1.eventId, mdmId)))[0]
        val snapshot2 = repository.findSnapshot(listOf(SnapshotKey(result2.eventId, mdmId)))[0]

        snapshot0.mdmEntity.mdmUpdateMeta.from shouldBe result0.from
        snapshot1.mdmEntity.mdmUpdateMeta.from shouldBe result1.from
        snapshot2.mdmEntity.mdmUpdateMeta.from shouldBe result2.from
    }

    @Test
    fun `test no significant changes do not store anything`() {
        // given
        val mdmId = idGenerator.id
        val entity = entity(mdmId, 0L)

        // when
        val result0 = repository.save(entity.toProto(), eventContext)
        val notUpdatedEntity = entity.toBuilder().version(MdmVersion(result0.from, null)).build()
        val result1 = repository.save(notUpdatedEntity.toProto(), eventContext)

        // then
        result1.code shouldBe SaveMdmEntityResult.Code.NO_OP
        result1.from shouldBe result0.from
        repository.findSnapshot(MdmEntitySnapshotFilter().mdmIdsIn(listOf(mdmId))) shouldHaveSize 1
        repository.find(mdmId)!!.mdmUpdateMeta.from shouldBe result0.from
    }

    @Test
    fun `test insert update and no op in one batch should work`() {
        // given
        val insertEntity = entity(idGenerator.id, 0L)
        var updateEntity = entity(idGenerator.id, 0L)
        var noOpEntity = entity(idGenerator.id, 0L)

        val versionForUpdate = repository.save(updateEntity.toProto(), eventContext).from
        val versionForNoOp = repository.save(noOpEntity.toProto(), eventContext).from

        updateEntity = entity(updateEntity.mdmId, versionForUpdate)
        noOpEntity = noOpEntity.toBuilder().version(MdmVersion(versionForNoOp, null)).build()

        // when
        val results = repository.save(listOf(
            insertEntity.toProto(),
            updateEntity.toProto(),
            noOpEntity.toProto()
        ), eventContext)

        // then
        // порядок гарантирован
        val insertResult = results[0]
        val updateResult = results[1]
        val noOpResult = results[2]

        insertResult.code shouldBe SaveMdmEntityResult.Code.OK
        updateResult.code shouldBe SaveMdmEntityResult.Code.OK
        noOpResult.code shouldBe SaveMdmEntityResult.Code.NO_OP

        repository.findSnapshot(MdmEntitySnapshotFilter().mdmIdsIn(listOf(insertResult.mdmId))) shouldHaveSize 1
        repository.findSnapshot(MdmEntitySnapshotFilter().mdmIdsIn(listOf(updateResult.mdmId))) shouldHaveSize 2
        repository.findSnapshot(MdmEntitySnapshotFilter().mdmIdsIn(listOf(noOpResult.mdmId))) shouldHaveSize 1
    }

    @Test
    fun `test delete should save appropriate diff`() {
        // given
        val entity = entity(idGenerator.id, 0L)
        val saveResult = repository.save(entity.toProto(), eventContext)
        saveResult.code shouldBe SaveMdmEntityResult.Code.OK
        wipeVersions(repository.find(entity.mdmId)!!) shouldBe entity.toProto()

        // when
        // Установим версию to. На самом деле конкретное значение не важно, его пересетит репозиторий на таймштамп
        // транзакции сохранения, а тут этого достаточно, чтобы появился флажок удалённости в протобуфке.
        val deletedEntity = entity.toBuilder().version(MdmVersion(saveResult.from, saveResult.from + 1)).build()
        val deleteResult = repository.save(deletedEntity.toProto(), eventContext)
        deleteResult.code shouldBe SaveMdmEntityResult.Code.OK

        // then
        val stored = repository.find(entity.mdmId)
        stored shouldBe null

        val snapshots = repository.findSnapshot(MdmEntitySnapshotFilter().mdmIdsIn(listOf(entity.mdmId)))
        snapshots shouldHaveSize 2

        val deletionDiff = snapshots.firstOrNull{ it.mdmEntity.mdmUpdateMeta.deleted }
        deletionDiff shouldNotBe null
    }
}
