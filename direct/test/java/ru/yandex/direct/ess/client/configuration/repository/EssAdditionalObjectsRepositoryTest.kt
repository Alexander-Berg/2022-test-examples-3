package ru.yandex.direct.ess.client.configuration.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbutil.QueryWithoutIndex
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.client.configuration.EssClientTestingConfiguration
import ru.yandex.direct.ess.client.repository.EssAdditionalObjectsRepository

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [EssClientTestingConfiguration::class])
internal class EssAdditionalObjectsRepositoryTest {
    @Autowired
    lateinit var essAdditionalObjectsRepository: EssAdditionalObjectsRepository

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    companion object {
        private const val SHARD = 1
    }

    @BeforeEach
    fun before() {
        dslContextProvider.ppc(SHARD)
            .truncateTable(Tables.ESS_ADDITIONAL_OBJECTS)
    }

    @Test
    fun addLogicObjectsForProcessorTest() {
        val objects = listOf(
            "{\"id\": 1}",
            "{\"id\": 3}"
        )
        essAdditionalObjectsRepository.addLogicObjectsForProcessor(SHARD, "test_processor", objects)
        val insertedObjects = getObjectsForProcessor("test_processor")
        assertThat(insertedObjects).hasSize(2)
        assertThat(insertedObjects).containsExactlyInAnyOrder(*objects.toTypedArray())
    }

    @Test
    fun clearLogicObjectsTest() {
        dslContextProvider.ppc(SHARD)
            .insertInto(Tables.ESS_ADDITIONAL_OBJECTS)
            .columns(Tables.ESS_ADDITIONAL_OBJECTS.LOGIC_PROCESS_NAME, Tables.ESS_ADDITIONAL_OBJECTS.LOGIC_OBJECT)
            .values("test_processor", "{\"id\":1}")
            .execute()
        essAdditionalObjectsRepository.clearLogicObjects(SHARD)
        val gotObjects = getObjectsForProcessor("test_processor")
        assertThat(gotObjects).isEmpty()
    }

    @Test
    fun clearLogicObjectsWithLimitTest() {
        dslContextProvider.ppc(SHARD)
            .insertInto(Tables.ESS_ADDITIONAL_OBJECTS)
            .columns(Tables.ESS_ADDITIONAL_OBJECTS.LOGIC_PROCESS_NAME, Tables.ESS_ADDITIONAL_OBJECTS.LOGIC_OBJECT)
            .values("test_processor", "{\"id\":1}")
            .values("test_processor", "{\"id\":2}")
            .values("test_processor", "{\"id\":3}")
            .execute()
        essAdditionalObjectsRepository.clearLogicObjects(SHARD, 2)
        val gotObjects = getObjectsForProcessor("test_processor")
        assertThat(gotObjects).hasSize(1)
    }

    @QueryWithoutIndex("Выборка записей для теста, в коде такой запрос не делается")
    private fun getObjectsForProcessor(logicProcessName: String): List<String> {
        return dslContextProvider.ppc(SHARD)
            .select(Tables.ESS_ADDITIONAL_OBJECTS.LOGIC_OBJECT)
            .from(Tables.ESS_ADDITIONAL_OBJECTS)
            .where(Tables.ESS_ADDITIONAL_OBJECTS.LOGIC_PROCESS_NAME.eq(logicProcessName))
            .fetch(Tables.ESS_ADDITIONAL_OBJECTS.LOGIC_OBJECT)
    }
}
