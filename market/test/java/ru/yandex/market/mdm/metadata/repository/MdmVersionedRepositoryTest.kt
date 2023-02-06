package ru.yandex.market.mdm.metadata.repository

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.mbo.lightmapper.reflective.LightMapper
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Embedded
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import ru.yandex.market.mdm.lib.model.mdm.MdmVersionedEntity
import ru.yandex.market.mdm.lib.service.PgSequenceMdmIdGenerator
import ru.yandex.market.mdm.lib.util.createLightMapper
import ru.yandex.market.mdm.metadata.filters.VersionedRepositorySearchFilter
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit

class MdmVersionedRepositoryTest : BaseAppTestClass() {

    @Autowired
    lateinit var mdmIdGenerator: PgSequenceMdmIdGenerator
    lateinit var itemRepository: ItemRepository

    @Before
    fun initTable() {
        jdbcTemplate.execute("drop schema if exists test cascade")
        jdbcTemplate.execute("create schema if not exists test")
        jdbcTemplate.execute(
            """
                create table if not exists $TABLE_NAME (
                  mdm_id                                bigint default nextval('metadata.mdm_seq'),
                  extra_field                           text                        not null,
                  version_from                          timestamp                   not null,
                  version_to                            timestamp                   default null,
                  audit_modified_uid                    text                        not null,
                  audit_event_id                        bigint                      not null
               )
            """
        )

        itemRepository = ItemRepository(
            namedJdbcTemplate,
            transactionTemplate,
            mdmIdGenerator
        )
    }

    @Test
    fun `insert versioned item`() {
        val startTime = Instant.now()

        val instance = Item(extraField = "some value")
        val inserted = itemRepository.insert(instance)

        assertThat(itemRepository.totalCount()).isEqualTo(1)
        assertThat(inserted.mdmId).isGreaterThan(0)

        val data = namedJdbcTemplate.queryForMap(
            "select * from $TABLE_NAME", emptyMap<String, Any>()
        )

        assertThat(data).contains(
            Assertions.entry("mdm_id", inserted.mdmId),
            Assertions.entry("extra_field", "some value"),
            Assertions.entry("version_to", null)
        )
        assertThat(data["version_from"] as Timestamp).isAfter(startTime.toString())
    }

    @Test
    fun `insert versioned items`() {
        val startTime = Instant.now().minusSeconds(1)

        val instance1 = Item(extraField = "some value1")
        val instance2 = Item(extraField = "some value2")
        val instance3 = Item(extraField = "some value3")
        val inserted = itemRepository.insertBatch(listOf(instance1, instance2, instance3))

        assertThat(itemRepository.totalCount()).isEqualTo(3)
        assertThat(inserted.map { it.mdmId }.minOrNull()).isGreaterThan(0)

        assertThat(itemRepository.findAll().map { it.extraField }).containsExactlyInAnyOrder(
            "some value1",
            "some value2",
            "some value3"
        )
        assertThat(itemRepository.findAll().map { it.version.to }).containsOnlyNulls()
        assertThat(itemRepository.findAll().map { it.version.from }).allMatch { it.isAfter(startTime) }
    }

    @Test
    fun `update versioned item`() {
        val instance = Item(extraField = "old value")
        val inserted = itemRepository.insert(instance)

        val newInstance = Item(
            mdmId = inserted.mdmId, extraField = "new value",
            version = MdmVersion(inserted.version.from)
        )
        val updated = itemRepository.insertOrUpdate(newInstance)

        val previousData = namedJdbcTemplate.queryForMap(
            "select * from $TABLE_NAME where mdm_id = :id and version_to is not null",
            mutableMapOf("id" to inserted.mdmId)
        )
        val currentData = namedJdbcTemplate.queryForMap(
            "select * from $TABLE_NAME where mdm_id = :id order by version_from desc limit 1",
            mutableMapOf("id" to inserted.mdmId)
        )

        assertThat(itemRepository.totalCount()).isEqualTo(2)
        assertThat(updated.version.from).isAfter(inserted.version.from)
        assertThat(updated.mdmId).isEqualTo(inserted.mdmId)

        assertThat(previousData["version_to"]).isNotNull
        assertThat(previousData["extra_field"]).isEqualTo("old value")

        assertThat(currentData["version_to"]).isNull()
        assertThat(currentData["extra_field"]).isEqualTo("new value")
    }

    @Test
    fun `update versioned items`() {
        val startTime = Instant.now().minusSeconds(1)

        val instance1 = Item(extraField = "old value1")
        val instance2 = Item(extraField = "old value2")
        val instance3 = Item(extraField = "old value3")
        val inserted = itemRepository.insertBatch(listOf(instance1, instance2, instance3))

        val toUpdate = inserted.map { it.copy(extraField = it.extraField.replace("old", "new")) }.toList()
        val updated = itemRepository.insertOrUpdateBatch(toUpdate)

        assertThat(itemRepository.totalCount()).isEqualTo(6)
        assertThat(updated.map { it.mdmId })
            .containsExactlyInAnyOrderElementsOf(inserted.map { it.mdmId })

        assertThat(itemRepository.findAllActive().map { it.extraField }).containsExactlyInAnyOrder(
            "new value1",
            "new value2",
            "new value3"
        )
        assertThat(itemRepository.findAllActive().map { it.version.to }).containsOnlyNulls()
        assertThat(itemRepository.findAllActive().map { it.version.from }).allMatch { it.isAfter(startTime) }
        assertThat(itemRepository.findAll().filter { it.version.to != null }).hasSize(3)
    }

    @Test
    fun `insert or update versioned items`() {
        val startTime = Instant.now().minusSeconds(1)

        val fresh = Item(extraField = "fresh value")
        val updated = itemRepository.insert(Item(extraField = "existing value")).copy(extraField = "updated value")

        val upserted = itemRepository.insertOrUpdateBatch(listOf(fresh, updated))
        assertThat(itemRepository.totalCount()).isEqualTo(3)
        assertThat(upserted.map { it.mdmId }.minOrNull()).isGreaterThan(0)

        assertThat(itemRepository.findAllActive().map { it.extraField }).containsExactlyInAnyOrder(
            "fresh value",
            "updated value"
        )
        assertThat(itemRepository.findAllActive().map { it.version.to }).containsOnlyNulls()
        assertThat(itemRepository.findAllActive().map { it.version.from }).allMatch { it.isAfter(startTime) }
        assertThat(itemRepository.findAll().filter { it.version.to != null }).hasSize(1)
    }

    @Test
    fun `retire latest version`() {
        val startTime = Instant.now()

        val instance = Item(extraField = "some value")
        val inserted = itemRepository.insert(instance)

        itemRepository.retireLatestById(inserted.mdmId)

        val data = namedJdbcTemplate.queryForMap(
            "select * from $TABLE_NAME", emptyMap<String, Any>()
        )

        assertThat(data).contains(
            Assertions.entry("mdm_id", inserted.mdmId),
            Assertions.entry("extra_field", "some value")
        )
        assertThat(data["version_to"] as Timestamp).isAfter(startTime.toString())
    }

    @Test
    fun `findLatest return empty list for empty ids`() {
        assertThat(itemRepository.findLatestByIds(listOf())).hasSize(0)
    }

    @Test
    fun `insert do nothing for empty list`() {
        itemRepository.insertBatch(listOf())
        assertThat(itemRepository.findAll()).hasSize(0)
    }

    @Test
    fun `find latest by id return null for not existed id`() {
        val instance = itemRepository.insert(Item(extraField = "some value"))

        val founded = itemRepository.findLatestById(instance.mdmId + 100)

        assertThat(founded).isNull()
    }

    @Test
    fun `find by versioned filter`() {
        val now = Instant.now()
        val from = now.minus(1, ChronoUnit.DAYS)
        val to = now.plus(1, ChronoUnit.DAYS)

        val instance1 = itemRepository.insert(Item(extraField = "some value1", version = MdmVersion(from = from)))

        val instance2 = itemRepository.insert(
            Item(
                extraField = "some value2",
                version = MdmVersion(from = from, to = to)
            )
        )

        val instance3 = itemRepository.insert(
            Item(
                extraField = "some value3",
                version = MdmVersion(from = now.plus(3, ChronoUnit.DAYS), to = now.plus(6, ChronoUnit.DAYS))
            )
        )

        val founded1 = itemRepository.findBySearchFilter(
            VersionedRepositorySearchFilter(moment = now)
        )
        assertThat(founded1).containsExactlyInAnyOrderElementsOf(listOf(instance1, instance2))

        val founded2 = itemRepository.findBySearchFilter(
            VersionedRepositorySearchFilter(ids = listOf(instance1.mdmId), moment = now)
        )
        assertThat(founded2).containsExactly(instance1)
    }

    @Test
    fun `find all active returned only active rows from table`() {
        val instance1 = Item(extraField = "some value 1")
        val instance2 = Item(extraField = "some value 2")
        val inserted1 = itemRepository.insert(instance1)
        val inserted2 = itemRepository.insert(instance2)

        val beforeUpdateList = itemRepository.findAllActive()
        assertThat(beforeUpdateList).hasSize(2)
        assertThat(beforeUpdateList).containsExactlyInAnyOrder(inserted1, inserted2)

        itemRepository.retireLatestById(inserted1.mdmId)
        val afterUpdateList = itemRepository.findAllActive()
        assertThat(afterUpdateList).hasSize(1)
        assertThat(afterUpdateList).containsExactlyInAnyOrder(inserted2)
    }

    @Test
    fun `insert or update batch should delete item`() {
        // given
        val itemCreate = Item(extraField = "item to delete")
        itemRepository.insertOrUpdateBatch(listOf(itemCreate))
        val itemToDelete = itemCreate.copy(version = MdmVersion(to = Instant.now()))

        // when
        itemRepository.insertOrUpdateBatch(listOf(itemToDelete))
        val afterUpdate = itemRepository.findAll()

        // then
        assertThat(afterUpdate).hasSize(2)
        assertThat(itemRepository.findAllActive()).hasSize(0)
    }

    class ItemRepository(
        jdbcTemplate: NamedParameterJdbcTemplate,
        transactionTemplate: TransactionTemplate,
        mdmIdGenerator: PgSequenceMdmIdGenerator
    ) : MdmVersionedRepository<Item, Long> (
        MAPPER, jdbcTemplate, transactionTemplate, TABLE_NAME, mdmIdGenerator
    )

    data class Item(
        override var mdmId: Long = 0,
        var extraField: String,
        @Embedded
        override val version: MdmVersion = MdmVersion(),
        ) : MdmVersionedEntity

    companion object {
        private val MAPPER: LightMapper<Item> = createLightMapper()
        private const val TABLE_NAME: String = "test.immutable_test"
    }
}
